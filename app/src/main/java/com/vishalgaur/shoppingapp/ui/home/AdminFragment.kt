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
import com.beust.klaxon.Klaxon

private const val TAG = "AdminFragment"

class AdminFragment : Fragment() {

	private lateinit var binding: FragmentAdminBinding
	private val viewModel by viewModels<AdminViewModel>()
	private val focusChangeListener = MyOnFocusChangeListener()

	// arguments
	private var isEdit by Delegates.notNull<Boolean>()
	private lateinit var catName: String
	private lateinit var productId: String

	private lateinit var supplierArg: String

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAdminBinding.inflate(layoutInflater)

		isEdit = arguments?.getBoolean("isEdit") == true
		catName = arguments?.getString("categoryName").toString()
		productId = arguments?.getString("productId").toString()

		supplierArg = arguments?.getString("supplierArg").toString()

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

		viewModel.productCategoriesForAddProduct.observe(viewLifecycleOwner) {
			if(it.size > 0) {
				binding.addProCatEditText.setText(it[0],false)
			}
			binding.addProCatEditText.setAdapter(ArrayAdapter(requireContext(),android.R.layout.select_dialog_item,it))
		}

		setAddSupplierObservers()
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

		binding.addSupAddressBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminFragment_to_selectAddressFragment,
				bundleOf("supplierArg" to Klaxon().toJsonString(AdminToSelectAddressArg(binding.supplierNameEditText.text.toString())))
			)
		}

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

		if(supplierArg != "null") {
			val result = Klaxon().parse<SelectAddressToAdminArg>(supplierArg)
			if (result != null) {
				binding.supplierNameEditText.setText(result.supplierName)
			}
		}

		setAddProductCategoryViews()
	}

	private fun setAddProductCategoryViews() {
		binding.addCatBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminFragment_to_addProductCategoryFragment)
		}
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

	private fun setAddSupplierObservers() {}

	private fun setLoaderState(isVisible: Int = View.GONE) {
		binding.loaderLayout.loaderFrameLayout.visibility = isVisible
		if (isVisible == View.GONE) {
			binding.loaderLayout.circularLoader.hideAnimationBehavior
		} else {
			binding.loaderLayout.circularLoader.showAnimationBehavior
		}
	}
}