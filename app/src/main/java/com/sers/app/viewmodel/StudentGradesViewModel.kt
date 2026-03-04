package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Course
import com.sers.app.model.Grade

/**
 * StudentGradesViewModel — MVVM ViewModel
 * Loads grades and courses for StudentGradesFragment.
 */
class StudentGradesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var gradeListener: ListenerRegistration? = null

    private val _grades = MutableLiveData<List<Grade>>()
    val grades: LiveData<List<Grade>> = _grades

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
                startGradeListener(studentId)
            }
    }

    private fun startGradeListener(studentId: String) {
        gradeListener = db.collection("grades").whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { _message.value = "ERROR:${error.message}"; return@addSnapshotListener }
                val gradeList = snapshot?.documents?.map { doc ->
                    Grade(
                        gradeId = doc.getString("gradeId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        gradeType = doc.getString("gradeType") ?: "",
                        title = doc.getString("title") ?: "",
                        score = (doc.getLong("score") ?: 0).toInt(),
                        totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(),
                        docId = doc.id
                    )
                } ?: emptyList()
                _grades.value = gradeList

                val courseIds = gradeList.map { it.courseId }.distinct()
                if (courseIds.isNotEmpty()) {
                    db.collection("courses").whereIn("courseId", courseIds).get()
                        .addOnSuccessListener { courseDocs ->
                            _courses.value = courseDocs.documents.map { doc ->
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
                        }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        gradeListener?.remove()
    }
}
