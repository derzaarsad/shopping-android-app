package com.vishalgaur.shoppingapp.data.source

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.Supplier
import com.vishalgaur.shoppingapp.data.Result

interface InventoryDataSource {

	fun observeInventories(): LiveData<Result<List<Inventory>>?>

	suspend fun getInventoryById(inventoryId: String): Result<Inventory>

	suspend fun updateInventory(invData: Inventory)

	fun observeInventoriesBySellerId(sellerId: String): LiveData<Result<List<Inventory>>?> {
		return MutableLiveData()
	}

	suspend fun getAllInventoriesBySellerId(sellerId: String): Result<List<Inventory>>

	suspend fun uploadImage(uri: Uri, fileName: String): Uri? {
		return null
	}

	fun revertUpload(fileName: String) {}
	fun deleteImage(imgUrl: String) {}
	suspend fun deleteInventory(inventoryId: String)
	suspend fun deleteAllInventories() {}
	suspend fun insertMultipleInventories(data: List<Inventory>) {}
}