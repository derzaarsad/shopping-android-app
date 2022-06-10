package com.vishalgaur.shoppingapp.data.source.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.source.remote.CartItemData
import com.vishalgaur.shoppingapp.data.utils.SignUpErrors

interface AuthRepoInterface {
	suspend fun refreshData()
	suspend fun signUp(userData: UserData)
	fun login(userData: UserData, rememberMe: Boolean)
	suspend fun checkEmailAndMobile(email: String, mobile: String, context: Context): SignUpErrors?
	suspend fun checkLogin(mobile: String, password: String): UserData?
	suspend fun signOut()
	suspend fun refreshUserDataFromRemote()
	suspend fun insertAddressToUser(newAddress: UserData.Address, userId: String): Result<Boolean>
	suspend fun updateAddressOfUser(newAddress: UserData.Address, userId: String): Result<Boolean>
	suspend fun deleteAddressOfUser(addressId: String, userId: String): Result<Boolean>
	suspend fun insertCartItemByUserId(cartItem: CartItemData): Result<Boolean>
	suspend fun updateCartItemByUserId(cartItem: UserData.CartItem, userId: String): Result<Boolean>
	suspend fun deleteCartItemByUserId(itemId: String, userId: String): Result<Boolean>
	suspend fun placeOrder(newOrder: UserData.OrderItem, userId: String): Result<Boolean>
	suspend fun setStatusOfOrder(orderId: String, userId: String, status: String): Result<Boolean>
	suspend fun getOrdersByUserIdFromLocalSource(userId: String): Result<List<UserData.OrderItem>?>
	suspend fun getAddressesByUserIdFromLocalSource(userId: String): Result<List<UserData.Address>?>
	suspend fun getUserDataFromLocalSource(userId: String): UserData?
	fun getFirebaseAuth(): FirebaseAuth
	fun signInWithPhoneAuthCredential(
		credential: PhoneAuthCredential,
		isUserLoggedIn: MutableLiveData<Boolean>,
		context: Context
	)

	fun isRememberMeOn(): Boolean
}
