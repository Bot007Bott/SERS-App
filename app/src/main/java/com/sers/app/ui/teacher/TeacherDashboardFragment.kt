package com.sers.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentTeacherDashboardBinding

class TeacherDashboardFragment : Fragment() {

    private lateinit var binding: FragmentTeacherDashboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardCourses.setOnClickListener { findNavController().navigate(R.id.teacherCoursesFragment) }
        binding.cardGrades.setOnClickListener { findNavController().navigate(R.id.teacherGradesFragment) }
        binding.cardAttendance.setOnClickListener { findNavController().navigate(R.id.teacherAttendanceFragment) }
        binding.cardAnalytics.setOnClickListener { findNavController().navigate(R.id.teacherAnalyticsFragment) }
        binding.cardReport.setOnClickListener { findNavController().navigate(R.id.teacherReportFragment) }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.teacherSettingsFragment) }

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) return@addOnSuccessListener
                val teacherId = userDocs.documents[0].getString("teacherId") ?: return@addOnSuccessListener
                loadStats(teacherId)
            }
    }

    private fun loadStats(teacherId: String) {
        // My courses
        db.collection("courses").whereEqualTo("teacherId", teacherId).get()
            .addOnSuccessListener { courseDocs ->
                val courseIds = courseDocs.documents.mapNotNull { it.getString("courseId") }
                binding.tvMyCourses.text = courseIds.size.toString()

                if (courseIds.isEmpty()) {
                    binding.tvMyStudents.text = "0"
                    binding.tvGradesRecorded.text = "0"
                    binding.tvAvgPerformance.text = "0%"
                    return@addOnSuccessListener
                }

                // Enrolled students in my courses
                db.collection("enrollments").get().addOnSuccessListener { enrollDocs ->
                    val studentIds = enrollDocs.documents
                        .filter { it.getString("courseId") in courseIds }
                        .mapNotNull { it.getString("studentId") }.distinct()
                    binding.tvMyStudents.text = studentIds.size.toString()
                }

                // Grades in my courses
                db.collection("grades").get().addOnSuccessListener { gradeDocs ->
                    val myGrades = gradeDocs.documents.filter { it.getString("courseId") in courseIds }
                    binding.tvGradesRecorded.text = myGrades.size.toString()
                    if (myGrades.isNotEmpty()) {
                        val avg = myGrades.mapNotNull { doc ->
                            val score = (doc.getLong("score") ?: 0).toInt()
                            val total = (doc.getLong("totalMarks") ?: 100).toInt()
                            if (total > 0) score.toFloat() / total * 100 else null
                        }.average().toInt()
                        binding.tvAvgPerformance.text = "$avg%"
                    } else {
                        binding.tvAvgPerformance.text = "0%"
                    }
                }
            }
    }
}