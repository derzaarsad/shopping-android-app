package com.vishalgaur.shoppingapp.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.vishalgaur.shoppingapp.ShoppingApplication
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.Month
import java.util.*

private const val TAG = "HomeViewModel"

class HomeViewModel(application: Application) : AndroidViewModel(application) {

	private val inventoriesRepository =
		(application.applicationContext as ShoppingApplication).inventoriesRepository
	private val authRepository =
		(application.applicationContext as ShoppingApplication).authRepository

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)
	private val currentUser = sessionManager.getUserIdFromSession()
	val isUserAdmin = sessionManager.isUserAdmin()

	private var _inventories = MutableLiveData<List<Inventory>>()
	val inventories: LiveData<List<Inventory>> get() = _inventories

	private lateinit var _allInventories: MutableLiveData<List<Inventory>>
	val allInventories: LiveData<List<Inventory>> get() = _allInventories

	private var _userInventories = MutableLiveData<List<Inventory>>()
	val userInventories: LiveData<List<Inventory>> get() = _userInventories

	private var _userOrders = MutableLiveData<List<UserData.OrderItem>>()
	val userOrders: LiveData<List<UserData.OrderItem>> get() = _userOrders

	private var _userAddresses = MutableLiveData<List<UserData.Address>>()
	val userAddresses: LiveData<List<UserData.Address>> get() = _userAddresses

	private var _selectedOrder = MutableLiveData<UserData.OrderItem?>()
	val selectedOrder: LiveData<UserData.OrderItem?> get() = _selectedOrder

	private var _orderInventories = MutableLiveData<List<Inventory>>()
	val orderInventories: LiveData<List<Inventory>> get() = _orderInventories

	private var _productCategories = MutableLiveData<List<String>>()
	val productCategories: LiveData<List<String>> get() = _productCategories

	private var _filterCategory = MutableLiveData("All")
	val filterCategory: LiveData<String> get() = _filterCategory

	private val _storeDataStatus = MutableLiveData<StoreDataStatus>()
	val storeDataStatus: LiveData<StoreDataStatus> get() = _storeDataStatus

	private val _dataStatus = MutableLiveData<StoreDataStatus>()
	val dataStatus: LiveData<StoreDataStatus> get() = _dataStatus

	private val _userData = MutableLiveData<UserData?>()
	val userData: LiveData<UserData?> get() = _userData

	init {
		viewModelScope.launch {
			authRepository.refreshUserDataFromRemote()
		}

		getInventories()
	}

	fun setDataLoaded() {
		_storeDataStatus.value = StoreDataStatus.DONE
	}

	fun isInventoryInCart(inventoryId: String): Boolean {
		return false
	}

	fun toggleInventoryInCart(inventory: Inventory) {

	}

	fun setDataLoading() {
		_dataStatus.value = StoreDataStatus.LOADING
	}

	private fun getInventories() {
		_allInventories = Transformations.switchMap(inventoriesRepository.observeInventories()) {
			getInventoriesLiveData(it)
		} as MutableLiveData<List<Inventory>>
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			val res = async { inventoriesRepository.updateLocalInventoriesFromRemote(currentUser!!) }
			res.await()
			Log.d(TAG, "getAllInventories: status = ${_storeDataStatus.value}")
		}
	}

	fun getProductCategories() {
		viewModelScope.launch {
			val res = inventoriesRepository.getProductCategories()
			_productCategories.value = res ?: emptyList()
		}
	}

	private fun getInventoriesLiveData(result: Result<List<Inventory>?>?): LiveData<List<Inventory>> {
		val res = MutableLiveData<List<Inventory>>()
		if (result is Success) {
			Log.d(TAG, "result is success")
			_storeDataStatus.value = StoreDataStatus.DONE
			res.value = result.data ?: emptyList()
		} else {
			Log.d(TAG, "result is not success")
			res.value = emptyList()
			_storeDataStatus.value = StoreDataStatus.ERROR
			if (result is Error)
				Log.d(TAG, "getInventoriesLiveData: Error Occurred: ${result.exception}")
		}
		return res
	}

	private fun getInventoriesBySellerId() {
		_allInventories =
			Transformations.switchMap(inventoriesRepository.observeInventoriesBySellerId(currentUser!!)) {
				Log.d(TAG, it.toString())
				getInventoriesLiveData(it)
			} as MutableLiveData<List<Inventory>>
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			val res = async { inventoriesRepository.updateLocalInventoriesFromRemote(currentUser!!) }
			res.await()
			Log.d(TAG, "getInventoriesByOwner: status = ${_storeDataStatus.value}")
		}
	}

	fun refreshInventories() {
		getInventories()
	}

	fun filterBySearch(queryText: String) {
		filterInventories(_filterCategory.value!!)
		_inventories.value = _inventories.value?.filter { inventory ->
			inventory.name.contains(queryText, true) or
					((queryText.toDoubleOrNull() ?: 0.0).compareTo(inventory.price) == 0)
		}
	}

	fun filterInventories(filterType: String) {
		Log.d(TAG, "filterType is $filterType")
		_filterCategory.value = filterType
		_inventories.value = when (filterType) {
			"None" -> emptyList()
			"All" -> _allInventories.value
			else -> _allInventories.value?.filter { inventory ->
				inventory.category == filterType
			}
		}
	}

	fun deleteInventory(inventoryId: String) {
		viewModelScope.launch {
			val delRes = async { inventoriesRepository.deleteInventoryById(inventoryId) }
			when (val res = delRes.await()) {
				is Success -> Log.d(TAG, "onDelete: Success")
				is Error -> Log.d(TAG, "onDelete: Error, ${res.exception}")
				else -> Log.d(TAG, "onDelete: Some error occurred!")
			}
		}
	}

	fun signOut() {
		viewModelScope.launch {
			val deferredRes = async { authRepository.signOut() }
			deferredRes.await()
		}
	}

	fun getAllOrders() {
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			val deferredRes = async { authRepository.getOrdersByUserIdFromLocalSource(currentUser!!) }
			val res = deferredRes.await()
			if (res is Success) {
				_userOrders.value = res.data ?: emptyList()
				_storeDataStatus.value = StoreDataStatus.DONE
				Log.d(TAG, "Getting Orders: Success")
			} else {
				_userOrders.value = emptyList()
				_storeDataStatus.value = StoreDataStatus.ERROR
				if (res is Error)
					Log.d(TAG, "Getting Orders: Error, ${res.exception}")
			}
		}
	}

	fun getOrderDetailsByOrderId(orderId: String) {
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			if (_userOrders.value != null) {
				val orderData = _userOrders.value!!.find { it.orderId == orderId }
				if (orderData != null) {
					_selectedOrder.value = orderData
					_orderInventories.value =
						orderData.items.map {
							_allInventories.value?.find { pro -> pro.inventoryId == it.inventoryId }
								?: Inventory()
						}
					_storeDataStatus.value = StoreDataStatus.DONE
				} else {
					_selectedOrder.value = null
					_storeDataStatus.value = StoreDataStatus.ERROR
				}
			}
		}
	}

	fun onSetStatusOfOrder(orderId: String, status: String) {
		val currDate = Calendar.getInstance()
		val dateString =
			"${Month.values()[(currDate.get(Calendar.MONTH))].name} ${
				currDate.get(Calendar.DAY_OF_MONTH)
			}, ${currDate.get(Calendar.YEAR)}"
		Log.d(TAG, "Selected Status is $status ON $dateString")
		setStatusOfOrder(orderId, "$status ON $dateString")
	}

	private fun setStatusOfOrder(orderId: String, statusString: String) {
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			val deferredRes = async {
				authRepository.setStatusOfOrder(orderId, currentUser!!, statusString)
			}
			val res = deferredRes.await()
			if (res is Success) {
				val orderData = _selectedOrder.value
				orderData?.status = statusString
				_selectedOrder.value = orderData
				getOrderDetailsByOrderId(orderId)
			} else {
				_storeDataStatus.value = StoreDataStatus.ERROR
				if (res is Error)
					Log.d(TAG, "Error updating status, ${res.exception}")
			}
		}
	}

	fun getUserAddresses() {
		Log.d(TAG, "Getting Addresses")
		_dataStatus.value = StoreDataStatus.LOADING
		viewModelScope.launch {
			val res = authRepository.getAddressesByUserIdFromLocalSource(currentUser!!)
			if (res is Success) {
				_userAddresses.value = res.data ?: emptyList()
				_dataStatus.value = StoreDataStatus.DONE
				Log.d(TAG, "Getting Addresses: Success")
			} else {
				_userAddresses.value = emptyList()
				_dataStatus.value = StoreDataStatus.ERROR
				if (res is Error)
					Log.d(TAG, "Getting Addresses: Error Occurred, ${res.exception.message}")
			}
		}
	}

	fun deleteAddressOfCurrentUser(addressId: String) {
		viewModelScope.launch {
			val delRes = async { authRepository.deleteAddressOfUser(addressId, currentUser!!) }
			when (val res = delRes.await()) {
				is Success -> {
					Log.d(TAG, "onDeleteAddress: Success")
					val addresses = _userAddresses.value?.toMutableList()
					addresses?.let {
						val pos =
							addresses.indexOfFirst { address -> address.addressId == addressId }
						if (pos >= 0)
							it.removeAt(pos)
						_userAddresses.value = it
					}
				}
				is Error -> Log.d(TAG, "onDeleteAddress: Error, ${res.exception}")
				else -> Log.d(TAG, "onDeleteAddress: Some error occurred!")
			}
		}
	}

	fun getUserData() {
		viewModelScope.launch {
			_dataStatus.value = StoreDataStatus.LOADING
			val deferredRes = async { authRepository.getUserDataFromLocalSource(currentUser!!) }
			val res = deferredRes.await()
			if (res != null) {
				val uData = res
				_userData.value = uData
				_dataStatus.value = StoreDataStatus.DONE
			} else {
				_dataStatus.value = StoreDataStatus.ERROR
				_userData.value = null
			}
		}
	}
}