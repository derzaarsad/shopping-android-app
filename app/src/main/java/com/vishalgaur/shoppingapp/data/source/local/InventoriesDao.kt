package com.vishalgaur.shoppingapp.data.source.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vishalgaur.shoppingapp.data.Inventory

@Dao
interface InventoriesDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(inventory: Inventory)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertListOfInventories(inventories: List<Inventory>)

	@Query("SELECT * FROM inventories")
	suspend fun getAllInventories(): List<Inventory>

	@Query("SELECT * FROM inventories")
	fun observeInventories(): LiveData<List<Inventory>>

	@Query("SELECT * FROM inventories WHERE storeId = :store_id")
	fun observeInventoriesByStoreId(store_id: String): LiveData<List<Inventory>>

	@Query("SELECT * FROM inventories WHERE inventoryId = :invId")
	suspend fun getInventoryById(invId: String): Inventory?

	@Query("SELECT * FROM inventories WHERE storeId = :store_id")
	suspend fun getInventoriesByStoreId(store_id: String): List<Inventory>

	@Query("DELETE FROM inventories WHERE inventoryId = :invId")
	suspend fun deleteInventoryById(invId: String): Int

	@Query("DELETE FROM inventories")
	suspend fun deleteAllInventories()
}