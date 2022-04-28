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

	private var sizeList = mutableSetOf<Int>()
	private var colorsList = mutableSetOf<String>()
	private var imgList = mutableListOf<Uri>()

	private val getImages =
		registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { result ->
			imgList.addAll(result)
			if (imgList.size > 3) {
				imgList = imgList.subList(0, 3)
				makeToast("Maximum 3 images are allowed!")
			}
			val adapter = context?.let { AddProductImagesAdapter(it, imgList) }
			binding.addProImagesRv.adapter = adapter
		}

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

			imgList = inventory.images.map { it.toUri() } as MutableList<Uri>
			val adapter = AddProductImagesAdapter(requireContext(), imgList)
			binding.addProImagesRv.adapter = adapter

			setShoeSizesChips(inventory.availableSizes)
			setShoeColorsChips(inventory.availableColors)

			binding.addProBtn.setText(R.string.edit_product_btn_text)
		}

	}

	private fun setViews() {
		Log.d(TAG, "set views")

		if (!isEdit) {
			binding.addProAppBar.topAppBar.title =
				"Tambah Inventaris - ${viewModel.selectedCategory.value}"

			val adapter = AddProductImagesAdapter(requireContext(), imgList)
			binding.addProImagesRv.adapter = adapter
		}
		binding.addProImagesBtn.setOnClickListener {
			getImages.launch("image/*")
		}

		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE

		setShoeSizesChips()
		setShoeColorsChips()

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
			"onAddProduct: Add product initiated, $name, $price, $mrp, $desc, $sizeList, $colorsList, $imgList"
		)
		viewModel.submitProduct(
			name, price, mrp, desc, sizeList.toList(), colorsList.toList(), imgList
		)
	}

	private fun setShoeSizesChips(shoeList: List<Int>? = emptyList()) {
		binding.addProSizeChipGroup.apply {
			removeAllViews()
			for ((_, v) in ShoeSizes) {
				val chip = Chip(context)
				chip.id = v
				chip.tag = v

				chip.text = "$v"
				chip.isCheckable = true

				if (shoeList?.contains(v) == true) {
					chip.isChecked = true
					sizeList.add(chip.tag.toString().toInt())
				}

				chip.setOnCheckedChangeListener { buttonView, isChecked ->
					val tag = buttonView.tag.toString().toInt()
					if (!isChecked) {
						sizeList.remove(tag)
					} else {
						sizeList.add(tag)
					}
				}
				addView(chip)
			}
			invalidate()
		}
	}

	private fun setShoeColorsChips(colorList: List<String>? = emptyList()) {
		binding.addProColorChipGroup.apply {
			removeAllViews()
			var ind = 1
			for ((k, v) in ShoeColors) {
				val chip = Chip(context)
				chip.id = ind
				chip.tag = k

				chip.chipStrokeColor = ColorStateList.valueOf(Color.BLACK)
				chip.chipStrokeWidth = TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP,
					1F,
					context.resources.displayMetrics
				)
				chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(v))
				chip.isCheckable = true

				if (colorList?.contains(k) == true) {
					chip.isChecked = true
					colorsList.add(chip.tag.toString())
				}

				chip.setOnCheckedChangeListener { buttonView, isChecked ->
					val tag = buttonView.tag.toString()
					if (!isChecked) {
						colorsList.remove(tag)
					} else {
						colorsList.add(tag)
					}
				}
				addView(chip)
				ind++
			}
			invalidate()
		}
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

	private fun setSupplierCountrySelectTextField() {
		val isoCountriesMap = getISOCountriesMap()
		val countries = isoCountriesMap.values.toSortedSet().toList()
		val defaultCountry = Locale.getDefault().displayCountry
		val countryAdapter = ArrayAdapter(requireContext(), R.layout.country_list_item, countries)
		(binding.addressCountryEditText as? AutoCompleteTextView)?.let {
			it.setText(defaultCountry, false)
			it.setAdapter(countryAdapter)
		}
	}

	private fun setSupplierAddressViews() {
		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
		binding.addressFirstNameEditText.onFocusChangeListener = focusChangeListener
		binding.addressLastNameEditText.onFocusChangeListener = focusChangeListener
		binding.addressStreetAddEditText.onFocusChangeListener = focusChangeListener
		binding.addressStreetAdd2EditText.onFocusChangeListener = focusChangeListener
		binding.addressCityEditText.onFocusChangeListener = focusChangeListener
		binding.addressStateEditText.onFocusChangeListener = focusChangeListener
		binding.addressZipcodeEditText.onFocusChangeListener = focusChangeListener
		binding.addressPhoneEditText.onFocusChangeListener = focusChangeListener
		setSupplierCountrySelectTextField()

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
	}

	private fun onAddSupplierAddress() {
		val countryName = binding.addressCountryEditText.text.toString()
		val firstName = binding.addressFirstNameEditText.text.toString()
		val lastName = binding.addressLastNameEditText.text.toString()
		val streetAdd = binding.addressStreetAddEditText.text.toString()
		val streetAdd2 = binding.addressStreetAdd2EditText.text.toString()
		val city = binding.addressCityEditText.text.toString()
		val state = binding.addressStateEditText.text.toString()
		val zipCode = binding.addressZipcodeEditText.text.toString()
		val phoneNumber = binding.addressPhoneEditText.text.toString()

		val countryCode =
			getISOCountriesMap().keys.find { Locale("", it).displayCountry == countryName }

		Log.d(TAG, "onAddAddress: Add/Edit Address Initiated")
		addEditAddressViewModel.submitAddress(
			countryCode!!,
			firstName,
			lastName,
			streetAdd,
			streetAdd2,
			city,
			state,
			zipCode,
			phoneNumber
		)
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
		binding.fNameOutlinedTextField.error = null
		binding.lNameOutlinedTextField.error = null
		binding.streetAddOutlinedTextField.error = null
		binding.cityOutlinedTextField.error = null
		binding.stateOutlinedTextField.error = null
		binding.zipCodeOutlinedTextField.error = null
		binding.phoneOutlinedTextField.error = null
		errList.forEach { err ->
			when (err) {
				AddAddressViewErrors.EMPTY -> setAddSupplierEditTextsError(true)
				AddAddressViewErrors.ERR_FNAME_EMPTY ->
					setAddSupplierEditTextsError(true, binding.fNameOutlinedTextField)
				AddAddressViewErrors.ERR_LNAME_EMPTY ->
					setAddSupplierEditTextsError(true, binding.lNameOutlinedTextField)
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
			val countryName = getISOCountriesMap()[address.countryISOCode]
			binding.addressCountryEditText.setText(countryName, false)
			binding.addressFirstNameEditText.setText(address.fName)
			binding.addressLastNameEditText.setText(address.lName)
			binding.addressStreetAddEditText.setText(address.streetAddress)
			binding.addressStreetAdd2EditText.setText(address.streetAddress2)
			binding.addressCityEditText.setText(address.city)
			binding.addressStateEditText.setText(address.state)
			binding.addressZipcodeEditText.setText(address.zipCode)
			binding.addressPhoneEditText.setText(address.phoneNumber.substringAfter("+91"))
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