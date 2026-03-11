package com.sers.app.ui.student

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sers.app.R
import com.sers.app.databinding.FragmentStudentGradesBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.viewmodel.StudentGradesViewModel

/**
 * StudentGradesFragment — MVVM View
 * Observes StudentGradesViewModel for grades and courses data.
 */
class StudentGradesFragment : Fragment() {

    private lateinit var binding: FragmentStudentGradesBinding
    private lateinit var adapter: StudentGradeAdapter
    private val viewModel: StudentGradesViewModel by viewModels()

    private val gradeList = mutableListOf<Grade>()
    private val courseList = mutableListOf<Course>()
    private var currentSortOrder = "Default"

    private var isClosingFromX = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentGradesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudentGradeAdapter(mutableListOf(), mutableListOf())
        binding.rvGrades.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGrades.adapter = adapter

        // Observe ViewModel
        viewModel.grades.observe(viewLifecycleOwner) { grades ->
            gradeList.clear()
            gradeList.addAll(grades)
            refreshList()
        }

        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            courseList.clear()
            courseList.addAll(courses)
            refreshList()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.startsWith("ERROR:")) {
                com.google.android.material.snackbar.Snackbar
                    .make(binding.root, msg.removePrefix("ERROR:"), com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.loadData()

        // Search toggle
        binding.btnSearch.setOnClickListener {
            if (isClosingFromX) { isClosingFromX = false; return@setOnClickListener }
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

        binding.searchLayout.setEndIconOnClickListener {
            isClosingFromX = true
            binding.etSearch.setText("")
            binding.searchLayout.visibility = View.GONE
            binding.btnSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnSearch.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            binding.btnSearch.setIconResource(R.drawable.ic_search)
            binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
            refreshList()
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

    private fun refreshList() {
        val list = getSortedList()
        adapter.updateList(list, courseList)
        updateEmptyState(list)
    }

    private fun getSortedList(): MutableList<Grade> {
        return when (currentSortOrder) {
            "Score High-Low" -> gradeList.sortedByDescending { it.score }.toMutableList()
            "Score Low-High" -> gradeList.sortedBy { it.score }.toMutableList()
            "Course A-Z" -> gradeList.sortedBy { grade -> courseList.find { it.courseId == grade.courseId }?.courseName }.toMutableList()
            else -> gradeList.toMutableList()
        }
    }

    private fun updateSortButton() {
        binding.btnSort.text = if (currentSortOrder == "Default") "Sort" else "Sort •"
        binding.btnSort.setBackgroundColor(if (currentSortOrder == "Default") android.graphics.Color.TRANSPARENT else android.graphics.Color.parseColor("#1976D2"))
        binding.btnSort.setTextColor(if (currentSortOrder == "Default") android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
        binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(if (currentSortOrder == "Default") android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvGrades.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}