package com.sers.app.ui.teacher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentTeacherGradesBinding
import com.sers.app.model.Course
import com.sers.app.model.Enrollment
import com.sers.app.model.Grade
import com.sers.app.model.Student
import com.sers.app.ui.admin.GradeAdapter

class TeacherGradesFragment : Fragment() {

    private lateinit var binding: FragmentTeacherGradesBinding
    private lateinit var adapter: GradeAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val gradeList = mutableListOf<Grade>()
    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()
    private val enrollmentList = mutableListOf<Enrollment>()
    private var myTeacherId = ""

    private var currentFilterStudentId = ""
    private var currentFilterCourseId = ""
    private var currentSortOrder = "Default"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherGradesBinding.inflate(inflater, container, false)
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

        // Get teacher ID from logged in user
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                myTeacherId = docs.documents[0].getString("teacherId") ?: ""
                loadAllData()
            }

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
                val list = getFilteredSortedList()
                adapter.updateList(list)
                adapter.setStudentsAndCourses(studentList, courseList)
                updateEmptyState(list)
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val filtered = getFilteredSortedList().filter { grade ->
                    val student = studentList.find { it.studentId == grade.studentId }
                    val course = courseList.find { it.courseId == grade.courseId }
                    student?.firstName?.contains(query, ignoreCase = true) == true ||
                            student?.lastName?.contains(query, ignoreCase = true) == true ||
                            course?.courseName?.contains(query, ignoreCase = true) == true
                }.toMutableList()
                adapter.updateList(filtered)
                updateEmptyState(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchLayout.setEndIconOnClickListener {
            binding.etSearch.setText("")
            binding.searchLayout.visibility = View.GONE
            binding.btnSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnSearch.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            binding.btnSearch.setIconResource(R.drawable.ic_search)
            binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
            val list = getFilteredSortedList()
            adapter.updateList(list)
            adapter.setStudentsAndCourses(studentList, courseList)
            updateEmptyState(list)
        }

        binding.btnFilter.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter by")
                .setItems(arrayOf("Student", "Course")) { _, which ->
                    if (which == 0) showStudentFilterSheet() else showCourseFilterSheet()
                }.show()
        }

        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Score High-Low", "Score Low-High")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]
                    val list = getFilteredSortedList()
                    adapter.updateList(list)
                    adapter.setStudentsAndCourses(studentList, courseList)
                    updateEmptyState(list)
                    updateSortButton()
                    dialog.dismiss()
                }.show()
        }

        binding.btnAddGrade.setOnClickListener { showGradeDialog(null) }
    }

    private fun loadAllData() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("students").get().addOnSuccessListener { studentDocs ->
            studentList.clear()
            studentDocs.forEach { doc ->
                studentList.add(Student(
                    studentId = doc.getString("studentId") ?: "",
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    program = doc.getString("program") ?: "",
                    phone = doc.getString("phone") ?: "",
                    docId = doc.id
                ))
            }
            db.collection("courses").whereEqualTo("teacherId", myTeacherId).get()
                .addOnSuccessListener { courseDocs ->
                    courseList.clear()
                    courseDocs.forEach { doc ->
                        courseList.add(Course(
                            courseId = doc.getString("courseId") ?: "",
                            courseName = doc.getString("courseName") ?: "",
                            courseCode = doc.getString("courseCode") ?: "",
                            schedule = doc.getString("schedule") ?: "",
                            teacherId = doc.getString("teacherId") ?: "",
                            createdBy = doc.getString("createdBy") ?: "",
                            docId = doc.id
                        ))
                    }
                    db.collection("enrollments").get().addOnSuccessListener { enrollDocs ->
                        enrollmentList.clear()
                        enrollDocs.forEach { doc ->
                            enrollmentList.add(Enrollment(
                                enrollmentId = doc.getString("enrollmentId") ?: "",
                                studentId = doc.getString("studentId") ?: "",
                                courseId = doc.getString("courseId") ?: "",
                                docId = doc.id
                            ))
                        }
                        loadGrades()
                    }
                }
        }
    }

    private fun loadGrades() {
        db.collection("grades")
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                if (error != null) return@addSnapshotListener
                gradeList.clear()
                val myCourseIds = courseList.map { it.courseId }
                snapshot?.documents?.forEach { doc ->
                    val grade = Grade(
                        gradeId = doc.getString("gradeId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        gradeType = doc.getString("gradeType") ?: "",
                        title = doc.getString("title") ?: "",
                        score = (doc.getLong("score") ?: 0).toInt(),
                        totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(),
                        docId = doc.id
                    )
                    // Only show grades for teacher's courses
                    if (grade.courseId in myCourseIds) gradeList.add(grade)
                }
                val list = getFilteredSortedList()
                adapter.updateList(list)
                adapter.setStudentsAndCourses(studentList, courseList)
                updateEmptyState(list)
            }
    }

    private fun getFilteredSortedList(): MutableList<Grade> {
        var list = gradeList.toMutableList()
        if (currentFilterStudentId.isNotEmpty())
            list = list.filter { it.studentId == currentFilterStudentId }.toMutableList()
        if (currentFilterCourseId.isNotEmpty())
            list = list.filter { it.courseId == currentFilterCourseId }.toMutableList()
        list = when (currentSortOrder) {
            "Score High-Low" -> list.sortedByDescending { it.score }.toMutableList()
            "Score Low-High" -> list.sortedBy { it.score }.toMutableList()
            else -> list
        }
        return list
    }

    private fun showStudentFilterSheet() {
        val enrolledIds = enrollmentList
            .filter { e -> courseList.any { it.courseId == e.courseId } }
            .map { it.studentId }.distinct()
        val options = listOf(Pair("All Students", "")) +
                studentList.filter { it.studentId in enrolledIds }
                    .map { Pair("${it.firstName} ${it.lastName}", it.studentId) }
        showPickerSheet("Filter by Student", options) { name, id ->
            currentFilterStudentId = id
            val list = getFilteredSortedList()
            adapter.updateList(list)
            adapter.setStudentsAndCourses(studentList, courseList)
            updateEmptyState(list)
            updateFilterButton()
        }
    }

    private fun showCourseFilterSheet() {
        val options = listOf(Pair("All Courses", "")) +
                courseList.map { Pair(it.courseName, it.courseId) }
        showPickerSheet("Filter by Course", options) { name, id ->
            currentFilterCourseId = id
            val list = getFilteredSortedList()
            adapter.updateList(list)
            adapter.setStudentsAndCourses(studentList, courseList)
            updateEmptyState(list)
            updateFilterButton()
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
            val enrolledIds = enrollmentList
                .filter { e -> courseList.any { it.courseId == e.courseId } }
                .map { it.studentId }.distinct()
            val options = studentList.filter { it.studentId in enrolledIds }
                .map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }
            showPickerSheet("Select Student", options) { name, id ->
                selectedStudentId = id
                btnSelectStudent.text = name.substringBefore(" (")
                selectedCourseId = ""
                btnSelectCourse.text = "Select Course"
            }
        }

        btnSelectCourse.setOnClickListener {
            if (selectedStudentId.isEmpty()) {
                Snackbar.make(binding.root, "Please select a student first!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val enrolledCourseIds = enrollmentList.filter { it.studentId == selectedStudentId }.map { it.courseId }
            val availableCourses = courseList.filter { it.courseId in enrolledCourseIds }
            if (availableCourses.isEmpty()) {
                Snackbar.make(binding.root, "Student not enrolled in any of your courses!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showPickerSheet("Select Course", availableCourses.map { Pair(it.courseName, it.courseId) }) { name, id ->
                selectedCourseId = id
                btnSelectCourse.text = name
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val scoreStr = dialogView.findViewById<TextInputEditText>(R.id.etScore).text.toString().trim()
            val totalStr = dialogView.findViewById<TextInputEditText>(R.id.etTotalMarks).text.toString().trim()
            val gradeType = actvGradeType.text.toString().trim()
            val gradeTitle = etGradeTitle.text.toString().trim()

            if (selectedStudentId.isEmpty() || selectedCourseId.isEmpty() || scoreStr.isEmpty() || gradeType.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val score = scoreStr.toIntOrNull() ?: 0
            val total = totalStr.toIntOrNull() ?: 100
            if (score > total) {
                Snackbar.make(binding.root, "Score cannot exceed total marks!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
            .setMessage("Delete ${student?.firstName} ${student?.lastName}'s ${course?.courseName} grade?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Delete") { dialog, _ ->
                db.collection("grades").document(grade.docId)
                    .delete()
                    .addOnSuccessListener { Snackbar.make(binding.root, "Grade deleted!", Snackbar.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
                dialog.dismiss()
            }.show()
    }

    private fun updateFilterButton() {
        val isActive = currentFilterStudentId.isNotEmpty() || currentFilterCourseId.isNotEmpty()
        binding.btnFilter.text = if (!isActive) "Filter" else "Filter •"
        binding.btnFilter.setBackgroundColor(
            if (!isActive) android.graphics.Color.TRANSPARENT else android.graphics.Color.parseColor("#1976D2"))
        binding.btnFilter.setTextColor(
            if (!isActive) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
        binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(
            if (!isActive) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
    }

    private fun updateSortButton() {
        binding.btnSort.text = if (currentSortOrder == "Default") "Sort" else "Sort •"
        binding.btnSort.setBackgroundColor(
            if (currentSortOrder == "Default") android.graphics.Color.TRANSPARENT else android.graphics.Color.parseColor("#1976D2"))
        binding.btnSort.setTextColor(
            if (currentSortOrder == "Default") android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
        binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(
            if (currentSortOrder == "Default") android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
    }

    private fun showPickerSheet(title: String, options: List<Pair<String, String>>, onSelect: (String, String) -> Unit) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = title
        val allOptions = options.toMutableList()
        var filteredOptions = allOptions.toMutableList()
        val rvFilterOptions = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rvFilterOptions.layoutManager = LinearLayoutManager(requireContext())
        val filterAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class OptionViewHolder(val b: com.sers.app.databinding.ItemFilterOptionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val b = com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return OptionViewHolder(b)
            }
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val option = filteredOptions[position]
                (holder as OptionViewHolder).b.tvOption.text = option.first
                holder.b.ivCheck.visibility = View.GONE
                holder.b.root.setOnClickListener { onSelect(option.first, option.second); bottomSheet.dismiss() }
            }
            override fun getItemCount() = filteredOptions.size
            fun updateList(newList: MutableList<Pair<String, String>>) { filteredOptions = newList; notifyDataSetChanged() }
        }
        rvFilterOptions.adapter = filterAdapter
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch).addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                filterAdapter.updateList(if (query.isEmpty()) allOptions.toMutableList() else allOptions.filter { it.first.lowercase().contains(query) }.toMutableList())
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