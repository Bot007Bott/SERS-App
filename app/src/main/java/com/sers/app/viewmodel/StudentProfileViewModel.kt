package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * StudentProfileViewModel — MVVM ViewModel
 * Loads student profile and enrolled courses for StudentProfileFragment.
 */
class StudentProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    data class StudentProfileData(
        val firstName: String, val lastName: String,
        val email: String, val phone: String, val studentId: String
    )

    private val _profile = MutableLiveData<StudentProfileData?>()
    val profile: LiveData<StudentProfileData?> = _profile

    private val _enrolledCourseCount = MutableLiveData<Int>()
    val enrolledCourseCount: LiveData<Int> = _enrolledCourseCount

    private val _enrolledCourseNames = MutableLiveData<String>()
    val enrolledCourseNames: LiveData<String> = _enrolledCourseNames

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) return@addOnSuccessListener
                val doc = userDocs.documents[0]
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""
                val email = doc.getString("email") ?: ""
                val phone = doc.getString("phone") ?: ""
                val studentId = doc.getString("studentId") ?: ""
                _profile.value = StudentProfileData(firstName, lastName, email, phone, studentId)

                if (studentId.isNotEmpty()) {
                    loadEnrolledCourses(studentId)
                }
            }
    }

    private fun loadEnrolledCourses(studentId: String) {
        db.collection("enrollments").whereEqualTo("studentId", studentId).get()
            .addOnSuccessListener { enrollDocs ->
                val courseIds = enrollDocs.documents.mapNotNull { it.getString("courseId") }
                if (courseIds.isEmpty()) {
                    _enrolledCourseCount.value = 0
                    _enrolledCourseNames.value = "No courses enrolled"
                    return@addOnSuccessListener
                }
                db.collection("courses").whereIn("courseId", courseIds).get()
                    .addOnSuccessListener { courseDocs ->
                        _enrolledCourseCount.value = courseDocs.size()
                        _enrolledCourseNames.value = courseDocs.documents.joinToString("\n") {
                            "• ${it.getString("courseName") ?: ""}"
                        }
                    }
            }
    }
}
