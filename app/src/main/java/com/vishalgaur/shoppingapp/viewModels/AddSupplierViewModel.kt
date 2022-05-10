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
import com.vishalgaur.shoppingapp.ui.AddSupplierViewErrors
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private const val TAG = "AddSupplierViewModel"

class AddSupplierViewModel(application: Application) : AndroidViewModel(application) {

	private val inventoriesRepository =
		(application.applicationContext as ShoppingApplication).inventoriesRepository

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)

	private val currentUser = sessionManager.getUserIdFromSession()

	private val _addSupplierErrorStatus = MutableLiveData<AddSupplierViewErrors>()
	val addSupplierErrorStatus: LiveData<AddSupplierViewErrors> get() = _addSupplierErrorStatus

	private val _addSupplierStatus = MutableLiveData<AddObjectStatus?>()
	val addSupplierStatus: LiveData<AddObjectStatus?> get() = _addSupplierStatus

	init {
		_addSupplierErrorStatus.value = AddSupplierViewErrors.NONE
	}

	fun submitSupplier(
		supplierName: String, addressId: String
	) {
		var err = AddSupplierViewErrors.NONE
		if (supplierName.isBlank() || addressId.isBlank()) {
			err = AddSupplierViewErrors.EMPTY
		}

		_addSupplierErrorStatus.value = err

		if (err == AddSupplierViewErrors.NONE) {
			insertSupplier(supplierName,addressId)
		}
	}

	private fun insertSupplier(supplierName: String, addressId: String) {
		viewModelScope.launch {
			_addSupplierStatus.value = AddObjectStatus.ADDING
			val deferredRes = async {
				inventoriesRepository.insertSupplier(supplierName, addressId)
			}
			val res = deferredRes.await()
			if (res is Success) {
				Log.d(TAG, "onInsertSupplier: Success")
				_addSupplierStatus.value = AddObjectStatus.DONE
			} else {
				_addSupplierStatus.value = AddObjectStatus.ERR_ADD
				if (res is Error) {
					Log.d(TAG, "onInsertSupplier: Error, ${res.exception.message}")
				}
			}
		}
	}

	companion object {
		private const val TAG = "AddSupplierViewModel"
	}
}