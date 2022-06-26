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
	suspend fun insertOrReplace(inventory: Inventory)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertListOfInventories(inventories: List<Inventory>)

	@Query("SELECT * FROM inventories")
	suspend fun getAllInventories(): List<Inventory>

	@Query("SELECT * FROM inventories")
	fun observeInventories(): LiveData<List<Inventory>>

	@Query("SELECT * FROM inventories WHERE sellerId = :seller_id")
	fun observeInventoriesBySellerId(seller_id: String): LiveData<List<Inventory>>

	@Query("SELECT * FROM inventories WHERE inventoryId = :invId")
	suspend fun getInventoryById(invId: String): Inventory?

	@Query("SELECT * FROM inventories WHERE sellerId = :user_id OR ownerId = :user_id")
	suspend fun getInventoriesByUserId(user_id: String): List<Inventory>

	@Query("DELETE FROM inventories WHERE inventoryId = :invId")
	suspend fun deleteInventoryById(invId: String): Int

	@Query("DELETE FROM inventories")
	suspend fun deleteAllInventories()
}