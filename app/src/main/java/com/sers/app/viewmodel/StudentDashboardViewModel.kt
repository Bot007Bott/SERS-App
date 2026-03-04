package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * StudentDashboardViewModel — MVVM ViewModel
 * Loads student statistics for StudentDashboardFragment.
 */
class StudentDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _myCourses = MutableLiveData<String>()
    val myCourses: LiveData<String> = _myCourses

    private val _avgGrade = MutableLiveData<String>()
    val avgGrade: LiveData<String> = _avgGrade

    private val _attendanceRate = MutableLiveData<String>()
    val attendanceRate: LiveData<String> = _attendanceRate

    private val _absentCount = MutableLiveData<String>()
    val absentCount: LiveData<String> = _absentCount

    fun loadStats() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) return@addOnSuccessListener
                val studentId = userDocs.documents[0].getString("studentId") ?: return@addOnSuccessListener
                loadStudentStats(studentId)
            }
    }

    private fun loadStudentStats(studentId: String) {
        // Enrollment count
        db.collection("enrollments").whereEqualTo("studentId", studentId).get()
            .addOnSuccessListener { enrollDocs ->
                _myCourses.value = enrollDocs.size().toString()
            }

        // Average grade
        db.collection("grades").whereEqualTo("studentId", studentId).get()
            .addOnSuccessListener { gradeDocs ->
                if (gradeDocs.isEmpty) { _avgGrade.value = "0%"; return@addOnSuccessListener }
                val avg = gradeDocs.documents.mapNotNull { doc ->
                    val score = (doc.getLong("score") ?: 0).toInt()
                    val total = (doc.getLong("totalMarks") ?: 100).toInt()
                    if (total > 0) score.toFloat() / total * 100 else null
                }.average().toInt()
                _avgGrade.value = "$avg%"
            }

        // Attendance rate and absences
        db.collection("attendance").whereEqualTo("studentId", studentId).get()
            .addOnSuccessListener { attDocs ->
                val total = attDocs.size()
                val present = attDocs.documents.count { it.getString("status") == "Present" }
                val absent = attDocs.documents.count { it.getString("status") == "Absent" }
                val rate = if (total > 0) (present * 100 / total) else 0
                _attendanceRate.value = "$rate%"
                _absentCount.value = absent.toString()
            }
    }
}
