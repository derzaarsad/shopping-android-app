package com.vishalgaur.shoppingapp.data.source.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.source.UserDataSource
import com.vishalgaur.shoppingapp.data.source.repository.AuthRepository
import com.vishalgaur.shoppingapp.data.utils.EmailMobileData
import com.vishalgaur.shoppingapp.data.utils.OrderStatus
import kotlinx.coroutines.tasks.await

class AuthRemoteRestDataSource : UserDataSource {
	private val firebaseDb: FirebaseFirestore = Firebase.firestore

	private fun usersCollectionRef() = firebaseDb.collection(USERS_COLLECTION)
	private fun allEmailsMobilesRef() =
		firebaseDb.collection(USERS_COLLECTION).document(EMAIL_MOBILE_DOC)


	override suspend fun getUserById(userId: String): UserData? {
		try {
			var resRef = UserNetwork.retrofit.getUserById(AccessData(userId))
			resRef.orders = UserNetwork.retrofit.getOrdersByUserId(AccessData(userId))
			resRef.cart = getCartItemsBySellerId(AccessData(userId))
			return resRef
		} catch (e: Exception) {
			Log.d(TAG,"Error on authorization: " + e.toString())
			return null
		}
	}


	override suspend fun addUser(userData: UserData) {
		usersCollectionRef().add(userData.toHashMap())
			.addOnSuccessListener {
				Log.d(TAG, "Doc added")
			}
			.addOnFailureListener { e ->
				Log.d(TAG, "firestore error occurred: $e")
			}
	}

	suspend fun getOrdersByUserId(userId: String): Result<List<UserData.OrderItem>?> {
		try {
			val ordersRef = UserNetwork.retrofit.getOrdersByUserId(AccessData(userId))
			return Success(ordersRef)
		} catch (e: Exception) {
			return Error(Exception("Cannot receive order!"))
		}
	}

	override suspend fun getMemberAddressesByUserId(userId: String): Result<List<UserData.Address>?> {
		try {
			val resRef = UserNetwork.retrofit.getAddressesByUserId(AccessData(userId))
			return Success(resRef)
		} catch (e: Exception) {
			return Error(Exception("User Not Found!"))
		}
	}

	override suspend fun getUserByMobileAndPassword(
		mobile: String,
		password: String
	): UserData? {
		try {
			val idRef = UserNetwork.retrofit.getUserByMobileAndPassword(LoginData(mobile,password))
			if(idRef == "0") {
				Log.d(TAG,"User Not Found!")
				return null
			}
			return getUserById(idRef)
		} catch (e: Exception) {
			Log.d(TAG,"Error on authorization: " + e.toString())
			return null
		}
	}

	override suspend fun insertAddress(newAddress: UserData.Address) {
		UserNetwork.retrofit.insertAddress(newAddress)
	}

