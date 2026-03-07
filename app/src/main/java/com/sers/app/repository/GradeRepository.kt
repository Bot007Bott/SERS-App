package com.sers.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Grade
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GradeRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("grades")

    fun listenToGrades(
        onResult: (List<Grade>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error.message ?: "Unknown error"); return@addSnapshotListener }
            val grades = snapshot?.documents?.map { doc ->
                Grade(
                    gradeId = doc.getString("gradeId") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    gradeType = doc.getString("gradeType") ?: "",
                    title = doc.getString("title") ?: "",
                    score = (doc.getLong("score") ?: 0).toInt(),
                    totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(),
                    date = doc.getString("date") ?: "",
                    docId = doc.id
                )
            } ?: emptyList()
            onResult(grades)
        }
    }

    fun addGrade(
        studentId: String, courseId: String, gradeType: String,
        title: String, score: Int, totalMarks: Int,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val data = hashMapOf(
            "gradeId" to "G${System.currentTimeMillis()}",
            "studentId" to studentId,
            "courseId" to courseId,
            "gradeType" to gradeType,
            "title" to title,
            "score" to score,
            "totalMarks" to totalMarks,
            "date" to today
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to add grade") }
    }

    fun updateGrade(
        docId: String, studentId: String, courseId: String, gradeType: String,
        title: String, score: Int, totalMarks: Int,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        // Keep existing date when updating — don't overwrite it
        collection.document(docId)
            .update(
                "studentId", studentId, "courseId", courseId,
                "gradeType", gradeType, "title", title,
                "score", score, "totalMarks", totalMarks
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to update grade") }
    }

    fun deleteGrade(docId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        collection.document(docId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to delete grade") }
    }
}