package com.sers.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sers.app.databinding.FragmentStudentReportBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.viewmodel.ProfileViewModel
import com.sers.app.viewmodel.ReportViewModel

class StudentReportFragment : Fragment() {

    private lateinit var binding: FragmentStudentReportBinding
    private val viewModel: ReportViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private val courseList = mutableListOf<Course>()
    private val gradeList = mutableListOf<Grade>()
    private val attendanceList = mutableListOf<Attendance>()

    private var myStudentId = ""
    private var myStudentName = ""
    private var selectedFilter = "All"
    private var selectedMonth = ""
    private var selectedYear = ""
    private var reportGenerated = false

    private val monthNames = arrayOf(
        "All Months", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private val years = arrayOf("All Years", "2023", "2024", "2025", "2026")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reportContent.visibility = View.GONE
        binding.exportButtons.visibility = View.GONE

        setupFilters()
        setupObservers()

        binding.btnSelectMonth.setOnClickListener {
            val currentIndex = if (selectedMonth.isEmpty()) 0
            else selectedMonth.toIntOrNull() ?: 0
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Month")
                .setSingleChoiceItems(monthNames, currentIndex) { dialog, which ->
                    selectedMonth = if (which == 0) "" else String.format("%02d", which)
                    binding.btnSelectMonth.text = if (which == 0) "Month" else monthNames[which]
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
                    binding.btnSelectYear.text = if (which == 0) "Year" else years[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        binding.btnGenerateReport.setOnClickListener { generateReport() }
        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.btnExportCsv.setOnClickListener { exportCsv() }

        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                myStudentId = user.studentId
                myStudentName = "${user.firstName} ${user.lastName}"
                viewModel.loadAllData(myStudentId)
            }
        }
        profileViewModel.loadCurrentUser()
    }

    private fun setupFilters() {
        binding.btnFilterAll.setOnClickListener { setFilterType("All") }
        binding.btnFilterGrades.setOnClickListener { setFilterType("Grades") }
        binding.btnFilterAttendance.setOnClickListener { setFilterType("Attend.") }
        setFilterType("All")
    }

    private fun setupObservers() {
        viewModel.courses.observe(viewLifecycleOwner) { courseList.clear(); courseList.addAll(it) }
        viewModel.grades.observe(viewLifecycleOwner) { gradeList.clear(); gradeList.addAll(it) }
        viewModel.attendance.observe(viewLifecycleOwner) { attendanceList.clear(); attendanceList.addAll(it) }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.message.observe(viewLifecycleOwner) { if (it.isNotEmpty()) Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
    }

    private fun setFilterType(filter: String) {
        selectedFilter = filter
        val activeColor = android.graphics.Color.parseColor("#1976D2")
        val inactiveColor = android.graphics.Color.TRANSPARENT
        val activeText = android.graphics.Color.WHITE
        val inactiveText = android.graphics.Color.parseColor("#1976D2")
        binding.btnFilterAll.setBackgroundColor(if (filter == "All") activeColor else inactiveColor)
        binding.btnFilterAll.setTextColor(if (filter == "All") activeText else inactiveText)
        binding.btnFilterGrades.setBackgroundColor(if (filter == "Grades") activeColor else inactiveColor)
        binding.btnFilterGrades.setTextColor(if (filter == "Grades") activeText else inactiveText)
        binding.btnFilterAttendance.setBackgroundColor(if (filter == "Attend.") activeColor else inactiveColor)
        binding.btnFilterAttendance.setTextColor(if (filter == "Attend.") activeText else inactiveText)
    }

    private fun getPeriodLabel(): String {
        val mName = if (selectedMonth.isNotEmpty()) monthNames[selectedMonth.toInt()] else ""
        return when {
            mName.isNotEmpty() && selectedYear.isNotEmpty() -> "$mName $selectedYear"
            mName.isNotEmpty() -> mName
            selectedYear.isNotEmpty() -> selectedYear
            else -> "All Time"
        }
    }

    private fun generateReport() {
        if (myStudentId.isEmpty()) {
            Snackbar.make(binding.root, "Loading your profile, please wait...", Snackbar.LENGTH_SHORT).show()
            return
        }
        binding.reportContent.visibility = View.VISIBLE
        binding.exportButtons.visibility = View.VISIBLE
        reportGenerated = true
        binding.tvReportTitle.text = "My Evaluation Report"
        binding.tvReportSubtitle.text = "$myStudentName ($myStudentId)  |  Period: ${getPeriodLabel()}"

        val reportText = StringBuilder()

        if (selectedFilter == "All" || selectedFilter == "Grades") {
            reportText.append("GRADES\n─────────────────────────────\n")

            var filteredGrades = gradeList.filter { it.studentId == myStudentId }
            if (selectedYear.isNotEmpty()) filteredGrades = filteredGrades.filter { it.date.startsWith(selectedYear) }
            if (selectedMonth.isNotEmpty()) filteredGrades = filteredGrades.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }

            if (filteredGrades.isEmpty()) {
                reportText.append("No grade records found for this period.\n")
            } else {
                reportText.append(String.format("%-20s %6s %6s %6s\n", "Course", "Score", "Total", "Grade"))
                filteredGrades.forEach { g ->
                    val c = courseList.find { it.courseId == g.courseId }
                    val p = if (g.totalMarks > 0) (g.score.toFloat() / g.totalMarks * 100).toInt() else 0
                    val letter = when { p >= 90 -> "A"; p >= 80 -> "B"; p >= 70 -> "C"; p >= 60 -> "D"; else -> "F" }
                    reportText.append(String.format("%-20s %6d %6d %6s\n", (c?.courseName ?: g.courseId).take(18), g.score, g.totalMarks, letter))
                }
                val avg = filteredGrades.map { it.score.toDouble() }.average()
                reportText.append("\nOverall Average: ${"%.1f".format(avg)}\n")
            }
            reportText.append("\n")
        }

        if (selectedFilter == "All" || selectedFilter == "Attend.") {
            reportText.append("ATTENDANCE\n─────────────────────────────\n")

            var filteredAtt = attendanceList.filter { it.studentId == myStudentId }
            if (selectedYear.isNotEmpty()) filteredAtt = filteredAtt.filter { it.date.startsWith(selectedYear) }
            if (selectedMonth.isNotEmpty()) filteredAtt = filteredAtt.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }

            if (filteredAtt.isEmpty()) {
                reportText.append("No attendance records found for this period.\n")
            } else {
                val present = filteredAtt.count { it.status == "Present" }
                val absent = filteredAtt.count { it.status == "Absent" }
                val late = filteredAtt.count { it.status == "Late" }
                val total = filteredAtt.size
                val rate = (present.toFloat() / total * 100).toInt()
                reportText.append("Rate: $rate%  |  Present: $present  |  Absent: $absent  |  Late: $late  |  Total: $total\n\n")
                reportText.append(String.format("%-12s %-15s %-10s\n", "Date", "Course", "Status"))
                filteredAtt.sortedByDescending { it.date }.forEach { a ->
                    val c = courseList.find { it.courseId == a.courseId }
                    reportText.append(String.format("%-12s %-15s %-10s\n", a.date, (c?.courseName ?: a.courseId).take(13), a.status))
                }
            }
        }

        binding.tvReportContent.text = reportText.toString()
    }

