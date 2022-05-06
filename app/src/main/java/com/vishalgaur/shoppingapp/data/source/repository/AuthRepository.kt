package com.vishalgaur.shoppingapp.data.source.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.source.local.UserLocalDataSource
import com.vishalgaur.shoppingapp.data.source.remote.AuthRemoteRestDataSource
import com.vishalgaur.shoppingapp.data.utils.SignUpErrors
import com.vishalgaur.shoppingapp.data.utils.UserType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class AuthRepository(
	private val userLocalDataSource: UserLocalDataSource,
	private val authRemoteDataSource: AuthRemoteRestDataSource,
	private var sessionManager: ShoppingAppSessionManager
) : AuthRepoInterface {

	private var firebaseAuth: FirebaseAuth = Firebase.auth

	companion object {
		private const val TAG = "AuthRepository"
	}

	override fun getFirebaseAuth() = firebaseAuth

	override fun isRememberMeOn() = sessionManager.isRememberMeOn()

	override suspend fun refreshData() {
		Log.d(TAG, "refreshing userdata")
		if (sessionManager.isLoggedIn()) {
			updateUserInLocalSource(sessionManager.getPhoneNumber(),sessionManager.getPassword())
		} else {
			sessionManager.logoutFromSession()
			deleteAllUsersFromLocalSource()
		}
	}

	override suspend fun signUp(userData: UserData) {
		val isAdmin = userData.userType == UserType.ADMIN.name
		sessionManager.createLoginSession(
			userData.userId,
			userData.name,
			userData.mobile,
			userData.password,
			false,
			isAdmin
		)
		Log.d(TAG, "on SignUp: Updating user in Local Source")
		userLocalDataSource.addUser(userData)
		Log.d(TAG, "on SignUp: Updating userdata on Remote Source")
		authRemoteDataSource.addUser(userData)
		authRemoteDataSource.updateEmailsAndMobiles(userData.email, userData.mobile)
	}

	override fun login(userData: UserData, rememberMe: Boolean) {
		val isAdmin = userData.userType == UserType.ADMIN.name
		sessionManager.createLoginSession(
			userData.userId,
			userData.name,
			userData.mobile,
			userData.password,
			rememberMe,
			isAdmin
		)
	}

	override suspend fun checkEmailAndMobile(
		email: String,
		mobile: String,
		context: Context
	): SignUpErrors? {
		Log.d(TAG, "on SignUp: Checking email and mobile")
		var sErr: SignUpErrors? = null
		try {
			val queryResult = authRemoteDataSource.getEmailsAndMobiles()
			if (queryResult != null) {
				val mob = queryResult.mobiles.contains(mobile)
				val em = queryResult.emails.contains(email)
				if (!mob && !em) {
					sErr = SignUpErrors.NONE
				} else {
					sErr = SignUpErrors.SERR
					when {
						!mob && em -> makeErrToast("Email is already registered!", context)
						mob && !em -> makeErrToast("Mobile is already registered!", context)
						mob && em -> makeErrToast(
							"Email and mobile is already registered!",
							context
						)
					}
				}
			}
		} catch (e: Exception) {
			makeErrToast("Some Error Occurred", context)
		}
		return sErr
	}

	override suspend fun checkLogin(mobile: String, password: String): UserData? {
		Log.d(TAG, "on Login: checking mobile and password")
		var queryResult = mutableListOf<UserData>()
		try {
			queryResult = authRemoteDataSource.getUserByMobileAndPassword(mobile, password)
		} catch (e: Exception) {
			Log.d(TAG,"Error on Login: " + e.toString())
		}
		return if (queryResult.size > 0) {
			queryResult[0]
		} else {
			null
		}
	}

	override fun signInWithPhoneAuthCredential(
		credential: PhoneAuthCredential,
		isUserLoggedIn: MutableLiveData<Boolean>, context: Context
	) {
		try {
			firebaseAuth.signInWithCredential(credential)
				.addOnCompleteListener { task ->
					if (task.isSuccessful) {
						Log.d(TAG, "signInWithCredential:success")
						val user = task.result?.user
						if (user != null) {
							isUserLoggedIn.postValue(true)
						}

					} else {
						Log.w(TAG, "signInWithCredential:failure", task.exception)
						if (task.exception is FirebaseAuthInvalidCredentialsException) {
							Log.d(TAG, "createUserWithMobile:failure", task.exception)
							isUserLoggedIn.postValue(false)
							makeErrToast("Wrong OTP!", context)
						}
					}
				}.addOnFailureListener {
					Log.d(TAG, "createUserWithMobile:failure", it)
					isUserLoggedIn.postValue(false)
					makeErrToast("Invalid Request!", context)
				}
		} catch (e: Exception) {
			makeErrToast("Some Error Occurred", context)
		}
	}

	override suspend fun signOut() {
		sessionManager.logoutFromSession()
		firebaseAuth.signOut()
		userLocalDataSource.clearAllUsers()
	}

	private fun makeErrToast(text: String, context: Context) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}

	private suspend fun deleteAllUsersFromLocalSource() {
		userLocalDataSource.clearAllUsers()
	}

	private suspend fun updateUserInLocalSource(phoneNumber: String?,password: String?) {
		coroutineScope {
			launch {
				if (phoneNumber != null && password != null) {
					val getUser = userLocalDataSource.getUserByMobile(phoneNumber)
					if (getUser == null) {
						userLocalDataSource.clearAllUsers()
						val uData = checkLogin(phoneNumber,password)
						if (uData != null) {
							userLocalDataSource.addUser(uData)
						}
					}
				}
			}
		}
	}

	override suspend fun refreshUserDataFromRemote() {
		userLocalDataSource.clearAllUsers()
		val mobile = sessionManager.getPhoneNumber()
		val password = sessionManager.getPassword()
		if (mobile != null && password != null) {
			val uData = checkLogin(mobile,password)
			if (uData != null) {
				userLocalDataSource.addUser(uData)
			}
		}
	}

	override suspend fun insertAddressToUser(
		newAddress: UserData.Address,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onInsertAddressToUser: adding address to remote source")
				authRemoteDataSource.insertAddressToUser(newAddress, userId)
			}
			val localRes = async {
				Log.d(TAG, "onInsertAddressToUser: updating address to local source")
				val userRes = authRemoteDataSource.getUserById(userId)
				if (userRes is Success) {
					userLocalDataSource.clearAllUsers()
					userLocalDataSource.addUser(userRes.data!!)
				} else if (userRes is Error) {
					throw userRes.exception
				}
			}
			try {
				remoteRes.await()
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun updateAddressOfUser(
		newAddress: UserData.Address,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onUpdateAddressOfUser: updating address on remote source")
				authRemoteDataSource.updateAddressOfUser(newAddress, userId)
			}
			val localRes = async {
				Log.d(TAG, "onUpdateAddressOfUser: updating address on local source")
				val userRes =
					authRemoteDataSource.getUserById(userId)
				if (userRes is Success) {
					userLocalDataSource.clearAllUsers()
					userLocalDataSource.addUser(userRes.data!!)
				} else if (userRes is Error) {
					throw userRes.exception
				}
			}
			try {
				remoteRes.await()
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun deleteAddressOfUser(addressId: String, userId: String): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onDeleteAddressOfUser: deleting address from remote source")
				authRemoteDataSource.deleteAddressOfUser(addressId, userId)
			}
			val localRes = async {
				Log.d(TAG, "onDeleteAddressOfUser: deleting address from local source")
				val userRes =
					authRemoteDataSource.getUserById(userId)
				if (userRes is Success) {
					userLocalDataSource.clearAllUsers()
					userLocalDataSource.addUser(userRes.data!!)
				} else if (userRes is Error) {
					throw userRes.exception
				}
			}
			try {
				remoteRes.await()
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun insertCartItemByUserId(
		cartItem: UserData.CartItem,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onInsertCartItem: adding item to remote source")
				authRemoteDataSource.insertCartItem(cartItem, userId)
			}
			val localRes = async {
				Log.d(TAG, "onInsertCartItem: updating item to local source")
				userLocalDataSource.insertCartItem(cartItem, userId)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun updateCartItemByUserId(
		cartItem: UserData.CartItem,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onUpdateCartItem: updating cart item on remote source")
				authRemoteDataSource.updateCartItem(cartItem, userId)
			}
			val localRes = async {
				Log.d(TAG, "onUpdateCartItem: updating cart item on local source")
				userLocalDataSource.updateCartItem(cartItem, userId)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun deleteCartItemByUserId(itemId: String, userId: String): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onDelete: deleting cart item from remote source")
				authRemoteDataSource.deleteCartItem(itemId, userId)
			}
			val localRes = async {
				Log.d(TAG, "onDelete: deleting cart item from local source")
				userLocalDataSource.deleteCartItem(itemId, userId)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun placeOrder(newOrder: UserData.OrderItem, userId: String): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onPlaceOrder: adding item to remote source")
				authRemoteDataSource.placeOrder(newOrder, userId)
			}
			val localRes = async {
				Log.d(TAG, "onPlaceOrder: adding item to local source")
				val userRes = authRemoteDataSource.getUserById(userId)
				if (userRes is Success) {
					userLocalDataSource.clearAllUsers()
					userLocalDataSource.addUser(userRes.data!!)
				} else if (userRes is Error) {
					throw userRes.exception
				}
			}
			try {
				remoteRes.await()
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun setStatusOfOrder(
		orderId: String,
		userId: String,
		status: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onSetStatus: updating status on remote source")
				authRemoteDataSource.setStatusOfOrderByUserId(orderId, userId, status)
			}
			val localRes = async {
				Log.d(TAG, "onSetStatus: updating status on local source")
				userLocalDataSource.setStatusOfOrderByUserId(orderId, userId, status)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun getOrdersByUserIdFromLocalSource(userId: String): Result<List<UserData.OrderItem>?> {
		return userLocalDataSource.getOrdersByUserId(userId)
	}

	override suspend fun getAddressesByUserIdFromLocalSource(userId: String): Result<List<UserData.Address>?> {
		return userLocalDataSource.getAddressesByUserId(userId)
	}

	override suspend fun getUserDataFromLocalSource(userId: String): Result<UserData?> {
		return userLocalDataSource.getUserById(userId)
	}

}