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
	var name: String = "",
	var sellerId: String = "",
	var description: String = "",
	var category: String = "",
	var price: Double = 0.0,
	var mrp: Double = 0.0,
	var images: List<String> = ArrayList(),
	var rating: Double = 0.0,
	var quantity: Double = 0.0,
	var unit: String = ""
) : Parcelable {
	fun toHashMap(): HashMap<String, Any> {
		return hashMapOf(
			"inventoryId" to inventoryId,
			"name" to name,
			"sellerId" to sellerId,
			"description" to description,
			"category" to category,
			"price" to price,
			"mrp" to mrp,
			"images" to images,
			"rating" to rating,
			"quantity" to quantity,
			"unit" to unit
		)
	}
}