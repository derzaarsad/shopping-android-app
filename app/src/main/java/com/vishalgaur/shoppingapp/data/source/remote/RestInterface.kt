package com.vishalgaur.shoppingapp.data.source.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object UserNetwork {

	val retrofit by lazy {
		Retrofit.Builder()
			.baseUrl("https://5a2mwt9wb2.execute-api.eu-central-1.amazonaws.com/v1/")
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(UserAPI::class.java)
	}
}
