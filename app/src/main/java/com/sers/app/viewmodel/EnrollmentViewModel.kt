package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Course
import com.sers.app.model.Enrollment
import com.sers.app.model.Student
import com.sers.app.repository.CourseRepository
import com.sers.app.repository.EnrollmentRepository
import com.sers.app.repository.StudentRepository

/**
 * EnrollmentViewModel — MVVM ViewModel
 * Manages courses, students, and enrollments for EnrollmentFragment.
 */
class EnrollmentViewModel : ViewModel() {

    private val enrollmentRepository = EnrollmentRepository()
    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()
    private var enrollmentListener: ListenerRegistration? = null

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _enrollments = MutableLiveData<List<Enrollment>>()
    val enrollments: LiveData<List<Enrollment>> = _enrollments

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadAllData() {
        studentRepository.fetchUserMap(
            onResult = { _ ->
                courseRepository.fetchCoursesOnce(
                    onResult = { courses ->
                        _courses.value = courses
                        startEnrollmentListener()
                    },
                    onError = { startEnrollmentListener() }
                )
            },
            onError = {
                courseRepository.fetchCoursesOnce(
                    onResult = { courses -> _courses.value = courses; startEnrollmentListener() },
                    onError = { startEnrollmentListener() }
                )
            }
        )
        studentRepository.fetchUserMap(
            onResult = { _ ->
                studentRepository.listenToStudents(emptyMap(),
                    onResult = { students -> _students.value = students },
                    onError = {}
                )
            },
            onError = {}
        )
    }

    private fun startEnrollmentListener() {
        enrollmentListener = enrollmentRepository.listenToEnrollments(
            onResult = { enrollments -> _enrollments.value = enrollments },
            onError = { error -> _message.value = "ERROR:$error" }
        )
    }

    fun enrollStudent(studentId: String, courseId: String) {
        enrollmentRepository.addEnrollment(studentId, courseId,
            onSuccess = { _message.value = "Student enrolled!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun unenrollStudent(docId: String) {
        enrollmentRepository.deleteEnrollment(docId,
            onSuccess = { _message.value = "Student unenrolled!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    override fun onCleared() {
        super.onCleared()
        enrollmentListener?.remove()
    }
}
