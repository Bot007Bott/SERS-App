package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sers.app.repository.UserRepository

/**
 * DashboardViewModel — MVVM ViewModel
 * Provides dashboard statistics to DashboardFragment via LiveData.
 */
class DashboardViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _userCount = MutableLiveData<String>()
    val userCount: LiveData<String> = _userCount

    private val _studentCount = MutableLiveData<String>()
    val studentCount: LiveData<String> = _studentCount

    private val _courseCount = MutableLiveData<String>()
    val courseCount: LiveData<String> = _courseCount

    private val _teacherCount = MutableLiveData<String>()
    val teacherCount: LiveData<String> = _teacherCount

    fun loadStats() {
        userRepository.getCollectionCount("users",
            onResult = { _userCount.value = it.toString() },
            onError = { _userCount.value = "0" }
        )
        userRepository.getCollectionCount("students",
            onResult = { _studentCount.value = it.toString() },
            onError = { _studentCount.value = "0" }
        )
        userRepository.getCollectionCount("courses",
            onResult = { _courseCount.value = it.toString() },
            onError = { _courseCount.value = "0" }
        )
        userRepository.getCollectionCount("teachers",
            onResult = { _teacherCount.value = it.toString() },
            onError = { _teacherCount.value = "0" }
        )
    }
}
