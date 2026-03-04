package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

/**
 * AnalyticsViewModel — MVVM ViewModel
 * Loads grades and attendance data for AnalyticsFragment (Admin) and TeacherAnalyticsFragment.
 */
class AnalyticsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    data class AnalyticsData(
        val scores: List<Float>,
        val present: Int,
        val absent: Int,
        val late: Int
    )

    private val _analyticsData = MutableLiveData<AnalyticsData>()
    val analyticsData: LiveData<AnalyticsData> = _analyticsData

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadData(teacherId: String? = null) {
        val gradesQuery = db.collection("grades")

        gradesQuery.get().addOnSuccessListener { gradeDocs ->
            val scores = gradeDocs.documents.map { doc ->
                val score = (doc.getLong("score") ?: 0).toInt()
                val total = (doc.getLong("totalMarks") ?: 100).toInt()
                if (total > 0) score.toFloat() / total * 100 else 0f
            }

            val attQuery = if (teacherId != null) {
                // Teacher-specific: load only courses for this teacher first
                loadTeacherAttendance(scores, teacherId)
                return@addOnSuccessListener
            } else {
                db.collection("attendance")
            }

            attQuery.get().addOnSuccessListener { attDocs ->
                val present = attDocs.documents.count { it.getString("status") == "Present" }
                val absent = attDocs.documents.count { it.getString("status") == "Absent" }
                val late = attDocs.documents.count { it.getString("status") == "Late" }
                _analyticsData.value = AnalyticsData(scores, present, absent, late)
            }.addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
        }.addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    private fun loadTeacherAttendance(scores: List<Float>, teacherId: String) {
        db.collection("courses").whereEqualTo("teacherId", teacherId).get()
            .addOnSuccessListener { courseDocs ->
                val courseIds = courseDocs.documents.map { it.getString("courseId") ?: "" }
                db.collection("attendance").get().addOnSuccessListener { attDocs ->
                    val myAtt = attDocs.documents.filter { doc ->
                        doc.getString("courseId") in courseIds
                    }
                    val present = myAtt.count { it.getString("status") == "Present" }
                    val absent = myAtt.count { it.getString("status") == "Absent" }
                    val late = myAtt.count { it.getString("status") == "Late" }
                    _analyticsData.value = AnalyticsData(scores, present, absent, late)
                }
            }
    }
}
