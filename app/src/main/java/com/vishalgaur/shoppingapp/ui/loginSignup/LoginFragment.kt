package com.vishalgaur.shoppingapp.ui.loginSignup

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.vishalgaur.shoppingapp.MOB_ERROR_TEXT
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.utils.LogInErrors
import com.vishalgaur.shoppingapp.databinding.FragmentLoginBinding
import com.vishalgaur.shoppingapp.ui.LoginViewErrors

class LoginFragment : LoginSignupBaseFragment<FragmentLoginBinding>() {
	override fun setViewBinding(): FragmentLoginBinding {
		return FragmentLoginBinding.inflate(layoutInflater)
	}

	override fun observeView() {
		super.observeView()

		viewModel.errorStatusLoginFragment.observe(viewLifecycleOwner) { err ->
			modifyErrors(err)
		}

		viewModel.loginErrorStatus.observe(viewLifecycleOwner) { err ->
			when (err) {
				LogInErrors.LERR -> setErrorText(getString(R.string.login_error_text))
				else -> binding.loginErrorTextView.visibility = View.GONE
			}
		}
	}

	override fun setUpViews() {
		super.setUpViews()

		binding.loginErrorTextView.visibility = View.GONE

		binding.loginMobileEditText.onFocusChangeListener = focusChangeListener
		binding.loginPasswordEditText.onFocusChangeListener = focusChangeListener

		binding.loginLoginBtn.setOnClickListener(object : OnClickListener {
			override fun onClick(v: View?) {
				onLogin()
				if (viewModel.errorStatusLoginFragment.value == LoginViewErrors.NONE) {
					viewModel.loginErrorStatus.observe(viewLifecycleOwner) {
						if (it == LogInErrors.NONE) {
							val isRemOn = binding.loginRemSwitch.isChecked
							val bundle = bundleOf(
								"uData" to viewModel.userData.value,
								"loginRememberMe" to isRemOn
							)
							launchOtpActivity(getString(R.string.login_fragment_label), bundle)
						}
					}
				}
			}
		})
	}

	private fun modifyErrors(err: LoginViewErrors) {
		when (err) {
			LoginViewErrors.NONE -> setEditTextErrors()
			LoginViewErrors.ERR_EMPTY -> setErrorText("Fill all details")
			LoginViewErrors.ERR_MOBILE -> setEditTextErrors(MOB_ERROR_TEXT)
		}
	}

	private fun setErrorText(errText: String?) {
		binding.loginErrorTextView.visibility = View.VISIBLE
		if (errText != null) {
			binding.loginErrorTextView.text = errText
		}
	}

	private fun setEditTextErrors(mobError: String? = null) {
		binding.loginErrorTextView.visibility = View.GONE
		binding.loginMobileEditText.error = mobError
	}

	private fun onLogin() {
		val mob = binding.loginMobileEditText.text.toString()
		val pwd = binding.loginPasswordEditText.text.toString()

		viewModel.loginSubmitData(mob, pwd)
	}
}