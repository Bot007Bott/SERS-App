package com.sers.app.ui.teacher

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.sers.app.databinding.FragmentTeacherAnalyticsBinding
import com.sers.app.viewmodel.TeacherAnalyticsViewModel

/**
 * TeacherAnalyticsFragment — MVVM View
 * Observes TeacherAnalyticsViewModel for chart data.
 */
class TeacherAnalyticsFragment : Fragment() {

    private lateinit var binding: FragmentTeacherAnalyticsBinding
    private val viewModel: TeacherAnalyticsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        viewModel.loadData()
    }

    private fun setupObservers() {
        viewModel.analyticsData.observe(viewLifecycleOwner) { data ->
            setupSummaryCards(data.scores, data.present, data.absent, data.late)
            setupGradePieChart(data.scores)
            setupAttendancePieChart(data.present, data.absent, data.late)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            // binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun setupSummaryCards(scores: List<Float>, present: Int, absent: Int, late: Int) {
        val avg = if (scores.isNotEmpty()) scores.average().toInt() else 0
        val total = present + absent + late
        val attendanceRate = if (total > 0) (present * 100 / total) else 0
        binding.tvTotalGrades.text = scores.size.toString()
        binding.tvAvgPerformance.text = "$avg%"
        binding.tvSummaryGrades.text = scores.size.toString()
        binding.tvSummaryAvg.text = "$avg%"
        binding.tvSummaryAttendance.text = "$attendanceRate%"
    }

    private fun setupGradePieChart(scores: List<Float>) {
        val e = scores.count { it >= 90 }; val g = scores.count { it in 75f..89f }; val n = scores.count { it < 75 }
        val entries = mutableListOf<PieEntry>()
        if (e > 0) entries.add(PieEntry(e.toFloat(), "Excellent"))
        if (g > 0) entries.add(PieEntry(g.toFloat(), "Good"))
        if (n > 0) entries.add(PieEntry(n.toFloat(), "Needs Work"))
        if (entries.isEmpty()) return
        val ds = PieDataSet(entries, "").apply { colors = listOf(Color.parseColor("#43A047"), Color.parseColor("#1976D2"), Color.parseColor("#E53935")); valueTextColor = Color.WHITE; valueTextSize = 12f }
        binding.pieChart.let { chart ->
            chart.data = PieData(ds)
            chart.description.isEnabled = false
            chart.isDrawHoleEnabled = true
            chart.animateY(1000)
            chart.invalidate()
        }
    }

    private fun setupAttendancePieChart(p: Int, a: Int, l: Int) {
        val entries = mutableListOf<PieEntry>()
        if (p > 0) entries.add(PieEntry(p.toFloat(), "Present"))
        if (a > 0) entries.add(PieEntry(a.toFloat(), "Absent"))
        if (l > 0) entries.add(PieEntry(l.toFloat(), "Late"))
        if (entries.isEmpty()) return
        val ds = PieDataSet(entries, "").apply { colors = listOf(Color.parseColor("#43A047"), Color.parseColor("#E53935"), Color.parseColor("#FB8C00")); valueTextColor = Color.WHITE; valueTextSize = 12f }
        binding.pieChartAttendance.let { chart ->
            chart.data = PieData(ds)
            chart.description.isEnabled = false
            chart.isDrawHoleEnabled = true
            chart.animateY(1000)
            chart.invalidate()
        }
    }
}