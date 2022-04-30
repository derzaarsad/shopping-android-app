package com.vishalgaur.shoppingapp.data.utils

import java.util.*

enum class SignUpErrors { NONE, SERR }

enum class LogInErrors { NONE, LERR }

enum class AddInventoryErrors { NONE, ERR_ADD, ERR_ADD_IMG, ADDING }

enum class AddObjectStatus { DONE, ERR_ADD, ADDING }

enum class UserType { ADMIN, SELLER, CUSTOMER, SUPPLIER }

enum class OrderStatus { CONFIRMED, PACKAGING, PACKED, SHIPPING, SHIPPED, ARRIVING, DELIVERED }

enum class StoreDataStatus { LOADING, ERROR, DONE }

fun getISOCountriesMap(): Map<String, String> {
	val result = mutableMapOf<String, String>()
	val isoCountries = Locale.getISOCountries()
	val countriesList = isoCountries.map { isoCountry ->
		result[isoCountry] = Locale("", isoCountry).displayCountry
	}
	return result
}

fun getProvinces(): List<String> {
	return listOf(
		"ACEH",
		"SUMATERA UTARA",
		"SUMATERA BARAT",
		"RIAU",
		"JAMBI",
		"SUMATERA SELATAN",
		"BENGKULU",
		"LAMPUNG",
		"KEPULAUAN BANGKA BELITUNG",
		"KEPULAUAN RIAU",
		"DKI JAKARTA",
		"JAWA BARAT",
		"JAWA TENGAH",
		"DI YOGYAKARTA",
		"JAWA TIMUR",
		"BANTEN",
		"BALI",
		"NUSA TENGGARA BARAT",
		"NUSA TENGGARA TIMUR",
		"KALIMANTAN BARAT",
		"KALIMANTAN TENGAH",
		"KALIMANTAN SELATAN",
		"KALIMANTAN TIMUR",
		"KALIMANTAN UTARA",
		"SULAWESI UTARA",
		"SULAWESI TENGAH",
		"SULAWESI SELATAN",
		"SULAWESI TENGGARA",
		"GORONTALO",
		"SULAWESI BARAT",
		"MALUKU",
		"MALUKU UTARA",
		"PAPUA",
		"PAPUA BARAT"
	)
}

fun getDefaultProvince(): String {
	return getProvinces()[11]
}
