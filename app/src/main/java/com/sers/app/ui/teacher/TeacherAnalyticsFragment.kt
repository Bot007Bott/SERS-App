package com.sers.app.ui.teacher

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.databinding.FragmentTeacherAnalyticsBinding

class TeacherAnalyticsFragment : Fragment() {

    private lateinit var binding: FragmentTeacherAnalyticsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) return@addOnSuccessListener
                val teacherId = userDocs.documents[0].getString("teacherId") ?: return@addOnSuccessListener
                loadData(teacherId)
            }
    }

    private fun loadData(teacherId: String) {
        db.collection("courses").whereEqualTo("teacherId", teacherId).get()
            .addOnSuccessListener { courseDocs ->
                val courseIds = courseDocs.documents.mapNotNull { it.getString("courseId") }
                if (courseIds.isEmpty()) {
                    setupSummaryCards(emptyList(), 0, 0, 0)
                    return@addOnSuccessListener
                }

                db.collection("grades").get().addOnSuccessListener { gradeDocs ->
                    val myScores = gradeDocs.documents
                        .filter { it.getString("courseId") in courseIds }
                        .map { doc ->
                            val score = (doc.getLong("score") ?: 0).toInt()
                            val total = (doc.getLong("totalMarks") ?: 100).toInt()
                            if (total > 0) score.toFloat() / total * 100 else 0f
                        }

                    db.collection("attendance").get().addOnSuccessListener { attDocs ->
                        val myAtt = attDocs.documents.filter { it.getString("courseId") in courseIds }
                        val present = myAtt.count { it.getString("status") == "Present" }
                        val absent = myAtt.count { it.getString("status") == "Absent" }
                        val late = myAtt.count { it.getString("status") == "Late" }

                        setupSummaryCards(myScores, present, absent, late)
                        setupGradePieChart(myScores)
                        setupAttendancePieChart(present, absent, late)
                    }
                }
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
}