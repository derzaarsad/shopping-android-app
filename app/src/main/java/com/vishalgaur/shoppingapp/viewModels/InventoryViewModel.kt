package com.vishalgaur.shoppingapp.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vishalgaur.shoppingapp.ShoppingApplication
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.utils.AddObjectStatus
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.ui.AddItemErrors
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*

private const val TAG = "InventoryViewModel"

class InventoryViewModel(private val inventoryId: String, application: Application) :
	AndroidViewModel(application) {

	private val _inventoryData = MutableLiveData<Inventory?>()
	val inventoryData: LiveData<Inventory?> get() = _inventoryData

	private val _dataStatus = MutableLiveData<StoreDataStatus>()
	val dataStatus: LiveData<StoreDataStatus> get() = _dataStatus

	private val _errorStatus = MutableLiveData<List<AddItemErrors>>()
	val errorStatus: LiveData<List<AddItemErrors>> get() = _errorStatus

	private val _addItemStatus = MutableLiveData<AddObjectStatus?>()
	val addItemStatus: LiveData<AddObjectStatus?> get() = _addItemStatus

	private val _isItemInCart = MutableLiveData<Boolean>()
	val isItemInCart: LiveData<Boolean> get() = _isItemInCart

	private val inventoriesRepository =(application as ShoppingApplication).inventoriesRepository
	private val authRepository = (application as ShoppingApplication).authRepository
	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)
	private val currentUserId = sessionManager.getUserIdFromSession()

	init {
		_errorStatus.value = emptyList()
		viewModelScope.launch {
			Log.d(TAG, "init: inventoryId: $inventoryId")
			getProductDetails()
			checkIfInCart()
		}

	}

	private fun getProductDetails() {
		viewModelScope.launch {
			_dataStatus.value = StoreDataStatus.LOADING
			try {
				Log.d(TAG, "getting product Data")
				val res = inventoriesRepository.getInventoryById(inventoryId)
				if (res is Success) {
					_inventoryData.value = res.data
					_dataStatus.value = StoreDataStatus.DONE
				} else if (res is Error) {
					throw Exception("Error getting product")
				}
			} catch (e: Exception) {
				_inventoryData.value = Inventory()
				_dataStatus.value = StoreDataStatus.ERROR
			}
		}
	}

	fun checkIfInCart() {
		viewModelScope.launch {
			val deferredRes = async { authRepository.getUserDataFromLocalSource(currentUserId!!) }
			val userRes = deferredRes.await()
			if (userRes != null) {
				val uData = userRes
				if (uData != null) {
					val cartList = uData.cart
					val idx = cartList.indexOfFirst { it.inventoryId == inventoryId }
					_isItemInCart.value = idx >= 0
					Log.d(TAG, "Checking in Cart: Success, value = ${_isItemInCart.value}, ${cartList.size}")
				} else {
					_isItemInCart.value = false
				}
			} else {
				_isItemInCart.value = false
			}
		}
	}

	fun addToCart(size: Int?, color: String?) {
		val errList = mutableListOf<AddItemErrors>()
		// TODO: Use this later on
//		if (size == null) errList.add(AddItemErrors.ERROR_SIZE)
//		if (color.isNullOrBlank()) errList.add(AddItemErrors.ERROR_COLOR)

		if (errList.isEmpty()) {
			val itemId = UUID.randomUUID().toString()
			val newItem = UserData.CartItem(
				itemId, inventoryId, inventoryData.value!!.sellerId, 1, color, size
			)
			insertCartItem(newItem)
		}
	}

	private fun insertCartItem(item: UserData.CartItem) {
		viewModelScope.launch {
			_addItemStatus.value = AddObjectStatus.ADDING
			val deferredRes = async {
				authRepository.insertCartItemByUserId(item, currentUserId!!)
			}
			val res = deferredRes.await()
			if (res is Success) {
				Log.d(TAG, "onAddItem: Success")
				_addItemStatus.value = AddObjectStatus.DONE
			} else {
				_addItemStatus.value = AddObjectStatus.ERR_ADD
				if (res is Error) {
					Log.d(TAG, "onAddItem: Error, ${res.exception.message}")
				}
			}
		}
	}
}