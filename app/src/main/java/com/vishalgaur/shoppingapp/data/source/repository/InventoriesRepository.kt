package com.vishalgaur.shoppingapp.data.source.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import com.vishalgaur.shoppingapp.ERR_UPLOAD
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.*
import com.vishalgaur.shoppingapp.data.source.InventoryDataSource
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.util.*

class InventoriesRepository(
	private val inventoriesRemoteSource: InventoryDataSource,
	private val inventoriesLocalSource: InventoryDataSource
) : InventoriesRepoInterface {

	companion object {
		private const val TAG = "InventoriesRepository"
	}

	override fun observeInventories(): LiveData<Result<List<Inventory>>?> {
		return inventoriesLocalSource.observeInventories()
	}

	override fun observeInventoriesByStoreId(ownerId: String): LiveData<Result<List<Inventory>>?> {
		return inventoriesLocalSource.observeInventoriesByStoreId(ownerId)
	}

	override suspend fun getAllInventoriesByStoreId(ownerId: String): Result<List<Inventory>> {
		return inventoriesLocalSource.getAllInventoriesByStoreId(ownerId)
	}

	override suspend fun getInventoryById(inventoryId: String, forceUpdate: Boolean): Result<Inventory> {
		if (forceUpdate) {
			updateInventoryFromRemoteSource(inventoryId)
		}
		return inventoriesLocalSource.getInventoryById(inventoryId)
	}

	override suspend fun insertInventory(newInventory: Inventory): Result<Boolean> {
		return supervisorScope {
			val localRes = async {
				Log.d(TAG, "onInsertInventory: adding inventory to local source")
				inventoriesLocalSource.insertInventory(newInventory)
			}
			val remoteRes = async {
				Log.d(TAG, "onInsertInventory: adding inventory to remote source")
				inventoriesRemoteSource.insertInventory(newInventory)
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
				inventoriesLocalSource.insertInventory(inventory)
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

	override suspend fun updateLocalInventoriesFromRemote(ownerId: String): StoreDataStatus? {
		Log.d(TAG, "Updating Inventories in Room")
		var res: StoreDataStatus? = null
		try {
			val remoteProducts = inventoriesRemoteSource.getAllInventoriesByStoreId(ownerId)
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
				inventoriesLocalSource.insertInventory(remoteProduct.data)
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
}