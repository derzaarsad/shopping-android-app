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

	suspend fun getSuppliers(): List<String> {
		return listOf()
	}

	suspend fun getProductCategories(): List<String> {
		return listOf()
	}

	suspend fun insertAddress(newAddress: UserData.Address, userId: String) {}

	suspend fun updateAddress(newAddress: UserData.Address, userId: String) {}

	suspend fun deleteAddress(addressId: String, userId: String) {}

	suspend fun insertCartItem(newItem: UserData.CartItem, userId: String) {}

	suspend fun updateCartItem(item: UserData.CartItem, userId: String) {}

	suspend fun deleteCartItem(itemId: String, userId: String) {}

	suspend fun placeOrder(newOrder: UserData.OrderItem, userId: String) {}

	suspend fun setStatusOfOrderByUserId(orderId: String, userId: String, status: String) {}

	suspend fun clearUser() {}

	suspend fun getOrdersByUserId(userId: String): Result<List<UserData.OrderItem>?>

	suspend fun getAddressesByUserId(userId: String): Result<List<UserData.Address>?>
}