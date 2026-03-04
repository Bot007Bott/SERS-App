package com.sers.app.ui.teacher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.sers.app.databinding.FragmentTeacherAttendanceBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Enrollment
import com.sers.app.model.Student
import com.sers.app.ui.admin.AttendanceAdapter
import com.sers.app.viewmodel.TeacherAttendanceViewModel

/**
 * TeacherAttendanceFragment — MVVM View
 * Observes TeacherAttendanceViewModel for all data. No Firebase code here.
 */
class TeacherAttendanceFragment : Fragment() {

    private lateinit var binding: FragmentTeacherAttendanceBinding
    private lateinit var adapter: AttendanceAdapter
    private val viewModel: TeacherAttendanceViewModel by viewModels()

    private val attendanceList = mutableListOf<Attendance>()
    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()
    private val enrollmentList = mutableListOf<Enrollment>()

    private var currentFilterStudentId = "All"
    private var currentFilterCourseId = "All"
    private var currentFilterStatus = "All"
    private var currentSortOrder = "Default"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AttendanceAdapter(
            mutableListOf(),
            onEditClick = { showAttendanceDialog(it) },
            onDeleteClick = { showDeleteDialog(it) }
        )
        binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendance.adapter = adapter
        updateEmptyState(emptyList())

        // Observe ViewModel
        viewModel.attendance.observe(viewLifecycleOwner) { list ->
            attendanceList.clear(); attendanceList.addAll(list)
            updateSummaryCards(); refreshList()
        }
        viewModel.students.observe(viewLifecycleOwner) { students ->
            studentList.clear(); studentList.addAll(students)
        }
        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            courseList.clear(); courseList.addAll(courses)
        }
        viewModel.enrollments.observe(viewLifecycleOwner) { enrollments ->
            enrollmentList.clear(); enrollmentList.addAll(enrollments)
        }
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            val text = if (msg.startsWith("ERROR:")) msg.removePrefix("ERROR:") else msg
            Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT).show()
        }

        viewModel.loadAllData()

        binding.btnSearch.setOnClickListener {
            val isVisible = binding.searchLayout.visibility == View.VISIBLE
            binding.searchLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) binding.etSearch.requestFocus()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filtered = getFilteredSortedList().filter { att ->
                    val student = studentList.find { it.studentId == att.studentId }
                    val course = courseList.find { it.courseId == att.courseId }
                    query.isEmpty() || "${student?.firstName} ${student?.lastName}".lowercase().contains(query) ||
                            course?.courseName?.lowercase()?.contains(query) == true
                }.toMutableList()
                adapter.updateList(filtered)
                adapter.setStudentsAndCourses(studentList, courseList)
                updateEmptyState(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnFilter.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter by")
                .setItems(arrayOf("Student", "Course", "Status")) { _, which ->
                    when (which) {
                        0 -> showPickerFilterSheet("Filter by Student",
                            listOf(Pair("All", "All")) + studentList.map { Pair("${it.firstName} ${it.lastName}", it.studentId) }
                        ) { id -> currentFilterStudentId = id; refreshList(); updateFilterButton() }
                        1 -> showPickerFilterSheet("Filter by Course",
                            listOf(Pair("All", "All")) + courseList.map { Pair(it.courseName, it.courseId) }
                        ) { id -> currentFilterCourseId = id; refreshList(); updateFilterButton() }
                        2 -> showPickerFilterSheet("Filter by Status",
                            listOf("All", "Present", "Absent", "Late").map { Pair(it, it) }
                        ) { id -> currentFilterStatus = id; refreshList(); updateFilterButton() }
                    }
                }.show()
        }

        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Date Newest", "Date Oldest")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]; refreshList(); updateSortButton(); dialog.dismiss()
                }.show()
        }

        binding.btnMarkAttendance.setOnClickListener { showAttendanceDialog(null) }
    }

    private fun refreshList() {
        val list = getFilteredSortedList()
        adapter.updateList(list)
        adapter.setStudentsAndCourses(studentList, courseList)
        updateEmptyState(list)
    }

    private fun getFilteredSortedList(): MutableList<Attendance> {
        var list = attendanceList.toMutableList()
        if (currentFilterStudentId != "All") list = list.filter { it.studentId == currentFilterStudentId }.toMutableList()
        if (currentFilterCourseId != "All") list = list.filter { it.courseId == currentFilterCourseId }.toMutableList()
        if (currentFilterStatus != "All") list = list.filter { it.status == currentFilterStatus }.toMutableList()
        return when (currentSortOrder) {
            "Date Newest" -> list.sortedByDescending { it.date }.toMutableList()
            "Date Oldest" -> list.sortedBy { it.date }.toMutableList()
            else -> list
        }
    }

    private fun showAttendanceDialog(attendance: Attendance?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_attendance, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        val isEdit = attendance != null

        dialogView.findViewById<TextView>(R.id.tvTitle).text = if (isEdit) "Edit Attendance" else "Mark Attendance"
        dialogView.findViewById<MaterialButton>(R.id.btnSave).text = if (isEdit) "Update" else "Save"

        val btnSelectStudent = dialogView.findViewById<MaterialButton>(R.id.btnSelectStudent)
        val btnSelectCourse = dialogView.findViewById<MaterialButton>(R.id.btnSelectCourse)
        val btnSelectStatus = dialogView.findViewById<MaterialButton>(R.id.btnSelectStatus)
        val etSession = dialogView.findViewById<TextInputEditText>(R.id.etSession)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etDate)
        val cbMakeup = dialogView.findViewById<android.widget.CheckBox>(R.id.cbMakeup)
        val makeupLayout = dialogView.findViewById<android.widget.LinearLayout>(R.id.makeupLayout)
        val actvMakeupDay = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.actvMakeupDay)
        val etMakeupStart = dialogView.findViewById<TextInputEditText>(R.id.etMakeupStart)
        val etMakeupEnd = dialogView.findViewById<TextInputEditText>(R.id.etMakeupEnd)

        var selectedStudentId = ""
        var selectedCourseId = ""
        var selectedStatus = ""

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        actvMakeupDay.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days))

        fun buildMakeupSession() {
            val day = actvMakeupDay.text.toString().trim()
            val start = etMakeupStart.text.toString().trim()
            val end = etMakeupEnd.text.toString().trim()
            if (day.isNotEmpty() && start.isNotEmpty() && end.isNotEmpty())
                etSession.setText("$day $start - $end (Makeup)")
        }

        etMakeupStart.setOnClickListener {
            android.app.TimePickerDialog(requireContext(), { _, h, m ->
                etMakeupStart.setText(String.format("%02d:%02d", h, m)); buildMakeupSession()
            }, 9, 0, true).show()
        }
        etMakeupEnd.setOnClickListener {
            android.app.TimePickerDialog(requireContext(), { _, h, m ->
                etMakeupEnd.setText(String.format("%02d:%02d", h, m)); buildMakeupSession()
            }, 11, 0, true).show()
        }
        actvMakeupDay.setOnItemClickListener { _, _, _, _ -> buildMakeupSession() }

        cbMakeup.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { makeupLayout.visibility = View.VISIBLE; etSession.setText("") }
            else { makeupLayout.visibility = View.GONE; val course = courseList.find { it.courseId == selectedCourseId }; etSession.setText(course?.schedule ?: "") }
        }

        etDate.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
                etDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        if (isEdit) {
            val student = studentList.find { it.studentId == attendance!!.studentId }
            val course = courseList.find { it.courseId == attendance!!.courseId }
            btnSelectStudent.text = if (student != null) "${student.firstName} ${student.lastName}" else attendance!!.studentId
            btnSelectCourse.text = course?.courseName ?: attendance!!.courseId
            btnSelectStatus.text = attendance!!.status
            selectedStudentId = attendance!!.studentId; selectedCourseId = attendance!!.courseId; selectedStatus = attendance!!.status
            etDate.setText(attendance!!.date); etSession.setText(attendance!!.session)
        }

        btnSelectStudent.setOnClickListener {
            val enrolledIds = enrollmentList.filter { e -> courseList.any { it.courseId == e.courseId } }.map { it.studentId }.distinct()
            val options = studentList.filter { it.studentId in enrolledIds }.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }
            showPickerSheet("Select Student", options) { name, id ->
                selectedStudentId = id; btnSelectStudent.text = name.substringBefore(" (")
                selectedCourseId = ""; btnSelectCourse.text = "Select Course"; etSession.setText("")
            }
        }

        btnSelectCourse.setOnClickListener {
            if (selectedStudentId.isEmpty()) { Snackbar.make(binding.root, "Please select a student first!", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            val enrolledCourseIds = enrollmentList.filter { it.studentId == selectedStudentId }.map { it.courseId }
            val available = courseList.filter { it.courseId in enrolledCourseIds }
            if (available.isEmpty()) { Snackbar.make(binding.root, "Student is not enrolled in any of your courses!", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            showPickerSheet("Select Course", available.map { Pair(it.courseName, it.courseId) }) { name, id ->
                selectedCourseId = id; btnSelectCourse.text = name
                if (!cbMakeup.isChecked) { val course = courseList.find { it.courseId == id }; etSession.setText(course?.schedule ?: "") }
            }
        }

        btnSelectStatus.setOnClickListener {
            showPickerSheet("Select Status", listOf("Present", "Absent", "Late").map { Pair(it, it) }) { name, id -> selectedStatus = id; btnSelectStatus.text = name }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val date = etDate.text.toString().trim(); val session = etSession.text.toString().trim()
            if (selectedStudentId.isEmpty() || selectedCourseId.isEmpty() || selectedStatus.isEmpty() || date.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (isEdit) viewModel.updateAttendance(attendance!!.docId, selectedStudentId, selectedCourseId, selectedStatus, date, session)
            else viewModel.markAttendance(selectedStudentId, selectedCourseId, selectedStatus, date, session)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteDialog(attendance: Attendance) {
        val student = studentList.find { it.studentId == attendance.studentId }
        val course = courseList.find { it.courseId == attendance.courseId }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Record")
            .setMessage("Delete ${student?.firstName} ${student?.lastName}'s ${course?.courseName} attendance?")
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Delete") { d, _ -> viewModel.deleteAttendance(attendance.docId); d.dismiss() }.show()
    }

    private fun showPickerFilterSheet(title: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
        showPickerSheet(title, options) { _, id -> onSelect(id) }
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
                val opt = filteredOptions[position]; (holder as VH).b.tvOption.text = opt.first; holder.b.ivCheck.visibility = View.GONE
                holder.b.root.setOnClickListener { onSelect(opt.first, opt.second); bottomSheet.dismiss() }
            }
            override fun getItemCount() = filteredOptions.size
            fun updateList(newList: MutableList<Pair<String, String>>) { filteredOptions = newList; notifyDataSetChanged() }
        }
        rv.adapter = filterAdapter
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().lowercase()
                filterAdapter.updateList(if (q.isEmpty()) allOptions.toMutableList() else allOptions.filter { it.first.lowercase().contains(q) }.toMutableList())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        bottomSheet.show()
    }

    private fun updateSummaryCards() {
        binding.tvPresent.text = attendanceList.count { it.status == "Present" }.toString()
        binding.tvAbsent.text = attendanceList.count { it.status == "Absent" }.toString()
        binding.tvLate.text = attendanceList.count { it.status == "Late" }.toString()
    }

    private fun updateFilterButton() {
        val active = currentFilterStudentId != "All" || currentFilterCourseId != "All" || currentFilterStatus != "All"
        binding.btnFilter.text = if (active) "Filter •" else "Filter"
        binding.btnFilter.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnFilter.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun updateSortButton() {
        val active = currentSortOrder != "Default"
        binding.btnSort.text = if (active) "Sort •" else "Sort"
        binding.btnSort.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnSort.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAttendance.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}