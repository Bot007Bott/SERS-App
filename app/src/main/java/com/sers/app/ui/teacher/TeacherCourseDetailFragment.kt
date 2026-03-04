package com.sers.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sers.app.R
import com.sers.app.databinding.FragmentTeacherCourseDetailBinding
import com.sers.app.databinding.ItemStudentBinding
import com.sers.app.model.Student
import com.sers.app.viewmodel.TeacherCourseDetailViewModel

/**
 * TeacherCourseDetailFragment — MVVM View
 * Observes TeacherCourseDetailViewModel for enrollment management.
 */
class TeacherCourseDetailFragment : Fragment() {

    private lateinit var binding: FragmentTeacherCourseDetailBinding
    private val viewModel: TeacherCourseDetailViewModel by viewModels()

    private var courseId = ""
    private lateinit var adapter: EnrolledStudentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherCourseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        courseId = arguments?.getString("courseId") ?: ""
        binding.tvCourseName.text = arguments?.getString("courseName") ?: "Course"

        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.loadData(courseId)
    }

    private fun setupRecyclerView() {
        adapter = EnrolledStudentAdapter(mutableListOf()) { showRemoveDialog(it) }
        binding.rvEnrolledStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEnrolledStudents.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.enrollments.observe(viewLifecycleOwner) { updateList() }
        viewModel.allStudents.observe(viewLifecycleOwner) { updateList() }
        viewModel.message.observe(viewLifecycleOwner) { if (it.isNotEmpty()) Snackbar.make(binding.root, it.removePrefix("ERROR:"), Snackbar.LENGTH_SHORT).show() }
    }

    private fun setupListeners() {
        binding.btnAddStudent.setOnClickListener { showAddStudentDialog() }
    }

    private fun updateList() {
        val enrolls = viewModel.enrollments.value ?: return
        val allS = viewModel.allStudents.value ?: return
        val enrolled = enrolls.mapNotNull { e -> allS.find { it.studentId == e.studentId } }
        adapter.updateList(enrolled)
        binding.tvTotalEnrolled.text = enrolled.size.toString()
    }

    private fun showAddStudentDialog() {
        val allS = viewModel.allStudents.value ?: return
        val enrolls = viewModel.enrollments.value ?: return
        val enrolledIds = enrolls.map { it.studentId }
        val available = allS.filter { it.studentId !in enrolledIds }

        if (available.isEmpty()) { Snackbar.make(binding.root, "No students available!", Snackbar.LENGTH_SHORT).show(); return }

        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_enrolled_student, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(v).create()
        val actv = v.findViewById<AutoCompleteTextView>(R.id.actvStudent)
        val names = available.map { "${it.firstName} ${it.lastName} (${it.studentId})" }
        actv.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))

        v.findViewById<MaterialButton>(R.id.btnAdd).setOnClickListener {
            val idx = names.indexOf(actv.text.toString())
            if (idx != -1) viewModel.addStudentToCourse(available[idx].studentId, courseId)
            dialog.dismiss()
        }
        v.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showRemoveDialog(student: Student) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Remove Student").setMessage("Remove from course?")
            .setPositiveButton("Remove") { _, _ ->
                val docId = viewModel.enrollments.value?.find { it.studentId == student.studentId }?.docId ?: ""
                if (docId.isNotEmpty()) viewModel.removeStudentFromCourse(docId)
            }.setNegativeButton("Cancel", null).show()
    }

    inner class EnrolledStudentAdapter(private var st: MutableList<Student>, val onRemove: (Student) -> Unit) : RecyclerView.Adapter<EnrolledStudentAdapter.VH>() {
        inner class VH(val b: ItemStudentBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, vt: Int) = VH(ItemStudentBinding.inflate(LayoutInflater.from(p.context), p, false))
        override fun onBindViewHolder(h: VH, pos: Int) {
            val s = st[pos]; h.b.tvStudentName.text = "${s.firstName} ${s.lastName}"; h.b.tvStudentId.text = s.studentId
            h.b.tvAvatar.text = s.firstName.take(1).uppercase(); h.b.tvAccountStatus.visibility = View.GONE
            h.b.root.setOnLongClickListener { onRemove(s); true }
        }
        override fun getItemCount() = st.size
        fun updateList(l: List<Student>) { st = l.toMutableList(); notifyDataSetChanged() }
    }
}