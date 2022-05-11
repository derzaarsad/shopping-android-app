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
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.data.utils.AddInventoryErrors
import com.vishalgaur.shoppingapp.data.utils.AddObjectStatus
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.getProductId
import com.vishalgaur.shoppingapp.ui.AddProductCategoryViewErrors
import com.vishalgaur.shoppingapp.ui.AddProductViewErrors
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AddProductViewModel(application: Application) : AndroidViewModel(application) {

	private val inventoriesRepository =
		(application.applicationContext as ShoppingApplication).inventoriesRepository

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)

	private val currentUser = sessionManager.getUserIdFromSession()

	private val _selectedCategory = MutableLiveData<String>()
	val selectedCategory: LiveData<String> get() = _selectedCategory

	private val _inventoryId = MutableLiveData<String>()
	val inventoryId: LiveData<String> get() = _inventoryId

	private val _addProductErrorStatus = MutableLiveData<AddProductViewErrors>()
	val addProductErrorStatus: LiveData<AddProductViewErrors> get() = _addProductErrorStatus

	private val _dataStatus = MutableLiveData<StoreDataStatus>()
	val dataStatus: LiveData<StoreDataStatus> get() = _dataStatus

	private val _addInventoryErrors = MutableLiveData<AddInventoryErrors?>()
	val addInventoryErrors: LiveData<AddInventoryErrors?> get() = _addInventoryErrors

	private val _inventoryData = MutableLiveData<Inventory>()
	val inventoryData: LiveData<Inventory> get() = _inventoryData

	private var _productCategoriesForAddProduct = MutableLiveData<List<String>>()
	val productCategoriesForAddProduct: LiveData<List<String>> get() = _productCategoriesForAddProduct

	@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
	val newProductData = MutableLiveData<Inventory>()

	init {
		_addProductErrorStatus.value = AddProductViewErrors.NONE
	}

	fun getProductCategoriesForAddProduct() {
		viewModelScope.launch {
			val res = inventoriesRepository.getProductCategories()
			_productCategoriesForAddProduct.value = res ?: emptyList()
		}
	}

	fun setCategory(catName: String) {
		_selectedCategory.value = catName
	}

	fun setInventoryData(inventoryId: String) {
		_inventoryId.value = inventoryId
		viewModelScope.launch {
			Log.d(TAG, "onLoad: Getting product Data")
			_dataStatus.value = StoreDataStatus.LOADING
			val res = async { inventoriesRepository.getInventoryById(inventoryId) }
			val proRes = res.await()
			if (proRes is Success) {
				val proData = proRes.data
				_inventoryData.value = proData
				_selectedCategory.value = _inventoryData.value!!.category
				Log.d(TAG, "onLoad: Successfully retrieved product data")
				_dataStatus.value = StoreDataStatus.DONE
			} else if (proRes is Error) {
				_dataStatus.value = StoreDataStatus.ERROR
				Log.d(TAG, "onLoad: Error getting product data")
				_inventoryData.value = Inventory()
			}
		}
	}

	fun submitProduct(
		name: String,
		price: Double?,
		mrp: Double?,
		desc: String,
		sizes: List<Int>,
		colors: List<String>,
		imgList: List<Uri>,
	) {
		if (name.isBlank() || price == null || mrp == null || desc.isBlank() || sizes.isNullOrEmpty() || colors.isNullOrEmpty() || imgList.isNullOrEmpty()) {
			_addProductErrorStatus.value = AddProductViewErrors.EMPTY
		} else {
			if (price == 0.0 || mrp == 0.0) {
				_addProductErrorStatus.value = AddProductViewErrors.ERR_PRICE_0
			} else {
				_addProductErrorStatus.value = AddProductViewErrors.NONE
				val proId = getProductId(currentUser!!, selectedCategory.value!!)
				val newProduct =
					Inventory(
						proId,
						name.trim(),
						currentUser!!,
						desc.trim(),
						_selectedCategory.value!!,
						price,
						mrp,
						sizes,
						colors,
						emptyList(),
						0.0
					)
				newProductData.value = newProduct
				Log.d(TAG, "pro = $newProduct")
				insertProduct(imgList)
			}
		}
	}

	private fun updateProduct(imgList: List<Uri>) {
		viewModelScope.launch {
			if (newProductData.value != null && _inventoryData.value != null) {
				_addInventoryErrors.value = AddInventoryErrors.ADDING
				val resImg =
					async { inventoriesRepository.updateImages(imgList, _inventoryData.value!!.images) }
				val imagesPaths = resImg.await()
				newProductData.value?.images = imagesPaths
				if (newProductData.value?.images?.isNotEmpty() == true) {
					if (imagesPaths[0] == ERR_UPLOAD) {
						Log.d(TAG, "error uploading images")
						_addInventoryErrors.value = AddInventoryErrors.ERR_ADD_IMG
					} else {
						val updateRes =
							async { inventoriesRepository.updateInventory(newProductData.value!!) }
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
					Log.d(TAG, "Product images empty, Cannot Add Product")
				}
			} else {
				Log.d(TAG, "Product is Null, Cannot Add Product")
			}
		}
	}

	private fun insertProduct(imgList: List<Uri>) {
		viewModelScope.launch {
			if (newProductData.value != null) {
				_addInventoryErrors.value = AddInventoryErrors.ADDING
				val resImg = async { inventoriesRepository.insertImages(imgList) }
				val imagesPaths = resImg.await()
				newProductData.value?.images = imagesPaths
				if (newProductData.value?.images?.isNotEmpty() == true) {
					if (imagesPaths[0] == ERR_UPLOAD) {
						Log.d(TAG, "error uploading images")
						_addInventoryErrors.value = AddInventoryErrors.ERR_ADD_IMG
					} else {
						val deferredRes = async {
							inventoriesRepository.insertInventory(newProductData.value!!)
						}
						val res = deferredRes.await()
						if (res is Success) {
							Log.d(TAG, "onInsertProduct: Success")
							_addInventoryErrors.value = AddInventoryErrors.NONE
						} else {
							_addInventoryErrors.value = AddInventoryErrors.ERR_ADD
							if (res is Error)
								Log.d(TAG, "onInsertProduct: Error Occurred, ${res.exception}")
						}
					}
				} else {
					Log.d(TAG, "Product images empty, Cannot Add Product")
				}
			} else {
				Log.d(TAG, "Product is Null, Cannot Add Product")
			}
		}
	}

	companion object {
		private const val TAG = "AddProductViewModel"
	}
}