package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.model.User

/**
 * ProfileViewModel — MVVM ViewModel
 * Shared ViewModel for profile display (Admin, Teacher, Student).
 * Loads the current user's profile from Firestore.
 */
class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _userDocId = MutableLiveData<String>()
    val userDocId: LiveData<String> = _userDocId

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                val doc = docs.documents[0]
                _userDocId.value = doc.id
                _user.value = User(
                    uid = uid,
                    userId = doc.id,
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    username = doc.getString("username") ?: "",
                    phone = doc.getString("phone") ?: "",
                    docId = doc.id
                )
            }
    }

    fun saveInfo(docId: String, email: String, phone: String) {
        val user = auth.currentUser ?: return
        if (email != user.email) {
            // Email change needs re-auth — signal the UI to show confirm dialog
            _message.value = "NEED_REAUTH:$email:$phone"
        } else {
            saveToFirestore(docId, email, phone)
        }
    }

    fun saveToFirestore(docId: String, email: String, phone: String) {
        db.collection("users").document(docId)
            .update("email", email, "phone", phone)
            .addOnSuccessListener { _message.value = "Information updated successfully!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    fun reauthAndUpdateEmail(currentPassword: String, newEmail: String, phone: String, docId: String) {
        val user = auth.currentUser ?: return
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updateEmail(newEmail)
                    .addOnSuccessListener { saveToFirestore(docId, newEmail, phone) }
                    .addOnFailureListener { e -> _message.value = "ERROR:Email update failed: ${e.message}" }
            }
            .addOnFailureListener { _message.value = "ERROR:Incorrect password!" }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { _message.value = "Password changed successfully!" }
                    .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
            }
            .addOnFailureListener { _message.value = "ERROR:Current password is incorrect!" }
    }
}
