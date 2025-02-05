package com.vishalgaur.shoppingapp.data

import android.content.Context
import android.content.SharedPreferences

class ShoppingAppSessionManager(context: Context) {

	private var userSession: SharedPreferences =
		context.getSharedPreferences("userSessionData", Context.MODE_PRIVATE)
	private var editor: SharedPreferences.Editor = userSession.edit()


	fun createLoginSession(
		id: String,
		name: String,
		mobile: String,
		password: String,
		isRemOn: Boolean,
		isAdmin: Boolean
	) {
		editor.putBoolean(IS_LOGIN, true)
		editor.putString(KEY_ID, id)
		editor.putString(KEY_PASSWORD,password)
		editor.putString(KEY_NAME, name)
		editor.putString(KEY_MOBILE, mobile)
		editor.putBoolean(KEY_REMEMBER_ME, isRemOn)
		editor.putBoolean(KEY_IS_ADMIN, isAdmin)

		editor.commit()
	}

	fun isUserAdmin(): Boolean = userSession.getBoolean(KEY_IS_ADMIN, false)

	fun isRememberMeOn(): Boolean = userSession.getBoolean(KEY_REMEMBER_ME, false)

	fun getPhoneNumber(): String? = userSession.getString(KEY_MOBILE, null)

	fun getPassword(): String? = userSession.getString(KEY_PASSWORD, null)

	fun getUserDataFromSession(): HashMap<String, String?> {
		return hashMapOf(
			KEY_ID to userSession.getString(KEY_ID, null),
			KEY_NAME to userSession.getString(KEY_NAME, null),
			KEY_MOBILE to userSession.getString(KEY_MOBILE, null)
		)
	}

	fun getUserIdFromSession(): String? = userSession.getString(KEY_ID, null)

	fun isLoggedIn(): Boolean = userSession.getBoolean(IS_LOGIN, false)

	fun logoutFromSession() {
		editor.clear()
		editor.commit()
	}

	companion object {
		private const val IS_LOGIN = "isLoggedIn"
		private const val KEY_NAME = "userName"
		private const val KEY_MOBILE = "userMobile"
		private const val KEY_ID = "userId"
		private const val KEY_REMEMBER_ME = "isRemOn"
		private const val KEY_IS_ADMIN = "isAdmin"
		private const val KEY_PASSWORD = "userPassword"
	}
}