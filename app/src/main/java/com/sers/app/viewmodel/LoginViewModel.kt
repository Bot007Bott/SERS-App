package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * LoginViewModel — MVVM ViewModel
 * Handles all login and role-checking logic.
 * LoginActivity observes loginResult LiveData and reacts to it.
 */
class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Emits the role string on success ("Admin","Teacher","Student")
    // or emits an error message starting with "ERROR:" on failure
    private val _loginResult = MutableLiveData<String>()
    val loginResult: LiveData<String> = _loginResult

    /**
     * Called when the user taps Sign In.
     * Performs Firebase Auth sign-in then checks Firestore for the user's role.
     */
    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: run {
                    _loginResult.value = "ERROR:Authentication failed"
                    return@addOnSuccessListener
                }
                checkRoleAndEmit(uid)
            }
            .addOnFailureListener { e ->
                _loginResult.value = "ERROR:${e.message ?: "Login failed"}"
            }
    }

    /**
     * Called on startup if the user is already signed in (auto-login).
     */
    fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkRoleAndEmit(currentUser.uid)
        }
    }

    /**
     * Fetches the user's role from Firestore and posts it to loginResult.
     */
    private fun checkRoleAndEmit(uid: String) {
        db.collection("users")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    auth.signOut()
                    _loginResult.value = "ERROR:User data not found!"
                    return@addOnSuccessListener
                }
                val role = documents.documents[0].getString("role") ?: ""
                if (role.isEmpty()) {
                    auth.signOut()
                    _loginResult.value = "ERROR:Unknown role"
                } else {
                    _loginResult.value = role
                }
            }
            .addOnFailureListener { e ->
                auth.signOut()
                _loginResult.value = "ERROR:${e.message ?: "Failed to get user role"}"
            }
    }

    fun signOut() {
        auth.signOut()
    }
}
