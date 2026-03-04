package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.model.*

/**
 * TeacherDashboardViewModel — MVVM ViewModel for Teacher's dashboard stats.
 */
class TeacherDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _stats = MutableLiveData<Map<String, String>>()
    val stats: LiveData<Map<String, String>> = _stats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadStats() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) { _isLoading.value = false; return@addOnSuccessListener }
                val tid = userDocs.documents[0].getString("teacherId") ?: ""
                
                db.collection("courses").whereEqualTo("teacherId", tid).get()
                    .addOnSuccessListener { courseDocs ->
                        val courseIds = courseDocs.documents.mapNotNull { it.getString("courseId") }
                        val courseCount = courseIds.size
                        
                        if (courseIds.isEmpty()) {
                            _stats.value = mapOf("courses" to "0", "students" to "0", "grades" to "0", "avg" to "0%")
                            _isLoading.value = false
                            return@addOnSuccessListener
                        }

                        db.collection("enrollments").get().addOnSuccessListener { enrollDocs ->
                            val studentCount = enrollDocs.documents
                                .filter { it.getString("courseId") in courseIds }
                                .mapNotNull { it.getString("studentId") }.distinct().size

                            db.collection("grades").get().addOnSuccessListener { gradeDocs ->
                                val myGrades = gradeDocs.documents.filter { it.getString("courseId") in courseIds }
                                val gradeCount = myGrades.size
                                val avg = if (myGrades.isNotEmpty()) {
                                    myGrades.mapNotNull { doc ->
                                        val s = doc.getLong("score") ?: 0
                                        val t = doc.getLong("totalMarks") ?: 100
                                        if (t > 0) s.toFloat() / t * 100 else null
                                    }.average().toInt()
                                } else 0

                                _stats.value = mapOf(
                                    "courses" to courseCount.toString(),
                                    "students" to studentCount.toString(),
                                    "grades" to gradeCount.toString(),
                                    "avg" to "$avg%"
                                )
                                _isLoading.value = false
                            }
                        }
                    }
            }
    }
}
