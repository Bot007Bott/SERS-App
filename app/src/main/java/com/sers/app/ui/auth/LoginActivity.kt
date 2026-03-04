package com.sers.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sers.app.utils.ThemeHelper
import com.google.android.material.snackbar.Snackbar
import com.sers.app.databinding.ActivityLoginBinding
import com.sers.app.ui.admin.AdminMainActivity
import com.sers.app.ui.teacher.TeacherMainActivity
import com.sers.app.ui.student.StudentMainActivity
import com.sers.app.viewmodel.LoginViewModel

/**
 * LoginActivity — MVVM View
 * Observes LoginViewModel.loginResult LiveData.
 * No Firebase or business logic here.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeHelper.applySavedTheme(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observe login results from ViewModel
        viewModel.loginResult.observe(this) { result ->
            if (result.startsWith("ERROR:")) {
                val message = result.removePrefix("ERROR:")
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = "Sign In"
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            } else {
                // result is the role string
                navigateByRole(result)
            }
        }

        // Auto-login if already signed in
        viewModel.checkCurrentUser()

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required"
                return@setOnClickListener
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            } else {
                binding.tilEmail.error = null
            }

            if (password.isEmpty()) {
                binding.tilPassword.error = "Password is required"
                return@setOnClickListener
            } else if (password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            // Show loading state
            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Signing in..."

            // Delegate to ViewModel
            viewModel.login(email, password)
        }
    }

    private fun navigateByRole(role: String) {
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
                viewModel.signOut()
            }
        }
    }
}