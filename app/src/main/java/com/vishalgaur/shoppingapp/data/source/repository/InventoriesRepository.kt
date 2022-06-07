package com.vishalgaur.shoppingapp.data.source.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import com.vishalgaur.shoppingapp.ERR_UPLOAD
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.Supplier
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.*
import com.vishalgaur.shoppingapp.data.source.local.InventoriesLocalDataSource
import com.vishalgaur.shoppingapp.data.source.remote.InsertInventoryData
import com.vishalgaur.shoppingapp.data.source.remote.InventoriesRemoteRestDataSource
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.util.*

class InventoriesRepository(
	private val inventoriesRemoteSource: InventoriesRemoteRestDataSource,
	private val inventoriesLocalSource: InventoriesLocalDataSource
) : InventoriesRepoInterface {

	companion object {
		private const val TAG = "InventoriesRepository"
	}

	override fun observeInventories(): LiveData<Result<List<Inventory>>?> {
		return inventoriesLocalSource.observeInventories()
	}

	override fun observeInventoriesBySellerId(sellerId: String): LiveData<Result<List<Inventory>>?> {
		return inventoriesLocalSource.observeInventoriesBySellerId(sellerId)
	}

	override suspend fun getInventoriesBySellerId(sellerId: String): Result<List<Inventory>> {
		return inventoriesLocalSource.getInventoriesBySellerId(sellerId)
	}

	override suspend fun getInventoryById(inventoryId: String, forceUpdate: Boolean): Result<Inventory> {
		if (forceUpdate) {
			updateInventoryFromRemoteSource(inventoryId)
		}
		return inventoriesLocalSource.getInventoryById(inventoryId)
	}

	override suspend fun insertInventory(newInventory: InsertInventoryData): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onInsertInventory: adding inventory to remote source")
				inventoriesRemoteSource.insertInventory(newInventory)
			}
			try {
				val insertedInventory = remoteRes.await()
				if(insertedInventory == null) {
					throw Exception("Insert inventory failed")
				}
				val localRes = async {
					Log.d(TAG, "onInsertInventory: adding inventory to local source with id " + insertedInventory.inventoryId)
					inventoriesLocalSource.insertOrReplaceInventory(insertedInventory)
				}
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun insertImages(imgList: List<Uri>): List<String> {
		var urlList = mutableListOf<String>()
		imgList.forEach label@{ uri ->
			val uniId = UUID.randomUUID().toString()
			val fileName = uniId + uri.lastPathSegment?.split("/")?.last()
			try {
				val downloadUrl = inventoriesRemoteSource.uploadImage(uri, fileName)
				urlList.add(downloadUrl.toString())
			} catch (e: Exception) {
				inventoriesRemoteSource.revertUpload(fileName)
				Log.d(TAG, "exception: message = $e")
				urlList = mutableListOf()
				urlList.add(ERR_UPLOAD)
				return@label
			}
		}
		return urlList
	}

	override suspend fun updateInventory(inventory: Inventory): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onUpdate: updating inventory in remote source")
				inventoriesRemoteSource.updateInventory(inventory)
			}
			val localRes = async {
				Log.d(TAG, "onUpdate: updating inventory in local source")
				inventoriesLocalSource.insertOrReplaceInventory(inventory)
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

	override suspend fun updateImages(newList: List<Uri>, oldList: List<String>): List<String> {
		var urlList = mutableListOf<String>()
		newList.forEach label@{ uri ->
			if (!oldList.contains(uri.toString())) {
				val uniId = UUID.randomUUID().toString()
				val fileName = uniId + uri.lastPathSegment?.split("/")?.last()
				try {
					val downloadUrl = inventoriesRemoteSource.uploadImage(uri, fileName)
					urlList.add(downloadUrl.toString())
				} catch (e: Exception) {
					inventoriesRemoteSource.revertUpload(fileName)
					Log.d(TAG, "exception: message = $e")
					urlList = mutableListOf()
					urlList.add(ERR_UPLOAD)
					return@label
				}
			} else {
				urlList.add(uri.toString())
			}
		}
		oldList.forEach { imgUrl ->
			if (!newList.contains(imgUrl.toUri())) {
				inventoriesRemoteSource.deleteImage(imgUrl)
			}
		}
		return urlList
	}

	override suspend fun deleteInventoryById(inventoryId: String): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onDelete: deleting inventory from remote source")
				inventoriesRemoteSource.deleteInventory(inventoryId)
			}
			val localRes = async {
				Log.d(TAG, "onDelete: deleting inventory from local source")
				inventoriesLocalSource.deleteInventory(inventoryId)
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

	override suspend fun updateLocalInventoriesFromRemote(sellerId: String): StoreDataStatus? {
		Log.d(TAG, "Updating Inventories in Room")
		var res: StoreDataStatus? = null
		try {
			val remoteProducts = inventoriesRemoteSource.getInventoriesBySellerId(sellerId)
			if (remoteProducts is Success) {
				Log.d(TAG, "inv list = ${remoteProducts.data}")
				inventoriesLocalSource.deleteAllInventories()
				inventoriesLocalSource.insertMultipleInventories(remoteProducts.data)
				res = StoreDataStatus.DONE
			} else {
				res = StoreDataStatus.ERROR
				if (remoteProducts is Error)
					throw remoteProducts.exception
			}
		} catch (e: Exception) {
			Log.d(TAG, "onUpdateLocalInventoriesFromRemote: Exception occurred, ${e.message}")
		}

		return res
	}

	private suspend fun updateInventoryFromRemoteSource(inventoryId: String): StoreDataStatus? {
		var res: StoreDataStatus? = null
		try {
			val remoteProduct = inventoriesRemoteSource.getInventoryById(inventoryId)
			if (remoteProduct is Success) {
				inventoriesLocalSource.insertOrReplaceInventory(remoteProduct.data)
				res = StoreDataStatus.DONE
			} else {
				res = StoreDataStatus.ERROR
				if (remoteProduct is Error)
					throw remoteProduct.exception
			}
		} catch (e: Exception) {
			Log.d(TAG, "onUpdateInventoryFromRemoteSource: Exception occurred, ${e.message}")
		}
		return res
	}

	override suspend fun getProductCategories(): List<String>? {
		var queryResult = listOf<String>()
		try {
			queryResult = inventoriesRemoteSource.getProductCategories()
			Log.d(InventoriesRepository.TAG,"getting product categories success: " + queryResult.size + " product categories found")
		} catch (e: Exception) {
			Log.d(InventoriesRepository.TAG,"Error on getting product categories: " + e.toString())
		}
		return if (queryResult.size > 0) {
			queryResult
		} else {
			null
		}
	}

	override suspend fun getProducts(): List<Product>? {
		var queryResult = listOf<Product>()
		try {
			queryResult = inventoriesRemoteSource.getProducts()
			Log.d(InventoriesRepository.TAG,"getting products success: " + queryResult.size + " products found")
		} catch (e: Exception) {
			Log.d(InventoriesRepository.TAG,"Error on getting products: " + e.toString())
		}
		return if (queryResult.size > 0) {
			queryResult
		} else {
			null
		}
	}

	override suspend fun getSuppliers(): List<Supplier>? {
		var queryResult = listOf<Supplier>()
		try {
			queryResult = inventoriesRemoteSource.getSuppliers()
			Log.d(InventoriesRepository.TAG,"getting suppliers success: " + queryResult.size + " suppliers found")
		} catch (e: Exception) {
			Log.d(InventoriesRepository.TAG,"Error on getting suppliers: " + e.toString())
		}
		return if (queryResult.size > 0) {
			queryResult
		} else {
			null
		}
	}

	override suspend fun insertProductCategory(
		name: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(InventoriesRepository.TAG, "onInsertProductCategory: adding product category to remote source")
				inventoriesRemoteSource.insertProductCategory(name)
			}
			try {
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun insertSupplier(
		supplierName: String,addressId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(InventoriesRepository.TAG, "onInsertSupplier: adding supplier to remote source")
				inventoriesRemoteSource.insertSupplier(supplierName,addressId)
			}
			try {
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun insertProduct(
		productName: String,description: String,upc: String,unit: String,categoryName: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(InventoriesRepository.TAG, "onInsertProduct: adding product to remote source")
				inventoriesRemoteSource.insertProduct(productName,description,upc,unit,categoryName)
			}
			try {
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}
}