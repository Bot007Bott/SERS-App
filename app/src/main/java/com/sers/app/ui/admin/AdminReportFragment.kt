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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentAdminReportBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Enrollment
import com.sers.app.model.Grade
import com.sers.app.model.Student
import com.sers.app.model.Teacher

class AdminReportFragment : Fragment() {

    private lateinit var binding: FragmentAdminReportBinding
    private val db = FirebaseFirestore.getInstance()

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
    private var dataLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAdminReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reportContent.visibility = View.GONE
        binding.exportButtons.visibility = View.GONE

        loadAllData()

        binding.btnStudentReport.setOnClickListener {
            selectedReportType = "student"
            selectedStudentId = ""; selectedTeacherId = ""; selectedMonth = ""; selectedYear = ""
            binding.btnStudentReport.setBackgroundColor(android.graphics.Color.parseColor("#1976D2"))
            binding.btnStudentReport.setTextColor(android.graphics.Color.WHITE)
            binding.btnTeacherReport.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnTeacherReport.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            binding.layoutStudentFilter.visibility = View.VISIBLE
            binding.layoutTeacherFilter.visibility = View.GONE
            binding.reportContent.visibility = View.GONE
            binding.exportButtons.visibility = View.GONE
            binding.btnSelectTarget.text = "Select Student"
            binding.btnSelectMonthStudent.text = "All Months"
            binding.btnSelectYearStudent.text = "All Years"
        }

        binding.btnTeacherReport.setOnClickListener {
            selectedReportType = "teacher"
            selectedStudentId = ""; selectedTeacherId = ""; selectedMonth = ""; selectedYear = ""
            binding.btnTeacherReport.setBackgroundColor(android.graphics.Color.parseColor("#1976D2"))
            binding.btnTeacherReport.setTextColor(android.graphics.Color.WHITE)
            binding.btnStudentReport.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnStudentReport.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            binding.layoutTeacherFilter.visibility = View.VISIBLE
            binding.layoutStudentFilter.visibility = View.GONE
            binding.reportContent.visibility = View.GONE
            binding.exportButtons.visibility = View.GONE
            binding.btnSelectTeacher.text = "Select Teacher"
            binding.btnSelectMonthTeacher.text = "All Months"
            binding.btnSelectYearTeacher.text = "All Years"
        }

