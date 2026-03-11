package com.sers.app.ui.admin

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.sers.app.R
import com.sers.app.databinding.FragmentCoursesBinding
import com.sers.app.model.Course
import com.sers.app.model.Teacher
import com.sers.app.viewmodel.CourseViewModel

class CoursesFragment : Fragment() {

    private lateinit var binding: FragmentCoursesBinding
    private lateinit var adapter: CourseAdapter
    private val viewModel: CourseViewModel by viewModels()

    private val teacherList = mutableListOf<Teacher>()
    private var currentSortOrder = "Default"
    private var currentFilter = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CourseAdapter(
            mutableListOf(),
            onEditClick = { course -> showCourseDialog(course) },
            onDeleteClick = { course -> showDeleteDialog(course) }
        )
        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = adapter

        setupObservers()
        setupSearch()
        setupSort()
        setupFilter()

        binding.btnAddCourse.setOnClickListener { showCourseDialog(null) }

        viewModel.loadTeachersAndCourses()
    }

    private fun setupObservers() {
        viewModel.courses.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = View.GONE
            refreshList()
        }
        viewModel.teachers.observe(viewLifecycleOwner) { teachers ->
            teacherList.clear()
            teacherList.addAll(teachers)
            adapter.setTeachers(teachers)
        }
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupFilter() {
        binding.btnFilter.setOnClickListener {
            val opts = listOf("All Teachers" to "") + teacherList.map { "${it.firstName} ${it.lastName} (${it.teacherId})" to it.teacherId }
            showPickerSheet("Filter by Teacher", opts) { _, id ->
                currentFilter = id
                refreshList()
                val active = currentFilter.isNotEmpty()
                binding.btnFilter.text = if (active) "Filter •" else "Filter"
                binding.btnFilter.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
                binding.btnFilter.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
                binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(
                    if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
            }
        }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            if (binding.searchLayout.visibility == View.GONE) {
                binding.searchLayout.visibility = View.VISIBLE
                binding.btnSearch.setBackgroundColor(android.graphics.Color.parseColor("#1976D2"))
                binding.btnSearch.setTextColor(android.graphics.Color.WHITE)
                binding.btnSearch.setIconResource(R.drawable.ic_close)
                binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            } else {
                binding.searchLayout.visibility = View.GONE
                binding.etSearch.setText("")
                binding.btnSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                binding.btnSearch.setTextColor(android.graphics.Color.parseColor("#1976D2"))
                binding.btnSearch.setIconResource(R.drawable.ic_search)
                binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
                refreshList()
            }
        }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshList() }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.searchLayout.setEndIconOnClickListener {
            binding.etSearch.setText("")
            binding.searchLayout.visibility = View.GONE
            binding.btnSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnSearch.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            binding.btnSearch.setIconResource(R.drawable.ic_search)
            binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
            refreshList()
        }
    }

    private fun setupSort() {
        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Name A-Z", "Name Z-A")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]
                    refreshList()
                    updateSortButtonUI()
                    dialog.dismiss()
                }.show()
        }
    }

    private fun refreshList() {
        val query = binding.etSearch.text.toString().lowercase()
        val allCourses = viewModel.courses.value ?: emptyList()

        var list = allCourses.filter {
            it.courseName.contains(query, ignoreCase = true) ||
                    it.courseCode.contains(query, ignoreCase = true)
        }

        if (currentFilter.isNotEmpty()) list = list.filter { it.teacherId == currentFilter }

        list = when (currentSortOrder) {
            "Name A-Z" -> list.sortedBy { it.courseName }
            "Name Z-A" -> list.sortedByDescending { it.courseName }
            else -> list
        }

        adapter.updateList(list.toMutableList())
        updateEmptyState(list)
    }

    private fun updateSortButtonUI() {
        val active = currentSortOrder != "Default"
        binding.btnSort.text = if (active) "Sort •" else "Sort"
        binding.btnSort.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnSort.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(
            if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun showCourseDialog(course: Course?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_course, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        val isEdit = course != null

        dialogView.findViewById<TextView>(R.id.tvTitle).text = if (isEdit) "Edit Course" else "Add New Course"
        dialogView.findViewById<TextView>(R.id.tvSubtitle).text = if (isEdit) "Update course information" else "Fill in the course information below"
        dialogView.findViewById<MaterialButton>(R.id.btnSave).text = if (isEdit) "Update Course" else "Add Course"

        // Teacher — now a button that opens bottom sheet picker
        val btnSelectTeacher = dialogView.findViewById<MaterialButton>(R.id.actvTeacher)
        val actvDay = dialogView.findViewById<AutoCompleteTextView>(R.id.actvDay)
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        actvDay.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days))

        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.etEndTime)
        val etSchedule = dialogView.findViewById<TextInputEditText>(R.id.etSchedule)
        val etCourseName = dialogView.findViewById<TextInputEditText>(R.id.etCourseName)
        val etCourseCode = dialogView.findViewById<TextInputEditText>(R.id.etCourseCode)

        var selectedTeacherId = ""

        fun buildSchedule() {
            val day = actvDay.text.toString().trim()
            val start = etStartTime.text.toString().trim()
            val end = etEndTime.text.toString().trim()
            if (day.isNotEmpty() && start.isNotEmpty() && end.isNotEmpty()) {
                etSchedule.setText("$day $start - $end")
            }
        }

        etStartTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                etStartTime.setText(String.format("%02d:%02d", h, m)); buildSchedule()
            }, 9, 0, true).show()
        }
        etEndTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                etEndTime.setText(String.format("%02d:%02d", h, m)); buildSchedule()
            }, 11, 0, true).show()
        }
        actvDay.setOnItemClickListener { _, _, _, _ -> buildSchedule() }

        etCourseName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isEdit) {
                    val prefix = s.toString().trim().take(3).uppercase()
                    if (prefix.isNotEmpty()) {
                        val existingCodes = viewModel.courses.value?.map { it.courseCode } ?: emptyList()
                        var num = 101
                        while (existingCodes.contains("$prefix$num")) num++
                        etCourseCode.setText("$prefix$num")
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (isEdit) {
            etCourseName.setText(course!!.courseName)
            etCourseCode.setText(course.courseCode)
            val parts = course.schedule.split(" ")
            if (parts.size >= 4) {
                actvDay.setText(parts[0], false)
                etStartTime.setText(parts[1])
                etEndTime.setText(parts[3])
            }
            etSchedule.setText(course.schedule)
            val teacher = teacherList.find { it.teacherId == course.teacherId }
            if (teacher != null) {
                selectedTeacherId = teacher.teacherId
                btnSelectTeacher.text = "${teacher.firstName} ${teacher.lastName}"
            }
        }

        // Teacher picker — bottom sheet with search
        btnSelectTeacher.setOnClickListener {
            val opts = listOf("No Teacher" to "") + teacherList.map { "${it.firstName} ${it.lastName} — ${it.department} (${it.teacherId})" to it.teacherId }
            showPickerSheet("Assign Teacher", opts) { _, id ->
                selectedTeacherId = id
                if (id.isEmpty()) {
                    btnSelectTeacher.text = "No Teacher"
                } else {
                    val t = teacherList.find { it.teacherId == id }
                    btnSelectTeacher.text = if (t != null) "${t.firstName} ${t.lastName}" else id
                }
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val name = etCourseName.text.toString().trim()
            val code = etCourseCode.text.toString().trim()
            val schedule = etSchedule.text.toString().trim()

            if (name.isEmpty() || code.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val list = viewModel.courses.value ?: emptyList()
            if (list.any { it.courseName.equals(name, true) && it.docId != (course?.docId ?: "") }) {
                Snackbar.make(binding.root, "Course name already exists!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (list.any { it.courseCode.equals(code, true) && it.docId != (course?.docId ?: "") }) {
                Snackbar.make(binding.root, "Course code already exists!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEdit) viewModel.updateCourse(course!!.docId, name, code, schedule, selectedTeacherId)
            else viewModel.addCourse(Course(courseId = "C${System.currentTimeMillis()}", courseName = name, courseCode = code, schedule = schedule, teacherId = selectedTeacherId, createdBy = "Admin", docId = ""))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteDialog(course: Course) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete ${course.courseName}?")
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Delete") { d, _ -> viewModel.deleteCourse(course.docId); d.dismiss() }
            .show()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvCourses.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showPickerSheet(title: String, options: List<Pair<String, String>>, onSelect: (String, String) -> Unit) {
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
                holder.b.ivCheck.visibility = if (opt.second == currentFilter) View.VISIBLE else View.GONE
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