package com.sers.app.ui.teacher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentTeacherCoursesBinding
import com.sers.app.model.Course
import com.sers.app.model.Teacher
import com.sers.app.ui.admin.CourseAdapter

class TeacherCoursesFragment : Fragment() {

    private lateinit var binding: FragmentTeacherCoursesBinding
    private lateinit var adapter: CourseAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val courseList = mutableListOf<Course>()
    private var myTeacher: Teacher? = null
    private var currentSortOrder = "Default"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTeacherCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CourseAdapter(
            mutableListOf(),
            onEditClick = { course ->
                findNavController().navigate(
                    R.id.teacherCourseDetailFragment,
                    bundleOf("courseId" to course.courseId, "courseName" to course.courseName)
                )
            },
            onDeleteClick = { }
        )

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = adapter

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                val teacherId = docs.documents[0].getString("teacherId") ?: ""
                if (teacherId.isEmpty()) return@addOnSuccessListener
                db.collection("teachers").whereEqualTo("teacherId", teacherId).get()
                    .addOnSuccessListener { teacherDocs ->
                        val teacher = teacherDocs.documents.firstOrNull()
                        val firstName = teacher?.getString("firstName") ?: ""
                        val lastName = teacher?.getString("lastName") ?: ""
                        myTeacher = Teacher(teacherId = teacherId, firstName = firstName, lastName = lastName)
                        loadCourses(teacherId)
                    }
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
                val list = getSortedList()
                adapter.updateList(list)
                updateEmptyState(list)
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val filtered = getSortedList().filter {
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
            val list = getSortedList()
            adapter.updateList(list)
            updateEmptyState(list)
        }

        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Name A-Z", "Name Z-A")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]
                    val list = getSortedList()
                    adapter.updateList(list)
                    updateEmptyState(list)
                    updateSortButton()
                    dialog.dismiss()
                }.show()
        }
    }

    private fun loadCourses(teacherId: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("courses").whereEqualTo("teacherId", teacherId)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                if (error != null) return@addSnapshotListener
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
                val list = getSortedList()
                adapter.updateList(list)
                myTeacher?.let { adapter.setTeachers(listOf(it)) }
                updateEmptyState(list)
            }
    }

    private fun getSortedList(): MutableList<Course> {
        return when (currentSortOrder) {
            "Name A-Z" -> courseList.sortedBy { it.courseName }.toMutableList()
            "Name Z-A" -> courseList.sortedByDescending { it.courseName }.toMutableList()
            else -> courseList.toMutableList()
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
        binding.rvCourses.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}