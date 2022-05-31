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
import com.vishalgaur.shoppingapp.data.utils.ShoeColors
import com.vishalgaur.shoppingapp.data.utils.ShoeSizes
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.databinding.FragmentAddEditInventoryBinding
import com.vishalgaur.shoppingapp.ui.AddInventoryViewErrors
import com.vishalgaur.shoppingapp.ui.MyOnFocusChangeListener
import com.vishalgaur.shoppingapp.viewModels.AddEditInventoryViewModel
import kotlin.properties.Delegates

private const val TAG = "AddInventoryFragment"

class AddEditInventoryFragment : Fragment() {

	private lateinit var binding: FragmentAddEditInventoryBinding
	private val viewModel by viewModels<AddEditInventoryViewModel>()
	private val focusChangeListener = MyOnFocusChangeListener()

	// arguments
	private var isEdit by Delegates.notNull<Boolean>()
	private lateinit var catName: String
	private lateinit var productId: String

	private var currentProductIdx: Int = 0
	private var currentSupplierIdx: Int = 0

	private var imgList = mutableListOf<Uri>()

	private val getImages =
		registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { result ->
			imgList.addAll(result)
			if (imgList.size > 3) {
				imgList = imgList.subList(0, 3)
				makeToast("Maximum 3 images are allowed!")
			}
			val adapter = context?.let { AddInventoryImagesAdapter(it, imgList) }
			binding.addInvImagesRv.adapter = adapter
		}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAddEditInventoryBinding.inflate(layoutInflater)

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
		viewModel.getProducts()
		viewModel.getSuppliers()
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
		binding.addInvErrorTextView.visibility = View.VISIBLE
		binding.addInvErrorTextView.text = errText

	}

	private fun fillDataInAllViews() {
		viewModel.inventoryData.value?.let { inventory ->
			Log.d(TAG, "fill data in views")
			binding.addProAppBar.topAppBar.title = "Edit Product - ${inventory.name}"
			//binding.proNameEditText.setText(inventory.name)
			binding.invPriceEditText.setText(inventory.price.toString())
			binding.invOrdernumEditText.setText(inventory.mrp.toString())
			binding.invDescEditText.setText(inventory.description)

			imgList = inventory.images.map { it.toUri() } as MutableList<Uri>
			val adapter = AddInventoryImagesAdapter(requireContext(), imgList)
			binding.addInvImagesRv.adapter = adapter

			binding.addInvBtn.setText(R.string.edit_product_btn_text)
		}

	}

	private fun setViews() {
		Log.d(TAG, "set views")

		if (!isEdit) {
			binding.addProAppBar.topAppBar.title =
				"Tambah Inventaris - ${viewModel.selectedCategory.value}"

			val adapter = AddInventoryImagesAdapter(requireContext(), imgList)
			binding.addInvImagesRv.adapter = adapter
		}
		binding.addInvImagesBtn.setOnClickListener {
			getImages.launch("image/*")
		}

		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE

		binding.addInvErrorTextView.visibility = View.GONE
		//binding.proNameEditText.onFocusChangeListener = focusChangeListener
		binding.invPriceEditText.onFocusChangeListener = focusChangeListener
		binding.invOrdernumEditText.onFocusChangeListener = focusChangeListener
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
	}

	private fun onAddInventory() {
		val name = binding.invProEditText.text.toString()
		val price = binding.invPriceEditText.text.toString().toDoubleOrNull()
		val mrp = binding.invOrdernumEditText.text.toString().toDoubleOrNull()
		val desc = binding.invDescEditText.text.toString()
		Log.d(
			TAG,
			"onAddInventory: Add inventory initiated, $name, $price, $mrp, $desc, $imgList"
		)
		viewModel.submitPurchaseInventory(
			name,
			if (viewModel.suppliers.value != null) viewModel.suppliers.value!![currentSupplierIdx].supplierId else null,
			if (viewModel.products.value != null) viewModel.products.value!![currentProductIdx].productId else null,
			price, mrp, desc, imgList
		)
	}

	private fun modifyErrors(err: AddInventoryViewErrors) {
		when (err) {
			AddInventoryViewErrors.NONE -> binding.addInvErrorTextView.visibility = View.GONE
			AddInventoryViewErrors.EMPTY -> {
				binding.addInvErrorTextView.visibility = View.VISIBLE
				binding.addInvErrorTextView.text = getString(R.string.add_product_error_string)
			}
			AddInventoryViewErrors.ERR_PRICE_0 -> {
				binding.addInvErrorTextView.visibility = View.VISIBLE
				binding.addInvErrorTextView.text = getString(R.string.add_pro_error_price_string)
			}
		}
	}

	private fun makeToast(text: String) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}
}