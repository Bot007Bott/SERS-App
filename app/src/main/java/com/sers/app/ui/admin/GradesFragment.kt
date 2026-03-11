package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.sers.app.R
import com.sers.app.databinding.FragmentGradesBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.model.Student
import com.sers.app.viewmodel.GradeViewModel

class GradesFragment : Fragment() {

    private lateinit var binding: FragmentGradesBinding
    private lateinit var adapter: GradeAdapter
    private val viewModel: GradeViewModel by viewModels()

    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()
    private val gradeList = mutableListOf<Grade>()

    private var selectedFilterStudentId = ""
    private var selectedFilterCourseId = ""
    private var currentSortOrder = "Default"

    private var isClosingFromX = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGradesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = GradeAdapter(
            mutableListOf(),
            onEditClick = { grade -> showGradeDialog(grade) },
            onDeleteClick = { grade -> showDeleteDialog(grade) }
        )
        binding.rvGrades.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGrades.adapter = adapter

        setupObservers()
        setupSearch()
        setupSort()

        binding.btnFilter.setOnClickListener { showFilterDialog() }
        binding.btnAddGrade.setOnClickListener { showGradeDialog(null) }

        viewModel.loadAllData()
    }

    private fun setupObservers() {
        viewModel.grades.observe(viewLifecycleOwner) { records ->
            binding.progressBar.visibility = View.GONE
            gradeList.clear(); gradeList.addAll(records)
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
            val sortOptions = arrayOf("Default", "Score High-Low", "Score Low-High")
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
            "By Course: ${if (selectedFilterCourseId.isEmpty()) "All" else courseList.find { it.courseId == selectedFilterCourseId }?.courseName ?: "All"}"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter Grades")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { selectedFilterStudentId = ""; selectedFilterCourseId = ""; updateFilterButtonUI(); refreshList() }
                    1 -> {
                        val opts = listOf("All Students" to "") + studentList.map { "${it.firstName} ${it.lastName} (${it.studentId})" to it.studentId }
                        showPickerSheet("Filter by Student", opts) { _, id -> selectedFilterStudentId = id; updateFilterButtonUI(); refreshList() }
                    }
                    2 -> {
                        val opts = listOf("All Courses" to "") + courseList.map { "${it.courseName} (${it.courseCode})" to it.courseId }
                        showPickerSheet("Filter by Course", opts) { _, id -> selectedFilterCourseId = id; updateFilterButtonUI(); refreshList() }
                    }
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun updateFilterButtonUI() {
        val active = selectedFilterStudentId.isNotEmpty() || selectedFilterCourseId.isNotEmpty()
        binding.btnFilter.text = if (active) "Filter •" else "Filter"
        binding.btnFilter.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnFilter.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun refreshList() {
        val query = binding.etSearch.text.toString().lowercase()
        var list = gradeList
            .filter { selectedFilterStudentId.isEmpty() || it.studentId == selectedFilterStudentId }
            .filter { selectedFilterCourseId.isEmpty() || it.courseId == selectedFilterCourseId }
            .filter {
                val student = studentList.find { st -> st.studentId == it.studentId }
                val course = courseList.find { c -> c.courseId == it.courseId }
                query.isEmpty() ||
                        "${student?.firstName} ${student?.lastName}".lowercase().contains(query) ||
                        course?.courseName?.lowercase()?.contains(query) == true
            }

        list = when (currentSortOrder) {
            "Score High-Low" -> list.sortedByDescending { it.score }
            "Score Low-High" -> list.sortedBy { it.score }
            else -> list
        }

        adapter.updateList(list.toMutableList())
        adapter.setStudentsAndCourses(studentList, courseList)
        updateEmptyState(list)
    }

    private fun showGradeDialog(grade: Grade?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_grade, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        val isEdit = grade != null
        val btnSelectStudent = dialogView.findViewById<MaterialButton>(R.id.btnSelectStudent)
        val btnSelectCourse = dialogView.findViewById<MaterialButton>(R.id.btnSelectCourse)
        val etScore = dialogView.findViewById<TextInputEditText>(R.id.etScore)
        val etTotal = dialogView.findViewById<TextInputEditText>(R.id.etTotalMarks)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etGradeTitle)
        val actvType = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.actvGradeType)
        var selectedStudentId = ""
        var selectedCourseId = ""

        actvType.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("Midterm", "Final", "Quiz", "Assignment", "Other")))

        if (isEdit) {
            val student = studentList.find { it.studentId == grade!!.studentId }
            val course = courseList.find { it.courseId == grade!!.courseId }
            btnSelectStudent.text = if (student != null) "${student.firstName} ${student.lastName}" else grade!!.studentId
            btnSelectCourse.text = course?.courseName ?: grade!!.courseId
            selectedStudentId = grade!!.studentId
            selectedCourseId = grade!!.courseId
            actvType.setText(grade!!.gradeType, false)
            etTitle.setText(grade!!.title)
            etScore.setText(grade!!.score.toString())
            etTotal.setText(grade!!.totalMarks.toString())
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
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val sStr = etScore.text.toString().trim()
            val tStr = etTotal.text.toString().trim()
            val type = actvType.text.toString().trim()
            val title = etTitle.text.toString().trim()
            if (selectedStudentId.isEmpty() || selectedCourseId.isEmpty() || sStr.isEmpty() || type.isEmpty()) {
                Snackbar.make(binding.root, "Required fields missing!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val score = sStr.toIntOrNull() ?: run {
                Snackbar.make(binding.root, "Invalid score", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val total = tStr.toIntOrNull() ?: 100
            if (score > total) {
                Snackbar.make(binding.root, "Score cannot exceed total marks", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isEdit) viewModel.updateGrade(grade!!.docId, selectedStudentId, selectedCourseId, type, title, score, total)
            else viewModel.addGrade(selectedStudentId, selectedCourseId, type, title, score, total)
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDeleteDialog(grade: Grade) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Grade")
            .setMessage("Confirm deletion?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteGrade(grade.docId) }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvGrades.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
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

        val currentFilterValue = when {
            title.contains("Student", ignoreCase = true) -> selectedFilterStudentId
            title.contains("Course", ignoreCase = true) -> selectedFilterCourseId
            else -> ""
        }

        val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class VH(val b: com.sers.app.databinding.ItemFilterOptionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
                VH(com.sers.app.databinding.ItemFilterOptionBinding.inflate(layoutInflater, parent, false))
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                val opt = filteredOptions[pos]
                (holder as VH).b.tvOption.text = opt.first
                // Show checkmark if this option matches the current filter
                holder.b.ivCheck.visibility = if (opt.second == currentFilterValue) View.VISIBLE else View.GONE
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