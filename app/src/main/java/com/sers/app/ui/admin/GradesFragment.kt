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
import com.sers.app.databinding.FragmentGradesBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.model.Student

class GradesFragment : Fragment() {

    private lateinit var binding: FragmentGradesBinding
    private lateinit var adapter: GradeAdapter
    private val db = FirebaseFirestore.getInstance()

    private val gradeList = mutableListOf<Grade>()
    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()

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
                    query.isEmpty() || "${student?.firstName} ${student?.lastName}".lowercase().contains(query)
                            || course?.courseName?.lowercase()?.contains(query) == true
                }.toMutableList()
                adapter.updateList(filtered)
                adapter.setStudentsAndCourses(studentList, courseList)
                updateEmptyState(filtered)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnFilter.setOnClickListener { showFilterSheet() }

        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Score High-Low", "Score Low-High")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]
                    refreshList()
                    val active = currentSortOrder != "Default"
                    binding.btnSort.text = if (active) "Sort •" else "Sort"
                    binding.btnSort.setBackgroundColor(if (active) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.TRANSPARENT)
                    binding.btnSort.setTextColor(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
                    binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1976D2"))
                    dialog.dismiss()
                }.show()
        }

        binding.btnAddGrade.setOnClickListener { showGradeDialog(null) }
    }

    private fun showFilterSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = "Filter Grades"
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch).visibility = View.GONE

        val filterOptions = listOf(
            Pair("Clear All Filters", "all"),
            Pair("By Student: ${if (selectedFilterStudentId.isEmpty()) "All" else studentList.find { it.studentId == selectedFilterStudentId }?.firstName ?: selectedFilterStudentId}", "student"),
            Pair("By Course: ${if (selectedFilterCourseId.isEmpty()) "All" else courseList.find { it.courseId == selectedFilterCourseId }?.courseName ?: selectedFilterCourseId}", "course")
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
                            selectedFilterStudentId = ""; selectedFilterCourseId = ""
                            updateFilterButtonState(); refreshList(); bottomSheet.dismiss()
                        }
                        "student" -> {
                            bottomSheet.dismiss()
                            showPickerSheet("Filter by Student",
                                listOf(Pair("All Students", "")) + studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }
                            ) { _, id -> selectedFilterStudentId = id; updateFilterButtonState(); refreshList() }
                        }
                        "course" -> {
                            bottomSheet.dismiss()
                            showPickerSheet("Filter by Course",
                                listOf(Pair("All Courses", "")) + courseList.map { Pair("${it.courseName} (${it.courseCode})", it.courseId) }
                            ) { _, id -> selectedFilterCourseId = id; updateFilterButtonState(); refreshList() }
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
        val hasFilter = selectedFilterStudentId.isNotEmpty() || selectedFilterCourseId.isNotEmpty()
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
                loadGrades()
            }
        }
    }

    private fun loadGrades() {
        db.collection("grades").addSnapshotListener { snapshot, error ->
            binding.progressBar.visibility = View.GONE
            if (error != null) { Snackbar.make(binding.root, "Error: ${error.message}", Snackbar.LENGTH_SHORT).show(); return@addSnapshotListener }
            gradeList.clear()
            snapshot?.documents?.forEach { doc ->
                gradeList.add(Grade(
                    gradeId = doc.getString("gradeId") ?: "", studentId = doc.getString("studentId") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    gradeType = doc.getString("gradeType") ?: "",
                    title = doc.getString("title") ?: "",
                    score = (doc.getLong("score") ?: 0).toInt(),
                    totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(), docId = doc.id
                ))
            }
            refreshList()
        }
    }

    private fun getFilteredSortedList(): MutableList<Grade> {
        var list = gradeList.toMutableList()
        if (selectedFilterStudentId.isNotEmpty()) list = list.filter { it.studentId == selectedFilterStudentId }.toMutableList()
        if (selectedFilterCourseId.isNotEmpty()) list = list.filter { it.courseId == selectedFilterCourseId }.toMutableList()
        return when (currentSortOrder) {
            "Score High-Low" -> list.sortedByDescending { it.score }.toMutableList()
            "Score Low-High" -> list.sortedBy { it.score }.toMutableList()
            else -> list
        }
    }

    private fun showGradeDialog(grade: Grade?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_grade, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        val isEdit = grade != null

        dialogView.findViewById<TextView>(R.id.tvTitle).text = if (isEdit) "Edit Grade" else "Add Grade"
        dialogView.findViewById<MaterialButton>(R.id.btnSave).text = if (isEdit) "Update Grade" else "Add Grade"

        val btnSelectStudent = dialogView.findViewById<MaterialButton>(R.id.btnSelectStudent)
        val btnSelectCourse = dialogView.findViewById<MaterialButton>(R.id.btnSelectCourse)
        val actvGradeType = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.actvGradeType)
        val etGradeTitle = dialogView.findViewById<TextInputEditText>(R.id.etGradeTitle)
        var selectedStudentId = ""
        var selectedCourseId = ""

        // Setup grade type dropdown
        val gradeTypes = listOf("Midterm", "Final", "Quiz", "Assignment", "Other")
        val typeAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, gradeTypes)
        actvGradeType.setAdapter(typeAdapter)

        if (isEdit) {
            val student = studentList.find { it.studentId == grade!!.studentId }
            val course = courseList.find { it.courseId == grade!!.courseId }
            btnSelectStudent.text = if (student != null) "${student.firstName} ${student.lastName}" else grade!!.studentId
            btnSelectCourse.text = course?.courseName ?: grade!!.courseId
            selectedStudentId = grade!!.studentId
            selectedCourseId = grade!!.courseId
            actvGradeType.setText(grade!!.gradeType, false)
            etGradeTitle.setText(grade!!.title)
            dialogView.findViewById<TextInputEditText>(R.id.etScore).setText(grade!!.score.toString())
            dialogView.findViewById<TextInputEditText>(R.id.etTotalMarks).setText(grade!!.totalMarks.toString())
        }

        btnSelectStudent.setOnClickListener {
            showPickerSheet("Select Student", studentList.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }) { name, id ->
                selectedStudentId = id; btnSelectStudent.text = name.substringBefore(" (")
            }
        }
        btnSelectCourse.setOnClickListener {
            showPickerSheet("Select Course", courseList.map { Pair("${it.courseName} (${it.courseCode})", it.courseId) }) { name, id ->
                selectedCourseId = id; btnSelectCourse.text = name.substringBefore(" (")
            }
        }
        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val scoreStr = dialogView.findViewById<TextInputEditText>(R.id.etScore).text.toString().trim()
            val totalStr = dialogView.findViewById<TextInputEditText>(R.id.etTotalMarks).text.toString().trim()
            val gradeType = actvGradeType.text.toString().trim()
            val gradeTitle = etGradeTitle.text.toString().trim()
            if (selectedStudentId.isEmpty() || selectedCourseId.isEmpty() || scoreStr.isEmpty() || gradeType.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val score = scoreStr.toIntOrNull() ?: 0
            val total = totalStr.toIntOrNull() ?: 100
            if (score > total) { Snackbar.make(binding.root, "Score cannot exceed total marks!", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            val data = hashMapOf("studentId" to selectedStudentId, "courseId" to selectedCourseId,
                "gradeType" to gradeType, "title" to gradeTitle, "score" to score, "totalMarks" to total)
            if (isEdit) {
                db.collection("grades").document(grade!!.docId).update(data as Map<String, Any>)
                    .addOnSuccessListener { Snackbar.make(binding.root, "Grade updated!", Snackbar.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
            } else {
                data["gradeId"] = "G${System.currentTimeMillis()}"
                db.collection("grades").add(data)
                    .addOnSuccessListener { Snackbar.make(binding.root, "Grade added!", Snackbar.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteDialog(grade: Grade) {
        val student = studentList.find { it.studentId == grade.studentId }
        val course = courseList.find { it.courseId == grade.courseId }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Grade")
            .setMessage("Delete grade for ${student?.firstName} ${student?.lastName} in ${course?.courseName}?")
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Delete") { d, _ ->
                db.collection("grades").document(grade.docId).delete()
                    .addOnSuccessListener { Snackbar.make(binding.root, "Grade deleted!", Snackbar.LENGTH_SHORT).show() }
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
                val opt = filteredOptions[position]; (holder as VH).b.tvOption.text = opt.first; holder.b.ivCheck.visibility = View.GONE
                holder.b.root.setOnClickListener { onSelect(opt.first, opt.second); bottomSheet.dismiss() }
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
        binding.rvGrades.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}
