package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Enrollment
import com.sers.app.model.Student

/**
 * TeacherAttendanceViewModel — MVVM ViewModel
 * Handles the complex data chain for TeacherAttendanceFragment:
 * user → teacherId → courses → students → enrollments → attendance (real-time)
 */
class TeacherAttendanceViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var attendanceListener: ListenerRegistration? = null

    private val _attendance = MutableLiveData<List<Attendance>>()
    val attendance: LiveData<List<Attendance>> = _attendance

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _enrollments = MutableLiveData<List<Enrollment>>()
    val enrollments: LiveData<List<Enrollment>> = _enrollments

    private val _teacherId = MutableLiveData<String>()
    val teacherId: LiveData<String> = _teacherId

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadAllData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                val tid = docs.documents[0].getString("teacherId") ?: ""
                _teacherId.value = tid
                loadStudents(tid)
            }
    }

    private fun loadStudents(tid: String) {
        db.collection("students").get().addOnSuccessListener { studentDocs ->
            _students.value = studentDocs.map { doc ->
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
            db.collection("enrollments").get().addOnSuccessListener { enrollDocs ->
                _enrollments.value = enrollDocs.map { doc ->
                    Enrollment(
                        enrollmentId = doc.getString("enrollmentId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        docId = doc.id
                    )
                }
                db.collection("courses").whereEqualTo("teacherId", tid).get()
                    .addOnSuccessListener { courseDocs ->
                        val courseList = courseDocs.map { doc ->
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
                        _courses.value = courseList
                        startAttendanceListener(courseList.map { it.courseId })
                    }
            }
        }
    }

    private fun startAttendanceListener(courseIds: List<String>) {
        if (courseIds.isEmpty()) { _attendance.value = emptyList(); return }
        attendanceListener = db.collection("attendance").addSnapshotListener { snapshot, error ->
            if (error != null) { _message.value = "ERROR:${error.message}"; return@addSnapshotListener }
            _attendance.value = snapshot?.documents
                ?.filter { doc -> doc.getString("courseId") in courseIds }
                ?.map { doc ->
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
        }
    }

    fun markAttendance(studentId: String, courseId: String, status: String, date: String, session: String) {
        db.collection("attendance").add(hashMapOf(
            "attendanceId" to "A${System.currentTimeMillis()}",
            "studentId" to studentId, "courseId" to courseId,
            "status" to status, "date" to date, "timeSlot" to session
        )).addOnSuccessListener { _message.value = "Attendance marked!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    fun updateAttendance(docId: String, studentId: String, courseId: String, status: String, date: String, session: String) {
        db.collection("attendance").document(docId)
            .update("studentId", studentId, "courseId", courseId, "status", status, "date", date, "timeSlot", session)
            .addOnSuccessListener { _message.value = "Attendance updated!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    fun deleteAttendance(docId: String) {
        db.collection("attendance").document(docId).delete()
            .addOnSuccessListener { _message.value = "Record deleted!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    override fun onCleared() {
        super.onCleared()
        attendanceListener?.remove()
    }
}
