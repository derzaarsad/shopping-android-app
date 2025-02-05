package com.vishalgaur.shoppingapp.data.source.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.*
import com.vishalgaur.shoppingapp.data.source.InventoryDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InventoriesLocalDataSource internal constructor(
	private val inventoriesDao: InventoriesDao,
	private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : InventoryDataSource {
	override fun observeInventories(): LiveData<Result<List<Inventory>>?> {
		return try {
			Transformations.map(inventoriesDao.observeInventories()) {
				Success(it)
			}
		} catch (e: Exception) {
			Transformations.map(MutableLiveData(e)) {
				Error(e)
			}
		}
	}

	override fun observeInventoriesBySellerId(sellerId: String): LiveData<Result<List<Inventory>>?> {
		return try {
			Transformations.map(inventoriesDao.observeInventoriesBySellerId(sellerId)) {
				Success(it)
			}
		} catch (e: Exception) {
			Transformations.map(MutableLiveData(e)) {
				Error(e)
			}
		}
	}

	override suspend fun getInventoriesByUserId(userId: String): Result<List<Inventory>> =
		withContext(ioDispatcher) {
			return@withContext try {
				Success(inventoriesDao.getInventoriesByUserId(userId))
			} catch (e: Exception) {
				Error(e)
			}
		}

	override suspend fun getInventoryById(inventoryId: String): Result<Inventory> =
		withContext(ioDispatcher) {
			try {
				val inventory = inventoriesDao.getInventoryById(inventoryId)
				if (inventory != null) {
					return@withContext Success(inventory)
				} else {
					return@withContext Error(Exception("Inventory Not Found!"))
				}
			} catch (e: Exception) {
				return@withContext Error(e)
			}
		}

	suspend fun insertOrReplaceInventory(newInventory: Inventory) = withContext(ioDispatcher) {
		inventoriesDao.insertOrReplace(newInventory)
	}

	override suspend fun insertMultipleInventories(data: List<Inventory>) = withContext(ioDispatcher) {
		inventoriesDao.insertListOfInventories(data)
	}

	override suspend fun deleteInventory(inventoryId: String): Unit = withContext(ioDispatcher) {
		inventoriesDao.deleteInventoryById(inventoryId)
	}

	override suspend fun deleteAllInventories() = withContext(ioDispatcher) {
		inventoriesDao.deleteAllInventories()
	}
}