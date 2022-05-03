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
import com.vishalgaur.shoppingapp.databinding.FragmentAddProductCategoryBinding
import com.vishalgaur.shoppingapp.ui.AddProductCategoryViewErrors
import com.vishalgaur.shoppingapp.ui.MyOnFocusChangeListener
import com.vishalgaur.shoppingapp.viewModels.AddProductCategoryViewModel
import java.util.*
import kotlin.properties.Delegates

private const val TAG = "AddProductCategoryFragment"

class AddProductCategoryFragment : Fragment() {

	private lateinit var binding: FragmentAddProductCategoryBinding
	private val viewModel by viewModels<AddProductCategoryViewModel>()
	private val focusChangeListener = MyOnFocusChangeListener()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAddProductCategoryBinding.inflate(layoutInflater)

		setViews()

		setObservers()
		return binding.root
	}

	private fun setObservers() {
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
	}

	private fun setViews() {
		Log.d(TAG, "set views")

		binding.addProAppBar.topAppBar.title = "Tambah Kategori Produk"

		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigate(R.id.action_addProductCategoryFragment_to_adminFragment)
		}

		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE

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

	private fun makeToast(text: String) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}

	private fun onAddProductCategory() {
		val productCategory = binding.catNameEditText.text.toString()

		Log.d(TAG, "onAddProductCategory: Add Product Category Initiated")
		viewModel.submitProductCategory(productCategory)
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
