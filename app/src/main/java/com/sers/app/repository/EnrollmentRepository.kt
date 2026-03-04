package com.sers.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Enrollment

/**
 * EnrollmentRepository — MVC Data Layer
 * Handles all Firebase Firestore operations for Enrollment data.
 * Fragments (Controller) call this repository instead of accessing Firestore directly.
 */
class EnrollmentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("enrollments")

    /**
     * Listen for real-time enrollment updates for a specific course.
     */
    fun listenToEnrollmentsByCourse(
        courseId: String,
        onResult: (List<Enrollment>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection.whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }
                val enrollments = snapshot?.documents?.map { doc ->
                    Enrollment(
                        enrollmentId = doc.getString("enrollmentId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        docId = doc.id
                    )
                } ?: emptyList()
                onResult(enrollments)
            }
    }

    /**
     * Fetch all enrollments once — used for reports and analytics.
     */
    fun fetchAllEnrollments(
        onResult: (List<Enrollment>) -> Unit,
        onError: (String) -> Unit
    ) {
        collection.get()
            .addOnSuccessListener { docs ->
                val enrollments = docs.map { doc ->
                    Enrollment(
                        enrollmentId = doc.getString("enrollmentId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        docId = doc.id
                    )
                }
                onResult(enrollments)
            }
            .addOnFailureListener { onError(it.message ?: "Failed to fetch enrollments") }
    }

    /**
     * Enroll a student into a course.
     */
    fun enrollStudent(
        studentId: String,
        courseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "enrollmentId" to "E${System.currentTimeMillis()}",
            "studentId" to studentId,
            "courseId" to courseId
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to enroll student") }
    }

    /**
     * Remove a student from a course (unenroll).
     */
    fun unenrollStudent(
        docId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to unenroll student") }
    }

    // MVVM aliases — used by EnrollmentViewModel
    fun listenToEnrollments(
        onResult: (List<Enrollment>) -> Unit,
        onError: (String) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error.message ?: "Unknown error"); return@addSnapshotListener }
            val enrollments = snapshot?.documents?.map { doc ->
                Enrollment(
                    enrollmentId = doc.getString("enrollmentId") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    docId = doc.id
                )
            } ?: emptyList()
            onResult(enrollments)
        }
    }

    fun addEnrollment(studentId: String, courseId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        enrollStudent(studentId, courseId, onSuccess, onError)
    }

    fun deleteEnrollment(docId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        unenrollStudent(docId, onSuccess, onError)
    }
}
