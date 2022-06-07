package com.vishalgaur.shoppingapp.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.utils.AddInventoryErrors
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.databinding.FragmentAddEditInventoryBinding
import com.vishalgaur.shoppingapp.ui.AddInventoryViewErrors
import com.vishalgaur.shoppingapp.ui.MyOnFocusChangeListener
import com.vishalgaur.shoppingapp.viewModels.AddEditInventoryViewModel
import java.time.LocalDate
import java.util.*
import kotlin.properties.Delegates

private const val TAG = "AddInventoryFragment"

class AddEditInventoryFragment : Fragment() {

	private lateinit var binding: FragmentAddEditInventoryBinding
	private val viewModel by viewModels<AddEditInventoryViewModel>()
	private val focusChangeListener = MyOnFocusChangeListener()

	// arguments
	private var isEdit by Delegates.notNull<Boolean>()
	private lateinit var catName: String
	private lateinit var inventoryId: String

	private var currentProductIdx: Int = 0
	private var currentSupplierIdx: Int = 0

// TODO: UPLOADIMAGE
//	private var imgList = mutableListOf<Uri>()
//
//	private val getImages =
//		registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { result ->
//			imgList.addAll(result)
//			if (imgList.size > 3) {
//				imgList = imgList.subList(0, 3)
//				makeToast("Maximum 3 images are allowed!")
//			}
//			val adapter = context?.let { AddInventoryImagesAdapter(it, imgList) }
//			binding.addInvImagesRv.adapter = adapter
//		}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAddEditInventoryBinding.inflate(layoutInflater)

		isEdit = arguments?.getBoolean("isEdit") == true
		catName = arguments?.getString("categoryName").toString()
		inventoryId = arguments?.getString("inventoryId").toString()

		initViewModel()

		setViews()