    private fun exportCsv() {
        if (!reportGenerated) { Snackbar.make(binding.root, "Generate a report first", Snackbar.LENGTH_SHORT).show(); return }
        try {
            val fileName = "Student_Report_${System.currentTimeMillis()}.csv"
            val csv = StringBuilder()

            if (selectedFilter == "All" || selectedFilter == "Grades") {
                csv.append("Type,Course,Score,Total,Grade,Date\n")
                var filteredGrades = gradeList.filter { it.studentId == myStudentId }
                if (selectedYear.isNotEmpty()) filteredGrades = filteredGrades.filter { it.date.startsWith(selectedYear) }
                if (selectedMonth.isNotEmpty()) filteredGrades = filteredGrades.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }
                filteredGrades.forEach { g ->
                    val c = courseList.find { it.courseId == g.courseId }
                    val p = if (g.totalMarks > 0) (g.score.toFloat() / g.totalMarks * 100).toInt() else 0
                    val letter = when { p >= 90 -> "A"; p >= 80 -> "B"; p >= 70 -> "C"; p >= 60 -> "D"; else -> "F" }
                    csv.append("Grade,${c?.courseName ?: g.courseId},${g.score},${g.totalMarks},$letter,${g.date}\n")
                }
            }
            if (selectedFilter == "All" || selectedFilter == "Attend.") {
                csv.append("\nType,Date,Course,Status\n")
                var filteredAtt = attendanceList.filter { it.studentId == myStudentId }
                if (selectedYear.isNotEmpty()) filteredAtt = filteredAtt.filter { it.date.startsWith(selectedYear) }
                if (selectedMonth.isNotEmpty()) filteredAtt = filteredAtt.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }
                filteredAtt.sortedByDescending { it.date }.forEach { a ->
                    val c = courseList.find { it.courseId == a.courseId }
                    csv.append("Attendance,${a.date},${c?.courseName ?: a.courseId},${a.status}\n")
                }
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
        if (!reportGenerated) { Snackbar.make(binding.root, "Generate a report first", Snackbar.LENGTH_SHORT).show(); return }
        try {
            val fileName = "Student_Report_${System.currentTimeMillis()}.pdf"
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

            canvas.drawText("SERS - Student Evaluation Report", margin, y, titlePaint); y += 25f
            canvas.drawText("$myStudentName ($myStudentId)", margin, y, headerPaint); y += 18f
            canvas.drawText("Period: ${getPeriodLabel()}", margin, y, bodyPaint); y += 15f
            canvas.drawLine(margin, y, 555f, y, linePaint); y += 20f

            if (selectedFilter == "All" || selectedFilter == "Grades") {
                canvas.drawText("GRADES", margin, y, headerPaint); y += 18f
                var filteredGrades = gradeList.filter { it.studentId == myStudentId }
                if (selectedYear.isNotEmpty()) filteredGrades = filteredGrades.filter { it.date.startsWith(selectedYear) }
                if (selectedMonth.isNotEmpty()) filteredGrades = filteredGrades.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }

                if (filteredGrades.isEmpty()) {
                    canvas.drawText("  No grade records found for this period.", margin, y, bodyPaint); y += 18f
                } else {
                    filteredGrades.forEach { g ->
                        if (y > 780f) { y = 50f }
                        val c = courseList.find { it.courseId == g.courseId }
                        val p = if (g.totalMarks > 0) (g.score.toFloat() / g.totalMarks * 100).toInt() else 0
                        val letter = when { p >= 90 -> "A"; p >= 80 -> "B"; p >= 70 -> "C"; p >= 60 -> "D"; else -> "F" }
                        canvas.drawText("  ${(c?.courseName ?: g.courseId).take(25)}   ${g.score}/${g.totalMarks}  ($letter)", margin, y, bodyPaint); y += 15f
                    }
                }
                canvas.drawLine(margin, y, 555f, y, linePaint); y += 20f
            }

            if (selectedFilter == "All" || selectedFilter == "Attend.") {
                canvas.drawText("ATTENDANCE", margin, y, headerPaint); y += 18f
                var filteredAtt = attendanceList.filter { it.studentId == myStudentId }
                if (selectedYear.isNotEmpty()) filteredAtt = filteredAtt.filter { it.date.startsWith(selectedYear) }
                if (selectedMonth.isNotEmpty()) filteredAtt = filteredAtt.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }

                if (filteredAtt.isEmpty()) {
                    canvas.drawText("  No attendance records found for this period.", margin, y, bodyPaint); y += 18f
                } else {
                    val present = filteredAtt.count { it.status == "Present" }
                    val rate = (present.toFloat() / filteredAtt.size * 100).toInt()
                    canvas.drawText("  Rate: $rate%  |  Present: $present  |  Total: ${filteredAtt.size}", margin, y, bodyPaint); y += 18f
                    filteredAtt.sortedByDescending { it.date }.forEach { a ->
                        if (y > 780f) { y = 50f }
                        val c = courseList.find { it.courseId == a.courseId }
                        canvas.drawText("  ${a.date}   ${(c?.courseName ?: a.courseId).take(20)}   ${a.status}", margin, y, bodyPaint); y += 15f
                    }
                }
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