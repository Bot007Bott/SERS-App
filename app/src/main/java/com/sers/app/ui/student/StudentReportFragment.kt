package com.sers.app.ui.student

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentStudentReportBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Grade

class StudentReportFragment : Fragment() {

    private lateinit var binding: FragmentStudentReportBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val courseList = mutableListOf<Course>()
    private val gradeList = mutableListOf<Grade>()
    private val attendanceList = mutableListOf<Attendance>()

    private var myStudentId = ""
    private var myStudentName = ""
    private var selectedFilter = "All"
    private var selectedMonth = ""
    private var selectedYear = ""
    private var reportGenerated = false
    private var dataLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reportContent.visibility = View.GONE
        binding.exportButtons.visibility = View.GONE

        binding.btnFilterAll.setOnClickListener { setFilterType("All") }
        binding.btnFilterGrades.setOnClickListener { setFilterType("Grades") }
        binding.btnFilterAttendance.setOnClickListener { setFilterType("Attend.") }
        setFilterType("All")

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                val doc = docs.documents[0]
                myStudentId = doc.getString("studentId") ?: ""
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""
                myStudentName = "$firstName $lastName"
                loadAllData()
            }

        binding.btnSelectMonth.setOnClickListener {
            showMonthPicker { name, value ->
                selectedMonth = value
                binding.btnSelectMonth.text = if (name == "All") "All Months" else name
            }
        }

        binding.btnSelectYear.setOnClickListener {
            showYearPicker { name, value ->
                selectedYear = value
                binding.btnSelectYear.text = if (name == "All") "All Years" else name
            }
        }

        binding.btnGenerateReport.setOnClickListener { generateReport() }
        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.btnExportCsv.setOnClickListener { exportCsv() }
    }

    private fun loadAllData() {
        binding.progressBar.visibility = View.VISIBLE
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
            db.collection("grades").whereEqualTo("studentId", myStudentId).get()
                .addOnSuccessListener { gDocs ->
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
                    db.collection("attendance").whereEqualTo("studentId", myStudentId).get()
                        .addOnSuccessListener { aDocs ->
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
        val monthNames = listOf("", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        return when {
            selectedMonth.isNotEmpty() && selectedYear.isNotEmpty() -> "${monthNames[selectedMonth.toInt()]} $selectedYear"
            selectedMonth.isNotEmpty() -> monthNames[selectedMonth.toInt()]
            selectedYear.isNotEmpty() -> selectedYear
            else -> "All Time"
        }
    }

    private fun generateReport() {
        if (!dataLoaded) {
            Snackbar.make(binding.root, "Data still loading, please wait...", Snackbar.LENGTH_SHORT).show(); return
        }
        binding.reportContent.visibility = View.VISIBLE
        binding.exportButtons.visibility = View.VISIBLE
        reportGenerated = true

        binding.tvReportTitle.text = "My Report"
        binding.tvReportSubtitle.text = "$myStudentName ($myStudentId) | Period: ${getPeriodLabel()}"

        val reportText = StringBuilder()

        if (selectedFilter == "All" || selectedFilter == "Grades") {
            reportText.append("GRADES\n")
            reportText.append("─────────────────────────────\n")
            if (gradeList.isEmpty()) {
                reportText.append("No grades found\n")
            } else {
                reportText.append(String.format("%-20s %6s %6s %6s\n", "Course", "Score", "Total", "Grade"))
                reportText.append("─────────────────────────────\n")
                gradeList.forEach { grade ->
                    val course = courseList.find { it.courseId == grade.courseId }
                    val percent = (grade.score.toFloat() / grade.totalMarks * 100).toInt()
                    val letter = when { percent >= 90 -> "A"; percent >= 80 -> "B"; percent >= 70 -> "C"; percent >= 60 -> "D"; else -> "F" }
                    reportText.append(String.format("%-20s %6d %6d %6s\n", (course?.courseName ?: grade.courseId).take(18), grade.score, grade.totalMarks, letter))
                }
            }
            reportText.append("\n")
        }

        if (selectedFilter == "All" || selectedFilter == "Attend.") {
            var attendance = attendanceList.toMutableList()
            if (selectedMonth.isNotEmpty()) attendance = attendance.filter { it.date.length >= 7 && it.date.substring(5, 7) == selectedMonth }.toMutableList()
            if (selectedYear.isNotEmpty()) attendance = attendance.filter { it.date.length >= 4 && it.date.substring(0, 4) == selectedYear }.toMutableList()

            val present = attendance.count { it.status == "Present" }
            val absent = attendance.count { it.status == "Absent" }
            val late = attendance.count { it.status == "Late" }
            val total = attendance.size
            val rate = if (total > 0) (present.toFloat() / total * 100).toInt() else 0

            reportText.append("ATTENDANCE\n")
            reportText.append("─────────────────────────────\n")
            reportText.append("Present: $present | Absent: $absent | Late: $late\n")
            reportText.append("Attendance Rate: $rate%\n")
            reportText.append("─────────────────────────────\n")

            if (attendance.isEmpty()) {
                reportText.append("No attendance records for ${getPeriodLabel()}\n")
            } else {
                reportText.append(String.format("%-15s %-20s %-10s\n", "Date", "Course", "Status"))
                reportText.append("─────────────────────────────\n")
                attendance.sortedByDescending { it.date }.forEach { att ->
                    val course = courseList.find { it.courseId == att.courseId }
                    reportText.append(String.format("%-15s %-20s %-10s\n", att.date, (course?.courseName ?: att.courseId).take(18), att.status))
                }
            }
        }

        binding.tvReportContent.text = reportText.toString()
    }

    private fun exportPdf() {
        if (!reportGenerated) return
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val paint = android.graphics.Paint()
            val pageWidth = 595; val pageHeight = 842; val margin = 40f; val lineHeight = 16f; val maxY = pageHeight - margin
            val lines = mutableListOf(Pair(binding.tvReportTitle.text.toString(), true), Pair(binding.tvReportSubtitle.text.toString(), false), Pair("", false))
            binding.tvReportContent.text.toString().split("\n").forEach { lines.add(Pair(it, false)) }
            var pageNumber = 1
            var currentPage = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = currentPage.canvas; var yPosition = 60f
            lines.forEachIndexed { index, (text, isBold) ->
                if (yPosition > maxY) { pdfDocument.finishPage(currentPage); pageNumber++; currentPage = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()); canvas = currentPage.canvas; yPosition = margin }
                paint.textSize = if (index == 0) 16f else if (index == 1) 12f else 10f; paint.isFakeBoldText = isBold
                canvas.drawText(text, margin, yPosition, paint); yPosition += lineHeight
            }
            pdfDocument.finishPage(currentPage)
            val fileName = "SERS_My_Report_${System.currentTimeMillis()}.pdf"
            val cv = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, fileName); put(MediaStore.Downloads.MIME_TYPE, "application/pdf"); put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
            val uri = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            uri?.let { requireContext().contentResolver.openOutputStream(it)?.use { s -> pdfDocument.writeTo(s) } }
            pdfDocument.close()
            Snackbar.make(binding.root, "PDF saved to Downloads!", Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) { Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show() }
    }

    private fun exportCsv() {
        if (!reportGenerated) return
        try {
            val fileName = "SERS_My_Report_${System.currentTimeMillis()}.csv"
            val cv = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, fileName); put(MediaStore.Downloads.MIME_TYPE, "text/csv"); put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
            val uri = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            uri?.let { requireContext().contentResolver.openOutputStream(it)?.use { s -> s.write(binding.tvReportContent.text.toString().toByteArray()) } }
            Snackbar.make(binding.root, "CSV saved to Downloads!", Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) { Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show() }
    }

    private fun showMonthPicker(onSelect: (String, String) -> Unit) {
        val months = listOf("All", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
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
        val allOptions = options.toMutableList(); var filteredOptions = allOptions.toMutableList()
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val filterAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class VH(val b: com.sers.app.databinding.ItemFilterOptionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder { val b = com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false); return VH(b) }
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val option = filteredOptions[position]; (holder as VH).b.tvOption.text = option.first; holder.b.ivCheck.visibility = View.GONE
                holder.b.root.setOnClickListener { onSelect(option.first, option.second); bottomSheet.dismiss() }
            }
            override fun getItemCount() = filteredOptions.size
            fun updateList(newList: MutableList<Pair<String, String>>) { filteredOptions = newList; notifyDataSetChanged() }
        }
        rv.adapter = filterAdapter
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch).addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { val q = s.toString().lowercase(); filterAdapter.updateList(if (q.isEmpty()) allOptions.toMutableList() else allOptions.filter { it.first.lowercase().contains(q) }.toMutableList()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        bottomSheet.show()
    }
}