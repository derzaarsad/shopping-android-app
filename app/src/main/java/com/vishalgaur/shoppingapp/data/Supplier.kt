package com.vishalgaur.shoppingapp.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Supplier @JvmOverloads constructor(
	var supplierId: String = "",
	var name: String = ""
) : Parcelable {
	fun toHashMap(): HashMap<String, Any> {
		return hashMapOf(
			"supplierId" to supplierId,
			"name" to name
		)
	}
}
