package com.sers.app.ui.admin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.sers.app.R
import com.sers.app.databinding.FragmentSettingsBinding
import com.sers.app.utils.ThemeHelper
import com.sers.app.viewmodel.ProfileViewModel

/**
 * AdminSettingsFragment — MVVM View
 * Observes ProfileViewModel for user data and delegates all data operations.
 */
class AdminSettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: ProfileViewModel by viewModels()
    private var userDocId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe user data from ViewModel
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.tvUserName.text = "${user.firstName} ${user.lastName}"
            binding.tvUserRole.text = "Administrator"
            binding.tvAvatar.text = user.firstName.firstOrNull()?.uppercase() ?: "A"
            binding.etEmail.setText(user.email)
            binding.etPhone.setText(user.phone)
        }

        viewModel.userDocId.observe(viewLifecycleOwner) { docId ->
            userDocId = docId
        }

        // Observe messages from ViewModel
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            when {
                msg.startsWith("NEED_REAUTH:") -> {
                    val parts = msg.split(":")
                    val newEmail = parts[1]
                    val phone = parts[2]
                    showReauthDialog(newEmail, phone)
                }
                msg.startsWith("ERROR:") -> {
                    Snackbar.make(binding.root, msg.removePrefix("ERROR:"), Snackbar.LENGTH_SHORT).show()
                }
                else -> Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.loadCurrentUser()

        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener(null)
        binding.switchDarkMode.isChecked = ThemeHelper.isDarkModeEnabled(requireContext())
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeHelper.setDarkMode(requireContext(), isChecked)
        }

        binding.btnSaveInfo.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            if (email.isEmpty()) {
                Snackbar.make(binding.root, "Email is required", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(binding.root, "Please enter a valid email address", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.isNotEmpty() && !phone.matches(Regex("^[+]?[0-9]{7,15}$"))) {
                Snackbar.make(binding.root, "Please enter a valid phone number", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveInfo(userDocId, email, phone)
        }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
    }

    private fun showReauthDialog(newEmail: String, phone: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val confirmDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView).setTitle("Confirm Password").create()
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            .setOnClickListener { confirmDialog.dismiss() }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).apply {
            text = "Confirm"
            setOnClickListener {
                val currentPass = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword).text.toString().trim()
                if (currentPass.isEmpty()) {
                    Snackbar.make(binding.root, "Enter current password to change email", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.reauthAndUpdateEmail(currentPass, newEmail, phone, userDocId)
                confirmDialog.dismiss()
            }
        }
        confirmDialog.show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            .setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)
            .setOnClickListener {
                val current = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword).text.toString().trim()
                val newPass = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword).text.toString().trim()
                val confirm = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword).text.toString().trim()
                if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                    Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass != confirm) { Snackbar.make(binding.root, "Passwords do not match!", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
                if (newPass.length < 6) { Snackbar.make(binding.root, "Password must be at least 6 characters!", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
                viewModel.changePassword(current, newPass)
                dialog.dismiss()
            }
        dialog.show()
    }
}