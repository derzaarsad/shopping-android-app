package com.vishalgaur.shoppingapp.data.source.remote

import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.Supplier
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

data class LoginData(
	val mobile: String,
	val password: String
)

data class AccessData(
	val userId: String
)

data class CartItemData(
	val inventoryId: String,
	val sellerId: String,
	val quantity: Double
)

data class InventoryIdData(
	val inventoryId: String
)

data class InsertInventoryData(
	val supplierId: String,
	val ownerId: String,
	val productId: String,
	val sellerId: String,
	val purchasePrice: Double,
	val orderNumber: String,
	val sku: String,
	val minSellPrice: Double,
	val quantity: Double,
	val expiryDate: String
)

data class UpdateInventoryData(
	val inventoryId: String,
	val minSellPrice: Double
)

data class InsertOrderData(
	val sellerId: String,
	val items: List<String>,
	val deliveryAddressId: String,
	val shippingCharges: Double,
	val paymentMethod: String
)

data class ProductCategoryData(
	val name: String
)

data class SupplierData(
	val supplierName: String,
	val addressId: String
)

data class ProductData(
	val productName: String,
	val description: String,
	val upc: String,
	val unit: String,
	val categoryName: String
)

interface KomodiAPI {

	@POST("getAccessToken")
	suspend fun getUserByMobileAndPassword(@Body body: LoginData): MutableList<UserData>

	@POST("getInventoriesBySellerId")
	suspend fun getInventoriesBySellerId(@Body body: AccessData): List<Inventory>

	@POST("getInventoryById")
	suspend fun getInventoryById(@Body body: InventoryIdData): Inventory

	@PUT("insertCartItem")
	suspend fun insertCartItem(@Body body: CartItemData): UserData.CartItem

	@POST("getCartItemsBySellerId")
	suspend fun getCartItemsBySellerId(@Body body: AccessData): List<UserData.CartItem>

	@POST("getAddressesByUserId")
	suspend fun getAddressesByUserId(@Body body: AccessData): List<UserData.Address>

	@GET("getProductCategories")
	suspend fun getProductCategories(): List<String>

	@GET("getProducts")
	suspend fun getProducts(): List<Product>

	@GET("getSuppliers")
	suspend fun getSuppliers(): List<Supplier>

	@PUT("insertProductCategory")
	suspend fun insertProductCategory(@Body body: ProductCategoryData): String

	@PUT("insertSupplier")
	suspend fun insertSupplier(@Body body: SupplierData): String

	@PUT("insertProduct")
	suspend fun insertProduct(@Body body: ProductData): String

	@PUT("insertAddress")
	suspend fun insertAddress(@Body body: UserData.Address): String

	@PUT("insertInventory")
	suspend fun insertInventory(@Body body: InsertInventoryData): Inventory

	@POST("updateInventory")
	suspend fun updateInventory(@Body body: UpdateInventoryData): Inventory

	@PUT("insertOrder")
	suspend fun insertOrder(@Body body: InsertOrderData): String

	@POST("getUserById")
	suspend fun getUserById(@Body body: AccessData): UserData

	@POST("getAllProductsByOwner")
	suspend fun getOrdersByUserId(@Body body: AccessData): List<UserData.OrderItem>
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
