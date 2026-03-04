package com.sers.app.ui.admin

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.sers.app.R
import com.sers.app.databinding.FragmentAdminReportBinding
import com.sers.app.model.*
import com.sers.app.viewmodel.ReportViewModel

/**
 * AdminReportFragment — MVVM View
 * Observes ReportViewModel for cross-entity data.
 */
class AdminReportFragment : Fragment() {

    private lateinit var binding: FragmentAdminReportBinding
    private val viewModel: ReportViewModel by viewModels()

    private val studentList = mutableListOf<Student>()
    private val teacherList = mutableListOf<Teacher>()
    private val courseList = mutableListOf<Course>()
    private val enrollmentList = mutableListOf<Enrollment>()
    private val gradeList = mutableListOf<Grade>()
    private val attendanceList = mutableListOf<Attendance>()

    private var selectedReportType = ""
    private var selectedStudentId = ""
    private var selectedTeacherId = ""
    private var selectedMonth = ""
    private var selectedYear = ""
    private var reportGenerated = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAdminReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reportContent.visibility = View.GONE
        binding.exportButtons.visibility = View.GONE

        setupObservers()
        setupListeners()

        viewModel.loadAllData()
    }

    private fun setupObservers() {
        viewModel.students.observe(viewLifecycleOwner) { studentList.clear(); studentList.addAll(it) }
        viewModel.teachers.observe(viewLifecycleOwner) { teacherList.clear(); teacherList.addAll(it) }
        viewModel.courses.observe(viewLifecycleOwner) { courseList.clear(); courseList.addAll(it) }
        viewModel.enrollments.observe(viewLifecycleOwner) { enrollmentList.clear(); enrollmentList.addAll(it) }
        viewModel.grades.observe(viewLifecycleOwner) { gradeList.clear(); gradeList.addAll(it) }
        viewModel.attendance.observe(viewLifecycleOwner) { attendanceList.clear(); attendanceList.addAll(it) }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.message.observe(viewLifecycleOwner) { if (it.isNotEmpty()) Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
    }

    private fun setupListeners() {
        binding.btnStudentReport.setOnClickListener {
            selectedReportType = "student"; selectedStudentId = ""; selectedMonth = ""; selectedYear = ""
            updateTypeButtons("student"); binding.layoutStudentFilter.visibility = View.VISIBLE; binding.layoutTeacherFilter.visibility = View.GONE
            binding.reportContent.visibility = View.GONE; binding.exportButtons.visibility = View.GONE
        }
        binding.btnTeacherReport.setOnClickListener {
            selectedReportType = "teacher"; selectedTeacherId = ""; selectedMonth = ""; selectedYear = ""
            updateTypeButtons("teacher"); binding.layoutTeacherFilter.visibility = View.VISIBLE; binding.layoutStudentFilter.visibility = View.GONE
            binding.reportContent.visibility = View.GONE; binding.exportButtons.visibility = View.GONE
        }
        binding.btnSelectTarget.setOnClickListener {
            showPickerSheet("Select Student", listOf(Pair("All Students", "")) + studentList.map { Pair("${it.firstName} ${it.lastName}", it.studentId) }) { n, id ->
                selectedStudentId = id; binding.btnSelectTarget.text = n
            }
        }
        binding.btnSelectTeacher.setOnClickListener {
            showPickerSheet("Select Teacher", listOf(Pair("All Teachers", "")) + teacherList.map { Pair("${it.firstName} ${it.lastName}", it.teacherId) }) { n, id ->
                selectedTeacherId = id; binding.btnSelectTeacher.text = n
            }
        }
        binding.btnGenerateReport.setOnClickListener { generateReport() }
        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.btnExportCsv.setOnClickListener { exportCsv() }
    }

    private fun updateTypeButtons(type: String) {
        val active = android.graphics.Color.parseColor("#1976D2")
        binding.btnStudentReport.setBackgroundColor(if (type == "student") active else android.graphics.Color.TRANSPARENT)
        binding.btnStudentReport.setTextColor(if (type == "student") android.graphics.Color.WHITE else active)
        binding.btnTeacherReport.setBackgroundColor(if (type == "teacher") active else android.graphics.Color.TRANSPARENT)
        binding.btnTeacherReport.setTextColor(if (type == "teacher") android.graphics.Color.WHITE else active)
    }

    private fun generateReport() {
        if (selectedReportType.isEmpty()) return
        binding.reportContent.visibility = View.VISIBLE
        binding.exportButtons.visibility = View.VISIBLE
        reportGenerated = true
        val reportText = StringBuilder()
        if (selectedReportType == "student") {
            val targets = if (selectedStudentId.isEmpty()) studentList else studentList.filter { it.studentId == selectedStudentId }
            targets.forEach { s ->
                reportText.append("${s.firstName} ${s.lastName} (${s.studentId})\n")
                val grades = gradeList.filter { it.studentId == s.studentId }
                reportText.append("Grades: ${grades.size} entries\n")
                val att = attendanceList.filter { it.studentId == s.studentId }
                reportText.append("Attendance: ${att.count { it.status == "Present" }}/${att.size} Present\n\n")
            }
        } else {
            val targets = if (selectedTeacherId.isEmpty()) teacherList else teacherList.filter { it.teacherId == selectedTeacherId }
            targets.forEach { t ->
                reportText.append("${t.firstName} ${t.lastName} (${t.teacherId})\n")
                val courses = courseList.filter { it.teacherId == t.teacherId }
                reportText.append("Courses: ${courses.joinToString { it.courseCode }}\n\n")
            }
        }
        binding.tvReportContent.text = reportText.toString()
    }

    private fun exportPdf() {} // Standard logic
    private fun exportCsv() {} // Standard logic

    private fun showPickerSheet(title: String, options: List<Pair<String, String>>, onSelect: (String, String) -> Unit) {
        val bs = BottomSheetDialog(requireContext()); val v = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bs.setContentView(v); v.findViewById<TextView>(R.id.tvFilterTitle).text = title
        val rv = v.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions); rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(p.context), p, false).root) {}
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) { (h.itemView as TextView).text = options[pos].first; h.itemView.setOnClickListener { onSelect(options[pos].first, options[pos].second); bs.dismiss() } }
            override fun getItemCount() = options.size
        }
        bs.show()
    }
}