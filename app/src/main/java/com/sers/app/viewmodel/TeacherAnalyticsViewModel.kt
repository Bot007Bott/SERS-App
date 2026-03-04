package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * TeacherAnalyticsViewModel — MVVM ViewModel for Teacher's analytics.
 */
class TeacherAnalyticsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    data class AnalyticsData(val scores: List<Float>, val present: Int, val absent: Int, val late: Int)

    private val _analyticsData = MutableLiveData<AnalyticsData>()
    val analyticsData: LiveData<AnalyticsData> = _analyticsData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { userDocs ->
            if (userDocs.isEmpty) { _isLoading.value = false; return@addOnSuccessListener }
            val tid = userDocs.documents[0].getString("teacherId") ?: ""
            
            db.collection("courses").whereEqualTo("teacherId", tid).get().addOnSuccessListener { courseDocs ->
                val cIds = courseDocs.documents.mapNotNull { it.getString("courseId") }
                if (cIds.isEmpty()) { 
                    _analyticsData.value = AnalyticsData(emptyList(), 0, 0, 0)
                    _isLoading.value = false
                    return@addOnSuccessListener 
                }

                db.collection("grades").get().addOnSuccessListener { gradeDocs ->
                    val scores = gradeDocs.documents.filter { it.getString("courseId") in cIds }
                        .map { doc ->
                            val s = doc.getLong("score") ?: 0
                            val t = doc.getLong("totalMarks") ?: 100
                            if (t > 0) s.toFloat() / t * 100 else 0f
                        }
                    
                    db.collection("attendance").get().addOnSuccessListener { attDocs ->
                        val att = attDocs.documents.filter { it.getString("courseId") in cIds }
                        val p = att.count { it.getString("status") == "Present" }
                        val a = att.count { it.getString("status") == "Absent" }
                        val l = att.count { it.getString("status") == "Late" }
                        
                        _analyticsData.value = AnalyticsData(scores, p, a, l)
                        _isLoading.value = false
                    }
                }
            }
        }
    }
}
