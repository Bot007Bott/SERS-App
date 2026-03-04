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
import com.sers.app.databinding.FragmentGradesBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.model.Student
import com.sers.app.viewmodel.GradeViewModel

/**
 * GradesFragment — MVVM View
 * Observes GradeViewModel for grade, student, and course data.
 */
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

        binding.btnFilter.setOnClickListener { showFilterSheet() }
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
            val sortOptions = arrayOf("Default", "Score High-Low", "Score Low-High")
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
        sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = "Filter Grades"
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch).visibility = View.GONE
        val filterOptions = listOf(
            Pair("Clear All Filters", "all"),
            Pair("By Student: ${if (selectedFilterStudentId.isEmpty()) "All" else studentList.find { it.studentId == selectedFilterStudentId }?.firstName ?: "All"}", "student"),
            Pair("By Course: ${if (selectedFilterCourseId.isEmpty()) "All" else courseList.find { it.courseId == selectedFilterCourseId }?.courseName ?: "All"}", "course")
        )
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions); rv.layoutManager = LinearLayoutManager(requireContext())
        val fAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(p.context), p, false).root) {}
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) { (h.itemView as TextView).text = filterOptions[pos].first; h.itemView.setOnClickListener {
                when (filterOptions[pos].second) {
                    "all" -> { selectedFilterStudentId = ""; selectedFilterCourseId = ""; updateFilterButtonUI(); refreshList(); bottomSheet.dismiss() }
                    "student" -> { bottomSheet.dismiss(); showPickerSheet("Filter Student", listOf(Pair("All Students", "")) + studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }) { _, id -> selectedFilterStudentId = id; updateFilterButtonUI(); refreshList() } }
                    "course" -> { bottomSheet.dismiss(); showPickerSheet("Filter Course", listOf(Pair("All Courses", "")) + courseList.map { Pair("${it.courseName} (${it.courseCode})", it.courseId) }) { _, id -> selectedFilterCourseId = id; updateFilterButtonUI(); refreshList() } }
                }
            } }
            override fun getItemCount() = filterOptions.size
        }
        rv.adapter = fAdapter; bottomSheet.show()
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
        var list = gradeList.filter { selectedFilterStudentId.isEmpty() || it.studentId == selectedFilterStudentId }
            .filter { selectedFilterCourseId.isEmpty() || it.courseId == selectedFilterCourseId }
            .filter {
                val student = studentList.find { st -> st.studentId == it.studentId }
                val course = courseList.find { c -> c.courseId == it.courseId }
                query.isEmpty() || "${student?.firstName} ${student?.lastName}".lowercase().contains(query) || course?.courseName?.lowercase()?.contains(query) == true
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
        var selectedStudentId = ""; var selectedCourseId = ""

        actvType.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("Midterm", "Final", "Quiz", "Assignment", "Other")))

        if (isEdit) {
            val student = studentList.find { it.studentId == grade!!.studentId }
            val course = courseList.find { it.courseId == grade!!.courseId }
            btnSelectStudent.text = if (student != null) "${student.firstName} ${student.lastName}" else grade!!.studentId
            btnSelectCourse.text = course?.courseName ?: grade!!.courseId
            selectedStudentId = grade!!.studentId; selectedCourseId = grade!!.courseId
            actvType.setText(grade!!.gradeType, false); etTitle.setText(grade!!.title); etScore.setText(grade!!.score.toString()); etTotal.setText(grade!!.totalMarks.toString())
        }

        btnSelectStudent.setOnClickListener { showPickerSheet("Select Student", studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }) { name, id -> selectedStudentId = id; btnSelectStudent.text = name.substringBefore(" (") } }
        btnSelectCourse.setOnClickListener { showPickerSheet("Select Course", courseList.map { Pair("${it.courseName} (${it.courseCode})", it.courseId) }) { name, id -> selectedCourseId = id; btnSelectCourse.text = name.substringBefore(" (") } }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val sStr = etScore.text.toString().trim(); val tStr = etTotal.text.toString().trim(); val type = actvType.text.toString().trim(); val title = etTitle.text.toString().trim()
            if (selectedStudentId.isEmpty() || selectedCourseId.isEmpty() || sStr.isEmpty() || type.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val score = sStr.toIntOrNull()
            val total = tStr.toIntOrNull()
            if (score == null || score < 0) {
                Snackbar.make(binding.root, "Score must be a valid positive number", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (total == null || total <= 0) {
                Snackbar.make(binding.root, "Total marks must be greater than zero", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (score > total) {
                Snackbar.make(binding.root, "Score cannot be greater than total marks", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (isEdit) viewModel.updateGrade(grade!!.docId, selectedStudentId, selectedCourseId, type, title, score, total)
            else viewModel.addGrade(selectedStudentId, selectedCourseId, type, title, score, total)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteDialog(grade: Grade) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Grade").setMessage("Confirm deletion?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteGrade(grade.docId) }.setNegativeButton("Cancel") { d, _ -> d.dismiss() }.show()
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
        binding.rvGrades.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}
