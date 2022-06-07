package com.vishalgaur.shoppingapp.viewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vishalgaur.shoppingapp.ERR_UPLOAD
import com.vishalgaur.shoppingapp.ShoppingApplication
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.Supplier
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.data.utils.AddInventoryErrors
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.getProductId
import com.vishalgaur.shoppingapp.ui.AddInventoryViewErrors
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TAG = "AddEditInventoryViewModel"

class AddEditInventoryViewModel(application: Application) : AndroidViewModel(application) {

	private val inventoriesRepository =
		(application.applicationContext as ShoppingApplication).inventoriesRepository

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)

	private val currentUser = sessionManager.getUserIdFromSession()

	private val _inventoryId = MutableLiveData<String>()
	val inventoryId: LiveData<String> get() = _inventoryId

	private val _isEdit = MutableLiveData<Boolean>()
	val isEdit: LiveData<Boolean> get() = _isEdit

	private val _errorStatus = MutableLiveData<AddInventoryViewErrors>()
	val errorStatus: LiveData<AddInventoryViewErrors> get() = _errorStatus

	private val _dataStatus = MutableLiveData<StoreDataStatus>()
	val dataStatus: LiveData<StoreDataStatus> get() = _dataStatus

	private val _addInventoryErrors = MutableLiveData<AddInventoryErrors?>()
	val addInventoryErrors: LiveData<AddInventoryErrors?> get() = _addInventoryErrors

	private val _inventoryData = MutableLiveData<Inventory>()
	val inventoryData: LiveData<Inventory> get() = _inventoryData

	private var _products = MutableLiveData<List<Product>>()
	val products: LiveData<List<Product>> get() = _products

	private var _suppliers = MutableLiveData<List<Supplier>>()
	val suppliers: LiveData<List<Supplier>> get() = _suppliers

	init {
		_errorStatus.value = AddInventoryViewErrors.NONE
	}

	fun setIsEdit(state: Boolean) {
		_isEdit.value = state
	}

	fun setInventoryData(inventoryId: String) {
		_inventoryId.value = inventoryId
		viewModelScope.launch {
			Log.d(TAG, "onLoad: Getting inventory Data")
			_dataStatus.value = StoreDataStatus.LOADING
			val res = async { inventoriesRepository.getInventoryById(inventoryId) }
			val proRes = res.await()
			if (proRes is Success) {
				val proData = proRes.data
				_inventoryData.value = proData
				Log.d(TAG, "onLoad: Successfully retrieved inventory data")
				_dataStatus.value = StoreDataStatus.DONE
			} else if (proRes is Error) {
				_dataStatus.value = StoreDataStatus.ERROR
				Log.d(TAG, "onLoad: Error getting inventory data")
				_inventoryData.value = Inventory()
			}
		}
	}

	fun submitPurchaseInventory(
		supplierId: String,
		productId: String,
		supplierName: String,
		productName: String,
		minSellPrice: Double?,
		quantity: Double?,
		purchasePrice: Double?,
		orderNum: String,
		sku: String,
		desc: String,
		expiryDate: LocalDate,
		unit: String
		// imgList: List<Uri> // TODO: UPLOADIMAGE
	) {
		if (supplierId.isBlank() || supplierName.isBlank()) {
			_errorStatus.value = AddInventoryViewErrors.ERR_SUPPLIER_EMPTY
		}
		else if (productId.isBlank() || productName.isBlank()) {
			_errorStatus.value = AddInventoryViewErrors.ERR_PRODUCT_EMPTY
		}
		else if (quantity == null || quantity <= 0.0) {
			_errorStatus.value = AddInventoryViewErrors.ERR_QUANTITY_0
		}
		else if (purchasePrice == null) {
			_errorStatus.value = AddInventoryViewErrors.ERR_PURCHASE_PRICE_EMPTY
		}
		else if (minSellPrice == null || minSellPrice <= purchasePrice) {
			_errorStatus.value = AddInventoryViewErrors.ERR_MINSELLPRICE_NOT_BIGGER
		}
		else if (orderNum.isBlank()) {
			_errorStatus.value = AddInventoryViewErrors.ERR_ORDERNUM_EMPTY
		}
		else if (sku.isBlank()) {
			_errorStatus.value = AddInventoryViewErrors.ERR_SKU_EMPTY
		}
// TODO: UPLOADIMAGE
//		else if (imgList.isNullOrEmpty()) {
//			_errorStatus.value = AddInventoryViewErrors.ERR_IMG_EMPTY
//		}
		else {
			val now = LocalDate.now()
			if(expiryDate.year < now.year) {
				_errorStatus.value = AddInventoryViewErrors.ERR_NOT_FUTURE_DATE
			}
			else if(expiryDate.year == now.year && expiryDate.month < now.month) {
				_errorStatus.value = AddInventoryViewErrors.ERR_NOT_FUTURE_DATE
			}
			else if(expiryDate.year == now.year && expiryDate.month == now.month && expiryDate.dayOfMonth <= now.dayOfMonth) {
				_errorStatus.value = AddInventoryViewErrors.ERR_NOT_FUTURE_DATE
			}
			else {
				_errorStatus.value = AddInventoryViewErrors.NONE
				val invId = if (_isEdit.value == true) _inventoryId.value!! else ""
				val newInventory =
					Inventory(
						invId,
						supplierId,
						currentUser!!,
						productId,
						currentUser!!,
						purchasePrice,
						orderNum.trim(),
						sku.trim(),
						minSellPrice,
						quantity,
						expiryDate.toString(),
						desc.trim(),
						emptyList(),
						unit
					)
				Log.d(TAG, "inv = $newInventory")
				if (_isEdit.value == true) {
					updateInventory(newInventory/*imgList TODO: UPLOADIMAGE*/)
				} else {
					insertInventory(newInventory/*imgList TODO: UPLOADIMAGE*/)
				}
			}
		}
	}

	private fun updateInventory(newInventoryData: Inventory?/*imgList: List<Uri> TODO: UPLOADIMAGE*/) {
		viewModelScope.launch {
			if (newInventoryData != null && _inventoryData.value != null) {
				_addInventoryErrors.value = AddInventoryErrors.ADDING
// TODO: UPLOADIMAGE
//				val resImg =
//					async { inventoriesRepository.updateImages(imgList, _inventoryData.value!!.images) }
//				val imagesPaths = resImg.await()
				newInventoryData?.images = listOf("https://www.hdnicewallpapers.com/Walls/Big/Tiger/Download_Image_of_Animal_Tiger.jpg")//imagesPaths // TODO: UPLOADIMAGE
				if (newInventoryData?.images?.isNotEmpty() == true) {
// TODO: UPLOADIMAGE
//					if (imagesPaths[0] == ERR_UPLOAD) {
//						Log.d(TAG, "error uploading images")
//						_addInventoryErrors.value = AddInventoryErrors.ERR_ADD_IMG
//					} else {
						val updateRes =
							async { inventoriesRepository.updateInventory(newInventoryData!!) }
						val res = updateRes.await()
						if (res is Success) {
							Log.d(TAG, "onUpdate: Success")
							_addInventoryErrors.value = AddInventoryErrors.NONE
						} else {
							Log.d(TAG, "onUpdate: Some error occurred!")
							_addInventoryErrors.value = AddInventoryErrors.ERR_ADD
							if (res is Error)
								Log.d(TAG, "onUpdate: Error, ${res.exception}")
						}
//					} // TODO: UPLOADIMAGE
				} else {
					Log.d(TAG, "Inventory images empty, Cannot Add Inventory")
				}
			} else {
				Log.d(TAG, "Inventory is Null, Cannot Add Inventory")
			}
		}
	}

	private fun insertInventory(newInventoryData: Inventory?/*imgList: List<Uri> TODO: UPLOADIMAGE*/) {
		viewModelScope.launch {
			if (newInventoryData != null) {
				_addInventoryErrors.value = AddInventoryErrors.ADDING
// TODO: UPLOADIMAGE
//				val resImg = async { inventoriesRepository.insertImages(imgList) }
//				val imagesPaths = resImg.await()
				newInventoryData?.images = listOf("https://www.hdnicewallpapers.com/Walls/Big/Tiger/Download_Image_of_Animal_Tiger.jpg")//imagesPaths // TODO: UPLOADIMAGE
				if (newInventoryData?.images?.isNotEmpty() == true) {
// TODO: UPLOADIMAGE
//					if (imagesPaths[0] == ERR_UPLOAD) {
//						Log.d(TAG, "error uploading images")
//						_addInventoryErrors.value = AddInventoryErrors.ERR_ADD_IMG
//					} else {
						val deferredRes = async {
							inventoriesRepository.insertInventory(newInventoryData!!)
						}
						val res = deferredRes.await()
						if (res is Success) {
							Log.d(TAG, "onInsertInventory: Success")
							_addInventoryErrors.value = AddInventoryErrors.NONE
						} else {
							_addInventoryErrors.value = AddInventoryErrors.ERR_ADD
							if (res is Error)
								Log.d(TAG, "onInsertInventory: Error Occurred, ${res.exception}")
						}
//					} // TODO: UPLOADIMAGE
				} else {
					Log.d(TAG, "Inventory images empty, Cannot Add Inventory")
				}
			} else {
				Log.d(TAG, "Inventory is Null, Cannot Add Inventory")
			}
		}
	}

	fun getProducts() {
		viewModelScope.launch {
			val res = inventoriesRepository.getProducts()
			_products.value = res ?: emptyList()
		}
	}

	fun getSuppliers() {
		viewModelScope.launch {
			val res = inventoriesRepository.getSuppliers()
			_suppliers.value = res ?: emptyList()
		}
	}

}