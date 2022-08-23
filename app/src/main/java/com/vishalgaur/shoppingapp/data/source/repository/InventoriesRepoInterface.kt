package com.vishalgaur.shoppingapp.data.source.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.Supplier
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.source.remote.InsertInventoryData
import com.vishalgaur.shoppingapp.data.source.remote.MoveInventoryData
import com.vishalgaur.shoppingapp.data.source.remote.UpdateInventoryData
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus

interface InventoriesRepoInterface {
	suspend fun updateLocalInventoriesFromRemote(userId: String): StoreDataStatus?
	fun observeInventories(): LiveData<Result<List<Inventory>>?>
	fun observeInventoriesBySellerId(sellerId: String): LiveData<Result<List<Inventory>>?>
	suspend fun getInventoriesByUserId(userId: String): Result<List<Inventory>>
	suspend fun getInventoryById(inventoryId: String, forceUpdate: Boolean = false): Result<Inventory>
	suspend fun insertInventory(newInventory: InsertInventoryData): Result<Boolean>
	suspend fun moveInventory(inventoryToMove: MoveInventoryData): Result<Boolean>
	suspend fun insertImages(imgList: List<Uri>): List<String>
	suspend fun updateInventory(updateInventory: UpdateInventoryData): Result<Boolean>
	suspend fun updateImages(newList: List<Uri>, oldList: List<String>): List<String>
	suspend fun deleteInventoryById(inventoryId: String): Result<Boolean>
	suspend fun getProductCategories(): List<String>?
	suspend fun getProducts(): List<Product>?
	suspend fun getSuppliers(): List<Supplier>?
	suspend fun insertProductCategory(name: String): Result<Boolean>
	suspend fun insertSupplier(supplierName: String,addressId: String): Result<Boolean>
	suspend fun insertProduct(productName: String,description: String,upc: String,unit: String,categoryName: String): Result<Boolean>
}