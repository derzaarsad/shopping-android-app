package com.vishalgaur.shoppingapp.data.source.remote

import com.vishalgaur.shoppingapp.data.Inventory
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
	val userId: String
)

data class CartItemData(
	val cartItem: UserData.CartItem,
	val userId: String
)

data class InventoryData(
	val inventoryId: String
)

interface KomodiAPI {

	@POST("getAccessToken")
	suspend fun getUserByMobileAndPassword(@Body body: LoginData): MutableList<UserData>

	@POST("getAllProductsByOwner")
	suspend fun getAllInventoriesBySellerId(@Body body: AccessData): List<Inventory>

	@POST("getAllProductsByOwner")
	suspend fun getAllInventories(@Body body: AccessData): List<Inventory>

	@POST("getProductById")
	suspend fun getInventoryById(@Body body: InventoryData): Inventory

	@POST("insertCartItem")
	suspend fun insertCartItem(@Body body: CartItemData): AccessData

	@POST("getAddressesByUserId")
	suspend fun getAddressesByUserId(@Body body: AccessData): List<UserData.Address>

	@GET("getSuppliers")
	suspend fun getSuppliers(): List<String>

	@GET("getProductCategories")
	suspend fun getProductCategories(): List<String>
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
