package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Teacher
import com.sers.app.repository.TeacherRepository

class TeacherManagementViewModel : ViewModel() {

    private val teacherRepository = TeacherRepository()
    private var listenerRegistration: ListenerRegistration? = null

    private val _teachers = MutableLiveData<List<Teacher>>()
    val teachers: LiveData<List<Teacher>> = _teachers

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadTeachers() {
        teacherRepository.fetchUserMap(
            onResult = { userMap ->
                listenerRegistration = teacherRepository.listenToTeachers(
                    userMap = userMap,
                    onResult = { teachers -> _teachers.value = teachers },
                    onError = { error -> _message.value = "ERROR:$error" }
                )
            },
            onError = { error -> _message.value = "ERROR:$error" }
        )
    }

    fun addTeacher(teacher: Teacher) {
        teacherRepository.addTeacher(
            teacher = teacher,
            onSuccess = { _message.value = "Teacher added!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun updateTeacher(docId: String, firstName: String, lastName: String,
                      email: String, department: String, phone: String) {
        teacherRepository.updateTeacher(
            docId = docId, firstName = firstName, lastName = lastName,
            email = email, department = department, phone = phone,
            onSuccess = { _message.value = "Teacher updated!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    fun deleteTeacher(docId: String) {
        teacherRepository.deleteTeacher(
            docId = docId,
            onSuccess = { _message.value = "Teacher deleted!" },
            onError = { e -> _message.value = "ERROR:$e" }
        )
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
