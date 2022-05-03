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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.databinding.FragmentAdminBinding
import java.util.*
import kotlin.properties.Delegates

private const val TAG = "AdminFragment"

class AdminFragment : Fragment() {

	private lateinit var binding: FragmentAdminBinding

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		binding = FragmentAdminBinding.inflate(layoutInflater)

		binding.adminTopAppBar.topAppBar.title = "Halaman Admin"

		binding.loaderLayout.loaderFrameLayout.visibility = View.GONE

		binding.addProBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminFragment_to_addProductFragment)
		}

		binding.addSupBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminFragment_to_addSupplierFragment)
		}

		binding.addCatBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminFragment_to_addProductCategoryFragment)
		}

		return binding.root
	}
}