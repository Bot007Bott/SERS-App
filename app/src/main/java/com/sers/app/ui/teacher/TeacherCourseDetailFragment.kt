package com.sers.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentTeacherCourseDetailBinding
import com.sers.app.databinding.ItemStudentBinding
import com.sers.app.model.Enrollment
import com.sers.app.model.Student

class TeacherCourseDetailFragment : Fragment() {

    private lateinit var binding: FragmentTeacherCourseDetailBinding
    private val db = FirebaseFirestore.getInstance()

    private val allStudents = mutableListOf<Student>()
    private val enrolledStudents = mutableListOf<Student>()
    private val enrollmentList = mutableListOf<Enrollment>()

    private var courseId = ""
    private lateinit var adapter: EnrolledStudentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherCourseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        courseId = arguments?.getString("courseId") ?: ""
        val courseName = arguments?.getString("courseName") ?: "Course"
        binding.tvCourseName.text = courseName

        adapter = EnrolledStudentAdapter(mutableListOf()) { student -> showRemoveDialog(student) }
        binding.rvEnrolledStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEnrolledStudents.adapter = adapter

        loadAllData()

        binding.btnAddStudent.setOnClickListener { showAddStudentDialog() }
    }

    private fun loadAllData() {
        db.collection("students").get().addOnSuccessListener { studentDocs ->
            allStudents.clear()
            studentDocs.forEach { doc ->
                allStudents.add(Student(
                    studentId = doc.getString("studentId") ?: "",
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    program = doc.getString("program") ?: "",
                    phone = doc.getString("phone") ?: "",
                    docId = doc.id
                ))
            }
            loadEnrollments()
        }
    }

    private fun loadEnrollments() {
        db.collection("enrollments").whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Snackbar.make(binding.root, "Error: ${error.message}", Snackbar.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                enrollmentList.clear()
                snapshot?.documents?.forEach { doc ->
                    enrollmentList.add(Enrollment(
                        enrollmentId = doc.getString("enrollmentId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        docId = doc.id
                    ))
                }
                enrolledStudents.clear()
                enrollmentList.forEach { enrollment ->
                    val student = allStudents.find { it.studentId == enrollment.studentId }
                    if (student != null) enrolledStudents.add(student)
                }
                adapter.updateList(enrolledStudents)
                updateCount()
            }
    }

    private fun updateCount() {
        binding.tvTotalEnrolled.text = enrolledStudents.size.toString()
    }

    private fun showAddStudentDialog() {
        val enrolledIds = enrollmentList.map { it.studentId }
        val available = allStudents.filter { it.studentId !in enrolledIds }

        if (available.isEmpty()) {
            Snackbar.make(binding.root, "All students are already enrolled!", Snackbar.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_enrolled_student, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()

        val studentNames = available.map { "${it.firstName} ${it.lastName} (${it.studentId})" }
        val actvStudent = dialogView.findViewById<AutoCompleteTextView>(R.id.actvStudent)
        actvStudent.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, studentNames))

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.btnAdd).setOnClickListener {
            val selectedText = actvStudent.text.toString()
            if (selectedText.isEmpty()) {
                Snackbar.make(binding.root, "Please select a student", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val index = studentNames.indexOf(selectedText)
            if (index != -1) {
                val student = available[index]
                db.collection("enrollments").add(hashMapOf(
                    "enrollmentId" to "E${System.currentTimeMillis()}",
                    "studentId" to student.studentId,
                    "courseId" to courseId
                ))
                    .addOnSuccessListener { Snackbar.make(binding.root, "Student added to course!", Snackbar.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showRemoveDialog(student: Student) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Student")
            .setMessage("Remove ${student.firstName} ${student.lastName} from this course?")
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Remove") { d, _ ->
                val enrollment = enrollmentList.find { it.studentId == student.studentId }
                if (enrollment != null) {
                    db.collection("enrollments").document(enrollment.docId).delete()
                        .addOnSuccessListener { Snackbar.make(binding.root, "Student removed!", Snackbar.LENGTH_SHORT).show() }
                        .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
                }
                d.dismiss()
            }.show()
    }

    inner class EnrolledStudentAdapter(
        private var students: MutableList<Student>,
        private val onRemoveClick: (Student) -> Unit
    ) : RecyclerView.Adapter<EnrolledStudentAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemStudentBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val student = students[position]
            holder.binding.tvStudentName.text = "${student.firstName} ${student.lastName}"
            holder.binding.tvStudentId.text = student.studentId
            holder.binding.tvAvatar.text = student.firstName.firstOrNull()?.uppercase() ?: "S"
            holder.binding.tvAccountStatus.visibility = View.GONE
            holder.binding.root.setOnLongClickListener { onRemoveClick(student); true }
        }

        override fun getItemCount() = students.size

        fun updateList(newList: List<Student>) {
            students = newList.toMutableList()
            notifyDataSetChanged()
        }
    }
}