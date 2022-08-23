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
import com.vishalgaur.shoppingapp.data.source.remote.InsertOrderData
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.getRandomString
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*

private const val TAG = "OrderViewModel"

class OrderViewModel(application: Application) : AndroidViewModel(application) {

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)
	private val currentUser = sessionManager.getUserIdFromSession()

	private val authRepository = (application as ShoppingApplication).authRepository
	private val inventoriesRepository = (application as ShoppingApplication).inventoriesRepository

	private val _memberAddresses = MutableLiveData<List<UserData.Address>>()
	val memberAddresses: LiveData<List<UserData.Address>> get() = _memberAddresses

	private val _cartItems = MutableLiveData<List<UserData.CartItem>>()
	val cartItems: LiveData<List<UserData.CartItem>> get() = _cartItems

	private val _priceList = MutableLiveData<Map<String, Double>>()
	val priceList: LiveData<Map<String, Double>> get() = _priceList

	private val _cartInventories = MutableLiveData<List<Inventory>>()
	val cartInventories: LiveData<List<Inventory>> get() = _cartInventories

	private val _dataStatus = MutableLiveData<StoreDataStatus>()
	val dataStatus: LiveData<StoreDataStatus> get() = _dataStatus

	private val _orderStatus = MutableLiveData<StoreDataStatus>()
	val orderStatus: LiveData<StoreDataStatus> get() = _orderStatus

	private val _selectedAddress = MutableLiveData<String>()
	private val _selectedPaymentMethod = MutableLiveData<String>()
	private val newOrderData = MutableLiveData<UserData.OrderItem>()

	init {
		viewModelScope.launch {
		}
	}

	fun getCartItems() {
		Log.d(TAG, "Getting Cart Items")
		_dataStatus.value = StoreDataStatus.LOADING
		viewModelScope.launch {
			val deferredRes = async {
				authRepository.refreshUserDataFromRemote()
				authRepository.getUserDataFromLocalSource(currentUser!!)
			}
			val userRes = deferredRes.await()
			if (userRes != null) {
				val uData = userRes
				if (uData != null) {
					_cartItems.value = uData.cart
					val priceRes = async { getAllInventoriesInCart() }
					priceRes.await()
					Log.d(TAG, "Getting Cart Items: Success ${_priceList.value}")
				} else {
					_cartItems.value = emptyList()
					_dataStatus.value = StoreDataStatus.ERROR
					Log.d(TAG, "Getting Cart Items: User Not Found")
				}
			} else {
				_cartItems.value = emptyList()
				_dataStatus.value = StoreDataStatus.ERROR
				Log.d(TAG, "Getting Cart Items: Error Occurred")
			}
		}
	}

	fun getMemberAddresses() {
		Log.d(TAG, "Getting Addresses")
		_dataStatus.value = StoreDataStatus.LOADING
		viewModelScope.launch {
			val res = authRepository.getMemberAddressesByUserIdFromLocalSource(currentUser!!)
			if (res is Success) {
				_memberAddresses.value = res.data ?: emptyList()
				_dataStatus.value = StoreDataStatus.DONE
				Log.d(TAG, "Getting Addresses: Success")
			} else {
				_memberAddresses.value = emptyList()
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
					val addresses = _memberAddresses.value?.toMutableList()
					addresses?.let {
						val pos =
							addresses.indexOfFirst { address -> address.addressId == addressId }
						if (pos >= 0)
							it.removeAt(pos)
						_memberAddresses.value = it
					}
				}
				is Error -> Log.d(TAG, "onDeleteAddress: Error, ${res.exception}")
				else -> Log.d(TAG, "onDeleteAddress: Some error occurred!")
			}
		}
	}

	fun getItemsPriceTotal(): Double {
		var totalPrice = 0.0
		_priceList.value?.forEach { (inventoryId, price) ->
			totalPrice += price * (_cartItems.value?.find { it.inventoryId == inventoryId }?.quantity?.toInt() ?: 1)
		}
		return totalPrice
	}

	fun getItemsCount(): Int {
		var totalCount = 0
		_cartItems.value?.forEach {
			totalCount += it.quantity.toInt()
		}
		return totalCount
	}

	fun setQuantityOfItem(inventoryId: String, value: Int) {
		viewModelScope.launch {
//			_dataStatus.value = StoreDataStatus.LOADING
			var cartList: MutableList<UserData.CartItem>
			_cartItems.value?.let { items ->
				val item = items.find { it.inventoryId == inventoryId }
				val itemPos = items.indexOfFirst { it.inventoryId == inventoryId }
				cartList = items.toMutableList()
				if (item != null) {
					item.quantity = item.quantity + value
					val deferredRes = async {
						authRepository.updateCartItemByUserId(item, currentUser!!)
					}
					val res = deferredRes.await()
					if (res is Success) {
						cartList[itemPos] = item
						_cartItems.value = cartList
						_dataStatus.value = StoreDataStatus.DONE
					} else {
						_dataStatus.value = StoreDataStatus.ERROR
						if (res is Error)
							Log.d(TAG, "onUpdateQuantity: Error Occurred: ${res.exception.message}")
					}
				}
			}
		}
	}

	fun deleteItemFromCart(inventoryId: String) {
		viewModelScope.launch {
//			_dataStatus.value = StoreDataStatus.LOADING
			var cartList: MutableList<UserData.CartItem>
			_cartItems.value?.let { items ->
				val itemPos = items.indexOfFirst { it.inventoryId == inventoryId }
				cartList = items.toMutableList()
				val deferredRes = async {
					authRepository.deleteCartItemByUserId(inventoryId, currentUser!!)
				}
				val res = deferredRes.await()
				if (res is Success) {
					cartList.removeAt(itemPos)
					_cartItems.value = cartList
					val priceRes = async { getAllInventoriesInCart() }
					priceRes.await()
				} else {
					_dataStatus.value = StoreDataStatus.ERROR
					if (res is Error)
						Log.d(TAG, "onUpdateQuantity: Error Occurred: ${res.exception.message}")
				}
			}
		}
	}

	fun setSelectedAddress(addressId: String) {
		_selectedAddress.value = addressId
	}

	fun setSelectedPaymentMethod(method: String) {
		_selectedPaymentMethod.value = method
	}

	fun finalizeOrder() {
		_orderStatus.value = StoreDataStatus.LOADING
		val deliveryAddress =
			_memberAddresses.value?.find { it.addressId == _selectedAddress.value }
		val paymentMethod = _selectedPaymentMethod.value
		val currDate = Date()
		val orderId = getRandomString(6, currDate.time.toString(), 1)
		val items = _cartItems.value
		val itemPrices = _priceList.value
		val shippingCharges = 0.0
		if (deliveryAddress != null && paymentMethod != null && !items.isNullOrEmpty() && !itemPrices.isNullOrEmpty()) {
			val newOrder = UserData.OrderItem(
				orderId,
				currentUser!!, // TODO: change to real customer
				items,
				itemPrices,
				deliveryAddress,
				shippingCharges,
				paymentMethod,
				currDate,
			)
			newOrderData.value = newOrder
			insertOrder()
		} else {
			Log.d(TAG, "orFinalizeOrder: Error, data null or empty")
			_orderStatus.value = StoreDataStatus.ERROR
		}
	}

	private fun insertOrder() {
		viewModelScope.launch {
			if (newOrderData.value != null) {
				_orderStatus.value = StoreDataStatus.LOADING
				val deferredRes = async {
					authRepository.placeOrder(InsertOrderData(currentUser!!,newOrderData.value!!.items.map { it.inventoryId },newOrderData.value!!.deliveryAddress.addressId,newOrderData.value!!.shippingCharges,newOrderData.value!!.paymentMethod))
				}
				val res = deferredRes.await()
				if (res is Success) {
					Log.d(TAG, "onInsertOrder: Success")
					_cartItems.value = emptyList()
					_cartInventories.value = emptyList()
					_priceList.value = emptyMap()
					_orderStatus.value = StoreDataStatus.DONE
				} else {
					_orderStatus.value = StoreDataStatus.ERROR
					if (res is Error) {
						Log.d(TAG, "onInsertOrder: Error, ${res.exception}")
					}
				}
			} else {
				Log.d(TAG, "orInsertOrder: Error, newInventory Null")
				_orderStatus.value = StoreDataStatus.ERROR
			}
		}
	}

	private suspend fun getAllInventoriesInCart() {
		viewModelScope.launch {
//			_dataStatus.value = StoreDataStatus.LOADING
			val priceMap = mutableMapOf<String, Double>()
			val proList = mutableListOf<Inventory>()
			var res = true
			_cartItems.value?.let { itemList ->
				itemList.forEach label@{ item ->
					val inventoryDeferredRes = async {
						inventoriesRepository.getInventoryById(item.inventoryId, true)
					}
					val proRes = inventoryDeferredRes.await()
					if (proRes is Success) {
						val proData = proRes.data
						proList.add(proData)
						priceMap[item.inventoryId] = proData.purchasePrice
					} else {
						res = false
						return@label
					}
				}
			}
			_priceList.value = priceMap
			_cartInventories.value = proList
			if (!res) {
				_dataStatus.value = StoreDataStatus.ERROR
			} else {
				_dataStatus.value = StoreDataStatus.DONE
			}
		}
	}
}