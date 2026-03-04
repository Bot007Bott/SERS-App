package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sers.app.R
import com.sers.app.databinding.FragmentEnrollmentBinding
import com.sers.app.model.Course
import com.sers.app.model.Enrollment
import com.sers.app.model.Student
import com.sers.app.viewmodel.EnrollmentViewModel

/**
 * EnrollmentFragment — MVVM View
 * Observes EnrollmentViewModel for course, student, and enrollment data.
 */
class EnrollmentFragment : Fragment() {

    private lateinit var binding: FragmentEnrollmentBinding
    private val viewModel: EnrollmentViewModel by viewModels()

    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()
    private val enrollmentList = mutableListOf<Enrollment>()

    private var selectedCourseId = ""
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

        viewModel.loadAllData()
    }

    private fun setupObservers() {
        viewModel.students.observe(viewLifecycleOwner) { students ->
            studentList.clear(); studentList.addAll(students)
        }
        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            courseList.clear(); courseList.addAll(courses)
            courseAdapter.updateList(courses.toMutableList())
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
                    binding.enrollmentDetail.visibility = View.GONE; binding.courseListView.visibility = View.VISIBLE; selectedCourseId = ""
                } else { isEnabled = false; requireActivity().onBackPressedDispatcher.onBackPressed() }
            }
        })
        binding.btnBack.setOnClickListener { binding.enrollmentDetail.visibility = View.GONE; binding.courseListView.visibility = View.VISIBLE; selectedCourseId = "" }
        binding.btnEnrollStudent.setOnClickListener { if (selectedCourseId.isEmpty()) Snackbar.make(binding.root, "Select a course first!", Snackbar.LENGTH_SHORT).show() else showEnrollDialog() }
    }

    private fun showEnrolledStudents(course: Course) {
        binding.courseListView.visibility = View.GONE; binding.enrollmentDetail.visibility = View.VISIBLE
        binding.tvSelectedCourse.text = "${course.courseName} (${course.courseCode})"
        val enrolled = enrollmentList.filter { it.courseId == course.courseId }
        binding.tvEnrolledCount.text = "${enrolled.size} students enrolled"
        enrolledAdapter.updateList(enrolled.toMutableList(), studentList)
    }

    private fun showEnrollDialog() {
        val enrolledIds = enrollmentList.filter { it.courseId == selectedCourseId }.map { it.studentId }
        val available = studentList.filter { it.studentId !in enrolledIds }
        if (available.isEmpty()) { Snackbar.make(binding.root, "All students enrolled!", Snackbar.LENGTH_SHORT).show(); return }
        val options = available.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }
        showPickerSheet("Enroll Student", options) { _, id -> viewModel.enrollStudent(id, selectedCourseId) }
    }

    private fun showUnenrollDialog(enrollment: Enrollment) {
        val student = studentList.find { it.studentId == enrollment.studentId }
        val name = if (student != null) "${student.firstName} ${student.lastName}" else enrollment.studentId
        MaterialAlertDialogBuilder(requireContext()).setTitle("Unenroll Student").setMessage("Remove $name?").setPositiveButton("Unenroll") { _, _ -> viewModel.unenrollStudent(enrollment.docId) }.setNegativeButton("Cancel") { d, _ -> d.dismiss() }.show()
    }

    private fun showPickerSheet(title: String, options: List<Pair<String, String>>, onSelect: (String, String) -> Unit) {
        val bottomSheet = BottomSheetDialog(requireContext()); val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView); sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = title
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions); rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(p.context), p, false).root) {}
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) { (h.itemView as TextView).text = options[pos].first; h.itemView.setOnClickListener { onSelect(options[pos].first, options[pos].second); bottomSheet.dismiss() } }
            override fun getItemCount() = options.size
        }
        rv.adapter = adapter; bottomSheet.show()
    }
}