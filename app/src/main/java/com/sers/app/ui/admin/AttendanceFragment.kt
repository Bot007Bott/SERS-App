package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentAttendanceBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Student

class AttendanceFragment : Fragment() {

    private lateinit var binding: FragmentAttendanceBinding
    private lateinit var adapter: AttendanceAdapter
    private val db = FirebaseFirestore.getInstance()

    private val attendanceList = mutableListOf<Attendance>()
    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()

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

        loadAllData()

        binding.btnSearch.setOnClickListener {
            val isVisible = binding.searchLayout.visibility == View.VISIBLE
            binding.searchLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) binding.etSearch.requestFocus()
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filtered = getFilteredSortedList().filter {
                    val student = studentList.find { st -> st.studentId == it.studentId }
                    val course = courseList.find { c -> c.courseId == it.courseId }
                    val studentName = "${student?.firstName} ${student?.lastName}".lowercase()
                    val courseName = course?.courseName?.lowercase() ?: ""
                    query.isEmpty() || studentName.contains(query) || courseName.contains(query)
                }.toMutableList()
                adapter.updateList(filtered)
                adapter.setStudentsAndCourses(studentList, courseList)
                updateEmptyState(filtered)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnFilter.setOnClickListener { showFilterSheet() }

        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Date Newest", "Date Oldest")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]
                    val list = getFilteredSortedList()
                    adapter.updateList(list)
                    adapter.setStudentsAndCourses(studentList, courseList)
                    updateEmptyState(list)
                    binding.btnSort.text = if (currentSortOrder == "Default") "Sort" else "Sort •"
                    val active = currentSortOrder != "Default"
                    binding.btnSort.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
                    binding.btnSort.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
                    binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
                    dialog.dismiss()
                }.show()
        }

        binding.btnMarkAttendance.setOnClickListener { showAttendanceDialog(null) }
    }

    private fun showFilterSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = "Filter Attendance"
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch).visibility = View.GONE

        val filterOptions = listOf(
            Pair("Clear All Filters", "all"),
            Pair("By Student: ${if (selectedFilterStudentId.isEmpty()) "All" else studentList.find { it.studentId == selectedFilterStudentId }?.firstName ?: selectedFilterStudentId}", "student"),
            Pair("By Course: ${if (selectedFilterCourseId.isEmpty()) "All" else courseList.find { it.courseId == selectedFilterCourseId }?.courseName ?: selectedFilterCourseId}", "course"),
            Pair("By Status: ${if (selectedFilterStatus.isEmpty()) "All" else selectedFilterStatus}", "status")
        )

        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val filterAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class VH(val b: com.sers.app.databinding.ItemFilterOptionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val b = com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false); return VH(b)
            }
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                (holder as VH).b.tvOption.text = filterOptions[position].first
                holder.b.ivCheck.visibility = View.GONE
                holder.b.root.setOnClickListener {
                    when (filterOptions[position].second) {
                        "all" -> {
                            selectedFilterStudentId = ""; selectedFilterCourseId = ""; selectedFilterStatus = ""
                            updateFilterButtonState(); refreshList(); bottomSheet.dismiss()
                        }
                        "student" -> {
                            bottomSheet.dismiss()
                            showPickerSheet("Filter by Student", listOf(Pair("All Students", "")) + studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }) { _, id ->
                                selectedFilterStudentId = id; updateFilterButtonState(); refreshList()
                            }
                        }
                        "course" -> {
                            bottomSheet.dismiss()
                            showPickerSheet("Filter by Course", listOf(Pair("All Courses", "")) + courseList.map { Pair("${it.courseName} (${it.courseCode})", it.courseId) }) { _, id ->
                                selectedFilterCourseId = id; updateFilterButtonState(); refreshList()
                            }
                        }
                        "status" -> {
                            bottomSheet.dismiss()
                            showPickerSheet("Filter by Status", listOf(Pair("All Status", "")) + listOf("Present", "Absent", "Late").map { Pair(it, it) }) { _, id ->
                                selectedFilterStatus = id; updateFilterButtonState(); refreshList()
                            }
                        }
                    }
                }
            }
            override fun getItemCount() = filterOptions.size
        }
        rv.adapter = filterAdapter
        bottomSheet.show()
    }

    private fun updateFilterButtonState() {
        val hasFilter = selectedFilterStudentId.isNotEmpty() || selectedFilterCourseId.isNotEmpty() || selectedFilterStatus.isNotEmpty()
        binding.btnFilter.text = if (hasFilter) "Filter •" else "Filter"
        binding.btnFilter.setBackgroundColor(if (hasFilter) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnFilter.setTextColor(if (hasFilter) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(if (hasFilter) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun refreshList() {
        val list = getFilteredSortedList()
        adapter.updateList(list)
        adapter.setStudentsAndCourses(studentList, courseList)
        updateEmptyState(list)
    }

    private fun loadAllData() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("students").get().addOnSuccessListener { studentDocs ->
            studentList.clear()
            studentDocs.forEach { doc ->
                studentList.add(Student(
                    studentId = doc.getString("studentId") ?: "", firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "", email = doc.getString("email") ?: "",
                    program = doc.getString("program") ?: "", phone = doc.getString("phone") ?: "", docId = doc.id
                ))
            }
            db.collection("courses").get().addOnSuccessListener { courseDocs ->
                courseList.clear()
                courseDocs.forEach { doc ->
                    courseList.add(Course(
                        courseId = doc.getString("courseId") ?: "", courseName = doc.getString("courseName") ?: "",
                        courseCode = doc.getString("courseCode") ?: "", schedule = doc.getString("schedule") ?: "",
                        teacherId = doc.getString("teacherId") ?: "", createdBy = doc.getString("createdBy") ?: "", docId = doc.id
                    ))
                }
                loadAttendance()
            }
        }
    }

    private fun loadAttendance() {
        db.collection("attendance").addSnapshotListener { snapshot, error ->
            binding.progressBar.visibility = View.GONE
            if (error != null) { Snackbar.make(binding.root, "Error: ${error.message}", Snackbar.LENGTH_SHORT).show(); return@addSnapshotListener }
            attendanceList.clear()
            snapshot?.documents?.forEach { doc ->
                attendanceList.add(Attendance(
                    attendanceId = doc.getString("attendanceId") ?: "", studentId = doc.getString("studentId") ?: "",
                    courseId = doc.getString("courseId") ?: "", session = doc.getString("timeSlot") ?: "",
                    date = doc.getString("date") ?: "", status = doc.getString("status") ?: "", docId = doc.id
                ))
            }
            binding.tvPresent.text = attendanceList.count { it.status == "Present" }.toString()
            binding.tvAbsent.text = attendanceList.count { it.status == "Absent" }.toString()
            binding.tvLate.text = attendanceList.count { it.status == "Late" }.toString()
            refreshList()
        }
    }

    private fun getFilteredSortedList(): MutableList<Attendance> {
        var list = attendanceList.toMutableList()
        if (selectedFilterStudentId.isNotEmpty()) list = list.filter { it.studentId == selectedFilterStudentId }.toMutableList()
        if (selectedFilterCourseId.isNotEmpty()) list = list.filter { it.courseId == selectedFilterCourseId }.toMutableList()
        if (selectedFilterStatus.isNotEmpty()) list = list.filter { it.status == selectedFilterStatus }.toMutableList()
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

        dialogView.findViewById<TextView>(R.id.tvTitle).text = if (isEdit) "Edit Attendance" else "Add Attendance"
        dialogView.findViewById<MaterialButton>(R.id.btnSave).text = if (isEdit) "Update" else "Add"

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
        actvMakeupDay.setAdapter(android.widget.ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, days))

        fun buildMakeupSession() {
            val day = actvMakeupDay.text.toString().trim()
            val start = etMakeupStart.text.toString().trim()
            val end = etMakeupEnd.text.toString().trim()
            if (day.isNotEmpty() && start.isNotEmpty() && end.isNotEmpty()) {
                etSession.setText("$day $start - $end (Makeup)")
            }
        }

        etMakeupStart.setOnClickListener {
            android.app.TimePickerDialog(requireContext(), { _, hour, minute ->
                etMakeupStart.setText(String.format("%02d:%02d", hour, minute))
                buildMakeupSession()
            }, 9, 0, true).show()
        }

        etMakeupEnd.setOnClickListener {
            android.app.TimePickerDialog(requireContext(), { _, hour, minute ->
                etMakeupEnd.setText(String.format("%02d:%02d", hour, minute))
                buildMakeupSession()
            }, 11, 0, true).show()
        }

        actvMakeupDay.setOnItemClickListener { _, _, _, _ -> buildMakeupSession() }

        cbMakeup.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                makeupLayout.visibility = android.view.View.VISIBLE
                etSession.setText("")
            } else {
                makeupLayout.visibility = android.view.View.GONE
                val course = courseList.find { it.courseId == selectedCourseId }
                etSession.setText(course?.schedule ?: "")
            }
        }

        etDate.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
                etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            }, cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
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
            showPickerSheet("Select Student", studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }) { name, id ->
                selectedStudentId = id; btnSelectStudent.text = name.substringBefore(" (")
            }
        }
        btnSelectCourse.setOnClickListener {
            showPickerSheet("Select Course", courseList.map { Pair("${it.courseName} (${it.courseCode})", it.courseId) }) { name, id ->
                selectedCourseId = id
                btnSelectCourse.text = name.substringBefore(" (")
                if (!cbMakeup.isChecked) {
                    val course = courseList.find { it.courseId == id }
                    etSession.setText(course?.schedule ?: "")
                }
            }
        }
        btnSelectStatus.setOnClickListener {
            showPickerSheet("Select Status", listOf("Present", "Absent", "Late").map { Pair(it, it) }) { name, id ->
                selectedStatus = id; btnSelectStatus.text = name
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val date = etDate.text.toString().trim()
            val session = etSession.text.toString().trim()
            if (selectedStudentId.isEmpty() || selectedCourseId.isEmpty() || selectedStatus.isEmpty() || date.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (isEdit) {
                db.collection("attendance").document(attendance!!.docId)
                    .update("studentId", selectedStudentId, "courseId", selectedCourseId, "status", selectedStatus, "date", date, "timeSlot", session)
                    .addOnSuccessListener { Snackbar.make(binding.root, "Attendance updated!", Snackbar.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
            } else {
                db.collection("attendance").add(hashMapOf(
                    "attendanceId" to "A${System.currentTimeMillis()}",
                    "studentId" to selectedStudentId,
                    "courseId" to selectedCourseId,
                    "status" to selectedStatus,
                    "date" to date,
                    "timeSlot" to session
                ))
                    .addOnSuccessListener { Snackbar.make(binding.root, "Attendance added!", Snackbar.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteDialog(attendance: Attendance) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Attendance").setMessage("Are you sure you want to delete this record?")
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Delete") { d, _ ->
                db.collection("attendance").document(attendance.docId).delete()
                    .addOnSuccessListener { Snackbar.make(binding.root, "Attendance deleted!", Snackbar.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
                d.dismiss()
            }.show()
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
                val option = filteredOptions[position]; (holder as VH).b.tvOption.text = option.first; holder.b.ivCheck.visibility = View.GONE
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

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAttendance.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}
