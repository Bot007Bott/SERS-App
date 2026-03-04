package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Course
import com.sers.app.model.Teacher
import com.sers.app.repository.CourseRepository
import com.sers.app.repository.TeacherRepository

/**
 * CourseViewModel — MVVM ViewModel
 * Handles course list and teacher loading for CoursesFragment.
 */
class CourseViewModel : ViewModel() {

    private val courseRepository = CourseRepository()
    private val teacherRepository = TeacherRepository()
    private var courseListener: ListenerRegistration? = null

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _teachers = MutableLiveData<List<Teacher>>()
    val teachers: LiveData<List<Teacher>> = _teachers

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadTeachersAndCourses() {
        teacherRepository.fetchTeachersOnce(
            onResult = { teachers ->
                _teachers.value = teachers
                startCourseListener()
            },
            onError = { startCourseListener() }
        )
    }

    private fun startCourseListener() {
        courseListener = courseRepository.listenToCourses(
            onResult = { courses -> _courses.value = courses },
            onError = { error -> _message.value = "ERROR:$error" }
        )
    }

    fun addCourse(course: Course) {
        courseRepository.addCourse(
            course = course,
            onSuccess = { _message.value = "Course added!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun updateCourse(docId: String, courseName: String, courseCode: String, schedule: String, teacherId: String) {
        courseRepository.updateCourse(
            docId = docId, courseName = courseName, courseCode = courseCode,
            schedule = schedule, teacherId = teacherId,
            onSuccess = { _message.value = "Course updated!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun deleteCourse(docId: String) {
        courseRepository.deleteCourse(
            docId = docId,
            onSuccess = { _message.value = "Course deleted!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    override fun onCleared() {
        super.onCleared()
        courseListener?.remove()
    }
}
