package com.sers.app.ui.admin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentSettingsBinding

class AdminSettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userDocId = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.ivProfilePicture.setImageURI(it)
            binding.tvAvatar.text = ""
            Snackbar.make(binding.root, "Profile picture updated!", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData()
        binding.btnChangePhoto.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSaveInfo.setOnClickListener { saveInfo() }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                val doc = docs.documents[0]
                userDocId = doc.id
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""
                val email = doc.getString("email") ?: ""
                val phone = doc.getString("phone") ?: ""
                binding.tvUserName.text = "$firstName $lastName"
                binding.tvUserRole.text = "Administrator"
                binding.tvAvatar.text = firstName.firstOrNull()?.uppercase() ?: "A"
                binding.etEmail.setText(email)
                binding.etPhone.setText(phone)
            }
    }

    private fun saveInfo() {
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        if (email.isEmpty() || phone.isEmpty()) {
            Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Snackbar.make(binding.root, "Please enter a valid email", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (userDocId.isEmpty()) return
        val user = auth.currentUser ?: return
        // If email changed, update Firebase Auth too (requires re-auth)
        if (email != user.email) {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
            val confirmDialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setTitle("Confirm Password")
                .create()
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
                .setOnClickListener { confirmDialog.dismiss() }
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).apply {
                text = "Confirm"
                setOnClickListener {
                    val currentPass = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCurrentPassword).text.toString().trim()
                    if (currentPass.isEmpty()) {
                        Snackbar.make(binding.root, "Enter your current password to change email", Snackbar.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email ?: "", currentPass)
                    user.reauthenticate(credential).addOnSuccessListener {
                        user.updateEmail(email).addOnSuccessListener {
                            saveToFirestore(email, phone)
                            confirmDialog.dismiss()
                        }.addOnFailureListener { e ->
                            Snackbar.make(binding.root, "Email update failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Snackbar.make(binding.root, "Incorrect password!", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            confirmDialog.show()
        } else {
            saveToFirestore(email, phone)
        }
    }

    private fun saveToFirestore(email: String, phone: String) {
        db.collection("users").document(userDocId)
            .update("email", email, "phone", phone)
            .addOnSuccessListener {
                Snackbar.make(binding.root, "Information updated successfully!", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
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
                if (newPass != confirm) {
                    Snackbar.make(binding.root, "Passwords do not match!", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass.length < 6) {
                    Snackbar.make(binding.root, "Password must be at least 6 characters!", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val user = auth.currentUser ?: return@setOnClickListener
                val credential = EmailAuthProvider.getCredential(user.email ?: "", current)
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.updatePassword(newPass)
                            .addOnSuccessListener {
                                Snackbar.make(binding.root, "Password changed successfully!", Snackbar.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .addOnFailureListener { e ->
                                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Snackbar.make(binding.root, "Current password is incorrect!", Snackbar.LENGTH_SHORT).show()
                    }
            }
        dialog.show()
    }
}