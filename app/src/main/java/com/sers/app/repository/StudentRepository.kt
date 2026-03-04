package com.sers.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Student

/**
 * StudentRepository — MVC Data Layer
 * Handles all Firebase Firestore operations for Student data.
 * Fragments (Controller) call this repository instead of accessing Firestore directly.
 */
class StudentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("students")

    /**
     * Listen for real-time student updates.
     * Returns a ListenerRegistration that the Fragment must remove when destroyed.
     */
    fun listenToStudents(
        userMap: Map<String, String>,
        onResult: (List<Student>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Unknown error")
                return@addSnapshotListener
            }
            val students = snapshot?.documents?.map { doc ->
                val sid = doc.getString("studentId") ?: ""
                Student(
                    studentId = sid,
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    program = doc.getString("program") ?: "",
                    phone = doc.getString("phone") ?: "",
                    userId = userMap[sid] ?: "",
                    docId = doc.id
                )
            } ?: emptyList()
            onResult(students)
        }
    }

    /**
     * Fetch user ID map (studentId -> userId) needed for cross-referencing.
     */
    fun fetchUserMap(
        onResult: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").get()
            .addOnSuccessListener { userDocs ->
                val map = mutableMapOf<String, String>()
                userDocs.forEach { doc ->
                    val sid = doc.getString("studentId") ?: ""
                    if (sid.isNotEmpty()) map[sid] = doc.id
                }
                onResult(map)
            }
            .addOnFailureListener { onError(it.message ?: "Failed to fetch users") }
    }

    /**
     * Add a new student to Firestore.
     */
    fun addStudent(
        student: Student,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "studentId" to student.studentId,
            "firstName" to student.firstName,
            "lastName" to student.lastName,
            "email" to student.email,
            "phone" to student.phone,
            "program" to student.program
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to add student") }
    }

    /**
     * Update an existing student in Firestore.
     */
    fun updateStudent(
        docId: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .update("firstName", firstName, "lastName", lastName, "email", email, "phone", phone)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to update student") }
    }

    /**
     * Delete a student from Firestore.
     */
    fun deleteStudent(
        docId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to delete student") }
    }
}
