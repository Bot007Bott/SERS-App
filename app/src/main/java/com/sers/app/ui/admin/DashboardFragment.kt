package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sers.app.R
import com.sers.app.databinding.FragmentDashboardBinding
import com.sers.app.viewmodel.DashboardViewModel

/**
 * DashboardFragment — MVVM View
 * Observes DashboardViewModel LiveData and updates the UI.
 * No Firebase or data logic here.
 */
class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe ViewModel LiveData
        viewModel.userCount.observe(viewLifecycleOwner) { binding.tvTotalUsers.text = it }
        viewModel.studentCount.observe(viewLifecycleOwner) { binding.tvTotalStudents.text = it }
        viewModel.courseCount.observe(viewLifecycleOwner) { binding.tvTotalCourses.text = it }
        viewModel.teacherCount.observe(viewLifecycleOwner) { binding.tvTotalTeachers.text = it }

        // Load data via ViewModel
        viewModel.loadStats()

        setupNavigation()
    }

    private fun setupNavigation() {
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
}
