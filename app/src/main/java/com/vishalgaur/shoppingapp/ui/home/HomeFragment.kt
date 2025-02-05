package com.vishalgaur.shoppingapp.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.databinding.FragmentHomeBinding
import com.vishalgaur.shoppingapp.ui.MyOnFocusChangeListener
import com.vishalgaur.shoppingapp.ui.RecyclerViewPaddingItemDecoration
import com.vishalgaur.shoppingapp.viewModels.HomeViewModel
import kotlinx.coroutines.*


private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {

	private lateinit var binding: FragmentHomeBinding
	private val viewModel: HomeViewModel by activityViewModels()
	private val focusChangeListener = MyOnFocusChangeListener()
	private lateinit var inventoryAdapter: InventoryAdapter

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentHomeBinding.inflate(layoutInflater)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setViews()
		setObservers()
	}

//	override fun onResume() {
//		super.onResume()
//		viewModel.getLikedProducts()
//	}

	private fun setViews() {
		setHomeTopAppBar()
		if (context != null) {
			setProductsAdapter(viewModel.inventories.value)
			binding.productsRecyclerView.apply {
				val gridLayoutManager = GridLayoutManager(context, 2)
				gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
					override fun getSpanSize(position: Int): Int {
						return when (inventoryAdapter.getItemViewType(position)) {
							2 -> 2 //ad
							else -> {
								val proCount = inventoryAdapter.data.count { it is Inventory }
								val adCount = inventoryAdapter.data.size - proCount
								val totalCount = proCount + (adCount * 2)
								// product, full for last item
								if (position + 1 == inventoryAdapter.data.size && totalCount % 2 == 1) 2 else 1
							}
						}
					}
				}
				layoutManager = gridLayoutManager
				adapter = inventoryAdapter
				val itemDecoration = RecyclerViewPaddingItemDecoration(requireContext())
				if (itemDecorationCount == 0) {
					addItemDecoration(itemDecoration)
				}
			}
		}

		if (!viewModel.isUserAdmin) {
			binding.homeFabAddProduct.visibility = View.GONE
		} else {
			viewModel.getProductCategories()
			binding.homeFabAddProduct.setOnClickListener {
				viewModel.getProductCategories()
				var productCategories = viewModel.productCategories.value ?: emptyList()
				showDialogWithItems(productCategories.toTypedArray(), 0, false)
			}
		}
		binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
		binding.loaderLayout.circularLoader.showAnimationBehavior
	}

	private fun setObservers() {
		viewModel.storeDataStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				StoreDataStatus.LOADING -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
					binding.loaderLayout.circularLoader.showAnimationBehavior
					binding.productsRecyclerView.visibility = View.GONE
				}
				else -> {
					binding.loaderLayout.circularLoader.hideAnimationBehavior
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
				}
			}
			if (status != null && status != StoreDataStatus.LOADING) {
				viewModel.inventories.observe(viewLifecycleOwner) { productsList ->
					if (productsList.isNotEmpty()) {
						binding.loaderLayout.circularLoader.hideAnimationBehavior
						binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
						binding.productsRecyclerView.visibility = View.VISIBLE
						binding.productsRecyclerView.adapter?.apply {
							inventoryAdapter.data =
								getMixedDataList(productsList, getAdsList())
							notifyDataSetChanged()
						}
					}
				}
			}
		}
		viewModel.allInventories.observe(viewLifecycleOwner) {
			if (it.isNotEmpty()) {
				viewModel.setDataLoaded()
				viewModel.filterInventories("All")
			}
		}
	}

	private fun performSearch(query: String) {
		viewModel.filterBySearch(query)
	}

	private fun setAppBarItemClicks(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.home_filter -> {
				viewModel.getProductCategories()
				var productCategories = viewModel.productCategories.value ?: emptyList()
				val extraFilters = arrayOf("All", "None")
				val categoryList = productCategories.plus(extraFilters)
				val checkedItem = categoryList.indexOf(viewModel.filterCategory.value)
				showDialogWithItems(categoryList.toTypedArray(), checkedItem, true)
				true
			}
			else -> false
		}
	}

	private fun setHomeTopAppBar() {
		var lastInput = ""
		val debounceJob: Job? = null
		val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
		binding.homeTopAppBar.topAppBar.inflateMenu(R.menu.home_app_bar_menu)
		binding.homeTopAppBar.homeSearchEditText.onFocusChangeListener = focusChangeListener
		binding.homeTopAppBar.homeSearchEditText.doAfterTextChanged { editable ->
			if (editable != null) {
				val newtInput = editable.toString()
				debounceJob?.cancel()
				if (lastInput != newtInput) {
					lastInput = newtInput
					uiScope.launch {
						delay(500)
						if (lastInput == newtInput) {
							performSearch(newtInput)
						}
					}
				}
			}
		}
		binding.homeTopAppBar.homeSearchEditText.setOnEditorActionListener { textView, actionId, _ ->
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				textView.clearFocus()
				val inputManager =
					requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
				inputManager.hideSoftInputFromWindow(textView.windowToken, 0)
				performSearch(textView.text.toString())
				true
			} else {
				false
			}
		}
		binding.homeTopAppBar.searchOutlinedTextLayout.setEndIconOnClickListener {
			it.clearFocus()
			binding.homeTopAppBar.homeSearchEditText.setText("")
			val inputManager =
				requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
			inputManager.hideSoftInputFromWindow(it.windowToken, 0)
//			viewModel.filterProducts("All")
		}
		binding.homeTopAppBar.topAppBar.setOnMenuItemClickListener { menuItem ->
			setAppBarItemClicks(menuItem)
		}
	}

	private fun setProductsAdapter(productsList: List<Inventory>?) {
		inventoryAdapter = InventoryAdapter(productsList ?: emptyList(), requireContext())
		inventoryAdapter.onClickListener = object : InventoryAdapter.OnClickListener {
			override fun onClick(productData: Inventory) {
				findNavController().navigate(
					R.id.action_seeInventory,
					bundleOf("inventoryId" to productData.inventoryId)
				)
			}

			override fun onDeleteClick(productData: Inventory) {
				Log.d(TAG, "onDeleteProduct: initiated for ${productData.inventoryId}")
				showDeleteDialog(productData.sku, productData.inventoryId)
			}

			override fun onEditClick(inventoryId: String) {
				Log.d(TAG, "onEditProduct: initiated for $inventoryId")
				navigateToAddEditInventoryFragment(isEdit = true, inventoryId = inventoryId)
			}

			override fun onAddToCartClick(productData: Inventory) {
				Log.d(TAG, "onToggleCartAddition: initiated")
				viewModel.toggleInventoryInCart(productData)
			}
		}
		inventoryAdapter.bindImageButtons = object : InventoryAdapter.BindImageButtons {

			override fun setCartButton(productId: String, imgView: ImageView) {
				if (viewModel.isInventoryInCart(productId)) {
					imgView.setImageResource(R.drawable.ic_remove_shopping_cart_24)
				} else {
					imgView.setImageResource(R.drawable.ic_add_shopping_cart_24)
				}
			}

		}
	}

	private fun showDeleteDialog(productName: String, productId: String) {
		context?.let {
			MaterialAlertDialogBuilder(it)
				.setTitle(getString(R.string.delete_dialog_title_text))
				.setMessage(getString(R.string.delete_dialog_message_text, productName))
				.setNegativeButton(getString(R.string.pro_cat_dialog_cancel_btn)) { dialog, _ ->
					dialog.cancel()
				}
				.setPositiveButton(getString(R.string.delete_dialog_delete_btn_text)) { dialog, _ ->
					viewModel.deleteInventory(productId)
					dialog.cancel()
				}
				.show()
		}
	}

	private fun showDialogWithItems(
		categoryItems: Array<String>,
		checkedOption: Int = 0,
		isFilter: Boolean
	) {
		var checkedItem = checkedOption
		context?.let {
			MaterialAlertDialogBuilder(it)
				.setTitle(getString(R.string.pro_cat_dialog_title))
				.setSingleChoiceItems(categoryItems, checkedItem) { _, which ->
					checkedItem = which
				}
				.setNegativeButton(getString(R.string.pro_cat_dialog_cancel_btn)) { dialog, _ ->
					dialog.cancel()
				}
				.setPositiveButton(getString(R.string.pro_cat_dialog_ok_btn)) { dialog, _ ->
					if (checkedItem == -1) {
						dialog.cancel()
					} else {
						if (isFilter) {
							viewModel.filterInventories(categoryItems[checkedItem])
						} else {
							navigateToAddEditInventoryFragment(
								isEdit = false,
								catName = categoryItems[checkedItem]
							)
						}
					}
					dialog.cancel()
				}
				.show()
		}
	}

	private fun navigateToAddEditInventoryFragment(
		isEdit: Boolean,
		catName: String? = null,
		inventoryId: String? = null
	) {
		findNavController().navigate(
			R.id.action_goto_addInventory,
			bundleOf("isEdit" to isEdit, "categoryName" to catName, "inventoryId" to inventoryId)
		)
	}

	private fun getMixedDataList(data: List<Inventory>, adsList: List<Int>): List<Any> {
		val itemsList = mutableListOf<Any>()
		itemsList.addAll(data.sortedBy { it.inventoryId })
		var currPos = 0
		if (itemsList.size >= 4) {
			adsList.forEach label@{ ad ->
				if (itemsList.size > currPos + 1) {
					itemsList.add(currPos, ad)
				} else {
					return@label
				}
				currPos += 5
			}
		}
		return itemsList
	}

	private fun getAdsList(): List<Int> {
		return listOf(R.drawable.ad_ex_2, R.drawable.ad_ex_1, R.drawable.ad_ex_3)
	}
}