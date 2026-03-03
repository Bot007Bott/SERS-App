package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadStats()

        binding.cardUserManagement.setOnClickListener { findNavController().navigate(R.id.userManagementFragment) }
        binding.cardStudentManagement.setOnClickListener { findNavController().navigate(R.id.studentManagementFragment) }
        binding.cardCourses.setOnClickListener { findNavController().navigate(R.id.coursesFragment) }
        binding.cardGrades.setOnClickListener { findNavController().navigate(R.id.gradesFragment) }
        binding.cardAttendance.setOnClickListener { findNavController().navigate(R.id.attendanceFragment) }
        binding.cardAnalytics.setOnClickListener { findNavController().navigate(R.id.analyticsFragment) }
        binding.cardTeacherManagement.setOnClickListener { findNavController().navigate(R.id.teacherManagementFragment) }
        binding.cardEnrollment.setOnClickListener { findNavController().navigate(R.id.enrollmentFragment) }
        binding.cardReport.setOnClickListener { findNavController().navigate(R.id.adminReportFragment) }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.adminSettingsFragment) }
    }

    private fun loadStats() {
        db.collection("users").get().addOnSuccessListener { docs ->
            binding.tvTotalUsers.text = docs.size().toString()
        }
        db.collection("students").get().addOnSuccessListener { docs ->
            binding.tvTotalStudents.text = docs.size().toString()
        }
        db.collection("courses").get().addOnSuccessListener { docs ->
            binding.tvTotalCourses.text = docs.size().toString()
        }
        db.collection("teachers").get().addOnSuccessListener { docs ->
            binding.tvTotalTeachers.text = docs.size().toString()
        }
    }
}