package com.sers.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sers.app.R
import com.sers.app.databinding.FragmentStudentDashboardBinding
import com.sers.app.viewmodel.StudentDashboardViewModel

/**
 * StudentDashboardFragment — MVVM View
 * Observes StudentDashboardViewModel LiveData to display student stats.
 * No Firebase code here.
 */
class StudentDashboardFragment : Fragment() {

    private lateinit var binding: FragmentStudentDashboardBinding
    private val viewModel: StudentDashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigation
        binding.cardGrades.setOnClickListener { findNavController().navigate(R.id.studentGradesFragment) }
        binding.cardAttendance.setOnClickListener { findNavController().navigate(R.id.studentAttendanceFragment) }
        binding.cardProfile.setOnClickListener { findNavController().navigate(R.id.studentProfileFragment) }
        binding.cardReport.setOnClickListener { findNavController().navigate(R.id.studentReportFragment) }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.studentSettingsFragment) }

        // Observe LiveData from ViewModel
        viewModel.myCourses.observe(viewLifecycleOwner) { binding.tvMyCourses.text = it }
        viewModel.avgGrade.observe(viewLifecycleOwner) { binding.tvAvgGrade.text = it }
        viewModel.attendanceRate.observe(viewLifecycleOwner) { binding.tvAttendanceRate.text = it }
        viewModel.absentCount.observe(viewLifecycleOwner) { binding.tvAbsent.text = it }

        // Load data via ViewModel
        viewModel.loadStats()
    }
}