package com.sers.app.ui.student

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentStudentGradesBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade

class StudentGradesFragment : Fragment() {

    private lateinit var binding: FragmentStudentGradesBinding
    private lateinit var adapter: StudentGradeAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val gradeList = mutableListOf<Grade>()
    private val courseList = mutableListOf<Course>()
    private var currentSortOrder = "Default"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentGradesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudentGradeAdapter(mutableListOf(), mutableListOf())
        binding.rvGrades.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGrades.adapter = adapter

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) return@addOnSuccessListener
                val studentId = userDocs.documents[0].getString("studentId") ?: return@addOnSuccessListener
                loadData(studentId)
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
                refreshList()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val filtered = getSortedList().filter { grade ->
                    val course = courseList.find { it.courseId == grade.courseId }
                    course?.courseName?.contains(query, ignoreCase = true) == true
                }.toMutableList()
                adapter.updateList(filtered, courseList)
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
            refreshList()
        }

        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Score High-Low", "Score Low-High", "Course A-Z")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]
                    refreshList()
                    updateSortButton()
                    dialog.dismiss()
                }.show()
        }
    }

    private fun loadData(studentId: String) {
        binding.progressBar.visibility = View.VISIBLE
        // First load grades, then load only the courses needed
        db.collection("grades").whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                if (error != null) return@addSnapshotListener
                gradeList.clear()
                val courseIds = mutableSetOf<String>()
                snapshot?.documents?.forEach { doc ->
                    val courseId = doc.getString("courseId") ?: ""
                    courseIds.add(courseId)
                    gradeList.add(Grade(
                        gradeId = doc.getString("gradeId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        courseId = courseId,
                        gradeType = doc.getString("gradeType") ?: "",
                        title = doc.getString("title") ?: "",
                        score = (doc.getLong("score") ?: 0).toInt(),
                        totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(),
                        docId = doc.id
                    ))
                }
                // Load only the courses this student has grades for
                if (courseIds.isNotEmpty()) {
                    db.collection("courses").whereIn("courseId", courseIds.toList()).get()
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
                            refreshList()
                        }
                } else {
                    refreshList()
                }
            }
    }

    private fun refreshList() {
        val list = getSortedList()
        adapter.updateList(list, courseList)
        updateEmptyState(list)
    }

    private fun getSortedList(): MutableList<Grade> {
        return when (currentSortOrder) {
            "Score High-Low" -> gradeList.sortedByDescending { it.score }.toMutableList()
            "Score Low-High" -> gradeList.sortedBy { it.score }.toMutableList()
            "Course A-Z" -> gradeList.sortedBy { grade ->
                courseList.find { it.courseId == grade.courseId }?.courseName
            }.toMutableList()
            else -> gradeList.toMutableList()
        }
    }

    private fun updateSortButton() {
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
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvGrades.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}