package com.vishalgaur.shoppingapp.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.PagerSnapHelper
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.utils.AddObjectStatus
import com.vishalgaur.shoppingapp.data.utils.ShoeColors
import com.vishalgaur.shoppingapp.data.utils.ShoeSizes
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.databinding.FragmentInventoryDetailsBinding
import com.vishalgaur.shoppingapp.ui.AddItemErrors
import com.vishalgaur.shoppingapp.ui.DotsIndicatorDecoration
import com.vishalgaur.shoppingapp.viewModels.InventoryViewModel

class InventoryDetailsFragment : Fragment() {

	inner class InventoryViewModelFactory(
		private val inventoryId: String,
		private val application: Application
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel?> create(modelClass: Class<T>): T {
			if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
				return InventoryViewModel(inventoryId, application) as T
			}
			throw IllegalArgumentException("Unknown ViewModel Class")
		}
	}

	private lateinit var binding: FragmentInventoryDetailsBinding
	private lateinit var viewModel: InventoryViewModel

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding = FragmentInventoryDetailsBinding.inflate(layoutInflater)
		val inventoryId = arguments?.getString("inventoryId")

		if (activity != null && inventoryId != null) {
			val viewModelFactory = InventoryViewModelFactory(inventoryId, requireActivity().application)
			viewModel = ViewModelProvider(this, viewModelFactory).get(InventoryViewModel::class.java)
		}

		binding.proDetailsAddCartBtn.visibility = View.VISIBLE
		binding.proDetailsAddCartBtn.setOnClickListener {
			if (viewModel.isItemInCart.value == true) {
				navigateToCartFragment()
			} else {
				onAddToCart()
				if (viewModel.errorStatus.value?.isEmpty() == true) {
					viewModel.addItemStatus.observe(viewLifecycleOwner) { status ->
						if (status == AddObjectStatus.DONE) {
							makeToast("Product Added To Cart")
							viewModel.checkIfInCart()
						}
					}
				}
			}
		}

		binding.loaderLayout.loaderFrameLayout.background =
			ResourcesCompat.getDrawable(resources, R.color.white, null)

		binding.layoutViewsGroup.visibility = View.GONE
		binding.proDetailsAddCartBtn.visibility = View.GONE
		setObservers()
		return binding.root
	}

	override fun onResume() {
		super.onResume()
		viewModel.checkIfInCart()
	}

	private fun setObservers() {
		viewModel.dataStatus.observe(viewLifecycleOwner) {
			when (it) {
				StoreDataStatus.DONE -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
					binding.proDetailsLayout.visibility = View.VISIBLE
					setViews()
				}
				else -> {
					binding.proDetailsLayout.visibility = View.GONE
					binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
				}
			}
		}
		viewModel.isItemInCart.observe(viewLifecycleOwner) {
			if (it == true) {
				binding.proDetailsAddCartBtn.text =
					getString(R.string.pro_details_go_to_cart_btn_text)
				binding.quantityOutlinedTextField.isEnabled = false
			} else {
				binding.proDetailsAddCartBtn.text =
					getString(R.string.pro_details_add_to_cart_btn_text)
				binding.quantityOutlinedTextField.isEnabled = true
			}
		}
		viewModel.errorStatus.observe(viewLifecycleOwner) {
			if (it.isNotEmpty())
				modifyErrors(it)
		}
	}

	@SuppressLint("ResourceAsColor")
	private fun modifyErrors(errList: List<AddItemErrors>) {
		if (!errList.isNullOrEmpty()) {
			errList.forEach { err ->
				when (err) {
					AddItemErrors.ERROR_QUANTITY -> {
						binding.addInvQuantityLabel.setTextColor(R.color.red_600)
						makeToast("Quantity cannot be more than " + viewModel.inventoryData.value?.quantity + ".")
					}
				}
			}
		}
	}

	private fun setViews() {
		binding.layoutViewsGroup.visibility = View.VISIBLE
		binding.proDetailsAddCartBtn.visibility = View.VISIBLE
		binding.addProAppBar.topAppBar.title = viewModel.inventoryData.value?.sku
		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}
		binding.addProAppBar.topAppBar.inflateMenu(R.menu.app_bar_menu)
		binding.addProAppBar.topAppBar.overflowIcon?.setTint(
			ContextCompat.getColor(
				requireContext(),
				R.color.gray
			)
		)

		setImagesView()

		binding.proDetailsTitleTv.text = viewModel.inventoryData.value?.sku ?: ""
		binding.proDetailsPriceTv.text = resources.getString(
			R.string.pro_details_price_value,
			viewModel.inventoryData.value?.purchasePrice.toString()
		)
		binding.invQuantityEditText.setText(viewModel.inventoryData.value?.quantity.toString())
		binding.proDetailsSpecificsText.text = viewModel.inventoryData.value?.description ?: ""
		binding.proDetailsLikeBtn.visibility = View.GONE
	}

	private fun onAddToCart() {
		viewModel.addToCart(binding.invQuantityEditText.text.toString().toDouble())
	}

	private fun navigateToCartFragment() {
		findNavController().navigate(R.id.action_inventoryDetailsFragment_to_cartFragment)
	}

	private fun makeToast(text: String) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}

	private fun setImagesView() {
		if (context != null) {
			binding.proDetailsImagesRecyclerview.isNestedScrollingEnabled = false
			val adapter = ProductImagesAdapter(
				requireContext(),
				viewModel.inventoryData.value?.images ?: emptyList()
			)
			binding.proDetailsImagesRecyclerview.adapter = adapter
			val rad = resources.getDimension(R.dimen.radius)
			val dotsHeight = resources.getDimensionPixelSize(R.dimen.dots_height)
			val inactiveColor = ContextCompat.getColor(requireContext(), R.color.gray)
			val activeColor = ContextCompat.getColor(requireContext(), R.color.blue_accent_300)
			val itemDecoration =
				DotsIndicatorDecoration(rad, rad * 4, dotsHeight, inactiveColor, activeColor)
			binding.proDetailsImagesRecyclerview.addItemDecoration(itemDecoration)
			PagerSnapHelper().attachToRecyclerView(binding.proDetailsImagesRecyclerview)
		}
	}
}