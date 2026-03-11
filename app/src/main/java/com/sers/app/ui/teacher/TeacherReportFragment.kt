package com.sers.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sers.app.databinding.FragmentTeacherReportBinding
import com.sers.app.model.*
import com.sers.app.viewmodel.ReportViewModel

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

    private val months = arrayOf(
        "All Months", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private val years = arrayOf("All Years", "2023", "2024", "2025", "2026")

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

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                myTeacherId = docs.documents[0].getString("teacherId") ?: ""
                myTeacherName = "${docs.documents[0].getString("firstName")} ${docs.documents[0].getString("lastName")}"
                viewModel.loadAllData()
            }
    }

    private fun setupObservers() {
        viewModel.courses.observe(viewLifecycleOwner) { list ->
            courseList.clear()
            courseList.addAll(list)
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
            val enrolled = enrollmentList
                .filter { e -> courseList.any { it.courseId == e.courseId } }
                .map { it.studentId }.distinct()
            val available = studentList.filter { it.studentId in enrolled }
            val options = arrayOf("All Students") + available.map { "${it.firstName} ${it.lastName} (${it.studentId})" }.toTypedArray()
            val currentIndex = if (selectedStudentId.isEmpty()) 0
            else available.indexOfFirst { it.studentId == selectedStudentId }.let { if (it == -1) 0 else it + 1 }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Student")
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    if (which == 0) { selectedStudentId = ""; binding.btnSelectStudent.text = "All Students" }
                    else { selectedStudentId = available[which - 1].studentId; binding.btnSelectStudent.text = "${available[which - 1].firstName} ${available[which - 1].lastName}" }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnSelectCourse.setOnClickListener {
            val options = arrayOf("All Courses") + courseList.map { "${it.courseName} (${it.courseCode})" }.toTypedArray()
            val currentIndex = if (selectedCourseId.isEmpty()) 0
            else courseList.indexOfFirst { it.courseId == selectedCourseId }.let { if (it == -1) 0 else it + 1 }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Course")
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    if (which == 0) { selectedCourseId = ""; binding.btnSelectCourse.text = "All Courses" }
                    else { selectedCourseId = courseList[which - 1].courseId; binding.btnSelectCourse.text = courseList[which - 1].courseName }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnSelectMonth.setOnClickListener {
            val currentIndex = if (selectedMonth.isEmpty()) 0 else months.indexOf(selectedMonth)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Month")
                .setSingleChoiceItems(months, currentIndex) { dialog, which ->
                    selectedMonth = if (which == 0) "" else months[which]
                    binding.btnSelectMonth.text = if (selectedMonth.isEmpty()) "Month" else selectedMonth
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnSelectYear.setOnClickListener {
            val currentIndex = if (selectedYear.isEmpty()) 0 else years.indexOf(selectedYear)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Year")
                .setSingleChoiceItems(years, currentIndex) { dialog, which ->
                    selectedYear = if (which == 0) "" else years[which]
                    binding.btnSelectYear.text = if (selectedYear.isEmpty()) "Year" else selectedYear
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
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
        reportText.append("=== TEACHER REPORT ===\n")
        reportText.append("Teacher: $myTeacherName\n")
        if (selectedMonth.isNotEmpty()) reportText.append("Month: $selectedMonth\n")
        if (selectedYear.isNotEmpty()) reportText.append("Year: $selectedYear\n")
        reportText.append("\n")

        val targetCourses = if (selectedCourseId.isEmpty()) courseList.filter { it.teacherId == myTeacherId }
        else courseList.filter { it.courseId == selectedCourseId }

        val enrolledSIds = enrollmentList
            .filter { e -> targetCourses.any { it.courseId == e.courseId } }
            .filter { selectedStudentId.isEmpty() || it.studentId == selectedStudentId }
            .map { it.studentId }.distinct()

        if (enrolledSIds.isEmpty()) {
            reportText.append("No students found for selected filters.\n")
        } else {
            enrolledSIds.forEach { sid ->
                val s = studentList.find { it.studentId == sid } ?: return@forEach
                reportText.append("Student: ${s.firstName} ${s.lastName} ($sid)\n")

                var grades = gradeList.filter { it.studentId == sid && targetCourses.any { tc -> tc.courseId == it.courseId } }
                var att = attendanceList.filter { it.studentId == sid && targetCourses.any { tc -> tc.courseId == it.courseId } }

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
                reportText.append("  Grades: ${grades.size} entries")
                if (grades.isNotEmpty()) reportText.append(", Avg: ${"%.1f".format(avg)}")
                reportText.append("\n")
                reportText.append("  Attendance: ${att.count { it.status == "Present" }}/${att.size} Present\n\n")
            }
        }

        binding.tvReportContent.text = reportText.toString()
    }

    private fun exportCsv() {
        if (!reportGenerated) {
            Snackbar.make(binding.root, "Generate a report first", Snackbar.LENGTH_SHORT).show()
            return
        }
        try {
            val fileName = "Teacher_Report_${System.currentTimeMillis()}.csv"
            val csv = StringBuilder()
            csv.append("Student ID,Student Name,Total Grades,Average Score,Present,Absent,Late,Total Attendance\n")

            val targetCourses = if (selectedCourseId.isEmpty()) courseList.filter { it.teacherId == myTeacherId }
            else courseList.filter { it.courseId == selectedCourseId }
            val enrolledSIds = enrollmentList
                .filter { e -> targetCourses.any { it.courseId == e.courseId } }
                .filter { selectedStudentId.isEmpty() || it.studentId == selectedStudentId }
                .map { it.studentId }.distinct()

            enrolledSIds.forEach { sid ->
                val s = studentList.find { it.studentId == sid } ?: return@forEach
                var grades = gradeList.filter { it.studentId == sid && targetCourses.any { tc -> tc.courseId == it.courseId } }
                var att = attendanceList.filter { it.studentId == sid && targetCourses.any { tc -> tc.courseId == it.courseId } }

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
                csv.append("${s.studentId},${s.firstName} ${s.lastName},${grades.size},${"%.1f".format(avg)},${att.count { it.status == "Present" }},${att.count { it.status == "Absent" }},${att.count { it.status == "Late" }},${att.size}\n")
            }

            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = requireContext().contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(csv.toString().toByteArray()) }
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
            val fileName = "Teacher_Report_${System.currentTimeMillis()}.pdf"
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = android.graphics.Paint().apply { textSize = 18f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#1976D2") }
            val headerPaint = android.graphics.Paint().apply { textSize = 13f; isFakeBoldText = true; color = android.graphics.Color.BLACK }
            val bodyPaint = android.graphics.Paint().apply { textSize = 11f; color = android.graphics.Color.DKGRAY }
            val linePaint = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }

            var y = 50f
            val margin = 40f

            canvas.drawText("SERS - Teacher Report", margin, y, titlePaint); y += 25f
            canvas.drawText("Teacher: $myTeacherName", margin, y, headerPaint); y += 18f
            if (selectedMonth.isNotEmpty() || selectedYear.isNotEmpty()) {
                canvas.drawText("Period: ${if (selectedMonth.isNotEmpty()) selectedMonth else "All Months"} ${if (selectedYear.isNotEmpty()) selectedYear else ""}".trim(), margin, y, bodyPaint); y += 15f
            }
            canvas.drawLine(margin, y, 555f, y, linePaint); y += 20f

            val targetCourses = if (selectedCourseId.isEmpty()) courseList.filter { it.teacherId == myTeacherId } else courseList.filter { it.courseId == selectedCourseId }
            val enrolledSIds = enrollmentList
                .filter { e -> targetCourses.any { it.courseId == e.courseId } }
                .filter { selectedStudentId.isEmpty() || it.studentId == selectedStudentId }
                .map { it.studentId }.distinct()

            enrolledSIds.forEach { sid ->
                if (y > 780f) { y = 50f }
                val s = studentList.find { it.studentId == sid } ?: return@forEach
                var grades = gradeList.filter { it.studentId == sid && targetCourses.any { tc -> tc.courseId == it.courseId } }
                var att = attendanceList.filter { it.studentId == sid && targetCourses.any { tc -> tc.courseId == it.courseId } }

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
                canvas.drawText("${s.firstName} ${s.lastName} (${s.studentId})", margin, y, headerPaint); y += 18f
                canvas.drawText("  Grades: ${grades.size}  |  Avg: ${"%.1f".format(avg)}", margin, y, bodyPaint); y += 15f
                canvas.drawText("  Attendance: ${att.count { it.status == "Present" }} Present  |  ${att.count { it.status == "Absent" }} Absent  |  ${att.count { it.status == "Late" }} Late", margin, y, bodyPaint); y += 15f
                canvas.drawLine(margin, y, 555f, y, linePaint); y += 15f
            }

            pdfDocument.finishPage(page)

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