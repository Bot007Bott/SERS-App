package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Attendance
import com.sers.app.model.Course

/**
 * StudentAttendanceViewModel — MVVM ViewModel
 * Loads attendance and courses for StudentAttendanceFragment.
 */
class StudentAttendanceViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var attendanceListener: ListenerRegistration? = null

    private val _attendance = MutableLiveData<List<Attendance>>()
    val attendance: LiveData<List<Attendance>> = _attendance

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) return@addOnSuccessListener
                val studentId = userDocs.documents[0].getString("studentId") ?: return@addOnSuccessListener
                loadCoursesThenAttendance(studentId)
            }
    }

    private fun loadCoursesThenAttendance(studentId: String) {
        db.collection("courses").get().addOnSuccessListener { courseDocs ->
            val courses = courseDocs.documents.map { doc ->
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
            _courses.value = courses

            attendanceListener = db.collection("attendance").whereEqualTo("studentId", studentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { _message.value = "ERROR:${error.message}"; return@addSnapshotListener }
                    val list = snapshot?.documents?.map { doc ->
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
                    _attendance.value = list
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        attendanceListener?.remove()
    }
}
