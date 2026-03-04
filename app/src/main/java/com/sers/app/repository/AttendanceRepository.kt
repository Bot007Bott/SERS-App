package com.sers.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Attendance

/**
 * AttendanceRepository — MVC Data Layer
 * Handles all Firebase Firestore operations for Attendance data.
 * Fragments (Controller) call this repository instead of accessing Firestore directly.
 */
class AttendanceRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("attendance")

    /**
     * Listen for real-time attendance updates.
     */
    fun listenToAttendance(
        onResult: (List<Attendance>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Unknown error")
                return@addSnapshotListener
            }
            val records = snapshot?.documents?.map { doc ->
                Attendance(
                    attendanceId = doc.getString("attendanceId") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    session = doc.getString("timeSlot") ?: "",
                    date = doc.getString("date") ?: "",
                    status = doc.getString("status") ?: "",
                    docId = doc.id
                )
            } ?: emptyList()
            onResult(records)
        }
    }

    /**
     * Add a new attendance record to Firestore.
     */
    fun addAttendance(
        studentId: String,
        courseId: String,
        status: String,
        date: String,
        session: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "attendanceId" to "A${System.currentTimeMillis()}",
            "studentId" to studentId,
            "courseId" to courseId,
            "status" to status,
            "date" to date,
            "timeSlot" to session
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to add attendance") }
    }

    /**
     * Update an existing attendance record in Firestore.
     */
    fun updateAttendance(
        docId: String,
        studentId: String,
        courseId: String,
        status: String,
        date: String,
        session: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .update(
                "studentId", studentId,
                "courseId", courseId,
                "status", status,
                "date", date,
                "timeSlot", session
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to update attendance") }
    }

    /**
     * Delete an attendance record from Firestore.
     */
    fun deleteAttendance(
        docId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to delete attendance") }
    }
}