	override suspend fun updateAddressOfUser(newAddress: UserData.Address, userId: String) {
		// TODO: legacy
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val oldAddressList =
				userRef.documents[0].toObject(UserData::class.java)?.memberAddresses?.toMutableList()
			val idx = oldAddressList?.indexOfFirst { it.addressId == newAddress.addressId } ?: -1
			if (idx != -1) {
				oldAddressList?.set(idx, newAddress)
			}
			usersCollectionRef().document(docId)
				.update(USERS_ADDRESSES_FIELD, oldAddressList?.map { it.toHashMap() })
		}
	}

	override suspend fun deleteAddressOfUser(addressId: String, userId: String) {
		// TODO: legacy
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val oldAddressList =
				userRef.documents[0].toObject(UserData::class.java)?.memberAddresses?.toMutableList()
			val idx = oldAddressList?.indexOfFirst { it.addressId == addressId } ?: -1
			if (idx != -1) {
				oldAddressList?.removeAt(idx)
			}
			usersCollectionRef().document(docId)
				.update(USERS_ADDRESSES_FIELD, oldAddressList?.map { it.toHashMap() })
		}
	}

	suspend fun getCartItemsBySellerId(accessData: AccessData): List<UserData.CartItem> = UserNetwork.retrofit.getCartItemsBySellerId(accessData)

	suspend fun insertCartItem(newItem: CartItemData): UserData.CartItem? {
		try {
			val resRef = UserNetwork.retrofit.insertCartItem(newItem)
			return resRef
		} catch (e: Exception) {
			return null
		}
	}

	override suspend fun updateCartItem(item: UserData.CartItem, userId: String) {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val oldCart =
				userRef.documents[0].toObject(UserData::class.java)?.cart?.toMutableList()
			val idx = oldCart?.indexOfFirst { it.inventoryId == item.inventoryId } ?: -1
			if (idx != -1) {
				oldCart?.set(idx, item)
			}
			usersCollectionRef().document(docId)
				.update(USERS_CART_FIELD, oldCart?.map { it.toHashMap() })
		}
	}

	override suspend fun deleteCartItem(inventoryId: String, userId: String) {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val oldCart =
				userRef.documents[0].toObject(UserData::class.java)?.cart?.toMutableList()
			val idx = oldCart?.indexOfFirst { it.inventoryId == inventoryId } ?: -1
			if (idx != -1) {
				oldCart?.removeAt(idx)
			}
			usersCollectionRef().document(docId)
				.update(USERS_CART_FIELD, oldCart?.map { it.toHashMap() })
		}
	}

	suspend fun insertOrder(newOrder: InsertOrderData): String = UserNetwork.retrofit.insertOrder(newOrder)

	override suspend fun setStatusOfOrderByUserId(orderId: String, userId: String, status: String) {
		// update on customer and owner
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val ordersList =
				userRef.documents[0].toObject(UserData::class.java)?.orders?.toMutableList()
			val idx = ordersList?.indexOfFirst { it.orderId == orderId } ?: -1
			if (idx != -1) {
				val orderData = ordersList?.get(idx)
				if (orderData != null) {
					usersCollectionRef().document(docId)
						.update(USERS_ORDERS_FIELD, FieldValue.arrayRemove(orderData.toHashMap()))
					orderData.status = status
					usersCollectionRef().document(docId)
						.update(USERS_ORDERS_FIELD, FieldValue.arrayUnion(orderData.toHashMap()))

					// updating customer status
					val custRef =
						usersCollectionRef().whereEqualTo(USERS_ID_FIELD, orderData.customerId)
							.get().await()
					if (!custRef.isEmpty) {
						val did = custRef.documents[0].id
						val orders =
							custRef.documents[0].toObject(UserData::class.java)?.orders?.toMutableList()
						val pos = orders?.indexOfFirst { it.orderId == orderId } ?: -1
						if (pos != -1) {
							val order = orders?.get(pos)
							if (order != null) {
								usersCollectionRef().document(did).update(
									USERS_ORDERS_FIELD,
									FieldValue.arrayRemove(order.toHashMap())
								)
								order.status = status
								usersCollectionRef().document(did).update(
									USERS_ORDERS_FIELD,
									FieldValue.arrayUnion(order.toHashMap())
								)
							}
						}
					}
				}
			}
		}
	}

	override fun updateEmailsAndMobiles(email: String, mobile: String) {
		allEmailsMobilesRef().update(EMAIL_MOBILE_EMAIL_FIELD, FieldValue.arrayUnion(email))
		allEmailsMobilesRef().update(EMAIL_MOBILE_MOB_FIELD, FieldValue.arrayUnion(mobile))
	}

	override suspend fun getEmailsAndMobiles() = allEmailsMobilesRef().get().await().toObject(
		EmailMobileData::class.java
	)

	companion object {
		private const val USERS_COLLECTION = "users"
		private const val USERS_ID_FIELD = "userId"
		private const val USERS_ADDRESSES_FIELD = "addresses"
		private const val USERS_LIKES_FIELD = "likes"
		private const val USERS_CART_FIELD = "cart"
		private const val USERS_ORDERS_FIELD = "orders"
		private const val USERS_MOBILE_FIELD = "mobile"
		private const val USERS_PWD_FIELD = "password"
		private const val EMAIL_MOBILE_DOC = "emailAndMobiles"
		private const val EMAIL_MOBILE_EMAIL_FIELD = "emails"
		private const val EMAIL_MOBILE_MOB_FIELD = "mobiles"
		private const val TAG = "AuthRemoteDataSource"
	}
}