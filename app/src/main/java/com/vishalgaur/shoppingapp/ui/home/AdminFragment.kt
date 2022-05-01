package com.vishalgaur.shoppingapp.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.utils.*
import com.vishalgaur.shoppingapp.databinding.FragmentAdminBinding
import com.vishalgaur.shoppingapp.ui.AddAddressViewErrors
import com.vishalgaur.shoppingapp.ui.AddProductCategoryViewErrors
import com.vishalgaur.shoppingapp.ui.AddProductViewErrors
import com.vishalgaur.shoppingapp.ui.MyOnFocusChangeListener
import com.vishalgaur.shoppingapp.viewModels.AddEditAddressViewModel
import com.vishalgaur.shoppingapp.viewModels.AdminViewModel
import java.util.*
import kotlin.properties.Delegates

private const val TAG = "AdminFragment"

class AdminFragment : Fragment() {

	private lateinit var binding: FragmentAdminBinding
	private val viewModel by viewModels<AdminViewModel>()
	private val addEditAddressViewModel by viewModels<AddEditAddressViewModel>()
	private val focusChangeListener = MyOnFocusChangeListener()

	// arguments
	private var isEdit by Delegates.notNull<Boolean>()
	private lateinit var catName: String
	private lateinit var productId: String

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAdminBinding.inflate(layoutInflater)

		isEdit = arguments?.getBoolean("isEdit") == true
		catName = arguments?.getString("categoryName").toString()
		productId = arguments?.getString("productId").toString()

		initViewModel()

		setViews()