		setObservers()
		return binding.root
	}

	private fun initViewModel() {
		Log.d(TAG, "init view model, isedit = $isEdit")

		viewModel.setIsEdit(isEdit)
		if (isEdit) {
			Log.d(TAG, "init view model, isedit = true, $inventoryId")
			viewModel.setInventoryData(inventoryId)
		} else {
			Log.d(TAG, "init view model, isedit = false, $catName")
			viewModel.getProducts()
			viewModel.getSuppliers()
		}
	}

	private fun setUnitField(unit: String) {
		when(unit) {
			"KILOGRAM" -> Log.d(TAG,"SWITCH TO KILOGRAM")
			"LITER" -> Log.d(TAG, "SWITCH TO LITER")
			"PIECE" -> Log.d(TAG,"SWITCH TO PIECE")
			else -> throw Exception("Unknown product unit")
		}
		when(unit) {
			"KILOGRAM" -> binding.invQuantityEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
			"LITER" -> binding.invQuantityEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
			"PIECE" -> binding.invQuantityEditText.inputType = InputType.TYPE_CLASS_NUMBER
			else -> throw Exception("Unknown product unit")
		}
		binding.invQuantityEditText.setText("")
		binding.addInvQuantityLabel.setText("Quantity in " + unit)
	}

	private fun setObservers() {
		viewModel.errorStatus.observe(viewLifecycleOwner) { err ->
			modifyErrors(err)
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
					setAddProductErrors(getString(R.string.add_product_error_img_upload))
				}
				AddInventoryErrors.ERR_ADD -> {
					setAddProductErrors(getString(R.string.add_product_insert_error))
				}
				AddInventoryErrors.NONE -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
					binding.loaderLayout.circularLoader.hideAnimationBehavior
				}
			}
		}

		viewModel.products.observe(viewLifecycleOwner) {
			val selectedDefaultProductIdx = 0
			if(it.size > 0) {
				binding.invProEditText.setText(it[selectedDefaultProductIdx].name,false)
			}
			binding.invProEditText.setAdapter(ArrayAdapter(requireContext(),android.R.layout.select_dialog_item,it.map { it.name }))
			setUnitField(it[selectedDefaultProductIdx].unit)
		}

		binding.invProEditText.setOnItemClickListener { adapterView, view, i, l ->
			currentProductIdx = i
			val currentUnit = viewModel.products.value!![currentProductIdx].unit
			setUnitField(currentUnit)
		}

		viewModel.suppliers.observe(viewLifecycleOwner) {
			if(it.size > 0) {
				binding.invSupEditText.setText(it[0].name,false)
			}
			binding.invSupEditText.setAdapter(ArrayAdapter(requireContext(),android.R.layout.select_dialog_item,it.map { it.name }))
		}

		binding.invSupEditText.setOnItemClickListener { adapterView, view, i, l ->
			currentSupplierIdx = i
		}
	}

	private fun setAddProductErrors(errText: String) {
		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
		binding.loaderLayout.circularLoader.hideAnimationBehavior
		makeToast(errText)

	}

	private fun fillDataInAllViews() {
		viewModel.inventoryData.value?.let { inventory ->
			Log.d(TAG, "fill data in views")
			binding.addProAppBar.topAppBar.title = "Edit Product - ${inventory.sku}"
			//binding.proNameEditText.setText(inventory.name)
			binding.invPurchasePriceEditText.setText(inventory.purchasePrice.toString())
			binding.invDescEditText.setText(inventory.description)

// TODO: UPLOADIMAGE
//			imgList = inventory.images.map { it.toUri() } as MutableList<Uri>
//			val adapter = AddInventoryImagesAdapter(requireContext(), imgList)
//			binding.addInvImagesRv.adapter = adapter

			binding.addInvBtn.setText(R.string.edit_product_btn_text)
		}

	}

	private fun setViews() {
		Log.d(TAG, "set views")

		if (!isEdit) {
			binding.addProAppBar.topAppBar.title =
				"Tambah Inventaris - ${catName}"

// TODO: UPLOADIMAGE
//			val adapter = AddInventoryImagesAdapter(requireContext(), imgList)
//			binding.addInvImagesRv.adapter = adapter
		}
// TODO: UPLOADIMAGE
//		binding.addInvImagesBtn.setOnClickListener {
//			getImages.launch("image/*")
//		}

		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE

		binding.invSupEditText.onFocusChangeListener = focusChangeListener
		binding.invProEditText.onFocusChangeListener = focusChangeListener
		binding.invQuantityEditText.onFocusChangeListener = focusChangeListener
		binding.invPurchasePriceEditText.onFocusChangeListener = focusChangeListener
		binding.invMinSellPriceEditText.onFocusChangeListener = focusChangeListener
		binding.invOrdernumEditText.onFocusChangeListener = focusChangeListener
		binding.invSkuEditText.onFocusChangeListener = focusChangeListener
		binding.invDescEditText.onFocusChangeListener = focusChangeListener

		binding.addInvBtn.setOnClickListener {
			onAddInventory()
			if (viewModel.errorStatus.value == AddInventoryViewErrors.NONE) {
				viewModel.addInventoryErrors.observe(viewLifecycleOwner) { err ->
					if (err == AddInventoryErrors.NONE) {
						findNavController().navigate(R.id.action_addInventoryFragment_to_homeFragment)
					}
				}
			}
		}

		// the minimum expiry date is set to now even though the minimum should be tomorrow.
		// this is meant to catch an error when someone is too lazy to fill the expiry date
		// and just keep the default date.
		val mCalendar = Calendar.getInstance()
		binding.expiryDatepicker.minDate = mCalendar.timeInMillis
	}

	private fun onAddInventory() {
		val supplierName = binding.invSupEditText.text.toString()
		val productName = binding.invProEditText.text.toString()
		val quantity = binding.invQuantityEditText.text.toString().toDoubleOrNull()
		val purchaseprice = binding.invPurchasePriceEditText.text.toString().toDoubleOrNull()
		val minsellprice = binding.invMinSellPriceEditText.text.toString().toDoubleOrNull()
		val ordernum = binding.invOrdernumEditText.text.toString()
		val sku = binding.invSkuEditText.text.toString()
		val desc = binding.invDescEditText.text.toString()
		val expiryDate = LocalDate.of(binding.expiryDatepicker.year,binding.expiryDatepicker.month+1,binding.expiryDatepicker.dayOfMonth)
		Log.d(
			TAG,
			"onAddInventory: Add inventory initiated, $supplierName, $productName, $quantity, $purchaseprice, $ordernum, $sku, $desc, $expiryDate"//, $imgList" // TODO: UPLOADIMAGE
		)
		viewModel.submitPurchaseInventory(
			if (viewModel.suppliers.value != null) viewModel.suppliers.value!![currentSupplierIdx].supplierId else "",
			if (viewModel.products.value != null) viewModel.products.value!![currentProductIdx].productId else "",
			supplierName, productName, minsellprice, quantity, purchaseprice, ordernum, sku, desc, expiryDate,
			if (viewModel.products.value != null) viewModel.products.value!![currentProductIdx].unit else "", // ,imgList // TODO: UPLOADIMAGE
		)
	}

	private fun modifyErrors(err: AddInventoryViewErrors) {
		when (err) {
			AddInventoryViewErrors.ERR_SUPPLIER_EMPTY -> {
				makeToast(getString(R.string.add_inv_error_sup_empty_string))
			}
			AddInventoryViewErrors.ERR_PRODUCT_EMPTY -> {
				makeToast(getString(R.string.add_inv_error_pro_empty_string))
			}
			AddInventoryViewErrors.ERR_QUANTITY_0 -> {
				makeToast(getString(R.string.add_inv_error_quantity_0_string))
			}
			AddInventoryViewErrors.ERR_PURCHASE_PRICE_EMPTY -> {
				makeToast(getString(R.string.add_inv_error_purchase_price_empty_string))
			}
			AddInventoryViewErrors.ERR_MINSELLPRICE_NOT_BIGGER -> {
				makeToast(getString(R.string.add_inv_error_min_sell_price_empty_string))
			}
			AddInventoryViewErrors.ERR_ORDERNUM_EMPTY -> {
				makeToast(getString(R.string.add_inv_error_ordernum_empty_string))
			}
			AddInventoryViewErrors.ERR_SKU_EMPTY -> {
				makeToast(getString(R.string.add_pro_sku_empty_err))
			}
			AddInventoryViewErrors.ERR_IMG_EMPTY -> {
				makeToast(getString(R.string.add_inv_error_img_empty_string))
			}
			AddInventoryViewErrors.ERR_NOT_FUTURE_DATE -> {
				makeToast(getString(R.string.add_inv_error_expiry_date_string))
			}
		}
	}

	private fun makeToast(text: String) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}
}