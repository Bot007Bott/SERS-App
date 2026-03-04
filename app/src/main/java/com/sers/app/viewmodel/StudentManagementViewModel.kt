package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Student
import com.sers.app.repository.StudentRepository

/**
 * StudentManagementViewModel — MVVM ViewModel
 * Handles student list loading and CRUD for StudentManagementFragment.
 */
class StudentManagementViewModel : ViewModel() {

    private val studentRepository = StudentRepository()
    private var listenerRegistration: ListenerRegistration? = null

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadStudents() {
        studentRepository.fetchUserMap(
            onResult = { userMap -> startStudentListener(userMap) },
            onError = { startStudentListener(emptyMap()) }
        )
    }

    private fun startStudentListener(userMap: Map<String, String>) {
        listenerRegistration = studentRepository.listenToStudents(
            userMap = userMap,
            onResult = { students -> _students.value = students },
            onError = { error -> _message.value = "ERROR:$error" }
        )
    }

    fun addStudent(student: Student) {
        studentRepository.addStudent(
            student = student,
            onSuccess = { _message.value = "Student added!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun updateStudent(docId: String, firstName: String, lastName: String, email: String, phone: String) {
        studentRepository.updateStudent(
            docId = docId, firstName = firstName, lastName = lastName, email = email, phone = phone,
            onSuccess = { _message.value = "Student updated!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun deleteStudent(docId: String) {
        studentRepository.deleteStudent(
            docId = docId,
            onSuccess = { _message.value = "Student deleted!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
