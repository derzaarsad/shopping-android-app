package com.vishalgaur.shoppingapp.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.utils.DateTypeConvertors
import com.vishalgaur.shoppingapp.data.utils.ListTypeConverter
import com.vishalgaur.shoppingapp.data.utils.ObjectListTypeConvertor

@Database(entities = [UserData::class, Product::class, Inventory::class], version = 2)
@TypeConverters(ListTypeConverter::class, ObjectListTypeConvertor::class, DateTypeConvertors::class)
abstract class ShoppingAppDatabase : RoomDatabase() {
	abstract fun userDao(): UserDao
	abstract fun productsDao(): ProductsDao
	abstract fun inventoriesDao(): InventoriesDao

	companion object {
		@Volatile
		private var INSTANCE: ShoppingAppDatabase? = null

		fun getInstance(context: Context): ShoppingAppDatabase =
			INSTANCE ?: synchronized(this) {
				INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
			}

		private fun buildDatabase(context: Context) =
			Room.databaseBuilder(
				context.applicationContext,
				ShoppingAppDatabase::class.java, "ShoppingAppDb"
			)
				.fallbackToDestructiveMigration()
				.allowMainThreadQueries()
				.build()
	}
}