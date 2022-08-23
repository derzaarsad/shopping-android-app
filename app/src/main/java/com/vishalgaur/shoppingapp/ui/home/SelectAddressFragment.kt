package com.vishalgaur.shoppingapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.beust.klaxon.Klaxon
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.data.utils.UserType
import com.vishalgaur.shoppingapp.databinding.FragmentSelectAddressBinding
import com.vishalgaur.shoppingapp.ui.getCompleteAddress
import com.vishalgaur.shoppingapp.viewModels.OrderViewModel

private const val TAG = "ShipToFragment"

class SelectAddressFragment : Fragment() {

	private lateinit var binding: FragmentSelectAddressBinding
	private val orderViewModel: OrderViewModel by activityViewModels()
	private lateinit var addressAdapter: AddressAdapter

	private lateinit var supplierArg: String

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding = FragmentSelectAddressBinding.inflate(layoutInflater)

		supplierArg = arguments?.getString("supplierArg").toString()

		setViews()
		setObservers()

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		orderViewModel.getMemberAddresses()
	}

	private fun setObservers() {
		orderViewModel.dataStatus.observe(viewLifecycleOwner) { status ->
			when (status) {
				StoreDataStatus.LOADING -> {
					binding.addressEmptyTextView.visibility = View.GONE
					binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
					binding.loaderLayout.circularLoader.showAnimationBehavior
				}
				else -> {
					binding.loaderLayout.circularLoader.hideAnimationBehavior
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
				}
			}
			if(status != null && status != StoreDataStatus.LOADING) {
				orderViewModel.memberAddresses.observe(viewLifecycleOwner) { addressList ->
					if (addressList.isNotEmpty()) {
						addressAdapter.data = addressList
						binding.shipToAddressesRecyclerView.adapter = addressAdapter
						binding.shipToAddressesRecyclerView.adapter?.notifyDataSetChanged()
					} else if (addressList.isEmpty()) {
						binding.shipToAddressesRecyclerView.visibility = View.GONE
						binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
						binding.loaderLayout.circularLoader.hideAnimationBehavior
						binding.addressEmptyTextView.visibility = View.VISIBLE
					}
				}
			}
		}
	}

	private fun setViews() {
		binding.shipToAppBar.topAppBar.inflateMenu(R.menu.menu_with_add_only)
		binding.shipToAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}
		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
		binding.shipToErrorTextView.visibility = View.GONE
		binding.addressEmptyTextView.visibility = View.GONE
		binding.shipToAppBar.topAppBar.setOnMenuItemClickListener { menuItem ->
			if (menuItem.itemId == R.id.add_item) {
				navigateToAddEditAddress(false)
				true
			} else {
				false
			}
		}

		if (context != null) {
			addressAdapter = AddressAdapter(
				requireContext(),
				orderViewModel.memberAddresses.value ?: emptyList(),
				true
			)
			addressAdapter.onClickListener = object : AddressAdapter.OnClickListener {
				override fun onEditClick(addressId: String) {
					Log.d(TAG, "onEditAddress: initiated")
					navigateToAddEditAddress(true, addressId)
				}

				override fun onDeleteClick(addressId: String) {
					Log.d(TAG, "onDeleteAddress: initiated")
					showDeleteDialog(addressId)
				}
			}

			binding.shipToAddressesRecyclerView.adapter = addressAdapter
		}

		if(supplierArg == "null") {
			binding.shipToAppBar.topAppBar.title = getString(R.string.ship_to_title)
			binding.shipToNextBtn.setOnClickListener {
				navigateToPaymentFragment(addressAdapter.lastCheckedAddress)
			}
		} else {
			binding.shipToAppBar.topAppBar.title = "Kontak Person"
			val result = Klaxon().parse<AdminToSelectAddressArg>(supplierArg)
			binding.shipToNextBtn.setOnClickListener {
				if (result != null) {
					val selectedAddress = orderViewModel.memberAddresses.value?.find { it.addressId == addressAdapter.lastCheckedAddress }
					val completeAddressText = if (selectedAddress != null) getCompleteAddress(selectedAddress) else null
					findNavController().navigate(R.id.action_selectAddressFragment_to_addSupplierFragment,
						bundleOf("supplierArg" to Klaxon().toJsonString(SelectAddressToAdminArg(result.supplierName,addressAdapter.lastCheckedAddress,completeAddressText)))
					)
				}
			}

			binding.shipToNextBtn.setText("Pilih Kontak Person")
		}
	}

	private fun showDeleteDialog(addressId: String) {
		context?.let {
			MaterialAlertDialogBuilder(it)
				.setTitle(getString(R.string.delete_dialog_title_text))
				.setMessage(getString(R.string.delete_address_message_text))
				.setNeutralButton(getString(R.string.pro_cat_dialog_cancel_btn)) { dialog, _ ->
					dialog.cancel()
				}
				.setPositiveButton(getString(R.string.delete_dialog_delete_btn_text)) { dialog, _ ->
					orderViewModel.deleteAddressOfCurrentUser(addressId)
					dialog.cancel()
				}
				.show()
		}
	}

	private fun navigateToPaymentFragment(addressId: String?) {
		if (addressId != null) {
			orderViewModel.setSelectedAddress(addressId)
			Log.d(TAG, "navigate to Payment")
			binding.shipToErrorTextView.visibility = View.GONE
			findNavController().navigate(R.id.action_selectAddressFragment_to_selectPaymentFragment)

		} else {
			Log.d(TAG, "error = select one address")
			binding.shipToErrorTextView.visibility = View.VISIBLE
		}
	}

	private fun navigateToAddEditAddress(isEdit: Boolean, addressId: String? = null) {
		val userType = if (supplierArg != "null") UserType.SUPPLIER.name else UserType.CUSTOMER.name
		findNavController().navigate(
			R.id.action_selectAddressFragment_to_addEditAddressFragment,
			bundleOf("isEdit" to isEdit, "userType" to userType, "addressId" to addressId)
		)
	}
}