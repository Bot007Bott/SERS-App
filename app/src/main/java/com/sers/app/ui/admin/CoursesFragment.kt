package com.sers.app.ui.admin

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentCoursesBinding
import com.sers.app.model.Course
import com.sers.app.model.Teacher
import android.app.TimePickerDialog

class CoursesFragment : Fragment() {

    private lateinit var binding: FragmentCoursesBinding
    private lateinit var adapter: CourseAdapter
    private val db = FirebaseFirestore.getInstance()

    private val courseList = mutableListOf<Course>()
    private val teacherList = mutableListOf<Teacher>()
    private var currentSortOrder = "Default"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        loadTeachersAndCourses()

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
                updateEmptyState(list)
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val filtered = getFilteredSortedList().filter {
                    it.courseName.contains(query, ignoreCase = true) ||
                            it.courseCode.contains(query, ignoreCase = true)
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
            updateEmptyState(list)
        }

        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Name A-Z", "Name Z-A")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]
                    val list = getFilteredSortedList()
                    adapter.updateList(list)
                    updateEmptyState(list)
                    binding.btnSort.text = if (currentSortOrder == "Default") "Sort" else "Sort •"
                    binding.btnSort.setBackgroundColor(
                        if (currentSortOrder == "Default") android.graphics.Color.TRANSPARENT
                        else android.graphics.Color.parseColor("#1976D2"))
                    binding.btnSort.setTextColor(
                        if (currentSortOrder == "Default") android.graphics.Color.parseColor("#1976D2")
                        else android.graphics.Color.WHITE)
                    binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(
                        if (currentSortOrder == "Default") android.graphics.Color.parseColor("#1976D2")
                        else android.graphics.Color.WHITE)
                    dialog.dismiss()
                }
                .show()
        }

        binding.btnAddCourse.setOnClickListener {
            showCourseDialog(null)
        }
    }

    private fun loadTeachersAndCourses() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("teachers").get()
            .addOnSuccessListener { teacherDocs ->
                teacherList.clear()
                teacherDocs.forEach { doc ->
                    teacherList.add(Teacher(
                        teacherId = doc.getString("teacherId") ?: "",
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        email = doc.getString("email") ?: "",
                        department = doc.getString("department") ?: "",
                        phone = doc.getString("phone") ?: "",
                        docId = doc.id
                    ))
                }
                loadCourses()
            }
    }

    private fun loadCourses() {
        db.collection("courses")
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                if (error != null) {
                    Snackbar.make(binding.root, "Error: ${error.message}", Snackbar.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                courseList.clear()
                snapshot?.documents?.forEach { doc ->
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
                val list = getFilteredSortedList()
                adapter.updateList(list)
                adapter.setTeachers(teacherList)
                updateEmptyState(list)
            }
    }

    private fun getFilteredSortedList(): MutableList<Course> {
        var list = courseList.toMutableList()
        list = when (currentSortOrder) {
            "Name A-Z" -> list.sortedBy { it.courseName }.toMutableList()
            "Name Z-A" -> list.sortedByDescending { it.courseName }.toMutableList()
            else -> list
        }
        return list
    }

    private fun showCourseDialog(course: Course?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_course, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val isEdit = course != null

        dialogView.findViewById<TextView>(R.id.tvTitle).text =
            if (isEdit) "Edit Course" else "Add New Course"
        dialogView.findViewById<TextView>(R.id.tvSubtitle).text =
            if (isEdit) "Update course information" else "Fill in the course information below"
        dialogView.findViewById<MaterialButton>(R.id.btnSave).text =
            if (isEdit) "Update Course" else "Add Course"

        // Setup teacher dropdown
        val actvTeacher = dialogView.findViewById<AutoCompleteTextView>(R.id.actvTeacher)
        val teacherNames = teacherList.map { "${it.firstName} ${it.lastName}" }
        actvTeacher.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, teacherNames))

        // Setup day dropdown
        val actvDay = dialogView.findViewById<AutoCompleteTextView>(R.id.actvDay)
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        actvDay.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days))

        // Setup time pickers
        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.etEndTime)
        val etSchedule = dialogView.findViewById<TextInputEditText>(R.id.etSchedule)

        fun buildSchedule() {
            val day = actvDay.text.toString().trim()
            val start = etStartTime.text.toString().trim()
            val end = etEndTime.text.toString().trim()
            if (day.isNotEmpty() && start.isNotEmpty() && end.isNotEmpty()) {
                etSchedule.setText("$day $start - $end")
            }
        }

        etStartTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                etStartTime.setText(String.format("%02d:%02d", hour, minute))
                buildSchedule()
            }, 9, 0, true).show()
        }

        etEndTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                etEndTime.setText(String.format("%02d:%02d", hour, minute))
                buildSchedule()
            }, 11, 0, true).show()
        }

        actvDay.setOnItemClickListener { _, _, _, _ -> buildSchedule() }

        val etCourseName = dialogView.findViewById<TextInputEditText>(R.id.etCourseName)
        val etCourseCode = dialogView.findViewById<TextInputEditText>(R.id.etCourseCode)

        etCourseName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isEdit) {
                    val prefix = s.toString().trim().take(3).uppercase()
                    if (prefix.isNotEmpty()) {
                        val existingCodes = courseList.map { it.courseCode }
                        var num = 101
                        while (existingCodes.contains("$prefix$num")) num++
                        etCourseCode.setText("$prefix$num")
                    }
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        var selectedTeacherId = ""

        if (isEdit) {
            etCourseName.setText(course!!.courseName)
            etCourseCode.setText(course.courseCode)
            val scheduleParts = course.schedule.split(" ")
            if (scheduleParts.size >= 4) {
                actvDay.setText(scheduleParts[0], false)
                etStartTime.setText(scheduleParts[1])
                etEndTime.setText(scheduleParts[3])
            }
            etSchedule.setText(course.schedule)
            val teacher = teacherList.find { it.teacherId == course.teacherId }
            if (teacher != null) {
                actvTeacher.setText("${teacher.firstName} ${teacher.lastName}", false)
                selectedTeacherId = teacher.teacherId
            }
        }

        actvTeacher.setOnItemClickListener { _, _, position, _ ->
            selectedTeacherId = teacherList[position].teacherId
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val courseName = dialogView.findViewById<TextInputEditText>(R.id.etCourseName).text.toString().trim()
            val courseCode = dialogView.findViewById<TextInputEditText>(R.id.etCourseCode).text.toString().trim()
            val schedule = dialogView.findViewById<TextInputEditText>(R.id.etSchedule).text.toString().trim()

            if (courseName.isEmpty() || courseCode.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duplicateName = courseList.any {
                it.courseName.equals(courseName, ignoreCase = true) && it.docId != (course?.docId ?: "")
            }
            val duplicateCode = courseList.any {
                it.courseCode.equals(courseCode, ignoreCase = true) && it.docId != (course?.docId ?: "")
            }
            if (duplicateName) {
                Snackbar.make(binding.root, "Course name \"$courseName\" already exists!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (duplicateCode) {
                Snackbar.make(binding.root, "Course code \"$courseCode\" already exists!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEdit) {
                db.collection("courses").document(course!!.docId)
                    .update(
                        "courseName", courseName,
                        "courseCode", courseCode,
                        "schedule", schedule,
                        "teacherId", selectedTeacherId
                    )
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Course updated!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
            } else {
                val newId = "C${System.currentTimeMillis()}"
                val newCourse = hashMapOf(
                    "courseId" to newId,
                    "courseName" to courseName,
                    "courseCode" to courseCode,
                    "schedule" to schedule,
                    "teacherId" to selectedTeacherId,
                    "createdBy" to "Admin"
                )
                db.collection("courses").add(newCourse)
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Course added!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteDialog(course: Course) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete ${course.courseName}?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Delete") { dialog, _ ->
                db.collection("courses").document(course.docId)
                    .delete()
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Course deleted!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvCourses.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}