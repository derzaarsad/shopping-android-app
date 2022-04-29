package com.vishalgaur.shoppingapp

import android.app.Application
import com.vishalgaur.shoppingapp.data.source.repository.AuthRepoInterface
import com.vishalgaur.shoppingapp.data.source.repository.ProductsRepoInterface
import com.vishalgaur.shoppingapp.data.source.repository.InventoriesRepoInterface

class ShoppingApplication : Application() {
	val authRepository: AuthRepoInterface
		get() = ServiceLocator.provideAuthRepository(this)

	val productsRepository: ProductsRepoInterface
		get() = ServiceLocator.provideProductsRepository(this)

	val inventoriesRepository: InventoriesRepoInterface
		get() = ServiceLocator.provideInventoriesRepository(this)

	override fun onCreate() {
		super.onCreate()
	}
}