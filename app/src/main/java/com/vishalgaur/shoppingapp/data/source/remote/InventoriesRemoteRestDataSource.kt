package com.vishalgaur.shoppingapp.data.source.remote

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.Supplier
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.source.InventoryDataSource
import kotlinx.coroutines.tasks.await

class InventoriesRemoteRestDataSource : InventoryDataSource {
	private val firebaseDb: FirebaseFirestore = Firebase.firestore
	private val firebaseStorage: FirebaseStorage = Firebase.storage

	private val observableInventories = MutableLiveData<Result<List<Inventory>>?>()

	private fun storageRef() = firebaseStorage.reference
	private fun inventoriesCollectionRef() = firebaseDb.collection(INVENTORY_COLLECTION)

	override fun observeInventories(): LiveData<Result<List<Inventory>>?> {
		return observableInventories
	}

	override suspend fun getInventoriesBySellerId(sellerId: String): Result<List<Inventory>> {
		val resRef = UserNetwork.retrofit.getInventoriesBySellerId(AccessData(sellerId))
		return Success(resRef)
	}

	suspend fun insertInventory(newInventory: InsertInventoryData): Inventory? {
		try {
			val resRef = UserNetwork.retrofit.insertInventory(newInventory)
			return resRef
		} catch (e: Exception) {
			return null
		}
	}

	suspend fun updateInventory(updateInventory: UpdateInventoryData): Inventory? {
		try {
			val resRef = UserNetwork.retrofit.updateInventory(updateInventory)
			return resRef
		} catch (e: Exception) {
			return null
		}
	}

	override suspend fun getInventoryById(inventoryId: String): Result<Inventory> {
		try {
			val resRef = UserNetwork.retrofit.getInventoryById(InventoryIdData(inventoryId))
			return Success(resRef)
		} catch (e: Exception) {
			return Error(Exception("Inventory with id: $inventoryId Not Found!"))
		}
	}

	override suspend fun deleteInventory(inventoryId: String) {
		Log.d(TAG, "onDeleteProduct: delete inventory with Id: $inventoryId initiated")
		val resRef = inventoriesCollectionRef().whereEqualTo(INVENTORY_ID_FIELD, inventoryId).get().await()
		if (!resRef.isEmpty) {
			val product = resRef.documents[0].toObject(Inventory::class.java)
			val imgUrls = product?.images

			//deleting images first
			imgUrls?.forEach { imgUrl ->
				deleteImage(imgUrl)
			}

			//deleting doc containing product
			val docId = resRef.documents[0].id
			inventoriesCollectionRef().document(docId).delete().addOnSuccessListener {
				Log.d(TAG, "onDelete: DocumentSnapshot successfully deleted!")
			}.addOnFailureListener { e ->
				Log.w(TAG, "onDelete: Error deleting document", e)
			}
		} else {
			Log.d(TAG, "onDeleteInventory: inventory with id: $inventoryId not found!")
		}
	}

	suspend fun getProductCategories(): List<String> = UserNetwork.retrofit.getProductCategories()

	suspend fun getProducts(): List<Product> = UserNetwork.retrofit.getProducts()

	suspend fun getSuppliers(): List<Supplier> = UserNetwork.retrofit.getSuppliers()

	suspend fun insertProductCategory(name: String) {
		UserNetwork.retrofit.insertProductCategory(ProductCategoryData(name))
	}

	suspend fun insertSupplier(supplierName: String,addressId: String) {
		UserNetwork.retrofit.insertSupplier(SupplierData(supplierName,addressId))
	}

	suspend fun insertProduct(productName: String,description: String,upc: String,unit: String,categoryName: String) {
		UserNetwork.retrofit.insertProduct(ProductData(productName,description,upc,unit,categoryName))
	}

	override suspend fun uploadImage(uri: Uri, fileName: String): Uri? {
		val imgRef = storageRef().child("$SHOES_STORAGE_PATH/$fileName")
		val uploadTask = imgRef.putFile(uri)
		val uriRef = uploadTask.continueWithTask { task ->
			if (!task.isSuccessful) {
				task.exception?.let { throw it }
			}
			imgRef.downloadUrl
		}
		return uriRef.await()
	}

	override fun deleteImage(imgUrl: String) {
		val ref = firebaseStorage.getReferenceFromUrl(imgUrl)
		ref.delete().addOnSuccessListener {
			Log.d(TAG, "onDelete: image deleted successfully!")
		}.addOnFailureListener { e ->
			Log.d(TAG, "onDelete: Error deleting image, error: $e")
		}
	}

	override fun revertUpload(fileName: String) {
		val imgRef = storageRef().child("${SHOES_STORAGE_PATH}/$fileName")
		imgRef.delete().addOnSuccessListener {
			Log.d(TAG, "onRevert: File with name: $fileName deleted successfully!")
		}.addOnFailureListener { e ->
			Log.d(TAG, "onRevert: Error deleting file with name = $fileName, error: $e")
		}
	}

	companion object {
		private const val INVENTORY_COLLECTION = "inventories"
		private const val INVENTORY_ID_FIELD = "inventoryId"
		private const val SHOES_STORAGE_PATH = "Shoes"
		private const val TAG = "InventoriesRemoteSource"
	}
}