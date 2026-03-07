package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sers.app.databinding.FragmentAdminReportBinding
import com.sers.app.model.*
import com.sers.app.viewmodel.ReportViewModel

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

    private val months = arrayOf(
        "All Months", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private val years = arrayOf("All Years", "2023", "2024", "2025", "2026")

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
            updateTypeButtons("student")
            binding.layoutStudentFilter.visibility = View.VISIBLE
            binding.layoutTeacherFilter.visibility = View.GONE
            binding.reportContent.visibility = View.GONE
            binding.exportButtons.visibility = View.GONE
            resetStudentFilterButtons()
        }

        binding.btnTeacherReport.setOnClickListener {
            selectedReportType = "teacher"; selectedTeacherId = ""; selectedMonth = ""; selectedYear = ""
            updateTypeButtons("teacher")
            binding.layoutTeacherFilter.visibility = View.VISIBLE
            binding.layoutStudentFilter.visibility = View.GONE
            binding.reportContent.visibility = View.GONE
            binding.exportButtons.visibility = View.GONE
            resetTeacherFilterButtons()
        }

        // Student filters
        binding.btnSelectTarget.setOnClickListener {
            val options = arrayOf("All Students") + studentList.map { "${it.firstName} ${it.lastName} (${it.studentId})" }.toTypedArray()
            val currentIndex = if (selectedStudentId.isEmpty()) 0
            else studentList.indexOfFirst { it.studentId == selectedStudentId }.let { if (it == -1) 0 else it + 1 }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Student")
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    if (which == 0) { selectedStudentId = ""; binding.btnSelectTarget.text = "All Students" }
                    else { selectedStudentId = studentList[which - 1].studentId; binding.btnSelectTarget.text = "${studentList[which - 1].firstName} ${studentList[which - 1].lastName}" }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnSelectMonthStudent.setOnClickListener {
            val currentIndex = if (selectedMonth.isEmpty()) 0 else months.indexOf(selectedMonth)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Month")
                .setSingleChoiceItems(months, currentIndex) { dialog, which ->
                    selectedMonth = if (which == 0) "" else months[which]
                    binding.btnSelectMonthStudent.text = if (selectedMonth.isEmpty()) "Month" else selectedMonth
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnSelectYearStudent.setOnClickListener {
            val currentIndex = if (selectedYear.isEmpty()) 0 else years.indexOf(selectedYear)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Year")
                .setSingleChoiceItems(years, currentIndex) { dialog, which ->
                    selectedYear = if (which == 0) "" else years[which]
                    binding.btnSelectYearStudent.text = if (selectedYear.isEmpty()) "Year" else selectedYear
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        // Teacher filters
        binding.btnSelectTeacher.setOnClickListener {
            val options = arrayOf("All Teachers") + teacherList.map { "${it.firstName} ${it.lastName} (${it.teacherId})" }.toTypedArray()
            val currentIndex = if (selectedTeacherId.isEmpty()) 0
            else teacherList.indexOfFirst { it.teacherId == selectedTeacherId }.let { if (it == -1) 0 else it + 1 }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Teacher")
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    if (which == 0) { selectedTeacherId = ""; binding.btnSelectTeacher.text = "All Teachers" }
                    else { selectedTeacherId = teacherList[which - 1].teacherId; binding.btnSelectTeacher.text = "${teacherList[which - 1].firstName} ${teacherList[which - 1].lastName}" }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnSelectMonthTeacher.setOnClickListener {
            val currentIndex = if (selectedMonth.isEmpty()) 0 else months.indexOf(selectedMonth)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Month")
                .setSingleChoiceItems(months, currentIndex) { dialog, which ->
                    selectedMonth = if (which == 0) "" else months[which]
                    binding.btnSelectMonthTeacher.text = if (selectedMonth.isEmpty()) "Month" else selectedMonth
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnSelectYearTeacher.setOnClickListener {
            val currentIndex = if (selectedYear.isEmpty()) 0 else years.indexOf(selectedYear)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Year")
                .setSingleChoiceItems(years, currentIndex) { dialog, which ->
                    selectedYear = if (which == 0) "" else years[which]
                    binding.btnSelectYearTeacher.text = if (selectedYear.isEmpty()) "Year" else selectedYear
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnGenerateReport.setOnClickListener { generateReport() }
        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.btnExportCsv.setOnClickListener { exportCsv() }
    }

    private fun resetStudentFilterButtons() {
        binding.btnSelectTarget.text = "All Students"
        binding.btnSelectMonthStudent.text = "Month"
        binding.btnSelectYearStudent.text = "Year"
    }

    private fun resetTeacherFilterButtons() {
        binding.btnSelectTeacher.text = "All Teachers"
        binding.btnSelectMonthTeacher.text = "Month"
        binding.btnSelectYearTeacher.text = "Year"
    }

    private fun updateTypeButtons(type: String) {
        val active = android.graphics.Color.parseColor("#1976D2")
        binding.btnStudentReport.setBackgroundColor(if (type == "student") active else android.graphics.Color.TRANSPARENT)
        binding.btnStudentReport.setTextColor(if (type == "student") android.graphics.Color.WHITE else active)
        binding.btnTeacherReport.setBackgroundColor(if (type == "teacher") active else android.graphics.Color.TRANSPARENT)
        binding.btnTeacherReport.setTextColor(if (type == "teacher") android.graphics.Color.WHITE else active)
    }

    private fun generateReport() {
        if (selectedReportType.isEmpty()) {
            Snackbar.make(binding.root, "Please select a report type first", Snackbar.LENGTH_SHORT).show()
            return
        }
        binding.reportContent.visibility = View.VISIBLE
        binding.exportButtons.visibility = View.VISIBLE
        reportGenerated = true
        val reportText = StringBuilder()

        if (selectedReportType == "student") {
            val targets = if (selectedStudentId.isEmpty()) studentList
            else studentList.filter { it.studentId == selectedStudentId }

            reportText.append("=== STUDENT REPORT ===\n")
            if (selectedMonth.isNotEmpty()) reportText.append("Month: $selectedMonth\n")
            if (selectedYear.isNotEmpty()) reportText.append("Year: $selectedYear\n")
            reportText.append("\n")

            targets.forEach { s ->
                reportText.append("Student: ${s.firstName} ${s.lastName} (${s.studentId})\n")

                var grades = gradeList.filter { it.studentId == s.studentId }
                var att = attendanceList.filter { it.studentId == s.studentId }

                // Filter both grades and attendance by year and month
                if (selectedYear.isNotEmpty()) {
                    grades = grades.filter { it.date.startsWith(selectedYear) }
                    att = att.filter { it.date.startsWith(selectedYear) }
                }
                if (selectedMonth.isNotEmpty()) {
                    val monthNum = months.indexOf(selectedMonth).toString().padStart(2, '0')
                    grades = grades.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                    att = att.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                }

                val avgScore = if (grades.isNotEmpty()) grades.map { it.score.toDouble() }.average() else 0.0
                reportText.append("  Grades: ${grades.size} entries")
                if (grades.isNotEmpty()) reportText.append(", Avg: ${"%.1f".format(avgScore)}")
                reportText.append("\n")
                reportText.append("  Attendance: ${att.count { it.status == "Present" }}/${att.size} Present\n\n")
            }

        } else {
            val targets = if (selectedTeacherId.isEmpty()) teacherList
            else teacherList.filter { it.teacherId == selectedTeacherId }

            reportText.append("=== TEACHER REPORT ===\n")
            if (selectedMonth.isNotEmpty()) reportText.append("Month: $selectedMonth\n")
            if (selectedYear.isNotEmpty()) reportText.append("Year: $selectedYear\n")
            reportText.append("\n")

            targets.forEach { t ->
                reportText.append("Teacher: ${t.firstName} ${t.lastName} (${t.teacherId})\n")
                val courses = courseList.filter { it.teacherId == t.teacherId }
                reportText.append("  Courses (${courses.size}): ${courses.joinToString { it.courseCode }}\n")

                courses.forEach { c ->
                    val enrolled = enrollmentList.count { it.courseId == c.courseId }
                    var att = attendanceList.filter { it.courseId == c.courseId }

                    if (selectedYear.isNotEmpty()) att = att.filter { it.date.startsWith(selectedYear) }
                    if (selectedMonth.isNotEmpty()) {
                        val monthNum = months.indexOf(selectedMonth).toString().padStart(2, '0')
                        att = att.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                    }

                    reportText.append("    ${c.courseName}: $enrolled enrolled, ${att.count { it.status == "Present" }} present records\n")
                }
                reportText.append("\n")
            }
        }

        if (reportText.toString().trim().endsWith("===")) {
            reportText.append("No data found for selected filters.\n")
        }

        binding.tvReportContent.text = reportText.toString()
    }

    private fun exportCsv() {
        if (!reportGenerated) {
            Snackbar.make(binding.root, "Generate a report first", Snackbar.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "SERS_Report_${System.currentTimeMillis()}.csv"
            val csvContent = StringBuilder()

            if (selectedReportType == "student") {
                csvContent.append("Student ID,Student Name,Total Grades,Average Score,Present,Absent,Late,Total Attendance\n")
                val targets = if (selectedStudentId.isEmpty()) studentList
                else studentList.filter { it.studentId == selectedStudentId }

                targets.forEach { s ->
                    var grades = gradeList.filter { it.studentId == s.studentId }
                    var att = attendanceList.filter { it.studentId == s.studentId }

                    if (selectedYear.isNotEmpty()) {
                        grades = grades.filter { it.date.startsWith(selectedYear) }
                        att = att.filter { it.date.startsWith(selectedYear) }
                    }
                    if (selectedMonth.isNotEmpty()) {
                        val monthNum = months.indexOf(selectedMonth).toString().padStart(2, '0')
                        grades = grades.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                        att = att.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                    }

                    val avg = if (grades.isNotEmpty()) grades.map { it.score.toDouble() }.average() else 0.0
                    val present = att.count { it.status == "Present" }
                    val absent = att.count { it.status == "Absent" }
                    val late = att.count { it.status == "Late" }
                    csvContent.append("${s.studentId},${s.firstName} ${s.lastName},${grades.size},${"%.1f".format(avg)},$present,$absent,$late,${att.size}\n")
                }
            } else {
                csvContent.append("Teacher ID,Teacher Name,Total Courses,Course Codes,Total Present Records\n")
                val targets = if (selectedTeacherId.isEmpty()) teacherList
                else teacherList.filter { it.teacherId == selectedTeacherId }

                targets.forEach { t ->
                    val courses = courseList.filter { it.teacherId == t.teacherId }
                    var totalPresent = 0
                    courses.forEach { c ->
                        var att = attendanceList.filter { it.courseId == c.courseId }
                        if (selectedYear.isNotEmpty()) att = att.filter { it.date.startsWith(selectedYear) }
                        if (selectedMonth.isNotEmpty()) {
                            val monthNum = months.indexOf(selectedMonth).toString().padStart(2, '0')
                            att = att.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                        }
                        totalPresent += att.count { it.status == "Present" }
                    }
                    val codes = courses.joinToString("|") { it.courseCode }
                    csvContent.append("${t.teacherId},${t.firstName} ${t.lastName},${courses.size},$codes,$totalPresent\n")
                }
            }

            // Save to Downloads using MediaStore (works Android 10+, no permission needed)
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = requireContext().contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(csvContent.toString().toByteArray()) }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Snackbar.make(binding.root, "CSV saved to Downloads: $fileName", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(binding.root, "Failed to save CSV", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun exportPdf() {
        if (!reportGenerated) {
            Snackbar.make(binding.root, "Generate a report first", Snackbar.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "SERS_Report_${System.currentTimeMillis()}.pdf"
            val pdfDocument = android.graphics.pdf.PdfDocument()

            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = android.graphics.Paint().apply {
                textSize = 18f
                isFakeBoldText = true
                color = android.graphics.Color.parseColor("#1976D2")
            }
            val headerPaint = android.graphics.Paint().apply {
                textSize = 13f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }
            val bodyPaint = android.graphics.Paint().apply {
                textSize = 11f
                color = android.graphics.Color.DKGRAY
            }
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
            }

            var y = 50f
            val margin = 40f
            val pageHeight = 842f

            fun nextPage(): android.graphics.Canvas {
                pdfDocument.finishPage(page)
                val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                val newPage = pdfDocument.startPage(newPageInfo)
                return newPage.canvas
            }

            // Title
            canvas.drawText("SERS - Student Education Records System", margin, y, titlePaint)
            y += 25f
            canvas.drawText(
                if (selectedReportType == "student") "Student Report" else "Teacher Report",
                margin, y, headerPaint
            )
            y += 15f
            if (selectedMonth.isNotEmpty() || selectedYear.isNotEmpty()) {
                canvas.drawText(
                    "Period: ${if (selectedMonth.isNotEmpty()) selectedMonth else "All Months"} ${if (selectedYear.isNotEmpty()) selectedYear else ""}".trim(),
                    margin, y, bodyPaint
                )
                y += 15f
            }
            canvas.drawLine(margin, y, 555f, y, linePaint)
            y += 20f

            if (selectedReportType == "student") {
                val targets = if (selectedStudentId.isEmpty()) studentList
                else studentList.filter { it.studentId == selectedStudentId }

                targets.forEach { s ->
                    if (y > pageHeight - 100f) { y = 50f }

                    canvas.drawText("${s.firstName} ${s.lastName} (${s.studentId})", margin, y, headerPaint)
                    y += 18f

                    var grades = gradeList.filter { it.studentId == s.studentId }
                    var att = attendanceList.filter { it.studentId == s.studentId }

                    if (selectedYear.isNotEmpty()) {
                        grades = grades.filter { it.date.startsWith(selectedYear) }
                        att = att.filter { it.date.startsWith(selectedYear) }
                    }
                    if (selectedMonth.isNotEmpty()) {
                        val monthNum = months.indexOf(selectedMonth).toString().padStart(2, '0')
                        grades = grades.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                        att = att.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                    }

                    val avg = if (grades.isNotEmpty()) grades.map { it.score.toDouble() }.average() else 0.0
                    val present = att.count { it.status == "Present" }
                    val absent = att.count { it.status == "Absent" }
                    val late = att.count { it.status == "Late" }

                    canvas.drawText("  Grades: ${grades.size} entries  |  Avg Score: ${"%.1f".format(avg)}", margin, y, bodyPaint)
                    y += 15f
                    canvas.drawText("  Attendance: $present Present  |  $absent Absent  |  $late Late  |  Total: ${att.size}", margin, y, bodyPaint)
                    y += 15f
                    canvas.drawLine(margin, y, 555f, y, linePaint)
                    y += 15f
                }
            } else {
                val targets = if (selectedTeacherId.isEmpty()) teacherList
                else teacherList.filter { it.teacherId == selectedTeacherId }

                targets.forEach { t ->
                    if (y > pageHeight - 100f) { y = 50f }

                    canvas.drawText("${t.firstName} ${t.lastName} (${t.teacherId})", margin, y, headerPaint)
                    y += 18f

                    val courses = courseList.filter { it.teacherId == t.teacherId }
                    canvas.drawText("  Courses (${courses.size}): ${courses.joinToString { it.courseCode }}", margin, y, bodyPaint)
                    y += 15f

                    courses.forEach { c ->
                        var att = attendanceList.filter { it.courseId == c.courseId }
                        if (selectedYear.isNotEmpty()) att = att.filter { it.date.startsWith(selectedYear) }
                        if (selectedMonth.isNotEmpty()) {
                            val monthNum = months.indexOf(selectedMonth).toString().padStart(2, '0')
                            att = att.filter { it.date.length >= 7 && it.date.substring(5, 7) == monthNum }
                        }
                        val enrolled = enrollmentList.count { it.courseId == c.courseId }
                        canvas.drawText("    ${c.courseName}: $enrolled enrolled  |  ${att.count { it.status == "Present" }} present records", margin, y, bodyPaint)
                        y += 15f
                    }
                    canvas.drawLine(margin, y, 555f, y, linePaint)
                    y += 15f
                }
            }

            pdfDocument.finishPage(page)

            // Save to Downloads
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = requireContext().contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Snackbar.make(binding.root, "PDF saved to Downloads: $fileName", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(binding.root, "Failed to save PDF", Snackbar.LENGTH_SHORT).show()
            }

            pdfDocument.close()

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
}