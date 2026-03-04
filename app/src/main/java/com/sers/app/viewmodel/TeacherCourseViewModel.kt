package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Course
import com.sers.app.model.Teacher

/**
 * TeacherCourseViewModel — MVVM ViewModel for Teacher's courses.
 */
class TeacherCourseViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _myTeacher = MutableLiveData<Teacher>()
    val myTeacher: LiveData<Teacher> = _myTeacher

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) { _isLoading.value = false; return@addOnSuccessListener }
                val tid = docs.documents[0].getString("teacherId") ?: ""
                
                db.collection("teachers").whereEqualTo("teacherId", tid).get()
                    .addOnSuccessListener { tDocs ->
                        val t = tDocs.documents.firstOrNull()
                        _myTeacher.value = Teacher(
                            teacherId = tid,
                            firstName = t?.getString("firstName") ?: "",
                            lastName = t?.getString("lastName") ?: ""
                        )
                        startListener(tid)
                    }
            }
    }

    private fun startListener(tid: String) {
        listener?.remove()
        listener = db.collection("courses").whereEqualTo("teacherId", tid)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) { _message.value = "ERROR:${error.message}"; return@addSnapshotListener }
                _courses.value = snapshot?.documents?.map { doc ->
                    Course(
                        courseId = doc.getString("courseId") ?: "",
                        courseName = doc.getString("courseName") ?: "",
                        courseCode = doc.getString("courseCode") ?: "",
                        schedule = doc.getString("schedule") ?: "",
                        teacherId = doc.getString("teacherId") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        docId = doc.id
                    )
                } ?: emptyList()
            }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
