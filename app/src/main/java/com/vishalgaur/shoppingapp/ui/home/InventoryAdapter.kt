package com.vishalgaur.shoppingapp.ui.home

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.databinding.LayoutHomeAdBinding
import com.vishalgaur.shoppingapp.databinding.InventoriesListItemBinding
import com.vishalgaur.shoppingapp.getOfferPercentage

class InventoryAdapter(proList: List<Any>, private val context: Context) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	var data = proList

	lateinit var onClickListener: OnClickListener
	lateinit var bindImageButtons: BindImageButtons
	private val sessionManager = ShoppingAppSessionManager(context)

	inner class ItemViewHolder(binding: InventoriesListItemBinding) :
		RecyclerView.ViewHolder(binding.root) {
		private val proName = binding.inventoryNameTv
		private val proPrice = binding.inventoryPriceTv
		private val productCard = binding.inventoryCard
		private val productImage = binding.inventoryImageView
		private val proDeleteButton = binding.inventoryDeleteButton
		private val proEditBtn = binding.inventoryEditButton
		private val proMrp = binding.inventoryActualPriceTv
		private val proOffer = binding.inventoryOfferValueTv
		private val proRatingBar = binding.inventoryRatingBar
		private val proCartButton = binding.inventoryAddToCartButton

		fun bind(productData: Inventory) {
			productCard.setOnClickListener {
				onClickListener.onClick(productData)
			}
			proName.text = productData.sku
			proPrice.text =
				context.getString(R.string.pro_details_price_value, productData.purchasePrice.toString())
			proRatingBar.rating = productData.rating.toFloat()
			proMrp.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
			proMrp.text =
				context.getString(
					R.string.pro_details_actual_strike_value,
					"Something"
				)
			proOffer.text = context.getString(
				R.string.pro_offer_precent_text,
				getOfferPercentage(21.0, productData.purchasePrice).toString()
			)
			if (productData.images.isNotEmpty()) {
				val imgUrl = productData.images[0].toUri().buildUpon().scheme("https").build()
				Glide.with(context)
					.asBitmap()
					.load(imgUrl)
					.into(productImage)

				productImage.clipToOutline = true
			}

			if (sessionManager.isUserAdmin()) {
				proEditBtn.setOnClickListener {
					onClickListener.onEditClick(productData.inventoryId)
				}

				proDeleteButton.setOnClickListener {
					onClickListener.onDeleteClick(productData)
				}
			} else {
				proEditBtn.visibility = View.GONE
				proDeleteButton.visibility = View.GONE
			}
			bindImageButtons.setCartButton(productData.inventoryId, proCartButton)
			proCartButton.setOnClickListener {
				onClickListener.onAddToCartClick(productData)
			}
		}
	}

	inner class AdViewHolder(binding: LayoutHomeAdBinding) : RecyclerView.ViewHolder(binding.root) {
		val adImageView: ImageView = binding.adImageView
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			VIEW_TYPE_AD -> AdViewHolder(
				LayoutHomeAdBinding.inflate(
					LayoutInflater.from(parent.context),
					parent,
					false
				)
			)
			else -> ItemViewHolder(
				InventoriesListItemBinding.inflate(
					LayoutInflater.from(parent.context),
					parent,
					false
				)
			)
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		when (val proData = data[position]) {
			is Int -> (holder as AdViewHolder).adImageView.setImageResource(proData)
			is Inventory -> (holder as ItemViewHolder).bind(proData)
		}
	}

	override fun getItemCount(): Int = data.size

	companion object {
		const val VIEW_TYPE_PRODUCT = 1
		const val VIEW_TYPE_AD = 2
	}

	override fun getItemViewType(position: Int): Int {
		return when (data[position]) {
			is Int -> VIEW_TYPE_AD
			is Inventory -> VIEW_TYPE_PRODUCT
			else -> VIEW_TYPE_PRODUCT
		}
	}

	interface BindImageButtons {
		fun setCartButton(productId: String, imgView: ImageView)
	}

	interface OnClickListener {
		fun onClick(productData: Inventory)
		fun onDeleteClick(productData: Inventory)
		fun onEditClick(inventoryId: String) {}
		fun onAddToCartClick(productData: Inventory) {}
	}
}