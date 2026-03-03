package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentEnrollmentBinding
import com.sers.app.model.Course
import com.sers.app.model.Enrollment
import com.sers.app.model.Student

class EnrollmentFragment : Fragment() {

    private lateinit var binding: FragmentEnrollmentBinding
    private val db = FirebaseFirestore.getInstance()

    private val enrollmentList = mutableListOf<Enrollment>()
    private val studentList = mutableListOf<Student>()
    private val courseList = mutableListOf<Course>()

    private var selectedCourseId = ""
    private var selectedCourseDocId = ""
    private lateinit var enrolledAdapter: EnrolledStudentAdapter
    private lateinit var courseAdapter: EnrollmentCourseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEnrollmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        courseAdapter = EnrollmentCourseAdapter(mutableListOf(), enrollmentList) { course ->
            selectedCourseId = course.courseId
            selectedCourseDocId = course.docId
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

        loadAllData()

        // Handle Android back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
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
            }
        )

        binding.btnBack.setOnClickListener {
            binding.enrollmentDetail.visibility = View.GONE
            binding.courseListView.visibility = View.VISIBLE
            selectedCourseId = ""
        }

        binding.btnEnrollStudent.setOnClickListener {
            if (selectedCourseId.isEmpty()) {
                Snackbar.make(binding.root, "Please select a course first!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showEnrollDialog()
        }
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
            db.collection("courses").get().addOnSuccessListener { courseDocs ->
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
                courseAdapter.updateList(courseList.toMutableList())
                loadEnrollments()
            }
        }
    }

    private fun loadEnrollments() {
        db.collection("enrollments")
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
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
                courseAdapter.notifyDataSetChanged()
                if (selectedCourseId.isNotEmpty()) {
                    val course = courseList.find { it.courseId == selectedCourseId }
                    if (course != null) showEnrolledStudents(course)
                }
            }
    }

    private fun showEnrolledStudents(course: Course) {
        binding.courseListView.visibility = View.GONE
        binding.enrollmentDetail.visibility = View.VISIBLE
        binding.tvSelectedCourse.text = "${course.courseName} (${course.courseCode})"

        val enrolledStudentIds = enrollmentList
            .filter { it.courseId == course.courseId }
            .map { it.studentId }

        val enrolledEnrollments = enrollmentList.filter { it.courseId == course.courseId }
        binding.tvEnrolledCount.text = "${enrolledStudentIds.size} students enrolled"

        val displayList = enrolledEnrollments.map { enrollment ->
            val student = studentList.find { it.studentId == enrollment.studentId }
            Pair(student, enrollment)
        }.filter { it.first != null }

        enrolledAdapter.updateList(displayList.map { (student, enrollment) ->
            Enrollment(
                enrollmentId = enrollment.enrollmentId,
                studentId = student!!.studentId,
                courseId = enrollment.courseId,
                docId = enrollment.docId
            )
        }.toMutableList(), studentList)
    }

    private fun showEnrollDialog() {
        val enrolledIds = enrollmentList
            .filter { it.courseId == selectedCourseId }
            .map { it.studentId }

        val availableStudents = studentList.filter { it.studentId !in enrolledIds }

        if (availableStudents.isEmpty()) {
            Snackbar.make(binding.root, "All students are already enrolled!", Snackbar.LENGTH_SHORT).show()
            return
        }

        val options = availableStudents.map { Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId) }
        showPickerSheet("Enroll Student", options) { _, studentId ->
            db.collection("enrollments").add(hashMapOf(
                "enrollmentId" to "E${System.currentTimeMillis()}",
                "studentId" to studentId,
                "courseId" to selectedCourseId
            ))
                .addOnSuccessListener { Snackbar.make(binding.root, "Student enrolled!", Snackbar.LENGTH_SHORT).show() }
                .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun showUnenrollDialog(enrollment: Enrollment) {
        val student = studentList.find { it.studentId == enrollment.studentId }
        val studentName = if (student != null) "${student.firstName} ${student.lastName}" else enrollment.studentId

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Unenroll Student")
            .setMessage("Remove $studentName from this course?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Unenroll") { dialog, _ ->
                db.collection("enrollments").document(enrollment.docId).delete()
                    .addOnSuccessListener { Snackbar.make(binding.root, "Student unenrolled!", Snackbar.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
                dialog.dismiss()
            }.show()
    }

    private fun showPickerSheet(
        title: String,
        options: List<Pair<String, String>>,
        onSelect: (String, String) -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<TextView>(R.id.tvFilterTitle).text = title

        val allOptions = options.toMutableList()
        var filteredOptions = allOptions.toMutableList()

        val rvFilterOptions = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rvFilterOptions.layoutManager = LinearLayoutManager(requireContext())

        val filterAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class OptionViewHolder(val b: com.sers.app.databinding.ItemFilterOptionBinding) :
                androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
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
        sheetView.findViewById<TextInputEditText>(R.id.etFilterSearch)
            .addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s.toString().lowercase()
                    filterAdapter.updateList(
                        if (query.isEmpty()) allOptions.toMutableList()
                        else allOptions.filter { it.first.lowercase().contains(query) }.toMutableList()
                    )
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        bottomSheet.show()
    }
}