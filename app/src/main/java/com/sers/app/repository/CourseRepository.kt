package com.sers.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Course

/**
 * CourseRepository — MVC Data Layer
 * Handles all Firebase Firestore operations for Course data.
 * Fragments (Controller) call this repository instead of accessing Firestore directly.
 */
class CourseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("courses")

    /**
     * Listen for real-time course updates.
     */
    fun listenToCourses(
        onResult: (List<Course>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Unknown error")
                return@addSnapshotListener
            }
            val courses = snapshot?.documents?.map { doc ->
                Course(
                    courseId = doc.getString("courseId") ?: "",
                    courseName = doc.getString("courseName") ?: "",
                    courseCode = doc.getString("courseCode") ?: "",
                    schedule = doc.getString("schedule") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    createdBy = doc.getString("createdBy") ?: "",
                    docId = doc.id
                )
            } ?: emptyList()
            onResult(courses)
        }
    }

    /**
     * Add a new course to Firestore.
     */
    fun addCourse(
        course: Course,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "courseId" to course.courseId,
            "courseName" to course.courseName,
            "courseCode" to course.courseCode,
            "schedule" to course.schedule,
            "teacherId" to course.teacherId,
            "createdBy" to course.createdBy
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to add course") }
    }

    /**
     * Update an existing course in Firestore.
     */
    fun updateCourse(
        docId: String,
        courseName: String,
        courseCode: String,
        schedule: String,
        teacherId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .update(
                "courseName", courseName,
                "courseCode", courseCode,
                "schedule", schedule,
                "teacherId", teacherId
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to update course") }
    }

    /**
     * Delete a course from Firestore.
     */
    fun deleteCourse(
        docId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        collection.document(docId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to delete course") }
    }

    /**
     * Fetch all courses once — used by EnrollmentViewModel.
     */
    fun fetchCoursesOnce(onResult: (List<Course>) -> Unit, onError: (String) -> Unit) {
        collection.get()
            .addOnSuccessListener { docs ->
                val courses = docs.map { doc ->
                    Course(
                        courseId = doc.getString("courseId") ?: "",
                        courseName = doc.getString("courseName") ?: "",
                        courseCode = doc.getString("courseCode") ?: "",
                        schedule = doc.getString("schedule") ?: "",
                        teacherId = doc.getString("teacherId") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        docId = doc.id
                    )
                }
                onResult(courses)
            }
            .addOnFailureListener { onError(it.message ?: "Failed to fetch courses") }
    }
}
