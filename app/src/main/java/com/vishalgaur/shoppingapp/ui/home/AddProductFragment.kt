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
import com.vishalgaur.shoppingapp.ui.AddSupplierViewErrors

private const val TAG = "AddProductFragment"

class AddProductFragment : Fragment() {

	private lateinit var binding: FragmentAddProductBinding
	private val viewModel by viewModels<AddProductViewModel>()
	private val focusChangeListener = MyOnFocusChangeListener()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAddProductBinding.inflate(layoutInflater)

		initViewModel()

		setViews()

		setObservers()
		return binding.root
	}

	private fun initViewModel() {
		viewModel.getProductCategoriesForAddProduct()
	}

	private fun setObservers() {
		viewModel.addProductErrorStatus.observe(viewLifecycleOwner) { err ->
			modifyAddProductErrors(err)
		}
		viewModel.addProductStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				AddObjectStatus.DONE -> setLoaderState()
				AddObjectStatus.ERR_ADD -> {
					setLoaderState()
					binding.addProErrorTextView.visibility = View.VISIBLE
					binding.addProErrorTextView.text =
						getString(R.string.save_supplier_error_text)
					makeToast(getString(R.string.save_supplier_error_text))
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
	}

	private fun setViews() {
		Log.d(TAG, "set views")

		binding.addProAppBar.topAppBar.title = "Tambah Produk"

		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
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
				viewModel.addProductStatus.observe(viewLifecycleOwner) { status ->
					if (status == AddObjectStatus.DONE) {
						makeToast("Product Saved!")
						findNavController().navigate(R.id.action_addProductFragment_to_adminFragment)
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