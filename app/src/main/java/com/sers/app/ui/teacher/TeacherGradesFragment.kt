package com.sers.app.ui.teacher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.sers.app.databinding.FragmentTeacherGradesBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.model.Student
import com.sers.app.ui.admin.GradeAdapter
import com.sers.app.viewmodel.TeacherGradeViewModel

class TeacherGradesFragment : Fragment() {

    private lateinit var binding: FragmentTeacherGradesBinding
    private lateinit var adapter: GradeAdapter
    private val viewModel: TeacherGradeViewModel by viewModels()

    private val fullGradeList = mutableListOf<Grade>()
    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()

    private var currentFilterStudentId = ""
    private var currentFilterCourseId = ""
    private var currentSortOrder = "Default"

    private var isClosingFromX = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherGradesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupObservers()
        viewModel.loadAllData()
    }

    private fun setupRecyclerView() {
        adapter = GradeAdapter(mutableListOf(), onEditClick = { showGradeDialog(it) }, onDeleteClick = { showDeleteDialog(it) })
        binding.rvGrades.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGrades.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.grades.observe(viewLifecycleOwner) { list ->
            fullGradeList.clear(); fullGradeList.addAll(list); updateUI()
        }
        viewModel.students.observe(viewLifecycleOwner) { list ->
            studentList.clear(); studentList.addAll(list)
            adapter.setStudentsAndCourses(studentList, courseList)
            updateUI()
        }
        viewModel.courses.observe(viewLifecycleOwner) { list ->
            courseList.clear(); courseList.addAll(list)
            adapter.setStudentsAndCourses(studentList, courseList)
            updateUI()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.message.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) Snackbar.make(binding.root, it.removePrefix("ERROR:"), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        val query = binding.etSearch.text.toString()
        var list = fullGradeList.filter { g ->
            if (query.isEmpty()) return@filter true
            val s = studentList.find { it.studentId == g.studentId }
            val c = courseList.find { it.courseId == g.courseId }
            "${s?.firstName} ${s?.lastName}".contains(query, true) || c?.courseName?.contains(query, true) == true
        }
        if (currentFilterStudentId.isNotEmpty()) list = list.filter { it.studentId == currentFilterStudentId }
        if (currentFilterCourseId.isNotEmpty()) list = list.filter { it.courseId == currentFilterCourseId }
        list = when (currentSortOrder) {
            "Score High-Low" -> list.sortedByDescending { it.score }
            "Score Low-High" -> list.sortedBy { it.score }
            else -> list
        }
        adapter.updateList(list.toMutableList())
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvGrades.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            if (isClosingFromX) { isClosingFromX = false; return@setOnClickListener }
            toggleSearch()
        }

        binding.searchLayout.setEndIconOnClickListener {
            isClosingFromX = true
            binding.searchLayout.visibility = View.GONE
            binding.etSearch.setText("")
            binding.btnSearch.setIconResource(R.drawable.ic_search)
            binding.btnSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnSearch.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
            updateUI()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { updateUI() }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnFilter.setOnClickListener { showFilterOptions() }
        binding.btnSort.setOnClickListener { showSortOptions() }
        binding.btnAddGrade.setOnClickListener { showGradeDialog(null) }
    }

    private fun toggleSearch() {
        if (binding.searchLayout.visibility == View.GONE) {
            binding.searchLayout.visibility = View.VISIBLE
            binding.btnSearch.setIconResource(R.drawable.ic_close)
            binding.btnSearch.setBackgroundColor(android.graphics.Color.parseColor("#1976D2"))
            binding.btnSearch.setTextColor(android.graphics.Color.WHITE)
            binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        } else {
            binding.searchLayout.visibility = View.GONE
            binding.etSearch.setText("")
            updateUI()
            binding.btnSearch.setIconResource(R.drawable.ic_search)
            binding.btnSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnSearch.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
        }
    }

    private fun showFilterOptions() {
        val options = arrayOf("By Student", "By Course", "Clear Filters")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Filter by").setItems(options) { _, which ->
            when (which) {
                0 -> showStudentPicker()
                1 -> showCoursePicker()
                2 -> { currentFilterStudentId = ""; currentFilterCourseId = ""; updateUI(); updateFilterButtonUI() }
            }
        }.show()
    }

    private fun showStudentPicker() {
        val enrolls = viewModel.enrollments.value ?: return
        val myCIds = courseList.map { it.courseId }
        val sIds = enrolls.filter { it.courseId in myCIds }.map { it.studentId }.distinct()
        val opts = listOf("All Students" to "") + studentList.filter { it.studentId in sIds }.map { "${it.firstName} ${it.lastName} (${it.studentId})" to it.studentId }
        showPickerSheet("Select Student", opts) { _, id -> currentFilterStudentId = id; updateUI(); updateFilterButtonUI() }
    }

    private fun showCoursePicker() {
        val opts = listOf("All Courses" to "") + courseList.map { "${it.courseName} (${it.courseCode})" to it.courseId }
        showPickerSheet("Select Course", opts) { _, id -> currentFilterCourseId = id; updateUI(); updateFilterButtonUI() }
    }

    private fun showSortOptions() {
        val opts = arrayOf("Default", "Score High-Low", "Score Low-High")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Sort by")
            .setSingleChoiceItems(opts, opts.indexOf(currentSortOrder)) { d, w ->
                currentSortOrder = opts[w]; updateUI(); updateSortButtonUI(); d.dismiss()
            }.show()
    }

    private fun showGradeDialog(grade: Grade?) {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_grade, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(v).create()
        val isEdit = grade != null

        v.findViewById<android.widget.TextView>(R.id.tvTitle).text = if (isEdit) "Edit Grade" else "Add Grade"

        val btnSelectS = v.findViewById<MaterialButton>(R.id.btnSelectStudent)
        val btnSelectC = v.findViewById<MaterialButton>(R.id.btnSelectCourse)
        val etScore = v.findViewById<TextInputEditText>(R.id.etScore)
        val etTotal = v.findViewById<TextInputEditText>(R.id.etTotalMarks)
        val actvType = v.findViewById<android.widget.AutoCompleteTextView>(R.id.actvGradeType)
        val etTitle = v.findViewById<TextInputEditText>(R.id.etGradeTitle)

        // Fix: set adapter for grade type dropdown
        val gradeTypes = listOf("Midterm", "Final", "Quiz", "Assignment", "Other")
        actvType.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, gradeTypes))
        actvType.setOnClickListener { actvType.showDropDown() }

        var sId = grade?.studentId ?: ""
        var cId = grade?.courseId ?: ""

        if (isEdit) {
            val s = studentList.find { it.studentId == sId }
            val c = courseList.find { it.courseId == cId }
            btnSelectS.text = if (s != null) "${s.firstName} ${s.lastName}" else sId
            btnSelectC.text = c?.courseName ?: cId
            etScore.setText(grade!!.score.toString())
            etTotal.setText(grade.totalMarks.toString())
            actvType.setText(grade.gradeType, false)
            etTitle.setText(grade.title)
        }

        btnSelectS.setOnClickListener {
            val enrolls = viewModel.enrollments.value ?: return@setOnClickListener
            val myCIds = courseList.map { it.courseId }
            val sIds = enrolls.filter { it.courseId in myCIds }.map { it.studentId }.distinct()
            val opts = studentList.filter { it.studentId in sIds }.map { "${it.firstName} ${it.lastName} (${it.studentId})" to it.studentId }
            showPickerSheet("Select Student", opts) { _, id ->
                sId = id
                val s = studentList.find { it.studentId == id }
                btnSelectS.text = if (s != null) "${s.firstName} ${s.lastName}" else id
                cId = ""
                btnSelectC.text = "Select Course"
            }
        }

        btnSelectC.setOnClickListener {
            if (sId.isEmpty()) {
                Snackbar.make(binding.root, "Please select a student first", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val enrolls = viewModel.enrollments.value ?: return@setOnClickListener
            val myCIds = courseList.map { it.courseId }
            val allowedC = enrolls.filter { it.studentId == sId && it.courseId in myCIds }.map { it.courseId }
            val opts = courseList.filter { it.courseId in allowedC }.map { "${it.courseName} (${it.courseCode})" to it.courseId }
            showPickerSheet("Select Course", opts) { _, id ->
                cId = id
                btnSelectC.text = courseList.find { it.courseId == id }?.courseName ?: id
            }
        }

        v.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val scoreStr = etScore.text.toString().trim()
            val totalStr = etTotal.text.toString().trim()
            val type = actvType.text.toString().trim()
            val title = etTitle.text.toString().trim()

            if (sId.isEmpty() || cId.isEmpty()) { Snackbar.make(binding.root, "Please select a student and course", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            if (type.isEmpty()) { Snackbar.make(binding.root, "Please select a grade type", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            if (scoreStr.isEmpty()) { Snackbar.make(binding.root, "Please enter a score", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }

            val score = scoreStr.toIntOrNull()
            val total = totalStr.toIntOrNull()
            if (score == null || score < 0) { Snackbar.make(binding.root, "Score must be a valid positive number", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            if (total == null || total <= 0) { Snackbar.make(binding.root, "Total marks must be greater than zero", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            if (score > total) { Snackbar.make(binding.root, "Score cannot be greater than total marks", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }

            val data = mapOf("studentId" to sId, "courseId" to cId, "score" to score, "totalMarks" to total, "gradeType" to type, "title" to title)
            viewModel.saveGrade(grade?.docId, data)
            dialog.dismiss()
        }

        v.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun updateFilterButtonUI() {
        val active = currentFilterStudentId.isNotEmpty() || currentFilterCourseId.isNotEmpty()
        binding.btnFilter.text = if (active) "Filter •" else "Filter"
        binding.btnFilter.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnFilter.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun updateSortButtonUI() {
        val active = currentSortOrder != "Default"
        binding.btnSort.text = if (active) "Sort •" else "Sort"
        binding.btnSort.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
        binding.btnSort.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
        binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
    }

    private fun showDeleteDialog(grade: Grade) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Grade").setMessage("Delete this record?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteGrade(grade.docId) }
            .setNegativeButton("Cancel", null).show()
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
                holder.b.ivCheck.visibility = View.GONE
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