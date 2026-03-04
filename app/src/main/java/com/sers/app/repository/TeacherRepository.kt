package com.sers.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Teacher

/**
 * TeacherRepository — MVC Data Layer
 * Handles all Firebase Firestore operations for Teacher data.
 * Fragments (Controller) call this repository instead of accessing Firestore directly.
 */
class TeacherRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("teachers")

    /**
     * Fetch user ID map (teacherId -> userId) needed for cross-referencing.
     */
    fun fetchUserMap(
        onResult: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").get()
            .addOnSuccessListener { userDocs ->
                val map = mutableMapOf<String, String>()
                userDocs.forEach { doc ->
                    val tid = doc.getString("teacherId") ?: ""
                    if (tid.isNotEmpty()) map[tid] = doc.id
                }
                onResult(map)
            }
            .addOnFailureListener { onError(it.message ?: "Failed to fetch users") }
    }

    /**
     * Listen for real-time teacher updates.
     */
    fun listenToTeachers(
        userMap: Map<String, String>,
        onResult: (List<Teacher>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Unknown error")
                return@addSnapshotListener
            }
            val teachers = snapshot?.documents?.map { doc ->
                val tid = doc.getString("teacherId") ?: ""
                Teacher(
                    teacherId = tid,
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    department = doc.getString("department") ?: "",
                    phone = doc.getString("phone") ?: "",
                    userId = userMap[tid] ?: "",
                    docId = doc.id
                )
            } ?: emptyList()
            onResult(teachers)
        }
    }

    /**
     * Fetch teachers once (no real-time listener) — used by CoursesFragment dropdown.
     */
    fun fetchTeachersOnce(
        onResult: (List<Teacher>) -> Unit,
        onError: (String) -> Unit
    ) {
        collection.get()
            .addOnSuccessListener { docs ->
                val teachers = docs.map { doc ->
                    Teacher(
                        teacherId = doc.getString("teacherId") ?: "",
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        email = doc.getString("email") ?: "",
                        department = doc.getString("department") ?: "",
                        phone = doc.getString("phone") ?: "",
                        docId = doc.id
                    )
                }
                onResult(teachers)
            }
            .addOnFailureListener { onError(it.message ?: "Failed to fetch teachers") }
    }

    /**
     * Add a new teacher to Firestore.
     */
    fun addTeacher(
        teacher: Teacher,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "teacherId" to teacher.teacherId,
            "firstName" to teacher.firstName,
            "lastName" to teacher.lastName,
            "email" to teacher.email,
            "phone" to teacher.phone,
            "department" to teacher.department
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to add teacher") }
    }

    /**
     * Update an existing teacher in Firestore.
     */
    fun updateTeacher(
        docId: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        department: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .update(
                "firstName", firstName,
                "lastName", lastName,
                "email", email,
                "phone", phone,
                "department", department
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to update teacher") }
    }

    /**
     * Delete a teacher from Firestore.
     */
    fun deleteTeacher(
        docId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to delete teacher") }
    }
}
