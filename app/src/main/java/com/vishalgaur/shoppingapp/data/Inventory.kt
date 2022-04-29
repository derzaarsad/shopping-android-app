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
	var storeId: String = "",
	var description: String = "",
	var category: String = "",
	var price: Double = 0.0,
	var mrp: Double = 0.0,
	var availableSizes: List<Int> = ArrayList(),
	var availableColors: List<String> = ArrayList(),
	var images: List<String> = ArrayList(),
	var rating: Double = 0.0
) : Parcelable {
	fun toHashMap(): HashMap<String, Any> {
		return hashMapOf(
			"inventoryId" to inventoryId,
			"name" to name,
			"storeId" to storeId,
			"description" to description,
			"category" to category,
			"price" to price,
			"mrp" to mrp,
			"availableSizes" to availableSizes,
			"availableColors" to availableColors,
			"images" to images,
			"rating" to rating
		)
	}
}