        binding.btnSelectTarget.setOnClickListener {
            val options = listOf(Pair("All Students", "")) +
                    studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }
            showPickerSheet("Select Student", options) { name, id ->
                selectedStudentId = id
                binding.btnSelectTarget.text = if (id.isEmpty()) "All Students" else name.substringBefore(" (")
            }
        }

        binding.btnSelectTeacher.setOnClickListener {
            val options = listOf(Pair("All Teachers", "")) +
                    teacherList.map { Pair("${it.firstName} ${it.lastName} (${it.teacherId})", it.teacherId) }
            showPickerSheet("Select Teacher", options) { name, id ->
                selectedTeacherId = id
                binding.btnSelectTeacher.text = if (id.isEmpty()) "All Teachers" else name.substringBefore(" (")
            }
        }

        binding.btnSelectMonthStudent.setOnClickListener {
            showMonthPicker { name, value ->
                selectedMonth = value
                binding.btnSelectMonthStudent.text = if (name == "All") "All Months" else name
            }
        }

        binding.btnSelectYearStudent.setOnClickListener {
            showYearPicker { name, value ->
                selectedYear = value
                binding.btnSelectYearStudent.text = if (name == "All") "All Years" else name
            }
        }

        binding.btnSelectMonthTeacher.setOnClickListener {
            showMonthPicker { name, value ->
                selectedMonth = value
                binding.btnSelectMonthTeacher.text = if (name == "All") "All Months" else name
            }
        }

        binding.btnSelectYearTeacher.setOnClickListener {
            showYearPicker { name, value ->
                selectedYear = value
                binding.btnSelectYearTeacher.text = if (name == "All") "All Years" else name
            }
        }

        binding.btnGenerateReport.setOnClickListener { generateReport() }
        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.btnExportCsv.setOnClickListener { exportCsv() }
    }

    private fun loadAllData() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("students").get().addOnSuccessListener { docs ->
            studentList.clear()
            docs.forEach { doc ->
                studentList.add(Student(
                    studentId = doc.getString("studentId") ?: "",
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    program = doc.getString("program") ?: "",
                    phone = doc.getString("phone") ?: "",
                    docId = doc.id
                ))
            }
            db.collection("teachers").get().addOnSuccessListener { tDocs ->
                teacherList.clear()
                tDocs.forEach { doc ->
                    teacherList.add(Teacher(
                        teacherId = doc.getString("teacherId") ?: "",
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        email = doc.getString("email") ?: "",
                        department = doc.getString("department") ?: "",
                        phone = doc.getString("phone") ?: "",
                        docId = doc.id
                    ))
                }
                db.collection("courses").get().addOnSuccessListener { cDocs ->
                    courseList.clear()
                    cDocs.forEach { doc ->
                        courseList.add(Course(
                            courseId = doc.getString("courseId") ?: "",
                            courseName = doc.getString("courseName") ?: "",
                            courseCode = doc.getString("courseCode") ?: "",
                            schedule = doc.getString("schedule") ?: "",
                            teacherId = doc.getString("teacherId") ?: "",
                            createdBy = doc.getString("createdBy") ?: "",
                            docId = doc.id
                        ))
                    }
                    db.collection("enrollments").get().addOnSuccessListener { eDocs ->
                        enrollmentList.clear()
                        eDocs.forEach { doc ->
                            enrollmentList.add(Enrollment(
                                enrollmentId = doc.getString("enrollmentId") ?: "",
                                studentId = doc.getString("studentId") ?: "",
                                courseId = doc.getString("courseId") ?: "",
                                docId = doc.id
                            ))
                        }
                        db.collection("grades").get().addOnSuccessListener { gDocs ->
                            gradeList.clear()
                            gDocs.forEach { doc ->
                                gradeList.add(Grade(
                                    gradeId = doc.getString("gradeId") ?: "",
                                    studentId = doc.getString("studentId") ?: "",
                                    courseId = doc.getString("courseId") ?: "",
                                    score = (doc.getLong("score") ?: 0).toInt(),
                                    totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(),
                                    docId = doc.id
                                ))
                            }
                            db.collection("attendance").get().addOnSuccessListener { aDocs ->
                                attendanceList.clear()
                                aDocs.forEach { doc ->
                                    attendanceList.add(Attendance(
                                        attendanceId = doc.getString("attendanceId") ?: "",
                                        studentId = doc.getString("studentId") ?: "",
                                        courseId = doc.getString("courseId") ?: "",
                                        session = doc.getString("timeSlot") ?: "",
                                        date = doc.getString("date") ?: "",
                                        status = doc.getString("status") ?: "",
                                        docId = doc.id
                                    ))
                                }
                                dataLoaded = true
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getPeriodLabel(): String {
        val monthNames = listOf("", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        return when {
            selectedMonth.isNotEmpty() && selectedYear.isNotEmpty() ->
                "${monthNames[selectedMonth.toInt()]} $selectedYear"
            selectedMonth.isNotEmpty() -> monthNames[selectedMonth.toInt()]
            selectedYear.isNotEmpty() -> selectedYear
            else -> "All Time"
        }
    }

    private fun generateReport() {
        if (selectedReportType.isEmpty()) {
            Snackbar.make(binding.root, "Please select report type!", Snackbar.LENGTH_SHORT).show(); return
        }
        if (!dataLoaded) {
            Snackbar.make(binding.root, "Data still loading, please wait...", Snackbar.LENGTH_SHORT).show(); return
        }
        binding.reportContent.visibility = View.VISIBLE
        binding.exportButtons.visibility = View.VISIBLE
        reportGenerated = true
        if (selectedReportType == "student") generateStudentReport()
        else generateTeacherReport()
    }

    private fun generateStudentReport() {
        val studentsToShow = if (selectedStudentId.isEmpty()) studentList
        else studentList.filter { it.studentId == selectedStudentId }

        binding.tvReportTitle.text = if (selectedStudentId.isEmpty()) "All Students Report"
        else "Student Report: ${studentsToShow.firstOrNull()?.firstName} ${studentsToShow.firstOrNull()?.lastName}"
        binding.tvReportSubtitle.text = "Period: ${getPeriodLabel()}"

        val reportText = StringBuilder()
        studentsToShow.forEach { student ->
            reportText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            reportText.append("${student.firstName} ${student.lastName} (${student.studentId})\n")
            reportText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

            val grades = gradeList.filter { it.studentId == student.studentId }
            var attendance = attendanceList.filter { it.studentId == student.studentId }
            if (selectedMonth.isNotEmpty()) attendance = attendance.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }
            if (selectedYear.isNotEmpty()) attendance = attendance.filter { it.date.length >= 4 && it.date.substring(0, 4) == selectedYear }

            reportText.append("GRADES\n")
            if (grades.isEmpty()) {
                reportText.append("  No grades found\n")
            } else {
                grades.forEach { grade ->
                    val course = courseList.find { it.courseId == grade.courseId }
                    val percent = (grade.score.toFloat() / grade.totalMarks * 100).toInt()
                    val letter = when { percent >= 90 -> "A"; percent >= 80 -> "B"; percent >= 70 -> "C"; percent >= 60 -> "D"; else -> "F" }
                    reportText.append("  ${course?.courseName ?: grade.courseId}: ${grade.score}/${grade.totalMarks} ($letter)\n")
                }
            }

            val present = attendance.count { it.status == "Present" }
            val absent = attendance.count { it.status == "Absent" }
            val late = attendance.count { it.status == "Late" }
            val total = attendance.size
            val rate = if (total > 0) (present.toFloat() / total * 100).toInt() else 0

            reportText.append("ATTENDANCE\n")
            if (attendance.isEmpty()) reportText.append("  No records for ${getPeriodLabel()}\n\n")
            else reportText.append("  Present: $present | Absent: $absent | Late: $late | Rate: $rate%\n\n")
        }
        binding.tvReportContent.text = reportText.toString()
    }

    private fun generateTeacherReport() {
        val teachersToShow = if (selectedTeacherId.isEmpty()) teacherList
        else teacherList.filter { it.teacherId == selectedTeacherId }

        binding.tvReportTitle.text = if (selectedTeacherId.isEmpty()) "All Teachers Report"
        else "Teacher Report: ${teachersToShow.firstOrNull()?.firstName} ${teachersToShow.firstOrNull()?.lastName}"
        binding.tvReportSubtitle.text = "Period: ${getPeriodLabel()}"

        val reportText = StringBuilder()
        teachersToShow.forEach { teacher ->
            val teacherCourses = courseList.filter { it.teacherId == teacher.teacherId }
            var attendance = attendanceList.filter { att -> teacherCourses.any { it.courseId == att.courseId } }
            if (selectedMonth.isNotEmpty()) attendance = attendance.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }
            if (selectedYear.isNotEmpty()) attendance = attendance.filter { it.date.length >= 4 && it.date.substring(0, 4) == selectedYear }

            reportText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            reportText.append("${teacher.firstName} ${teacher.lastName} (${teacher.teacherId})\n")
            reportText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            reportText.append("COURSES TAUGHT\n")

            if (teacherCourses.isEmpty()) {
                reportText.append("  No courses assigned\n\n")
            } else {
                teacherCourses.forEach { course ->
                    val enrolledCount = enrollmentList.count { it.courseId == course.courseId }
                    val sessions = attendance.filter { it.courseId == course.courseId }.map { it.date }.distinct().size
                    reportText.append("  ${course.courseName} (${course.courseCode})\n")
                    reportText.append("  Enrolled: $enrolledCount | Sessions: $sessions\n")
                }
                reportText.append("  Total Sessions: ${attendance.map { it.date }.distinct().size}\n\n")
            }
        }
        binding.tvReportContent.text = reportText.toString()
    }

    private fun exportPdf() {
        if (!reportGenerated) return
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val paint = android.graphics.Paint()
            val pageWidth = 595; val pageHeight = 842
            val margin = 40f; val lineHeight = 16f; val maxY = pageHeight - margin
            val lines = mutableListOf<Pair<String, Boolean>>()
            lines.add(Pair(binding.tvReportTitle.text.toString(), true))
            lines.add(Pair(binding.tvReportSubtitle.text.toString(), false))
            lines.add(Pair("", false))
            binding.tvReportContent.text.toString().split("\n").forEach { lines.add(Pair(it, false)) }
            var pageNumber = 1
            var currentPage = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = currentPage.canvas
            var yPosition = 60f
            lines.forEachIndexed { index, (text, isBold) ->
                if (yPosition > maxY) {
                    pdfDocument.finishPage(currentPage); pageNumber++
                    currentPage = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = currentPage.canvas; yPosition = margin
                }
                paint.textSize = if (index == 0) 16f else if (index == 1) 12f else 10f
                paint.isFakeBoldText = isBold
                canvas.drawText(text, margin, yPosition, paint)
                yPosition += lineHeight
            }
            pdfDocument.finishPage(currentPage)
            val fileName = "SERS_Report_${System.currentTimeMillis()}.pdf"
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { requireContext().contentResolver.openOutputStream(it)?.use { stream -> pdfDocument.writeTo(stream) } }
            pdfDocument.close()
            Snackbar.make(binding.root, "PDF saved to Downloads!", Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun exportCsv() {
        if (!reportGenerated) return
        try {
            val content = binding.tvReportContent.text.toString()
            val fileName = "SERS_Report_${System.currentTimeMillis()}.csv"
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { requireContext().contentResolver.openOutputStream(it)?.use { stream -> stream.write(content.toByteArray()) } }
            Snackbar.make(binding.root, "CSV saved to Downloads!", Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showMonthPicker(onSelect: (String, String) -> Unit) {
        val months = listOf("All", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        showPickerSheet("Select Month", months.mapIndexed { i, n -> Pair(n, if (i == 0) "" else String.format("%02d", i)) }, onSelect)
    }

    private fun showYearPicker(onSelect: (String, String) -> Unit) {
        showPickerSheet("Select Year", listOf("All", "2024", "2025", "2026").map { Pair(it, if (it == "All") "" else it) }, onSelect)
    }

    private fun showPickerSheet(title: String, options: List<Pair<String, String>>, onSelect: (String, String) -> Unit) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = title
        val allOptions = options.toMutableList()
        var filteredOptions = allOptions.toMutableList()
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val filterAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class VH(val b: com.sers.app.databinding.ItemFilterOptionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val b = com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false); return VH(b)
            }
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val option = filteredOptions[position]
                (holder as VH).b.tvOption.text = option.first
                holder.b.ivCheck.visibility = View.GONE
                holder.b.root.setOnClickListener { onSelect(option.first, option.second); bottomSheet.dismiss() }
            }
            override fun getItemCount() = filteredOptions.size
            fun updateList(newList: MutableList<Pair<String, String>>) { filteredOptions = newList; notifyDataSetChanged() }
        }
        rv.adapter = filterAdapter
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch).addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().lowercase()
                filterAdapter.updateList(if (q.isEmpty()) allOptions.toMutableList() else allOptions.filter { it.first.lowercase().contains(q) }.toMutableList())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        bottomSheet.show()
    }
}