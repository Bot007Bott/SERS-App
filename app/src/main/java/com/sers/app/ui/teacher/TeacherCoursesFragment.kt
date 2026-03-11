package com.sers.app.ui.teacher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sers.app.R
import com.sers.app.databinding.FragmentTeacherCoursesBinding
import com.sers.app.model.Course
import com.sers.app.ui.admin.CourseAdapter
import com.sers.app.viewmodel.TeacherCourseViewModel

/**
 * TeacherCoursesFragment — MVVM View
 * Observes TeacherCourseViewModel for courses list.
 */
class TeacherCoursesFragment : Fragment() {

    private lateinit var binding: FragmentTeacherCoursesBinding
    private lateinit var adapter: CourseAdapter
    private val viewModel: TeacherCourseViewModel by viewModels()

    private val fullCourseList = mutableListOf<Course>()
    private var currentSortOrder = "Default"

    private var isClosingFromX = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        viewModel.loadData()
    }

    private fun setupRecyclerView() {
        adapter = CourseAdapter(mutableListOf(), onEditClick = { course ->
            findNavController().navigate(R.id.teacherCourseDetailFragment, bundleOf("courseId" to course.courseId, "courseName" to course.courseName))
        }, onDeleteClick = { })
        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.courses.observe(viewLifecycleOwner) { list ->
            fullCourseList.clear(); fullCourseList.addAll(list)
            updateUI()
        }
        viewModel.myTeacher.observe(viewLifecycleOwner) { teacher ->
            adapter.setTeachers(listOf(teacher))
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
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
            updateUI()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateUI() }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnSort.setOnClickListener { showSortDialog() }
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

    private fun showSortDialog() {
        val options = arrayOf("Default", "Name A-Z", "Name Z-A")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Sort by")
            .setSingleChoiceItems(options, options.indexOf(currentSortOrder)) { dialog, which ->
                currentSortOrder = options[which]; updateUI(); dialog.dismiss()
            }.show()
    }

    private fun updateUI() {
        val query = binding.etSearch.text.toString()
        var list = fullCourseList.filter { it.courseName.contains(query, true) || it.courseCode.contains(query, true) }
        list = when(currentSortOrder) {
            "Name A-Z" -> list.sortedBy { it.courseName }
            "Name Z-A" -> list.sortedByDescending { it.courseName }
            else -> list
        }
        adapter.updateList(list.toMutableList())
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvCourses.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}