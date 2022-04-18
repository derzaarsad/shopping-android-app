package com.vishalgaur.shoppingapp.data.source.remote

import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.UserData
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class LoginData(
	val mobile: String,
	val password: String
)

data class AccessData(
	val ownerId: String
)

data class CartItemData(
	val cartItem: UserData.CartItem,
	val userId: String
)

interface KomodiAPI {

	@POST("getAccessToken")
	suspend fun getUserByMobileAndPassword(@Body body: LoginData): MutableList<UserData>
	@POST("getAccessToken")
	suspend fun getUserByMobile(@Body body: LoginData): UserData

	@POST("getAllProductsByOwner")
	suspend fun getAllProductsByOwner(@Body body: AccessData): List<Product>

	@POST("insertCartItem")
	suspend fun insertCartItem(@Body body: CartItemData): AccessData
}

object UserNetwork {

	val retrofit by lazy {
		Retrofit.Builder()
			.baseUrl("https://5a2mwt9wb2.execute-api.eu-central-1.amazonaws.com/v2/")
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(KomodiAPI::class.java)
	}
}
