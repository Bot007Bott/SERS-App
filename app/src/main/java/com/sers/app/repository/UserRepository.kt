package com.sers.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.model.User

/**
 * UserRepository — MVC Data Layer
 * Handles all Firebase Firestore operations for User data.
 * Used by Dashboard, Login, and Settings screens.
 */
class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("users")

    /**
     * Get a count of all documents in a collection.
     * Used by Dashboard to show stats.
     */
    fun getCollectionCount(
        collectionName: String,
        onResult: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection(collectionName).get()
            .addOnSuccessListener { onResult(it.size()) }
            .addOnFailureListener { onError(it.message ?: "Failed to count $collectionName") }
    }

    /**
     * Fetch a user document by UID.
     */
    fun fetchUserById(
        uid: String,
        onResult: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = User(
                        uid = doc.id,
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "",
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        teacherId = doc.getString("teacherId") ?: ""
                    )
                    onResult(user)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onError(it.message ?: "Failed to fetch user") }
    }

    /**
     * Update a user's email in Firestore.
     */
    fun updateEmail(
        uid: String,
        newEmail: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(uid)
            .update("email", newEmail)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to update email") }
    }

    /**
     * Listen for real-time user list updates — used by UserManagementFragment.
     */
    fun listenToUsers(
        onResult: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Unknown error")
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.map { doc ->
                User(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "",
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    teacherId = doc.getString("teacherId") ?: ""
                )
            } ?: emptyList()
            onResult(users)
        }
    }
}
