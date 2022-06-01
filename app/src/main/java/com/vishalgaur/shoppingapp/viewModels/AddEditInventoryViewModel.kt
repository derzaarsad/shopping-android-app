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

private const val TAG = "AddEditInventoryViewModel"

class AddEditInventoryViewModel(application: Application) : AndroidViewModel(application) {

	private val inventoriesRepository =
		(application.applicationContext as ShoppingApplication).inventoriesRepository

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)

	private val currentUser = sessionManager.getUserIdFromSession()

	private val _selectedCategory = MutableLiveData<String>()
	val selectedCategory: LiveData<String> get() = _selectedCategory

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

	@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
	val newInventoryData = MutableLiveData<Inventory>()

	init {
		_errorStatus.value = AddInventoryViewErrors.NONE
	}

	fun setIsEdit(state: Boolean) {
		_isEdit.value = state
	}

	fun setCategory(catName: String) {
		_selectedCategory.value = catName
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
				_selectedCategory.value = _inventoryData.value!!.category
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
		name: String,
		supplierId: String?,
		productId: String?,
		price: Double?,
		mrp: Double?,
		desc: String,
		imgList: List<Uri>,
	) {
		if (name.isBlank() || price == null || mrp == null || desc.isBlank() || imgList.isNullOrEmpty()) {
			_errorStatus.value = AddInventoryViewErrors.EMPTY
		} else {
			if (price == 0.0 || mrp == 0.0) {
				_errorStatus.value = AddInventoryViewErrors.ERR_PRICE_0
			} else {
				_errorStatus.value = AddInventoryViewErrors.NONE
				val invId = if (_isEdit.value == true) _inventoryId.value!! else
					getProductId(currentUser!!, selectedCategory.value!!)
				val newInventory =
					Inventory(
						invId,
						name.trim(),
						currentUser!!,
						desc.trim(),
						_selectedCategory.value!!,
						price,
						emptyList(),
						0.0
					)
				newInventoryData.value = newInventory
				Log.d(TAG, "inv = $newInventory")
				if (_isEdit.value == true) {
					updateInventory(imgList)
				} else {
					insertInventory(imgList)
				}
			}
		}
	}

	private fun updateInventory(imgList: List<Uri>) {
		viewModelScope.launch {
			if (newInventoryData.value != null && _inventoryData.value != null) {
				_addInventoryErrors.value = AddInventoryErrors.ADDING
				val resImg =
					async { inventoriesRepository.updateImages(imgList, _inventoryData.value!!.images) }
				val imagesPaths = resImg.await()
				newInventoryData.value?.images = imagesPaths
				if (newInventoryData.value?.images?.isNotEmpty() == true) {
					if (imagesPaths[0] == ERR_UPLOAD) {
						Log.d(TAG, "error uploading images")
						_addInventoryErrors.value = AddInventoryErrors.ERR_ADD_IMG
					} else {
						val updateRes =
							async { inventoriesRepository.updateInventory(newInventoryData.value!!) }
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
					}
				} else {
					Log.d(TAG, "Inventory images empty, Cannot Add Inventory")
				}
			} else {
				Log.d(TAG, "Inventory is Null, Cannot Add Inventory")
			}
		}
	}

	private fun insertInventory(imgList: List<Uri>) {
		viewModelScope.launch {
			if (newInventoryData.value != null) {
				_addInventoryErrors.value = AddInventoryErrors.ADDING
				val resImg = async { inventoriesRepository.insertImages(imgList) }
				val imagesPaths = resImg.await()
				newInventoryData.value?.images = imagesPaths
				if (newInventoryData.value?.images?.isNotEmpty() == true) {
					if (imagesPaths[0] == ERR_UPLOAD) {
						Log.d(TAG, "error uploading images")
						_addInventoryErrors.value = AddInventoryErrors.ERR_ADD_IMG
					} else {
						val deferredRes = async {
							inventoriesRepository.insertInventory(newInventoryData.value!!)
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
					}
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