package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.model.Student
import com.sers.app.repository.CourseRepository
import com.sers.app.repository.GradeRepository
import com.sers.app.repository.StudentRepository

/**
 * GradeViewModel — MVVM ViewModel
 * Handles grade list, students, and courses for GradesFragment.
 */
class GradeViewModel : ViewModel() {

    private val gradeRepository = GradeRepository()
    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()
    private var gradeListener: ListenerRegistration? = null
    private var courseListener: ListenerRegistration? = null

    private val _grades = MutableLiveData<List<Grade>>()
    val grades: LiveData<List<Grade>> = _grades

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
                gradeListener = gradeRepository.listenToGrades(
                    onResult = { grades -> _grades.value = grades },
                    onError = { error -> _message.value = "ERROR:$error" }
                )
            },
            onError = {}
        )
    }

    fun addGrade(studentId: String, courseId: String, gradeType: String, title: String, score: Int, totalMarks: Int) {
        gradeRepository.addGrade(studentId, courseId, gradeType, title, score, totalMarks,
            onSuccess = { _message.value = "Grade added!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun updateGrade(docId: String, studentId: String, courseId: String, gradeType: String, title: String, score: Int, totalMarks: Int) {
        gradeRepository.updateGrade(docId, studentId, courseId, gradeType, title, score, totalMarks,
            onSuccess = { _message.value = "Grade updated!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun deleteGrade(docId: String) {
        gradeRepository.deleteGrade(docId,
            onSuccess = { _message.value = "Grade deleted!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    override fun onCleared() {
        super.onCleared()
        gradeListener?.remove()
        courseListener?.remove()
    }
}
