package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Enrollment
import com.sers.app.model.Student

/**
 * TeacherCourseDetailViewModel — MVVM ViewModel for Course Details (Enrollment).
 */
class TeacherCourseDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var enrollmentListener: ListenerRegistration? = null

    private val _enrollments = MutableLiveData<List<Enrollment>>()
    val enrollments: LiveData<List<Enrollment>> = _enrollments

    private val _allStudents = MutableLiveData<List<Student>>()
    val allStudents: LiveData<List<Student>> = _allStudents

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadData(courseId: String) {
        _isLoading.value = true
        db.collection("students").get().addOnSuccessListener { docs ->
            _allStudents.value = docs.map { doc ->
                Student(
                    studentId = doc.getString("studentId") ?: "",
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    program = doc.getString("program") ?: "",
                    phone = doc.getString("phone") ?: "",
                    docId = doc.id
                )
            }
            startEnrollmentListener(courseId)
        }.addOnFailureListener { _message.value = it.message; _isLoading.value = false }
    }

    private fun startEnrollmentListener(courseId: String) {
        enrollmentListener?.remove()
        enrollmentListener = db.collection("enrollments").whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) { _message.value = "ERROR:${error.message}"; return@addSnapshotListener }
                _enrollments.value = snapshot?.documents?.map { doc ->
                    Enrollment(
                        enrollmentId = doc.getString("enrollmentId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        docId = doc.id
                    )
                } ?: emptyList()
            }
    }

    fun addStudentToCourse(studentId: String, courseId: String) {
        db.collection("enrollments").add(hashMapOf(
            "enrollmentId" to "E${System.currentTimeMillis()}",
            "studentId" to studentId,
            "courseId" to courseId
        )).addOnSuccessListener { _message.value = "Student added!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    fun removeStudentFromCourse(docId: String) {
        db.collection("enrollments").document(docId).delete()
            .addOnSuccessListener { _message.value = "Student removed!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    override fun onCleared() {
        super.onCleared()
        enrollmentListener?.remove()
    }
}
