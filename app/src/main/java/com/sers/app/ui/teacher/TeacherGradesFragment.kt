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
import com.sers.app.databinding.FragmentTeacherGradesBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.model.Student
import com.sers.app.ui.admin.GradeAdapter
import com.sers.app.viewmodel.TeacherGradeViewModel

/**
 * TeacherGradesFragment — MVVM View
 * Observes TeacherGradeViewModel for grades management.
 */
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
        viewModel.grades.observe(viewLifecycleOwner) { list -> fullGradeList.clear(); fullGradeList.addAll(list); updateUI() }
        viewModel.students.observe(viewLifecycleOwner) { list -> studentList.clear(); studentList.addAll(list); adapter.setStudentsAndCourses(studentList, courseList) }
        viewModel.courses.observe(viewLifecycleOwner) { list -> courseList.clear(); courseList.addAll(list); adapter.setStudentsAndCourses(studentList, courseList) }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.message.observe(viewLifecycleOwner) { if (it.isNotEmpty()) Snackbar.make(binding.root, it.removePrefix("ERROR:"), Snackbar.LENGTH_SHORT).show() }
    }

    private fun updateUI() {
        val query = binding.etSearch.text.toString()
        var list = fullGradeList.filter { g ->
            val s = studentList.find { it.studentId == g.studentId }
            val c = courseList.find { it.courseId == g.courseId }
            s?.firstName?.contains(query, true) == true || s?.lastName?.contains(query, true) == true || c?.courseName?.contains(query, true) == true
        }
        if (currentFilterStudentId.isNotEmpty()) list = list.filter { it.studentId == currentFilterStudentId }
        if (currentFilterCourseId.isNotEmpty()) list = list.filter { it.courseId == currentFilterCourseId }
        list = when(currentSortOrder) {
            "Score High-Low" -> list.sortedByDescending { it.score }
            "Score Low-High" -> list.sortedBy { it.score }
            else -> list
        }
        adapter.updateList(list.toMutableList())
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvGrades.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener { toggleSearch() }
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
        } else {
            binding.searchLayout.visibility = View.GONE
            binding.etSearch.setText(""); updateUI()
            binding.btnSearch.setIconResource(R.drawable.ic_search)
        }
    }

    private fun showFilterOptions() {
        val options = arrayOf("By Student", "By Course", "Clear Filters")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Filter by").setItems(options) { _, which ->
            when(which) {
                0 -> showStudentPicker()
                1 -> showCoursePicker()
                2 -> { currentFilterStudentId = ""; currentFilterCourseId = ""; updateUI() }
            }
        }.show()
    }

    private fun showStudentPicker() {
        val enrolls = viewModel.enrollments.value ?: return
        val myCIds = courseList.map { it.courseId }
        val sIds = enrolls.filter { it.courseId in myCIds }.map { it.studentId }.distinct()
        val opts = studentList.filter { it.studentId in sIds }.map { Pair("${it.firstName} ${it.lastName}", it.studentId) }
        showPickerSheet("Select Student", listOf(Pair("All Students", "")) + opts) { _, id -> currentFilterStudentId = id; updateUI() }
    }

    private fun showCoursePicker() {
        val opts = courseList.map { Pair(it.courseName, it.courseId) }
        showPickerSheet("Select Course", listOf(Pair("All Courses", "")) + opts) { _, id -> currentFilterCourseId = id; updateUI() }
    }

    private fun showSortOptions() {
        val opts = arrayOf("Default", "Score High-Low", "Score Low-High")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Sort by").setSingleChoiceItems(opts, opts.indexOf(currentSortOrder)) { d, w ->
            currentSortOrder = opts[w]; updateUI(); d.dismiss()
        }.show()
    }

    private fun showGradeDialog(grade: Grade?) {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_grade, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(v).create()
        val isEdit = grade != null
        v.findViewById<TextView>(R.id.tvTitle).text = if (isEdit) "Edit Grade" else "Add Grade"
        val btnSelectS = v.findViewById<MaterialButton>(R.id.btnSelectStudent)
        val btnSelectC = v.findViewById<MaterialButton>(R.id.btnSelectCourse)
        val etScore = v.findViewById<TextInputEditText>(R.id.etScore)
        val etTotal = v.findViewById<TextInputEditText>(R.id.etTotalMarks)
        val actvType = v.findViewById<android.widget.AutoCompleteTextView>(R.id.actvGradeType)
        val etTitle = v.findViewById<TextInputEditText>(R.id.etGradeTitle)

        var sId = grade?.studentId ?: ""; var cId = grade?.courseId ?: ""
        if (isEdit) {
            val s = studentList.find { it.studentId == sId }; val c = courseList.find { it.courseId == cId }
            btnSelectS.text = if (s != null) "${s.firstName} ${s.lastName}" else sId
            btnSelectC.text = c?.courseName ?: cId
            etScore.setText(grade!!.score.toString()); etTotal.setText(grade.totalMarks.toString())
            actvType.setText(grade.gradeType, false); etTitle.setText(grade.title)
        }

        btnSelectS.setOnClickListener {
            val enrolls = viewModel.enrollments.value ?: return@setOnClickListener
            val myCIds = courseList.map { it.courseId }
            val sIds = enrolls.filter { it.courseId in myCIds }.map { it.studentId }.distinct()
            val opts = studentList.filter { it.studentId in sIds }.map { Pair("${it.firstName} ${it.lastName}", it.studentId) }
            showPickerSheet("Select Student", opts) { n, id -> sId = id; btnSelectS.text = n; cId = ""; btnSelectC.text = "Select Course" }
        }

        btnSelectC.setOnClickListener {
            if (sId.isEmpty()) return@setOnClickListener
            val enrolls = viewModel.enrollments.value ?: return@setOnClickListener
            val myCIds = courseList.map { it.courseId }
            val allowedC = enrolls.filter { it.studentId == sId && it.courseId in myCIds }.map { it.courseId }
            val opts = courseList.filter { it.courseId in allowedC }.map { Pair(it.courseName, it.courseId) }
            showPickerSheet("Select Course", opts) { n, id -> cId = id; btnSelectC.text = n }
        }

        v.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val score = etScore.text.toString().toIntOrNull() ?: 0
            val total = etTotal.text.toString().toIntOrNull() ?: 100
            val type = actvType.text.toString(); val title = etTitle.text.toString()
            if (sId.isEmpty() || cId.isEmpty() || type.isEmpty()) return@setOnClickListener
            val data = mapOf("studentId" to sId, "courseId" to cId, "score" to score, "totalMarks" to total, "gradeType" to type, "title" to title)
            viewModel.saveGrade(grade?.docId, data); dialog.dismiss()
        }
        v.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDeleteDialog(grade: Grade) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Grade").setMessage("Delete this record?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteGrade(grade.docId) }.setNegativeButton("Cancel", null).show()
    }

    private fun showPickerSheet(title: String, options: List<Pair<String, String>>, onSelect: (String, String) -> Unit) {
        val bs = BottomSheetDialog(requireContext()); val v = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bs.setContentView(v); v.findViewById<TextView>(R.id.tvFilterTitle).text = title
        val rv = v.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions); rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(p.context), p, false).root) {}
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) { (h.itemView as TextView).text = options[pos].first; h.itemView.setOnClickListener { onSelect(options[pos].first, options[pos].second); bs.dismiss() } }
            override fun getItemCount() = options.size
        }
        bs.show()
    }
}