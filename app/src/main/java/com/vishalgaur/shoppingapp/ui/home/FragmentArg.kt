package com.vishalgaur.shoppingapp.ui.home

import com.beust.klaxon.Json

data class AdminToSelectAddressArg(
	@Json(name = "supplierName")
	val supplierName: String
)

data class SelectAddressToAdminArg(
	@Json(name = "supplierName")
	val supplierName: String,
	@Json(name = "addressId")
	val addressId: String?,
	@Json(name = "completeAddressText")
	val completeAddressText: String?
)
