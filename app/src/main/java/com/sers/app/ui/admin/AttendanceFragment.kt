package com.sers.app.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.sers.app.R
import com.sers.app.databinding.FragmentAttendanceBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Student
import com.sers.app.viewmodel.AttendanceViewModel
import java.util.Calendar

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

    private var isClosingFromX = false

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

        binding.btnFilter.setOnClickListener { showFilterDialog() }
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
            studentList.clear(); studentList.addAll(students); refreshList()
        }
        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            courseList.clear(); courseList.addAll(courses); refreshList()
        }
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            if (isClosingFromX) { isClosingFromX = false; return@setOnClickListener }
            if (binding.searchLayout.visibility == View.GONE) {
                binding.searchLayout.visibility = View.VISIBLE
                binding.etSearch.requestFocus()
            } else {
                binding.etSearch.setText("")
                binding.searchLayout.visibility = View.GONE
                refreshList()
            }
        }

        binding.searchLayout.setEndIconOnClickListener {
            isClosingFromX = true
            binding.etSearch.setText("")
            binding.searchLayout.visibility = View.GONE
            refreshList()
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

    private fun showFilterDialog() {
        val options = arrayOf(
            "Clear All Filters",
            "By Student: ${if (selectedFilterStudentId.isEmpty()) "All" else studentList.find { it.studentId == selectedFilterStudentId }?.let { "${it.firstName} ${it.lastName}" } ?: "All"}",
            "By Course: ${if (selectedFilterCourseId.isEmpty()) "All" else courseList.find { it.courseId == selectedFilterCourseId }?.courseName ?: "All"}",
            "By Status: ${if (selectedFilterStatus.isEmpty()) "All" else selectedFilterStatus}"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter Attendance")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { selectedFilterStudentId = ""; selectedFilterCourseId = ""; selectedFilterStatus = ""; updateFilterButtonUI(); refreshList() }
                    1 -> {
                        val opts = listOf("All Students" to "") + studentList.map { "${it.firstName} ${it.lastName} (${it.studentId})" to it.studentId }
                        showPickerSheet("Filter by Student", opts, selectedFilterStudentId) { _, id -> selectedFilterStudentId = id; updateFilterButtonUI(); refreshList() }
                    }
                    2 -> {
                        val opts = listOf("All Courses" to "") + courseList.map { "${it.courseName} (${it.courseCode})" to it.courseId }
                        showPickerSheet("Filter by Course", opts, selectedFilterCourseId) { _, id -> selectedFilterCourseId = id; updateFilterButtonUI(); refreshList() }
                    }
                    3 -> showStatusPickerDialog()
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showStatusPickerDialog() {
        val statuses = arrayOf("All Status", "Present", "Absent", "Late")
        val currentIndex = if (selectedFilterStatus.isEmpty()) 0 else statuses.indexOf(selectedFilterStatus)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Status")
            .setSingleChoiceItems(statuses, currentIndex) { dialog, which ->
                selectedFilterStatus = if (which == 0) "" else statuses[which]
                updateFilterButtonUI(); refreshList(); dialog.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
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
        var list = attendanceList
            .filter { selectedFilterStudentId.isEmpty() || it.studentId == selectedFilterStudentId }
            .filter { selectedFilterCourseId.isEmpty() || it.courseId == selectedFilterCourseId }
            .filter { selectedFilterStatus.isEmpty() || it.status == selectedFilterStatus }
            .filter {
                val student = studentList.find { st -> st.studentId == it.studentId }
                val course = courseList.find { c -> c.courseId == it.courseId }
                query.isEmpty() ||
                        "${student?.firstName} ${student?.lastName}".lowercase().contains(query) ||
                        course?.courseName?.lowercase()?.contains(query) == true
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
        val cbMakeup = dialogView.findViewById<MaterialCheckBox>(R.id.cbMakeup)
        val makeupLayout = dialogView.findViewById<View>(R.id.makeupLayout)
        val actvMakeupDay = dialogView.findViewById<AutoCompleteTextView>(R.id.actvMakeupDay)
        val etMakeupStart = dialogView.findViewById<TextInputEditText>(R.id.etMakeupStart)
        val etMakeupEnd = dialogView.findViewById<TextInputEditText>(R.id.etMakeupEnd)

        var selectedStudentId = ""
        var selectedCourseId = ""
        var selectedStatus = ""

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        actvMakeupDay.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days))

        fun buildMakeupSession() {
            val day = actvMakeupDay.text.toString().trim()
            val start = etMakeupStart.text.toString().trim()
            val end = etMakeupEnd.text.toString().trim()
            if (day.isNotEmpty() && start.isNotEmpty() && end.isNotEmpty()) {
                etSession.setText("$day $start - $end")
            }
        }

        actvMakeupDay.setOnItemClickListener { _, _, _, _ -> buildMakeupSession() }

        etMakeupStart.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                etMakeupStart.setText(String.format("%02d:%02d", h, m))
                buildMakeupSession()
            }, 9, 0, true).show()
        }

        etMakeupEnd.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                etMakeupEnd.setText(String.format("%02d:%02d", h, m))
                buildMakeupSession()
            }, 11, 0, true).show()
        }

        cbMakeup.setOnCheckedChangeListener { _, isChecked ->
            makeupLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                val course = courseList.find { it.courseId == selectedCourseId }
                etSession.setText(course?.schedule ?: "")
                actvMakeupDay.setText("")
                etMakeupStart.setText("")
                etMakeupEnd.setText("")
            }
        }

        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        if (isEdit) {
            val student = studentList.find { it.studentId == attendance!!.studentId }
            val course = courseList.find { it.courseId == attendance!!.courseId }
            btnSelectStudent.text = if (student != null) "${student.firstName} ${student.lastName}" else attendance!!.studentId
            btnSelectCourse.text = course?.courseName ?: attendance!!.courseId
            btnSelectStatus.text = attendance!!.status
            selectedStudentId = attendance!!.studentId
            selectedCourseId = attendance!!.courseId
            selectedStatus = attendance!!.status
            etDate.setText(attendance!!.date)
            etSession.setText(attendance!!.session)
        }

        btnSelectStudent.setOnClickListener {
            val opts = studentList.map { "${it.firstName} ${it.lastName} (${it.studentId})" to it.studentId }
            showPickerSheet("Select Student", opts) { _, id ->
                selectedStudentId = id
                val s = studentList.find { it.studentId == id }
                btnSelectStudent.text = if (s != null) "${s.firstName} ${s.lastName}" else id
            }
        }

        btnSelectCourse.setOnClickListener {
            val opts = courseList.map { "${it.courseName} (${it.courseCode})" to it.courseId }
            showPickerSheet("Select Course", opts) { _, id ->
                selectedCourseId = id
                btnSelectCourse.text = courseList.find { it.courseId == id }?.courseName ?: id
                if (!cbMakeup.isChecked) etSession.setText(courseList.find { it.courseId == id }?.schedule ?: "")
            }
        }

        btnSelectStatus.setOnClickListener {
            val statuses = arrayOf("Present", "Absent", "Late")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Status")
                .setItems(statuses) { _, which ->
                    selectedStatus = statuses[which]
                    btnSelectStatus.text = selectedStatus
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val date = etDate.text.toString().trim()
            val session = etSession.text.toString().trim()
            if (selectedStudentId.isEmpty() || selectedCourseId.isEmpty() || selectedStatus.isEmpty() || date.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isEdit) viewModel.updateAttendance(attendance!!.docId, selectedStudentId, selectedCourseId, selectedStatus, date, session)
            else viewModel.addAttendance(selectedStudentId, selectedCourseId, selectedStatus, date, session)
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDeleteDialog(attendance: Attendance) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Attendance")
            .setMessage("Confirm deletion?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteAttendance(attendance.docId) }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAttendance.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showPickerSheet(title: String, options: List<Pair<String, String>>, currentValue: String = "", onSelect: (String, String) -> Unit) {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<android.widget.TextView>(R.id.tvFilterTitle).text = title
        val allOptions = options.toMutableList()
        var filteredOptions = allOptions.toMutableList()
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class VH(val b: com.sers.app.databinding.ItemFilterOptionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
                VH(com.sers.app.databinding.ItemFilterOptionBinding.inflate(layoutInflater, parent, false))
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                val opt = filteredOptions[pos]
                (holder as VH).b.tvOption.text = opt.first
                holder.b.ivCheck.visibility = if (opt.second == currentValue) View.VISIBLE else View.GONE
                holder.b.root.setOnClickListener { onSelect(opt.first, opt.second); bottomSheet.dismiss() }
            }
            override fun getItemCount() = filteredOptions.size
            fun update(list: MutableList<Pair<String, String>>) { filteredOptions = list; notifyDataSetChanged() }
        }
        rv.adapter = adapter
        sheetView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFilterSearch)
            .addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                    val q = s.toString().lowercase()
                    adapter.update(if (q.isEmpty()) allOptions.toMutableList() else allOptions.filter { it.first.lowercase().contains(q) }.toMutableList())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        bottomSheet.show()
    }
}