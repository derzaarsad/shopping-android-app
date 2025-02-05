package com.vishalgaur.shoppingapp.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "inventories")
data class Inventory @JvmOverloads constructor(
	@PrimaryKey
	var inventoryId: String = "",
	var supplierName: String = "",
	var ownerId: String = "",
	var productName: String = "",
	var sellerId: String = "",
	var purchasePrice: Double = 0.0,
	var orderNumber: String = "",
	var sku: String = "",
	var minSellPrice: Double = 0.0,
	var quantity: Double = 0.0,
	val expiryDate: String = "",
	var description: String = "",
	var images: List<String> = ArrayList(),
	var unit: String = ""
) : Parcelable {
	fun toHashMap(): HashMap<String, Any> {
		return hashMapOf(
			"inventoryId" to inventoryId,
			"supplierName" to supplierName,
			"ownerId" to ownerId,
			"productName" to productName,
			"sellerId" to sellerId,
			"purchasePrice" to purchasePrice,
			"orderNumber" to orderNumber,
			"sku" to sku,
			"minSellPrice" to minSellPrice,
			"quantity" to quantity,
			"expiryDate" to expiryDate,
			"description" to description,
			"images" to images,
			"unit" to unit
		)
	}
}