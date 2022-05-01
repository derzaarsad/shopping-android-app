package com.vishalgaur.shoppingapp

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.data.source.InventoryDataSource
import com.vishalgaur.shoppingapp.data.source.ProductDataSource
import com.vishalgaur.shoppingapp.data.source.UserDataSource
import com.vishalgaur.shoppingapp.data.source.local.InventoriesLocalDataSource
import com.vishalgaur.shoppingapp.data.source.local.ProductsLocalDataSource
import com.vishalgaur.shoppingapp.data.source.local.ShoppingAppDatabase
import com.vishalgaur.shoppingapp.data.source.local.UserLocalDataSource
import com.vishalgaur.shoppingapp.data.source.remote.AuthRemoteRestDataSource
import com.vishalgaur.shoppingapp.data.source.remote.ProductsRemoteDataSource
import com.vishalgaur.shoppingapp.data.source.remote.InventoriesRemoteRestDataSource
import com.vishalgaur.shoppingapp.data.source.repository.AuthRepoInterface
import com.vishalgaur.shoppingapp.data.source.repository.AuthRepository
import com.vishalgaur.shoppingapp.data.source.repository.ProductsRepoInterface
import com.vishalgaur.shoppingapp.data.source.repository.ProductsRepository
import com.vishalgaur.shoppingapp.data.source.repository.InventoriesRepoInterface
import com.vishalgaur.shoppingapp.data.source.repository.InventoriesRepository

object ServiceLocator {
	private var database: ShoppingAppDatabase? = null
	private val lock = Any()

	@Volatile
	var authRepository: AuthRepoInterface? = null
		@VisibleForTesting set

	@Volatile
	var productsRepository: ProductsRepoInterface? = null
		@VisibleForTesting set

	@Volatile
	var inventoriesRepository: InventoriesRepoInterface? = null
		@VisibleForTesting set

	fun provideAuthRepository(context: Context): AuthRepoInterface {
		synchronized(this) {
			return authRepository ?: createAuthRepository(context)
		}
	}

	fun provideProductsRepository(context: Context): ProductsRepoInterface {
		synchronized(this) {
			return productsRepository ?: createProductsRepository(context)
		}
	}

	fun provideInventoriesRepository(context: Context): InventoriesRepoInterface {
		synchronized(this) {
			return inventoriesRepository ?: createInventoriesRepository(context)
		}
	}

	@VisibleForTesting
	fun resetRepository() {
		synchronized(lock) {
			database?.apply {
				clearAllTables()
				close()
			}
			database = null
			authRepository = null
		}
	}

	private fun createInventoriesRepository(context: Context): InventoriesRepoInterface {
		val newRepo =
			InventoriesRepository(InventoriesRemoteRestDataSource(), createInventoriesLocalDataSource(context))
		inventoriesRepository = newRepo
		return newRepo
	}

	private fun createProductsRepository(context: Context): ProductsRepoInterface {
		val newRepo =
			ProductsRepository(ProductsRemoteDataSource(), createProductsLocalDataSource(context))
		productsRepository = newRepo
		return newRepo
	}

	private fun createAuthRepository(context: Context): AuthRepoInterface {
		val appSession = ShoppingAppSessionManager(context.applicationContext)
		val newRepo =
			AuthRepository(createUserLocalDataSource(context), AuthRemoteRestDataSource(), appSession)
		authRepository = newRepo
		return newRepo
	}

	private fun createProductsLocalDataSource(context: Context): ProductDataSource {
		val database = database ?: ShoppingAppDatabase.getInstance(context.applicationContext)
		return ProductsLocalDataSource(database.productsDao())
	}

	private fun createUserLocalDataSource(context: Context): UserLocalDataSource {
		val database = database ?: ShoppingAppDatabase.getInstance(context.applicationContext)
		return UserLocalDataSource(database.userDao())
	}

	private fun createInventoriesLocalDataSource(context: Context): InventoryDataSource {
		val database = database ?: ShoppingAppDatabase.getInstance(context.applicationContext)
		return InventoriesLocalDataSource(database.inventoriesDao())
	}
}