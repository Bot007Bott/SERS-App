package com.sers.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentStudentDashboardBinding

class StudentDashboardFragment : Fragment() {

    private lateinit var binding: FragmentStudentDashboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardGrades.setOnClickListener { findNavController().navigate(R.id.studentGradesFragment) }
        binding.cardAttendance.setOnClickListener { findNavController().navigate(R.id.studentAttendanceFragment) }
        binding.cardProfile.setOnClickListener { findNavController().navigate(R.id.studentProfileFragment) }
        binding.cardReport.setOnClickListener { findNavController().navigate(R.id.studentReportFragment) }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.studentSettingsFragment) }

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) return@addOnSuccessListener
                val studentId = userDocs.documents[0].getString("studentId") ?: return@addOnSuccessListener
                loadStats(studentId)
            }
    }

    private fun loadStats(studentId: String) {
        // My courses
        db.collection("enrollments").whereEqualTo("studentId", studentId).get()
            .addOnSuccessListener { enrollDocs ->
                binding.tvMyCourses.text = enrollDocs.size().toString()
            }

        // My grades
        db.collection("grades").whereEqualTo("studentId", studentId).get()
            .addOnSuccessListener { gradeDocs ->
                if (gradeDocs.isEmpty) {
                    binding.tvAvgGrade.text = "0%"
                    return@addOnSuccessListener
                }
                val avg = gradeDocs.documents.mapNotNull { doc ->
                    val score = (doc.getLong("score") ?: 0).toInt()
                    val total = (doc.getLong("totalMarks") ?: 100).toInt()
                    if (total > 0) score.toFloat() / total * 100 else null
                }.average().toInt()
                binding.tvAvgGrade.text = "$avg%"
            }

        // My attendance
        db.collection("attendance").whereEqualTo("studentId", studentId).get()
            .addOnSuccessListener { attDocs ->
                val total = attDocs.size()
                val present = attDocs.documents.count { it.getString("status") == "Present" }
                val absent = attDocs.documents.count { it.getString("status") == "Absent" }
                val rate = if (total > 0) (present * 100 / total) else 0
                binding.tvAttendanceRate.text = "$rate%"
                binding.tvAbsent.text = absent.toString()
            }
    }
}