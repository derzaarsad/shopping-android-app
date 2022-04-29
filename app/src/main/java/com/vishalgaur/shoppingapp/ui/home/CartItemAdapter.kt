package com.vishalgaur.shoppingapp.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.Inventory
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.databinding.CartListItemBinding
import com.vishalgaur.shoppingapp.databinding.LayoutCircularLoaderBinding

class CartItemAdapter(
	private val context: Context, items: List<UserData.CartItem>,
	products: List<Inventory>
) : RecyclerView.Adapter<CartItemAdapter.ViewHolder>() {

	lateinit var onClickListener: OnClickListener
	var data: List<UserData.CartItem> = items
	var proList: List<Inventory> = products

	inner class ViewHolder(private val binding: CartListItemBinding) :
		RecyclerView.ViewHolder(binding.root) {
		fun bind(itemData: UserData.CartItem) {
			binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
			val proData = proList.find { it.inventoryId == itemData.productId } ?: Inventory()
			binding.cartProductTitleTv.text = proData.name
			binding.cartProductPriceTv.text =
				context.getString(R.string.price_text, proData.price.toString())
			if (proData.images.isNotEmpty()) {
				val imgUrl = proData.images[0].toUri().buildUpon().scheme("https").build()
				Glide.with(context)
					.asBitmap()
					.load(imgUrl)
					.into(binding.productImageView)
				binding.productImageView.clipToOutline = true
			}
			binding.cartProductQuantityTextView.text = itemData.quantity.toString()
			binding.cartProductDeleteBtn.setOnClickListener {
				binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
				onClickListener.onDeleteClick(itemData.itemId, binding.loaderLayout)
			}
			binding.cartProductPlusBtn.setOnClickListener {
				binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
				onClickListener.onPlusClick(itemData.itemId)
			}
			binding.cartProductMinusBtn.setOnClickListener {
				binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
				onClickListener.onMinusClick(itemData.itemId, itemData.quantity, binding.loaderLayout)
			}

		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(
			CartListItemBinding.inflate(
				LayoutInflater.from(parent.context), parent, false
			)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(data[position])
	}

	override fun getItemCount() = data.size

	interface OnClickListener {
		fun onDeleteClick(itemId: String, itemBinding: LayoutCircularLoaderBinding)
		fun onPlusClick(itemId: String)
		fun onMinusClick(itemId: String, currQuantity: Int, itemBinding: LayoutCircularLoaderBinding)
	}
}