package com.sers.app.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sers.app.R
import com.sers.app.databinding.FragmentEnrollmentBinding
import com.sers.app.model.Course
import com.sers.app.model.Enrollment
import com.sers.app.model.Student
import com.sers.app.viewmodel.EnrollmentViewModel

class EnrollmentFragment : Fragment() {

    private lateinit var binding: FragmentEnrollmentBinding
    private val viewModel: EnrollmentViewModel by viewModels()

    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()
    private val enrollmentList = mutableListOf<Enrollment>()

    private var selectedCourseId = ""
    private var currentSortOrder = "Default"
    private lateinit var enrolledAdapter: EnrolledStudentAdapter
    private lateinit var courseAdapter: EnrollmentCourseAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEnrollmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        courseAdapter = EnrollmentCourseAdapter(mutableListOf(), enrollmentList) { course ->
            selectedCourseId = course.courseId
            showEnrolledStudents(course)
        }

        enrolledAdapter = EnrolledStudentAdapter(mutableListOf()) { enrollment ->
            showUnenrollDialog(enrollment)
        }

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = courseAdapter

        binding.rvEnrolledStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEnrolledStudents.adapter = enrolledAdapter

        binding.enrollmentDetail.visibility = View.GONE

        setupObservers()
        setupListeners()
        setupSearch()
        setupSort()

        viewModel.loadAllData()
    }

    private fun setupObservers() {
        viewModel.students.observe(viewLifecycleOwner) { students ->
            studentList.clear(); studentList.addAll(students)
        }
        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            courseList.clear(); courseList.addAll(courses)
            refreshList()
        }
        viewModel.enrollments.observe(viewLifecycleOwner) { enrollments ->
            binding.progressBar.visibility = View.GONE
            enrollmentList.clear(); enrollmentList.addAll(enrollments)
            courseAdapter.notifyDataSetChanged()
            if (selectedCourseId.isNotEmpty()) {
                val course = courseList.find { it.courseId == selectedCourseId }
                if (course != null) showEnrolledStudents(course)
            }
        }
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.enrollmentDetail.visibility == View.VISIBLE) {
                    binding.enrollmentDetail.visibility = View.GONE
                    binding.courseListView.visibility = View.VISIBLE
                    selectedCourseId = ""
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        binding.btnBack.setOnClickListener {
            binding.enrollmentDetail.visibility = View.GONE
            binding.courseListView.visibility = View.VISIBLE
            selectedCourseId = ""
        }
        binding.btnEnrollStudent.setOnClickListener {
            if (selectedCourseId.isEmpty()) Snackbar.make(binding.root, "Select a course first!", Snackbar.LENGTH_SHORT).show()
            else showEnrollDialog()
        }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            if (binding.searchLayout.visibility == View.GONE) {
                binding.searchLayout.visibility = View.VISIBLE
            } else {
                binding.etSearch.setText("")
                binding.searchLayout.visibility = View.GONE
                refreshList()
            }
        }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshList() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSort() {
        binding.btnSort.setOnClickListener {
            val options = arrayOf("Default", "Name A-Z", "Name Z-A")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(options, options.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = options[which]
                    refreshList()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun refreshList() {
        val query = binding.etSearch.text.toString().lowercase()
        var list = courseList.filter {
            it.courseName.contains(query, ignoreCase = true) ||
                    it.courseCode.contains(query, ignoreCase = true)
        }
        list = when (currentSortOrder) {
            "Name A-Z" -> list.sortedBy { it.courseName }
            "Name Z-A" -> list.sortedByDescending { it.courseName }
            else -> list
        }
        courseAdapter.updateList(list.toMutableList())
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvCourses.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showEnrolledStudents(course: Course) {
        binding.courseListView.visibility = View.GONE
        binding.enrollmentDetail.visibility = View.VISIBLE
        binding.tvSelectedCourse.text = "${course.courseName} (${course.courseCode})"
        val enrolled = enrollmentList.filter { it.courseId == course.courseId }
        binding.tvEnrolledCount.text = "${enrolled.size} student${if (enrolled.size != 1) "s" else ""} enrolled"
        enrolledAdapter.updateList(enrolled.toMutableList(), studentList)
    }

    private fun showEnrollDialog() {
        val enrolledIds = enrollmentList.filter { it.courseId == selectedCourseId }.map { it.studentId }
        val available = studentList.filter { it.studentId !in enrolledIds }
        if (available.isEmpty()) {
            Snackbar.make(binding.root, "All students are already enrolled!", Snackbar.LENGTH_SHORT).show()
            return
        }
        val options = available.map { "${it.firstName} ${it.lastName} (${it.studentId})" to it.studentId }
        showPickerSheet("Enroll Student", options) { _, id ->
            viewModel.enrollStudent(id, selectedCourseId)
        }
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

    private fun showUnenrollDialog(enrollment: Enrollment) {
        val student = studentList.find { it.studentId == enrollment.studentId }
        val name = if (student != null) "${student.firstName} ${student.lastName}" else enrollment.studentId
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Unenroll Student")
            .setMessage("Remove $name from this course?")
            .setPositiveButton("Unenroll") { _, _ -> viewModel.unenrollStudent(enrollment.docId) }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }
}