		setObservers()
		return binding.root
	}

	private fun initViewModel() {
		Log.d(TAG, "init view model, isedit = $isEdit")

		viewModel.setIsEdit(isEdit)
		if (isEdit) {
			Log.d(TAG, "init view model, isedit = true, $productId")
			viewModel.setInventoryData(productId)
		} else {
			Log.d(TAG, "init view model, isedit = false, $catName")
			viewModel.setCategory(catName)
		}

		addEditAddressViewModel.setIsEdit(false)

		viewModel.getProductCategoriesForAddProduct()
	}

	private fun setObservers() {
		viewModel.addProductErrorStatus.observe(viewLifecycleOwner) { err ->
			modifyAddProductErrors(err)
		}
		viewModel.dataStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				StoreDataStatus.LOADING -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
					binding.loaderLayout.circularLoader.showAnimationBehavior
				}
				StoreDataStatus.DONE -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
					binding.loaderLayout.circularLoader.hideAnimationBehavior
					fillDataInAllViews()
				}
				else -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
					binding.loaderLayout.circularLoader.hideAnimationBehavior
					makeToast("Error getting Data, Try Again!")
				}
			}
		}
		viewModel.addInventoryErrors.observe(viewLifecycleOwner) { status ->
			when (status) {
				AddInventoryErrors.ADDING -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
					binding.loaderLayout.circularLoader.showAnimationBehavior
				}
				AddInventoryErrors.ERR_ADD_IMG -> {
					setAddInventoryErrors(getString(R.string.add_product_error_img_upload))
				}
				AddInventoryErrors.ERR_ADD -> {
					setAddInventoryErrors(getString(R.string.add_product_insert_error))
				}
				AddInventoryErrors.NONE -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
					binding.loaderLayout.circularLoader.hideAnimationBehavior
				}
			}
		}

		viewModel.addProductCategoryErrorStatus.observe(viewLifecycleOwner) { err ->
			if (err == AddProductCategoryViewErrors.EMPTY) {
				binding.addCatErrorTextView.visibility = View.VISIBLE
			} else {
				binding.addCatErrorTextView.visibility = View.GONE
			}
		}

		viewModel.addProductCategoryStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				AddObjectStatus.DONE -> setLoaderState()
				AddObjectStatus.ERR_ADD -> {
					setLoaderState()
					binding.addCatErrorTextView.visibility = View.VISIBLE
					binding.addCatErrorTextView.text =
						getString(R.string.save_category_error_text)
					makeToast(getString(R.string.save_category_error_text))
				}
				AddObjectStatus.ADDING -> {
					setLoaderState(View.VISIBLE)
				}
				else -> setLoaderState()
			}
		}

		viewModel.productCategoriesForAddProduct.observe(viewLifecycleOwner) {
			if(it.size > 0) {
				binding.addProCatEditText.setText(it[0],false)
			}
			binding.addProCatEditText.setAdapter(ArrayAdapter(requireContext(),android.R.layout.select_dialog_item,it))
		}

		setSupplierObservers()
	}

	private fun setAddInventoryErrors(errText: String) {
		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
		binding.loaderLayout.circularLoader.hideAnimationBehavior
		binding.addProErrorTextView.visibility = View.VISIBLE
		binding.addProErrorTextView.text = errText

	}

	private fun fillDataInAllViews() {
		viewModel.inventoryData.value?.let { inventory ->
			Log.d(TAG, "fill data in views")
			binding.addProAppBar.topAppBar.title = "Edit Product - ${inventory.name}"
			binding.proNameEditText.setText(inventory.name)
			//binding.proPriceEditText.setText(inventory.price.toString())
			binding.proMrpEditText.setText(inventory.mrp.toString())
			binding.proDescEditText.setText(inventory.description)

			binding.addProBtn.setText(R.string.edit_product_btn_text)
		}

	}

	private fun setViews() {
		Log.d(TAG, "set views")

		binding.addProAppBar.topAppBar.title = "Tambah Produk"

		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE

		binding.addProErrorTextView.visibility = View.GONE
		binding.proNameEditText.onFocusChangeListener = focusChangeListener
		//binding.proPriceEditText.onFocusChangeListener = focusChangeListener
		binding.proMrpEditText.onFocusChangeListener = focusChangeListener
		binding.proDescEditText.onFocusChangeListener = focusChangeListener

		binding.addProBtn.setOnClickListener {
			onAddProduct()
			if (viewModel.addProductErrorStatus.value == AddProductViewErrors.NONE) {
				viewModel.addInventoryErrors.observe(viewLifecycleOwner) { err ->
					if (err == AddInventoryErrors.NONE) {
						findNavController().navigate(R.id.action_addInventoryFragment_to_homeFragment)
					}
				}
			}
		}

		setSupplierAddressViews()
	}

	private fun onAddProduct() {
		val name = binding.proNameEditText.text.toString()
		val price = 345.0 // TODO: Remove
		val mrp = binding.proMrpEditText.text.toString().toDoubleOrNull()
		val desc = binding.proDescEditText.text.toString()
		Log.d(
			TAG,
			"onAddProduct: Add product initiated, $name, $price, $mrp, $desc"
		)
		viewModel.submitProduct(
			name, price, mrp, desc, listOf(), listOf(), listOf()
		)
	}

	private fun modifyAddProductErrors(err: AddProductViewErrors) {
		when (err) {
			AddProductViewErrors.NONE -> binding.addProErrorTextView.visibility = View.GONE
			AddProductViewErrors.EMPTY -> {
				binding.addProErrorTextView.visibility = View.VISIBLE
				binding.addProErrorTextView.text = getString(R.string.add_product_error_string)
			}
			AddProductViewErrors.ERR_PRICE_0 -> {
				binding.addProErrorTextView.visibility = View.VISIBLE
				binding.addProErrorTextView.text = getString(R.string.add_pro_error_price_string)
			}
		}
	}

	private fun makeToast(text: String) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}

	private fun setSupplierStateSelectTextField() {
		val states = getProvinces()
		val defaultState = getDefaultProvince()
		val stateAdapter = ArrayAdapter(requireContext(), R.layout.country_list_item, states)
		(binding.addressStateEditText as? AutoCompleteTextView)?.let {
			it.setText(defaultState, false)
			it.setAdapter(stateAdapter)
		}
	}

	private fun setSupplierAddressViews() {
		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
		binding.supplierNameEditText.onFocusChangeListener = focusChangeListener
		binding.supplierCpEditText.onFocusChangeListener = focusChangeListener
		binding.addressStreetAddEditText.onFocusChangeListener = focusChangeListener
		binding.addressStreetAdd2EditText.onFocusChangeListener = focusChangeListener
		binding.addressCityEditText.onFocusChangeListener = focusChangeListener
		binding.addressStateEditText.onFocusChangeListener = focusChangeListener
		binding.addressZipcodeEditText.onFocusChangeListener = focusChangeListener
		binding.addressPhoneEditText.onFocusChangeListener = focusChangeListener
		setSupplierStateSelectTextField()

		binding.addSupSaveBtn.setOnClickListener {
			onAddSupplierAddress()
			if (addEditAddressViewModel.errorStatus.value?.isEmpty() == true) {
				addEditAddressViewModel.addAddressStatus.observe(viewLifecycleOwner) { status ->
					if (status == AddObjectStatus.DONE) {
						makeToast("Address Saved!")
						findNavController().navigateUp()
					}
				}
			}
		}

		binding.addCatBtn.setOnClickListener {
			onAddProductCategory()
			if (viewModel.addProductCategoryErrorStatus.value == AddProductCategoryViewErrors.NONE) {
				viewModel.addProductCategoryStatus.observe(viewLifecycleOwner) { status ->
					if (status == AddObjectStatus.DONE) {
						makeToast("Category Saved!")
						findNavController().navigateUp()
					}
				}
			}
		}
	}

	private fun onAddSupplierAddress() {
		val supplierName = binding.supplierNameEditText.text.toString()
		val lastName = binding.supplierCpEditText.text.toString()
		val streetAdd = binding.addressStreetAddEditText.text.toString()
		val streetAdd2 = binding.addressStreetAdd2EditText.text.toString()
		val city = binding.addressCityEditText.text.toString()
		val state = binding.addressStateEditText.text.toString()
		val zipCode = binding.addressZipcodeEditText.text.toString()
		val phoneNumber = binding.addressPhoneEditText.text.toString()

		Log.d(TAG, "onAddAddress: Add/Edit Address Initiated")
		addEditAddressViewModel.submitAddress(
			supplierName,
			lastName,
			streetAdd,
			streetAdd2,
			city,
			state,
			zipCode,
			phoneNumber
		)
	}

	private fun onAddProductCategory() {
		val productCategory = binding.catNameEditText.text.toString()

		Log.d(TAG, "onAddProductCategory: Add Product Category Initiated")
		viewModel.submitProductCategory(productCategory)
	}

	private fun setSupplierObservers() {
		addEditAddressViewModel.errorStatus.observe(viewLifecycleOwner) { errList ->
			if (errList.isEmpty()) {
				binding.addSupErrorTextView.visibility = View.GONE
			} else {
				modifyAddSupplierErrors(errList)
			}
		}

		addEditAddressViewModel.dataStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				StoreDataStatus.LOADING -> setLoaderState(View.VISIBLE)
				StoreDataStatus.ERROR -> {
					setLoaderState()
					makeToast("Error getting Data, Try Again!")
				}
				StoreDataStatus.DONE -> {
					fillSupplierAddressDataInViews()
					setLoaderState()
				}
				else -> {
					setLoaderState()
				}
			}
		}

		addEditAddressViewModel.addAddressStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				AddObjectStatus.DONE -> setLoaderState()
				AddObjectStatus.ERR_ADD -> {
					setLoaderState()
					binding.addSupErrorTextView.visibility = View.VISIBLE
					binding.addSupErrorTextView.text =
						getString(R.string.save_address_error_text)
					makeToast(getString(R.string.save_address_error_text))
				}
				AddObjectStatus.ADDING -> {
					setLoaderState(View.VISIBLE)
				}
				else -> setLoaderState()
			}
		}
	}

	private fun modifyAddSupplierErrors(errList: List<AddAddressViewErrors>) {
		binding.supplierNameOutlinedTextField.error = null
		binding.cpOutlinedTextField.error = null
		binding.streetAddOutlinedTextField.error = null
		binding.cityOutlinedTextField.error = null
		binding.stateOutlinedTextField.error = null
		binding.zipCodeOutlinedTextField.error = null
		binding.phoneOutlinedTextField.error = null
		errList.forEach { err ->
			when (err) {
				AddAddressViewErrors.EMPTY -> setAddSupplierEditTextsError(true)
				AddAddressViewErrors.ERR_FNAME_EMPTY ->
					setAddSupplierEditTextsError(true, binding.supplierNameOutlinedTextField)
				AddAddressViewErrors.ERR_LNAME_EMPTY ->
					setAddSupplierEditTextsError(true, binding.cpOutlinedTextField)
				AddAddressViewErrors.ERR_STR1_EMPTY ->
					setAddSupplierEditTextsError(true, binding.streetAddOutlinedTextField)
				AddAddressViewErrors.ERR_CITY_EMPTY ->
					setAddSupplierEditTextsError(true, binding.cityOutlinedTextField)
				AddAddressViewErrors.ERR_STATE_EMPTY ->
					setAddSupplierEditTextsError(true, binding.stateOutlinedTextField)
				AddAddressViewErrors.ERR_ZIP_EMPTY ->
					setAddSupplierEditTextsError(true, binding.zipCodeOutlinedTextField)
				AddAddressViewErrors.ERR_ZIP_INVALID ->
					setAddSupplierEditTextsError(false, binding.zipCodeOutlinedTextField)
				AddAddressViewErrors.ERR_PHONE_INVALID ->
					setAddSupplierEditTextsError(false, binding.phoneOutlinedTextField)
				AddAddressViewErrors.ERR_PHONE_EMPTY ->
					setAddSupplierEditTextsError(true, binding.phoneOutlinedTextField)
			}
		}
	}

	private fun setAddSupplierEditTextsError(isEmpty: Boolean, editText: TextInputLayout? = null) {
		if (isEmpty) {
			binding.addSupErrorTextView.visibility = View.VISIBLE
			if (editText != null) {
				editText.error = "Please Fill the Form"
				editText.errorIconDrawable = null
			}
		} else {
			binding.addSupErrorTextView.visibility = View.GONE
			editText!!.error = "Invalid!"
			editText.errorIconDrawable = null
		}
	}

	private fun fillSupplierAddressDataInViews() {
		addEditAddressViewModel.addressData.value?.let { address ->
			binding.supplierNameEditText.setText(address.fName)
			binding.supplierCpEditText.setText(address.lName)
			binding.addressStreetAddEditText.setText(address.streetAddress)
			binding.addressStreetAdd2EditText.setText(address.streetAddress2)
			binding.addressCityEditText.setText(address.city)
			binding.addressStateEditText.setText(address.state)
			binding.addressZipcodeEditText.setText(address.zipCode)
			binding.addressPhoneEditText.setText(address.phoneNumber.substringAfter("+62"))
			binding.addSupSaveBtn.setText(R.string.save_address_btn_text)
		}
	}

	private fun setLoaderState(isVisible: Int = View.GONE) {
		binding.loaderLayout.loaderFrameLayout.visibility = isVisible
		if (isVisible == View.GONE) {
			binding.loaderLayout.circularLoader.hideAnimationBehavior
		} else {
			binding.loaderLayout.circularLoader.showAnimationBehavior
		}
	}
}