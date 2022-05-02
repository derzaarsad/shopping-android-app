package com.vishalgaur.shoppingapp.data.source

import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.utils.EmailMobileData

interface UserDataSource {
	suspend fun addUser(userData: UserData)

	suspend fun getUserById(userId: String): Result<UserData?>

	fun updateEmailsAndMobiles(email: String, mobile: String) {}

	suspend fun getEmailsAndMobiles(): EmailMobileData? {
		return null
	}

	suspend fun getUserByMobileAndPassword(
		mobile: String,
		password: String
	): MutableList<UserData> {
		return mutableListOf()
	}

	suspend fun insertAddressToUser(newAddress: UserData.Address, userId: String) {}

	suspend fun updateAddressOfUser(newAddress: UserData.Address, userId: String) {}

	suspend fun deleteAddressOfUser(addressId: String, userId: String) {}

	suspend fun insertCartItem(newItem: UserData.CartItem, userId: String) {}

	suspend fun updateCartItem(item: UserData.CartItem, userId: String) {}

	suspend fun deleteCartItem(itemId: String, userId: String) {}

	suspend fun placeOrder(newOrder: UserData.OrderItem, userId: String) {}

	suspend fun setStatusOfOrderByUserId(orderId: String, userId: String, status: String) {}

	suspend fun clearAllUsers() {}

	suspend fun getOrdersByUserId(userId: String): Result<List<UserData.OrderItem>?>

	suspend fun getAddressesByUserId(userId: String): Result<List<UserData.Address>?>
}