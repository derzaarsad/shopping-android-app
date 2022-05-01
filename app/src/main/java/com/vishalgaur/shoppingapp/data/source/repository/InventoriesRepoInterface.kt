package com.vishalgaur.shoppingapp.data.source.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus

interface InventoriesRepoInterface {
	suspend fun updateLocalInventoriesFromRemote(userId: String): StoreDataStatus?
	fun observeInventories(): LiveData<Result<List<Inventory>>?>
	fun observeInventoriesBySellerId(sellerId: String): LiveData<Result<List<Inventory>>?>
	suspend fun getAllInventoriesBySellerId(sellerId: String): Result<List<Inventory>>
	suspend fun getInventoryById(inventoryId: String, forceUpdate: Boolean = false): Result<Inventory>
	suspend fun insertInventory(newInventory: Inventory): Result<Boolean>
	suspend fun insertImages(imgList: List<Uri>): List<String>
	suspend fun updateInventory(inventory: Inventory): Result<Boolean>
	suspend fun updateImages(newList: List<Uri>, oldList: List<String>): List<String>
	suspend fun deleteInventoryById(inventoryId: String): Result<Boolean>
	suspend fun getProductCategories(): List<String>?
}