package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Student
import com.sers.app.repository.AttendanceRepository
import com.sers.app.repository.CourseRepository
import com.sers.app.repository.StudentRepository

/**
 * AttendanceViewModel — MVVM ViewModel
 * Handles attendance list, student, and course loading for AttendanceFragment.
 */
class AttendanceViewModel : ViewModel() {

    private val attendanceRepository = AttendanceRepository()
    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()
    private var attendanceListener: ListenerRegistration? = null
    private var courseListener: ListenerRegistration? = null

    private val _attendance = MutableLiveData<List<Attendance>>()
    val attendance: LiveData<List<Attendance>> = _attendance

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadAllData() {
        studentRepository.fetchUserMap(
            onResult = { userMap ->
                studentRepository.listenToStudents(userMap,
                    onResult = { students -> _students.value = students },
                    onError = {}
                )
                courseListener = courseRepository.listenToCourses(
                    onResult = { courses -> _courses.value = courses },
                    onError = {}
                )
                attendanceListener = attendanceRepository.listenToAttendance(
                    onResult = { records -> _attendance.value = records },
                    onError = { error -> _message.value = "ERROR:$error" }
                )
            },
            onError = {}
        )
    }

    fun addAttendance(studentId: String, courseId: String, status: String, date: String, session: String) {
        attendanceRepository.addAttendance(studentId, courseId, status, date, session,
            onSuccess = { _message.value = "Attendance added!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun updateAttendance(docId: String, studentId: String, courseId: String, status: String, date: String, session: String) {
        attendanceRepository.updateAttendance(docId, studentId, courseId, status, date, session,
            onSuccess = { _message.value = "Attendance updated!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun deleteAttendance(docId: String) {
        attendanceRepository.deleteAttendance(docId,
            onSuccess = { _message.value = "Attendance deleted!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    override fun onCleared() {
        super.onCleared()
        attendanceListener?.remove()
        courseListener?.remove()
    }
}
