package com.vishalgaur.shoppingapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.databinding.FragmentFavoritesBinding
import com.vishalgaur.shoppingapp.ui.RecyclerViewPaddingItemDecoration
import com.vishalgaur.shoppingapp.viewModels.HomeViewModel

private const val TAG = "FavoritesFragment"

class FavoritesFragment : Fragment() {
	private lateinit var binding: FragmentFavoritesBinding
	private val viewModel: HomeViewModel by activityViewModels()
	private lateinit var productsAdapter: LikedProductAdapter

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding = FragmentFavoritesBinding.inflate(layoutInflater)

		setViews()
		setObservers()
		return binding.root
	}

	private fun setViews() {
		viewModel.setDataLoading()
		viewModel.getLikedProducts()
		binding.favTopAppBar.topAppBar.title = "Barang Masuk"
		binding.favTopAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		// access the spinner
		var suppliers = viewModel.suppliers.value ?: emptyList()
		var supplierSpinnerAdapter = ArrayAdapter(requireContext(),android.R.layout.simple_spinner_item, suppliers)
		supplierSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		binding.supplierSpinner.adapter = supplierSpinnerAdapter

		if (context != null) {
			val proList = viewModel.likedProducts.value ?: emptyList()
			productsAdapter = LikedProductAdapter(proList, requireContext())
			productsAdapter.onClickListener = object : LikedProductAdapter.OnClickListener {
				override fun onClick(productData: Product) {
					Log.d(TAG, "Product: ${productData.productId} clicked")
					findNavController().navigate(
						R.id.action_favoritesFragment_to_productDetailsFragment,
						bundleOf("productId" to productData.productId)
					)
				}

				override fun onDeleteClick(productId: String) {
					viewModel.toggleLikeByProductId(productId)
				}
			}
			binding.favProductsRecyclerView.apply {
				val itemDecoration = RecyclerViewPaddingItemDecoration(requireContext())
				if (itemDecorationCount == 0) {
					addItemDecoration(itemDecoration)
				}
			}
		}
	}

	private fun setObservers() {
		viewModel.dataStatus.observe(viewLifecycleOwner) { status ->
			if (status == StoreDataStatus.LOADING) {
				binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
				binding.loaderLayout.circularLoader.showAnimationBehavior
			} else if (status != null) {
				viewModel.likedProducts.observe(viewLifecycleOwner) {
					if (it.isNotEmpty()) {
						productsAdapter.data = viewModel.likedProducts.value!!
						binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
						binding.loaderLayout.circularLoader.hideAnimationBehavior
						productsAdapter.data = it
						binding.favProductsRecyclerView.adapter = productsAdapter
						binding.favProductsRecyclerView.adapter?.apply {
							notifyDataSetChanged()
						}
					} else if (it.isEmpty()) {
						binding.favProductsRecyclerView.visibility = View.GONE
						binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
						binding.loaderLayout.circularLoader.hideAnimationBehavior
					}
				}
			}
		}
	}
}