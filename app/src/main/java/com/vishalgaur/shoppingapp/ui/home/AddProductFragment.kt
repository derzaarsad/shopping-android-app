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
import com.vishalgaur.shoppingapp.databinding.FragmentAddProductBinding
import com.vishalgaur.shoppingapp.ui.AddProductViewErrors
import com.vishalgaur.shoppingapp.ui.MyOnFocusChangeListener
import com.vishalgaur.shoppingapp.viewModels.AddProductViewModel
import java.util.*
import kotlin.properties.Delegates
import com.beust.klaxon.Klaxon

private const val TAG = "AddProductFragment"

class AddProductFragment : Fragment() {

	private lateinit var binding: FragmentAddProductBinding
	private val viewModel by viewModels<AddProductViewModel>()
	private val focusChangeListener = MyOnFocusChangeListener()

	// arguments
	private lateinit var catName: String
	private lateinit var productId: String

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAddProductBinding.inflate(layoutInflater)

		catName = arguments?.getString("categoryName").toString()
		productId = arguments?.getString("productId").toString()

		initViewModel()

		setViews()

		setObservers()
		return binding.root
	}

	private fun initViewModel() {
		Log.d(TAG, "init view model, $catName")
		viewModel.setCategory(catName)

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
			//binding.addProAppBar.topAppBar.title = "Edit Product - ${inventory.name}"
			binding.proNameEditText.setText(inventory.name)
			binding.proUpcEditText.setText(inventory.mrp.toString())
			binding.proDescEditText.setText(inventory.description)

			binding.addProBtn.setText(R.string.edit_product_btn_text)
		}

	}

	private fun setViews() {
		Log.d(TAG, "set views")

		binding.addProAppBar.topAppBar.title = "Tambah Produk"

		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigate(R.id.action_addProductFragment_to_adminFragment)
		}

		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE

		binding.addProErrorTextView.visibility = View.GONE
		binding.addProCatEditText.onFocusChangeListener = focusChangeListener
		binding.addProUnitEditText.onFocusChangeListener = focusChangeListener
		binding.proNameEditText.onFocusChangeListener = focusChangeListener
		binding.proUpcEditText.onFocusChangeListener = focusChangeListener
		binding.proSkuEditText.onFocusChangeListener = focusChangeListener
		binding.proDescEditText.onFocusChangeListener = focusChangeListener
		setUnitTextField()

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
	}

	private fun onAddProduct() {
		val name = binding.proNameEditText.text.toString()
		val price = 345.0 // TODO: Remove
		val mrp = binding.proUpcEditText.text.toString().toDoubleOrNull()
		val desc = binding.proDescEditText.text.toString()
		Log.d(
			TAG,
			"onAddProduct: Add product initiated, $name, $price, $mrp, $desc"
		)
		viewModel.submitProduct(
			name, price, mrp, desc, listOf(), listOf(), listOf()
		)
	}

	private fun setUnitTextField() {
		val units = getProductUnits()
		val defaultUnit = getDefaultUnit()
		val stateAdapter = ArrayAdapter(requireContext(), R.layout.country_list_item, units)
		(binding.addProUnitEditText as? AutoCompleteTextView)?.let {
			it.setText(defaultUnit, false)
			it.setAdapter(stateAdapter)
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

	private fun setLoaderState(isVisible: Int = View.GONE) {
		binding.loaderLayout.loaderFrameLayout.visibility = isVisible
		if (isVisible == View.GONE) {
			binding.loaderLayout.circularLoader.hideAnimationBehavior
		} else {
			binding.loaderLayout.circularLoader.showAnimationBehavior
		}
	}
}