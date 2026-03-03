package com.sers.app.ui.admin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.databinding.FragmentAnalyticsBinding

class AnalyticsFragment : Fragment() {

    private lateinit var binding: FragmentAnalyticsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    private fun loadData() {
        db.collection("grades").get().addOnSuccessListener { gradeDocs ->
            val scores = gradeDocs.documents.map { doc ->
                val score = (doc.getLong("score") ?: 0).toInt()
                val total = (doc.getLong("totalMarks") ?: 100).toInt()
                if (total > 0) score.toFloat() / total * 100 else 0f
            }

            db.collection("attendance").get().addOnSuccessListener { attDocs ->
                val present = attDocs.documents.count { it.getString("status") == "Present" }
                val absent = attDocs.documents.count { it.getString("status") == "Absent" }
                val late = attDocs.documents.count { it.getString("status") == "Late" }
                val total = present + absent + late

                setupSummaryCards(scores, present, total)
                setupGradePieChart(scores)
                setupAttendancePieChart(present, absent, late)
                setupAttendanceLineChart(present, absent, late)
            }
        }
    }

    private fun setupSummaryCards(scores: List<Float>, present: Int, total: Int) {
        val avg = if (scores.isNotEmpty()) scores.average().toInt() else 0
        val attendanceRate = if (total > 0) (present * 100 / total) else 0
        binding.tvTotalGrades.text = scores.size.toString()
        binding.tvAvgPerformance.text = "$avg%"
        binding.tvSummaryGrades.text = scores.size.toString()
        binding.tvSummaryAvg.text = "$avg%"
        binding.tvSummaryAttendance.text = "$attendanceRate%"
    }

    private fun setupGradePieChart(scores: List<Float>) {
        val excellent = scores.count { it >= 90 }
        val good = scores.count { it in 75f..89f }
        val needsWork = scores.count { it < 75 }

        val entries = mutableListOf<PieEntry>()
        if (excellent > 0) entries.add(PieEntry(excellent.toFloat(), "Excellent (90+)"))
        if (good > 0) entries.add(PieEntry(good.toFloat(), "Good (75-89)"))
        if (needsWork > 0) entries.add(PieEntry(needsWork.toFloat(), "Needs Work (<75)"))

        if (entries.isEmpty()) return

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#43A047"), Color.parseColor("#1976D2"), Color.parseColor("#E53935"))
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }
        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.WHITE)
            legend.isEnabled = true
            animateY(1000)
            invalidate()
        }
    }

    private fun setupAttendancePieChart(present: Int, absent: Int, late: Int) {
        val entries = mutableListOf<PieEntry>()
        if (present > 0) entries.add(PieEntry(present.toFloat(), "Present"))
        if (absent > 0) entries.add(PieEntry(absent.toFloat(), "Absent"))
        if (late > 0) entries.add(PieEntry(late.toFloat(), "Late"))

        if (entries.isEmpty()) return

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#43A047"), Color.parseColor("#E53935"), Color.parseColor("#FB8C00"))
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }
        binding.pieChartAttendance.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.WHITE)
            legend.isEnabled = true
            animateY(1000)
            invalidate()
        }
    }

    private fun setupAttendanceLineChart(present: Int, absent: Int, late: Int) {
        val entries = listOf(
            Entry(0f, present.toFloat()),
            Entry(1f, absent.toFloat()),
            Entry(2f, late.toFloat())
        )
        val dataSet = LineDataSet(entries, "Attendance").apply {
            color = Color.parseColor("#1976D2")
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#1976D2"))
            setDrawFilled(true)
            fillColor = Color.parseColor("#BBDEFB")
        }
        binding.lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            animateX(1000)
            invalidate()
        }
    }
}