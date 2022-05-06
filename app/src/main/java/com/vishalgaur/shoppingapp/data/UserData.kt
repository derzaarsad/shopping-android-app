package com.vishalgaur.shoppingapp.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.vishalgaur.shoppingapp.data.utils.ObjectListTypeConvertor
import com.vishalgaur.shoppingapp.data.utils.OrderStatus
import com.vishalgaur.shoppingapp.data.utils.UserType
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.ArrayList

@Parcelize
@Entity(tableName = "users")
data class UserData(
	@PrimaryKey
	var userId: String = "",
	var name: String = "",
	var mobile: String = "",
	var email: String = "",
	var password: String = "",
	@TypeConverters(ObjectListTypeConvertor::class)
	var addresses: List<Address> = ArrayList(),
	@TypeConverters(ObjectListTypeConvertor::class)
	var cart: List<CartItem> = ArrayList(),
	@TypeConverters(ObjectListTypeConvertor::class)
	var orders: List<OrderItem> = ArrayList(),
	var userType: String = UserType.CUSTOMER.name
) : Parcelable {
	fun toHashMap(): HashMap<String, Any> {
		return hashMapOf(
			"userId" to userId,
			"name" to name,
			"email" to email,
			"mobile" to mobile,
			"password" to password,
			"addresses" to addresses.map { it.toHashMap() },
			"userType" to userType
		)
	}

	@Parcelize
	data class OrderItem(
		var orderId: String = "",
		var customerId: String = "",
		var items: List<CartItem> = ArrayList(),
		var itemsPrices: Map<String, Double> = mapOf(),
		var deliveryAddress: Address = Address(),
		var shippingCharges: Double = 0.0,
		var paymentMethod: String = "",
		var orderDate: Date = Date(),
		var status: String = OrderStatus.CONFIRMED.name
	) : Parcelable {
		fun toHashMap(): HashMap<String, Any> {
			return hashMapOf(
				"orderId" to orderId,
				"customerId" to customerId,
				"items" to items.map { it.toHashMap() },
				"itemsPrices" to itemsPrices,
				"deliveryAddress" to deliveryAddress.toHashMap(),
				"shippingCharges" to shippingCharges,
				"paymentMethod" to paymentMethod,
				"orderDate" to orderDate,
				"status" to status
			)
		}
	}

	@Parcelize
	data class Address(
		var addressId: String = "",
		var name: String = "",
		var streetAddress: String = "",
		var streetAddress2: String = "",
		var city: String = "",
		var state: String = "",
		var zipCode: String = "",
		var phoneNumber: String = "",
		var userType: String = UserType.CUSTOMER.name
	) : Parcelable {
		fun toHashMap(): HashMap<String, String> {
			return hashMapOf(
				"addressId" to addressId,
				"name" to name,
				"streetAddress" to streetAddress,
				"streetAddress2" to streetAddress2,
				"city" to city,
				"state" to state,
				"zipCode" to zipCode,
				"phoneNumber" to phoneNumber,
				"userType" to userType
			)
		}
	}

	@Parcelize
	data class CartItem(
		var itemId: String = "",
		var inventoryId: String = "",
		var sellerId: String = "",
		var quantity: Int = 0,
		var color: String?,
		var size: Int?
	) : Parcelable {
		constructor() : this("", "", "", 0, "NA", -1)

		fun toHashMap(): HashMap<String, Any> {
			val hashMap = hashMapOf<String, Any>()
			hashMap["itemId"] = itemId
			hashMap["inventoryId"] = inventoryId
			hashMap["sellerId"] = sellerId
			hashMap["quantity"] = quantity
			if (color != null)
				hashMap["color"] = color!!
			if (size != null)
				hashMap["size"] = size!!
			return hashMap
		}
	}
}