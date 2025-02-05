package com.vishalgaur.shoppingapp.data.source.local

import android.util.Log
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.*
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.source.UserDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserLocalDataSource internal constructor(
	private val userDao: UserDao,
	private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserDataSource {

	override suspend fun addUser(userData: UserData) {
		withContext(ioDispatcher) {
			userDao.clearAllUsers()
			userDao.insert(userData)
		}
	}

	override suspend fun getUserById(userId: String): UserData? =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					return@withContext uData
				} else {
					return@withContext null
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetUser: Error Occurred, $e")
				return@withContext null
			}
		}

	suspend fun getUserByMobile(phoneNumber: String): UserData? =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getByMobile(phoneNumber)
				if (uData != null) {
					return@withContext uData
				} else {
					return@withContext null
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetUser: Error Occurred, $e")
				return@withContext null
			}
		}

	suspend fun getOrdersByUserIdFromLocalSource(userId: String): Result<List<UserData.OrderItem>?> =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val ordersList = uData.orders
					return@withContext Success(ordersList)
				} else {
					return@withContext Error(Exception("User Not Found"))
				}

			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetOrders: Error Occurred, ${e.message}")
				return@withContext Error(e)
			}
		}

	override suspend fun getMemberAddressesByUserId(userId: String): Result<List<UserData.Address>?> =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val addressList = uData.memberAddresses
					return@withContext Success(addressList)
				} else {
					return@withContext Error(Exception("User Not Found"))
				}

			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetMemberAddressesByUserId: Error Occurred, ${e.message}")
				return@withContext Error(e)
			}
		}

	suspend fun insertCartItem(newItem: UserData.CartItem, userId: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val cartItems = uData.cart.toMutableList()
					cartItems.add(newItem)
					uData.cart = cartItems
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onInsertCartItem: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun updateCartItem(item: UserData.CartItem, userId: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val cartItems = uData.cart.toMutableList()
					val pos = cartItems.indexOfFirst { it.inventoryId == item.inventoryId }
					if (pos >= 0) {
						cartItems[pos] = item
					}
					uData.cart = cartItems
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onInsertCartItem: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun deleteCartItem(inventoryId: String, userId: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val cartItems = uData.cart.toMutableList()
					val pos = cartItems.indexOfFirst { it.inventoryId == inventoryId }
					if (pos >= 0) {
						cartItems.removeAt(pos)
					}
					uData.cart = cartItems
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onInsertCartItem: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun setStatusOfOrderByUserId(orderId: String, userId: String, status: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val orders = uData.orders.toMutableList()
					val pos = orders.indexOfFirst { it.orderId == orderId }
					if (pos >= 0) {
						orders[pos].status = status
						val custId = orders[pos].customerId
						val custData = userDao.getById(custId)
						if (custData != null) {
							val orderList = custData.orders.toMutableList()
							val idx = orderList.indexOfFirst { it.orderId == orderId }
							if (idx >= 0) {
								orderList[idx].status = status
							}
							custData.orders = orderList
							userDao.updateUser(custData)
						}
					}
					uData.orders = orders
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onInsertCartItem: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun clearAllUsers() {
		withContext(ioDispatcher) {
			userDao.clearAllUsers()
		}
	}

}