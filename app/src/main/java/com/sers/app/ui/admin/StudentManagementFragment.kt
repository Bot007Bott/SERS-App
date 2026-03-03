package com.sers.app.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentStudentManagementBinding
import com.sers.app.model.Student

class StudentManagementFragment : Fragment() {

    private lateinit var binding: FragmentStudentManagementBinding
    private lateinit var adapter: StudentAdapter
    private val db = FirebaseFirestore.getInstance()

    private val studentList = mutableListOf<Student>()
    private var currentSortOrder = "Default"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStudentManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudentAdapter(
            mutableListOf(),
            onEditClick = { student -> showStudentDialog(student) },
            onDeleteClick = { student -> showDeleteDialog(student) }
        )

        binding.rvStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudents.adapter = adapter

        loadStudents()

        binding.btnSearch.setOnClickListener {
            if (binding.searchLayout.visibility == View.GONE) {
                binding.searchLayout.visibility = View.VISIBLE
                binding.btnSearch.setBackgroundColor(android.graphics.Color.parseColor("#1976D2"))
                binding.btnSearch.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                binding.btnSearch.setIconResource(R.drawable.ic_close)
                binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFFFFF"))
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
                    it.firstName.contains(query, ignoreCase = true) ||
                            it.lastName.contains(query, ignoreCase = true) ||
                            it.email.contains(query, ignoreCase = true)
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

        binding.btnAddStudent.setOnClickListener { showStudentDialog(null) }
    }

    private fun loadStudents() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("users").get().addOnSuccessListener { userDocs ->
            // Map studentId -> userId for cross-referencing
            val studentUserMap = mutableMapOf<String, String>()
            userDocs.forEach { userDoc ->
                val sid = userDoc.getString("studentId") ?: ""
                val uid = userDoc.id
                if (sid.isNotEmpty()) studentUserMap[sid] = uid
            }
            db.collection("students")
                .addSnapshotListener { snapshot, error ->
                    binding.progressBar.visibility = View.GONE
                    if (error != null) {
                        Snackbar.make(binding.root, "Error loading students: ${error.message}", Snackbar.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    studentList.clear()
                    snapshot?.documents?.forEach { doc ->
                        val sid = doc.getString("studentId") ?: ""
                        val student = Student(
                            studentId = sid,
                            firstName = doc.getString("firstName") ?: "",
                            lastName = doc.getString("lastName") ?: "",
                            email = doc.getString("email") ?: "",
                            program = doc.getString("program") ?: "",
                            phone = doc.getString("phone") ?: "",
                            userId = studentUserMap[sid] ?: "",
                            docId = doc.id
                        )
                        studentList.add(student)
                    }
                    val list = getFilteredSortedList()
                    adapter.updateList(list)
                    updateEmptyState(list)
                }
        }
    }

    private fun getFilteredSortedList(): MutableList<Student> {
        var list = studentList.toMutableList()
        list = when (currentSortOrder) {
            "Name A-Z" -> list.sortedBy { it.firstName }.toMutableList()
            "Name Z-A" -> list.sortedByDescending { it.firstName }.toMutableList()
            else -> list
        }
        return list
    }

    private fun showStudentDialog(student: Student?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_student, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val isEdit = student != null

        dialogView.findViewById<TextView>(R.id.tvTitle).text =
            if (isEdit) "Edit Student" else "Add New Student"
        dialogView.findViewById<TextView>(R.id.tvSubtitle).text =
            if (isEdit) "Update student information" else "Fill in the student information below"
        dialogView.findViewById<MaterialButton>(R.id.btnSave).text =
            if (isEdit) "Update Student" else "Add Student"

        if (isEdit) {
            dialogView.findViewById<TextInputEditText>(R.id.etFirstName).setText(student!!.firstName)
            dialogView.findViewById<TextInputEditText>(R.id.etLastName).setText(student.lastName)
            dialogView.findViewById<TextInputEditText>(R.id.etEmail).setText(student.email)
            dialogView.findViewById<TextInputEditText>(R.id.etPhone).setText(student.phone)
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val firstName = dialogView.findViewById<TextInputEditText>(R.id.etFirstName).text.toString().trim()
            val lastName = dialogView.findViewById<TextInputEditText>(R.id.etLastName).text.toString().trim()
            val email = dialogView.findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val phone = dialogView.findViewById<TextInputEditText>(R.id.etPhone).text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEdit) {
                // Update in Firestore
                val docId = student!!.docId
                if (docId.isNotEmpty()) {
                    db.collection("students").document(docId)
                        .update(
                            "firstName", firstName,
                            "lastName", lastName,
                            "email", email,
                            "phone", phone
                        )
                        .addOnSuccessListener {
                            Snackbar.make(binding.root, "Student updated!", Snackbar.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                        }
                }
            } else {
                // Generate new student ID
                val newId = "STD${String.format("%03d", studentList.size + 1)}"
                val newStudent = hashMapOf(
                    "studentId" to newId,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "phone" to phone,
                    "program" to ""
                )
                db.collection("students").add(newStudent)
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Student added!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteDialog(student: Student) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete ${student.firstName} ${student.lastName}?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Delete") { dialog, _ ->
                if (student.docId.isNotEmpty()) {
                    db.collection("students").document(student.docId)
                        .delete()
                        .addOnSuccessListener {
                            Snackbar.make(binding.root, "Student deleted!", Snackbar.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvStudents.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}