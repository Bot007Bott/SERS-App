package com.sers.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.databinding.ActivityLoginBinding
import com.sers.app.ui.admin.AdminMainActivity
import com.sers.app.ui.teacher.TeacherMainActivity
import com.sers.app.ui.student.StudentMainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Auto-login if already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkRoleAndNavigate(currentUser.uid)
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required"
                return@setOnClickListener
            } else {
                binding.tilEmail.error = null
            }

            if (password.isEmpty()) {
                binding.tilPassword.error = "Password is required"
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            // Show loading
            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Signing in..."

            // Step 1: Sign in with Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    // Step 2: Check role in Firestore
                    db.collection("users")
                        .whereEqualTo("uid", uid)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                binding.btnSignIn.isEnabled = true
                                binding.btnSignIn.text = "Sign In"
                                Snackbar.make(binding.root, "User data not found!", Snackbar.LENGTH_SHORT).show()
                                auth.signOut()
                                return@addOnSuccessListener
                            }

                            val userDoc = documents.documents[0]
                            val role = userDoc.getString("role") ?: ""

                            // Step 3: Navigate based on role
                            when (role) {
                                "Admin" -> {
                                    startActivity(Intent(this, AdminMainActivity::class.java))
                                    finish()
                                }
                                "Teacher" -> {
                                    startActivity(Intent(this, TeacherMainActivity::class.java))
                                    finish()
                                }
                                "Student" -> {
                                    startActivity(Intent(this, StudentMainActivity::class.java))
                                    finish()
                                }
                                else -> {
                                    binding.btnSignIn.isEnabled = true
                                    binding.btnSignIn.text = "Sign In"
                                    Snackbar.make(binding.root, "Unknown role: $role", Snackbar.LENGTH_SHORT).show()
                                    auth.signOut()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            binding.btnSignIn.isEnabled = true
                            binding.btnSignIn.text = "Sign In"
                            Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                            auth.signOut()
                        }
                }
                .addOnFailureListener { e ->
                    binding.btnSignIn.isEnabled = true
                    binding.btnSignIn.text = "Sign In"
                    Snackbar.make(binding.root, "Login failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }

        }

    }
    private fun checkRoleAndNavigate(uid: String) {
        db.collection("users")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) return@addOnSuccessListener
                val role = documents.documents[0].getString("role") ?: ""
                when (role) {
                    "Admin" -> { startActivity(Intent(this, AdminMainActivity::class.java)); finish() }
                    "Teacher" -> { startActivity(Intent(this, TeacherMainActivity::class.java)); finish() }
                    "Student" -> { startActivity(Intent(this, StudentMainActivity::class.java)); finish() }
                }
            }
    }
}