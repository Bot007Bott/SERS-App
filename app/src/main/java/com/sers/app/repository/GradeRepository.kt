package com.sers.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Grade

/**
 * GradeRepository — MVC Data Layer
 * Handles all Firebase Firestore operations for Grade data.
 * Fragments (Controller) call this repository instead of accessing Firestore directly.
 */
class GradeRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("grades")

    /**
     * Listen for real-time grade updates.
     */
    fun listenToGrades(
        onResult: (List<Grade>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Unknown error")
                return@addSnapshotListener
            }
            val grades = snapshot?.documents?.map { doc ->
                Grade(
                    gradeId = doc.getString("gradeId") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    gradeType = doc.getString("gradeType") ?: "",
                    title = doc.getString("title") ?: "",
                    score = (doc.getLong("score") ?: 0).toInt(),
                    totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(),
                    docId = doc.id
                )
            } ?: emptyList()
            onResult(grades)
        }
    }

    /**
     * Add a new grade to Firestore.
     */
    fun addGrade(
        studentId: String,
        courseId: String,
        gradeType: String,
        title: String,
        score: Int,
        totalMarks: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "gradeId" to "G${System.currentTimeMillis()}",
            "studentId" to studentId,
            "courseId" to courseId,
            "gradeType" to gradeType,
            "title" to title,
            "score" to score,
            "totalMarks" to totalMarks
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to add grade") }
    }

    /**
     * Update an existing grade in Firestore.
     */
    fun updateGrade(
        docId: String,
        studentId: String,
        courseId: String,
        gradeType: String,
        title: String,
        score: Int,
        totalMarks: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .update(
                "studentId", studentId,
                "courseId", courseId,
                "gradeType", gradeType,
                "title", title,
                "score", score,
                "totalMarks", totalMarks
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to update grade") }
    }

    /**
     * Delete a grade from Firestore.
     */
    fun deleteGrade(
        docId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to delete grade") }
    }
}
