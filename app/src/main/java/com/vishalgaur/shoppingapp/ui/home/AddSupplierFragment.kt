package com.vishalgaur.shoppingapp.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.utils.*
import com.vishalgaur.shoppingapp.databinding.FragmentAddSupplierBinding
import com.vishalgaur.shoppingapp.ui.AddProductViewErrors
import com.vishalgaur.shoppingapp.ui.MyOnFocusChangeListener
import com.vishalgaur.shoppingapp.viewModels.AddSupplierViewModel
import java.util.*
import kotlin.properties.Delegates
import com.beust.klaxon.Klaxon

private const val TAG = "AddSupplierFragment"

class AddSupplierFragment : Fragment() {

	private lateinit var binding: FragmentAddSupplierBinding
	private val viewModel by viewModels<AddSupplierViewModel>()
	private val focusChangeListener = MyOnFocusChangeListener()

	// arguments
	private lateinit var supplierArg: String

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAddSupplierBinding.inflate(layoutInflater)

		supplierArg = arguments?.getString("supplierArg").toString()

		setViews()

		setObservers()
		return binding.root
	}

	private fun setObservers() {
	}

	private fun setViews() {
		Log.d(TAG, "set views")

		binding.addProAppBar.topAppBar.title = "Tambah Pemasok"

		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigate(R.id.action_addSupplierFragment_to_adminFragment)
		}

		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE

		binding.addSupAddressBtn.setOnClickListener {
			findNavController().navigate(R.id.action_addSupplierFragment_to_selectAddressFragment,
				bundleOf("supplierArg" to Klaxon().toJsonString(AdminToSelectAddressArg(binding.supplierNameEditText.text.toString())))
			)
		}

		if(supplierArg != "null") {
			val result = Klaxon().parse<SelectAddressToAdminArg>(supplierArg)
			if (result != null) {
				binding.supplierNameEditText.setText(result.supplierName)
			}
		}
	}

	private fun makeToast(text: String) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}

	private fun setLoaderState(isVisible: Int = View.GONE) {
		binding.loaderLayout.loaderFrameLayout.visibility = isVisible
		if (isVisible == View.GONE) {
			binding.loaderLayout.circularLoader.hideAnimationBehavior
		} else {
			binding.loaderLayout.circularLoader.showAnimationBehavior
		}
	}
}