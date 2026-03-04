package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.sers.app.R
import com.sers.app.databinding.FragmentAttendanceBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Student
import com.sers.app.viewmodel.AttendanceViewModel

/**
 * AttendanceFragment — MVVM View
 * Observes AttendanceViewModel for attendance, student, and course data.
 */
class AttendanceFragment : Fragment() {

    private lateinit var binding: FragmentAttendanceBinding
    private lateinit var adapter: AttendanceAdapter
    private val viewModel: AttendanceViewModel by viewModels()

    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()
    private val attendanceList = mutableListOf<Attendance>()

    private var selectedFilterStudentId = ""
    private var selectedFilterCourseId = ""
    private var selectedFilterStatus = ""
    private var currentSortOrder = "Default"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AttendanceAdapter(
            mutableListOf(),
            onEditClick = { attendance -> showAttendanceDialog(attendance) },
            onDeleteClick = { attendance -> showDeleteDialog(attendance) }
        )
        binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendance.adapter = adapter

        setupObservers()
        setupSearch()
        setupSort()

        binding.btnFilter.setOnClickListener { showFilterSheet() }
        binding.btnMarkAttendance.setOnClickListener { showAttendanceDialog(null) }
        
        viewModel.loadAllData()
    }

    private fun setupObservers() {
        viewModel.attendance.observe(viewLifecycleOwner) { records ->
            binding.progressBar.visibility = View.GONE
            attendanceList.clear(); attendanceList.addAll(records)
            binding.tvPresent.text = records.count { it.status == "Present" }.toString()
            binding.tvAbsent.text = records.count { it.status == "Absent" }.toString()
            binding.tvLate.text = records.count { it.status == "Late" }.toString()
            refreshList()
        }
        viewModel.students.observe(viewLifecycleOwner) { students ->
            studentList.clear(); studentList.addAll(students)
            refreshList()
        }
        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            courseList.clear(); courseList.addAll(courses)
            refreshList()
        }
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            val isVisible = binding.searchLayout.visibility == View.VISIBLE
            binding.searchLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) binding.etSearch.requestFocus()
        }
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshList() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupSort() {
        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Date Newest", "Date Oldest")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]; refreshList(); updateSortButtonUI(); dialog.dismiss()
                }.show()
        }
    }

    private fun updateSortButtonUI() {
        val active = currentSortOrder != "Default"
        binding.btnSort.text = if (active) "Sort •" else "Sort"
        binding.btnSort.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnSort.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun showFilterSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = "Filter Attendance"
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch).visibility = View.GONE
        val filterOptions = listOf(
            Pair("Clear All Filters", "all"),
            Pair("By Student: ${if (selectedFilterStudentId.isEmpty()) "All" else studentList.find { it.studentId == selectedFilterStudentId }?.firstName ?: "All"}", "student"),
            Pair("By Course: ${if (selectedFilterCourseId.isEmpty()) "All" else courseList.find { it.courseId == selectedFilterCourseId }?.courseName ?: "All"}", "course"),
            Pair("By Status: ${if (selectedFilterStatus.isEmpty()) "All" else selectedFilterStatus}", "status")
        )
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val fAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class VH(val b: com.sers.app.databinding.ItemFilterOptionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
            override fun onCreateViewHolder(p: ViewGroup, vt: Int) = VH(com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(p.context), p, false))
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                (h as VH).b.tvOption.text = filterOptions[pos].first; h.b.ivCheck.visibility = View.GONE
                h.b.root.setOnClickListener {
                    when (filterOptions[pos].second) {
                        "all" -> { selectedFilterStudentId = ""; selectedFilterCourseId = ""; selectedFilterStatus = ""; updateFilterButtonUI(); refreshList(); bottomSheet.dismiss() }
                        "student" -> { bottomSheet.dismiss(); showPickerSheet("Filter Student", listOf(Pair("All Students", "")) + studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }) { _, id -> selectedFilterStudentId = id; updateFilterButtonUI(); refreshList() } }
                        "course" -> { bottomSheet.dismiss(); showPickerSheet("Filter Course", listOf(Pair("All Courses", "")) + courseList.map { Pair("${it.courseName} (${it.courseCode})", it.courseId) }) { _, id -> selectedFilterCourseId = id; updateFilterButtonUI(); refreshList() } }
                        "status" -> { bottomSheet.dismiss(); showPickerSheet("Filter Status", listOf(Pair("All Status", "")) + listOf("Present", "Absent", "Late").map { Pair(it, it) }) { _, id -> selectedFilterStatus = id; updateFilterButtonUI(); refreshList() } }
                    }
                }
            }
            override fun getItemCount() = filterOptions.size
        }
        rv.adapter = fAdapter
        bottomSheet.show()
    }

    private fun updateFilterButtonUI() {
        val active = selectedFilterStudentId.isNotEmpty() || selectedFilterCourseId.isNotEmpty() || selectedFilterStatus.isNotEmpty()
        binding.btnFilter.text = if (active) "Filter •" else "Filter"
        binding.btnFilter.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnFilter.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun refreshList() {
        val query = binding.etSearch.text.toString().lowercase()
        var list = attendanceList.filter {
            selectedFilterStudentId.isEmpty() || it.studentId == selectedFilterStudentId
        }.filter {
            selectedFilterCourseId.isEmpty() || it.courseId == selectedFilterCourseId
        }.filter {
            selectedFilterStatus.isEmpty() || it.status == selectedFilterStatus
        }.filter {
            val student = studentList.find { st -> st.studentId == it.studentId }
            val course = courseList.find { c -> c.courseId == it.courseId }
            query.isEmpty() || "${student?.firstName} ${student?.lastName}".lowercase().contains(query) || course?.courseName?.lowercase()?.contains(query) == true
        }

        list = when (currentSortOrder) {
            "Date Newest" -> list.sortedByDescending { it.date }
            "Date Oldest" -> list.sortedBy { it.date }
            else -> list
        }

        adapter.updateList(list.toMutableList())
        adapter.setStudentsAndCourses(studentList, courseList)
        updateEmptyState(list)
    }

    private fun showAttendanceDialog(attendance: Attendance?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_attendance, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        val isEdit = attendance != null

        val btnSelectStudent = dialogView.findViewById<MaterialButton>(R.id.btnSelectStudent)
        val btnSelectCourse = dialogView.findViewById<MaterialButton>(R.id.btnSelectCourse)
        val btnSelectStatus = dialogView.findViewById<MaterialButton>(R.id.btnSelectStatus)
        val etSession = dialogView.findViewById<TextInputEditText>(R.id.etSession)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etDate)
        var selectedStudentId = ""; var selectedCourseId = ""; var selectedStatus = ""

        if (isEdit) {
            val student = studentList.find { it.studentId == attendance!!.studentId }
            val course = courseList.find { it.courseId == attendance!!.courseId }
            btnSelectStudent.text = if (student != null) "${student.firstName} ${student.lastName}" else attendance!!.studentId
            btnSelectCourse.text = course?.courseName ?: attendance!!.courseId
            btnSelectStatus.text = attendance!!.status
            selectedStudentId = attendance!!.studentId; selectedCourseId = attendance!!.courseId; selectedStatus = attendance!!.status
            etDate.setText(attendance!!.date); etSession.setText(attendance!!.session)
        }

        btnSelectStudent.setOnClickListener { showPickerSheet("Select Student", studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }) { name, id -> selectedStudentId = id; btnSelectStudent.text = name.substringBefore(" (") } }
        btnSelectCourse.setOnClickListener { showPickerSheet("Select Course", courseList.map { Pair("${it.courseName} (${it.courseCode})", it.courseId) }) { name, id -> selectedCourseId = id; btnSelectCourse.text = name.substringBefore(" ("); val course = courseList.find { it.courseId == id }; etSession.setText(course?.schedule ?: "") } }
        btnSelectStatus.setOnClickListener { showPickerSheet("Select Status", listOf("Present", "Absent", "Late").map { Pair(it, it) }) { name, id -> selectedStatus = id; btnSelectStatus.text = name } }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val date = etDate.text.toString().trim(); val session = etSession.text.toString().trim()
            if (selectedStudentId.isEmpty() || selectedCourseId.isEmpty() || selectedStatus.isEmpty() || date.isEmpty()) { Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            if (isEdit) viewModel.updateAttendance(attendance!!.docId, selectedStudentId, selectedCourseId, selectedStatus, date, session)
            else viewModel.addAttendance(selectedStudentId, selectedCourseId, selectedStatus, date, session)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteDialog(attendance: Attendance) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Attendance").setMessage("Confirm deletion?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteAttendance(attendance.docId) }.setNegativeButton("Cancel") { d, _ -> d.dismiss() }.show()
    }

    private fun showPickerSheet(title: String, options: List<Pair<String, String>>, onSelect: (String, String) -> Unit) {
        val bottomSheet = BottomSheetDialog(requireContext()); val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView); sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = title
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions); rv.layoutManager = LinearLayoutManager(requireContext())
        val fAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(p.context), p, false).root) {}
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) { (h.itemView as TextView).text = options[pos].first; h.itemView.setOnClickListener { onSelect(options[pos].first, options[pos].second); bottomSheet.dismiss() } }
            override fun getItemCount() = options.size
        }
        rv.adapter = fAdapter; bottomSheet.show()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAttendance.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}
