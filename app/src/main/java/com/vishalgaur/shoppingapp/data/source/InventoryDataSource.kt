package com.vishalgaur.shoppingapp.data.source

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Result

interface InventoryDataSource {

	fun observeInventories(): LiveData<Result<List<Inventory>>?>

	suspend fun getInventoryById(inventoryId: String): Result<Inventory>

	suspend fun insertInventory(newInventory: Inventory)

	suspend fun updateInventory(invData: Inventory)

	fun observeInventoriesByStoreId(ownerId: String): LiveData<Result<List<Inventory>>?> {
		return MutableLiveData()
	}

	suspend fun getAllInventoriesByStoreId(ownerId: String): Result<List<Inventory>> {
		return Result.Success(emptyList())
	}

	suspend fun uploadImage(uri: Uri, fileName: String): Uri? {
		return null
	}

	fun revertUpload(fileName: String) {}
	fun deleteImage(imgUrl: String) {}
	suspend fun deleteInventory(inventoryId: String)
	suspend fun deleteAllInventories() {}
	suspend fun insertMultipleInventories(data: List<Inventory>) {}
}