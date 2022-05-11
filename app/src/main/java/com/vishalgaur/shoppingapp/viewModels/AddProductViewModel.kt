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
import com.vishalgaur.shoppingapp.ui.AddSupplierViewErrors
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AddProductViewModel(application: Application) : AndroidViewModel(application) {

	private val inventoriesRepository =
		(application.applicationContext as ShoppingApplication).inventoriesRepository

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)

	private val currentUser = sessionManager.getUserIdFromSession()

	private val _addProductErrorStatus = MutableLiveData<AddProductViewErrors>()
	val addProductErrorStatus: LiveData<AddProductViewErrors> get() = _addProductErrorStatus

	private val _addProductStatus = MutableLiveData<AddObjectStatus?>()
	val addProductStatus: LiveData<AddObjectStatus?> get() = _addProductStatus

	private var _productCategoriesForAddProduct = MutableLiveData<List<String>>()
	val productCategoriesForAddProduct: LiveData<List<String>> get() = _productCategoriesForAddProduct

	init {
		_addProductErrorStatus.value = AddProductViewErrors.NONE
	}

	fun getProductCategoriesForAddProduct() {
		viewModelScope.launch {
			val res = inventoriesRepository.getProductCategories()
			_productCategoriesForAddProduct.value = res ?: emptyList()
		}
	}

	private fun insertProduct(productName: String,description: String,upc: String,sku: String,unit: String,categoryName: String) {
		viewModelScope.launch {
			_addProductStatus.value = AddObjectStatus.ADDING
			val deferredRes = async {
				inventoriesRepository.insertProduct(productName,description,upc,sku,unit,categoryName)
			}
			val res = deferredRes.await()
			if (res is Success) {
				Log.d(TAG, "onInsertProduct: Success")
				_addProductStatus.value = AddObjectStatus.DONE
			} else {
				_addProductStatus.value = AddObjectStatus.ERR_ADD
				if (res is Error) {
					Log.d(TAG, "onInsertProduct: Error, ${res.exception.message}")
				}
			}
		}
	}

	fun submitProduct(
		productName: String,description: String,upc: String,sku: String,unit: String,categoryName: String
	) {
		var err = AddProductViewErrors.NONE

		if (sku.isBlank()) {
			err = AddProductViewErrors.ERR_SKU_EMPTY
		}

		if (upc.isBlank()) {
			err = AddProductViewErrors.ERR_UPC_EMPTY
		}

		if (productName.isBlank()) {
			err = AddProductViewErrors.ERR_NAME_EMPTY
		}

		if (unit.isBlank()) {
			err = AddProductViewErrors.ERR_UNIT_EMPTY
		}

		if (categoryName.isBlank()) {
			err = AddProductViewErrors.ERR_CAT_EMPTY
		}

		_addProductErrorStatus.value = err

		if (err == AddProductViewErrors.NONE) {
			insertProduct("","","","","","")
		}
	}

	companion object {
		private const val TAG = "AddProductViewModel"
	}
}