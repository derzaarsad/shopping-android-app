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
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.data.utils.AddObjectStatus
import com.vishalgaur.shoppingapp.ui.AddProductCategoryViewErrors
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AddProductCategoryViewModel(application: Application) : AndroidViewModel(application) {

	private val inventoriesRepository =
		(application.applicationContext as ShoppingApplication).inventoriesRepository

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)

	private val currentUser = sessionManager.getUserIdFromSession() // TODO: use this for authentication

	private val _addProductCategoryErrorStatus = MutableLiveData<AddProductCategoryViewErrors>()
	val addProductCategoryErrorStatus: LiveData<AddProductCategoryViewErrors> get() = _addProductCategoryErrorStatus

	private val _addProductCategoryStatus = MutableLiveData<AddObjectStatus?>()
	val addProductCategoryStatus: LiveData<AddObjectStatus?> get() = _addProductCategoryStatus

	init {
		_addProductCategoryErrorStatus.value = AddProductCategoryViewErrors.NONE
	}

	fun submitProductCategory(
		productCategory: String
	) {
		var err = AddProductCategoryViewErrors.NONE
		if (productCategory.isBlank()) {
			err = AddProductCategoryViewErrors.EMPTY
		}

		_addProductCategoryErrorStatus.value = err

		if (err == AddProductCategoryViewErrors.NONE) {
			insertProductCategory(productCategory)
		}
	}

	private fun insertProductCategory(productCategory: String) {
		viewModelScope.launch {
			_addProductCategoryStatus.value = AddObjectStatus.ADDING
				val deferredRes = async {
					inventoriesRepository.insertProductCategory(productCategory)
				}
				val res = deferredRes.await()
				if (res is Success) {
					Log.d(TAG, "onInsertProductCategory: Success")
					_addProductCategoryStatus.value = AddObjectStatus.DONE
				} else {
					_addProductCategoryStatus.value = AddObjectStatus.ERR_ADD
					if (res is Error) {
						Log.d(TAG, "onInsertProductCategory: Error, ${res.exception.message}")
					}
				}
		}
	}

	companion object {
		private const val TAG = "AddProductCategoryViewModel"
	}
}