package com.sers.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sers.app.R
import com.sers.app.databinding.FragmentTeacherDashboardBinding
import com.sers.app.viewmodel.TeacherDashboardViewModel

/**
 * TeacherDashboardFragment — MVVM View
 * Observes TeacherDashboardViewModel for stats.
 */
class TeacherDashboardFragment : Fragment() {

    private lateinit var binding: FragmentTeacherDashboardBinding
    private val viewModel: TeacherDashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNavigation()
        setupObservers()

        viewModel.loadStats()
    }

    private fun setupNavigation() {
        binding.cardCourses.setOnClickListener { findNavController().navigate(R.id.teacherCoursesFragment) }
        binding.cardGrades.setOnClickListener { findNavController().navigate(R.id.teacherGradesFragment) }
        binding.cardAttendance.setOnClickListener { findNavController().navigate(R.id.teacherAttendanceFragment) }
        binding.cardAnalytics.setOnClickListener { findNavController().navigate(R.id.teacherAnalyticsFragment) }
        binding.cardReport.setOnClickListener { findNavController().navigate(R.id.teacherReportFragment) }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.teacherSettingsFragment) }
    }

    private fun setupObservers() {
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.tvMyCourses.text = stats["courses"]
            binding.tvMyStudents.text = stats["students"]
            binding.tvGradesRecorded.text = stats["grades"]
            binding.tvAvgPerformance.text = stats["avg"]
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            // Can show progress if needed
        }
    }
}