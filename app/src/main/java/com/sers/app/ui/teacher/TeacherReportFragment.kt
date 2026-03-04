package com.sers.app.ui.teacher

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
import com.sers.app.databinding.FragmentTeacherReportBinding
import com.sers.app.model.*
import com.sers.app.viewmodel.ReportViewModel

/**
 * TeacherReportFragment — MVVM View
 * Observes ReportViewModel for cross-entity data.
 */
class TeacherReportFragment : Fragment() {

    private lateinit var binding: FragmentTeacherReportBinding
    private val viewModel: ReportViewModel by viewModels()

    private val courseList = mutableListOf<Course>()
    private val studentList = mutableListOf<Student>()
    private val enrollmentList = mutableListOf<Enrollment>()
    private val gradeList = mutableListOf<Grade>()
    private val attendanceList = mutableListOf<Attendance>()

    private var myTeacherId = ""
    private var myTeacherName = ""
    private var selectedStudentId = ""
    private var selectedCourseId = ""
    private var selectedMonth = ""
    private var selectedYear = ""
    private var reportGenerated = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reportContent.visibility = View.GONE
        binding.exportButtons.visibility = View.GONE

        setupObservers()
        setupListeners()

        // We need the teacherId to load data. For now let's assume it's fetched or available.
        // Actually, we should get it from Auth.
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                myTeacherId = docs.documents[0].getString("teacherId") ?: ""
                myTeacherName = "${docs.documents[0].getString("firstName")} ${docs.documents[0].getString("lastName")}"
                viewModel.loadAllData()
            }
    }

    private fun setupObservers() {
        viewModel.courses.observe(viewLifecycleOwner) { list -> 
            courseList.clear(); courseList.addAll(list.filter { it.teacherId == myTeacherId })
        }
        viewModel.students.observe(viewLifecycleOwner) { list -> studentList.clear(); studentList.addAll(list) }
        viewModel.enrollments.observe(viewLifecycleOwner) { list -> enrollmentList.clear(); enrollmentList.addAll(list) }
        viewModel.grades.observe(viewLifecycleOwner) { list -> gradeList.clear(); gradeList.addAll(list) }
        viewModel.attendance.observe(viewLifecycleOwner) { list -> attendanceList.clear(); attendanceList.addAll(list) }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.message.observe(viewLifecycleOwner) { if (it.isNotEmpty()) Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
    }

    private fun setupListeners() {
        binding.btnSelectStudent.setOnClickListener {
            val enrolled = enrollmentList.filter { e -> courseList.any { it.courseId == e.courseId } }.map { it.studentId }.distinct()
            val opts = studentList.filter { it.studentId in enrolled }.map { Pair("${it.firstName} ${it.lastName}", it.studentId) }
            showPickerSheet("Select Student", listOf(Pair("All Students", "")) + opts) { n, id -> 
                selectedStudentId = id; binding.btnSelectStudent.text = n 
            }
        }
        binding.btnSelectCourse.setOnClickListener {
            val opts = courseList.map { Pair(it.courseName, it.courseId) }
            showPickerSheet("Select Course", listOf(Pair("All Courses", "")) + opts) { n, id -> 
                selectedCourseId = id; binding.btnSelectCourse.text = n 
            }
        }
        binding.btnGenerateReport.setOnClickListener { generateReport() }
        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.btnExportCsv.setOnClickListener { exportCsv() }
    }

    private fun generateReport() {
        binding.reportContent.visibility = View.VISIBLE
        binding.exportButtons.visibility = View.VISIBLE
        reportGenerated = true
        val reportText = StringBuilder()
        reportText.append("Teacher: $myTeacherName\n\n")
        
        val targetCourses = if (selectedCourseId.isEmpty()) courseList else courseList.filter { it.courseId == selectedCourseId }
        val enrolledSIds = enrollmentList.filter { e -> targetCourses.any { it.courseId == e.courseId } }
            .filter { selectedStudentId.isEmpty() || it.studentId == selectedStudentId }
            .map { it.studentId }.distinct()
        
        enrolledSIds.forEach { sid ->
            val s = studentList.find { it.studentId == sid } ?: return@forEach
            reportText.append("${s.firstName} ${s.lastName} ($sid)\n")
            val grades = gradeList.filter { it.studentId == sid && targetCourses.any { tc -> tc.courseId == it.courseId } }
            reportText.append("  Grades: ${grades.joinToString { "${it.score}/${it.totalMarks}" }}\n")
            val att = attendanceList.filter { it.studentId == sid && targetCourses.any { tc -> tc.courseId == it.courseId } }
            reportText.append("  Attendance: ${att.count { it.status == "Present" }}/${att.size} Present\n\n")
        }
        binding.tvReportContent.text = reportText.toString()
    }

    private fun exportPdf() {}
    private fun exportCsv() {}

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