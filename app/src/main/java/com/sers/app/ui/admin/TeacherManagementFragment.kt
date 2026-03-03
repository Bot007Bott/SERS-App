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
import com.sers.app.databinding.FragmentTeacherManagementBinding
import com.sers.app.model.Teacher

class TeacherManagementFragment : Fragment() {

    private lateinit var binding: FragmentTeacherManagementBinding
    private lateinit var adapter: TeacherAdapter
    private val db = FirebaseFirestore.getInstance()

    private val teacherList = mutableListOf<Teacher>()
    private var currentSortOrder = "Default"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTeacherManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TeacherAdapter(
            mutableListOf(),
            onEditClick = { teacher -> showTeacherDialog(teacher) },
            onDeleteClick = { teacher -> showDeleteDialog(teacher) }
        )

        binding.rvTeachers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTeachers.adapter = adapter

        loadTeachers()

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
                            it.email.contains(query, ignoreCase = true) ||
                            it.department.contains(query, ignoreCase = true)
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

        binding.btnAddTeacher.setOnClickListener { showTeacherDialog(null) }
    }

    private fun loadTeachers() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("users").get().addOnSuccessListener { userDocs ->
            // Map teacherId -> userId for cross-referencing
            val teacherUserMap = mutableMapOf<String, String>()
            userDocs.forEach { userDoc ->
                val tid = userDoc.getString("teacherId") ?: ""
                val uid = userDoc.id
                if (tid.isNotEmpty()) teacherUserMap[tid] = uid
            }
            db.collection("teachers")
                .addSnapshotListener { snapshot, error ->
                    binding.progressBar.visibility = View.GONE
                    if (error != null) {
                        Snackbar.make(binding.root, "Error: ${error.message}", Snackbar.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    teacherList.clear()
                    snapshot?.documents?.forEach { doc ->
                        val tid = doc.getString("teacherId") ?: ""
                        teacherList.add(Teacher(
                            teacherId = tid,
                            firstName = doc.getString("firstName") ?: "",
                            lastName = doc.getString("lastName") ?: "",
                            email = doc.getString("email") ?: "",
                            department = doc.getString("department") ?: "",
                            phone = doc.getString("phone") ?: "",
                            userId = teacherUserMap[tid] ?: "",
                            docId = doc.id
                        ))
                    }
                    val list = getFilteredSortedList()
                    adapter.updateList(list)
                    updateEmptyState(list)
                }
        }
    }

    private fun getFilteredSortedList(): MutableList<Teacher> {
        var list = teacherList.toMutableList()
        list = when (currentSortOrder) {
            "Name A-Z" -> list.sortedBy { it.firstName }.toMutableList()
            "Name Z-A" -> list.sortedByDescending { it.firstName }.toMutableList()
            else -> list
        }
        return list
    }

    private fun showTeacherDialog(teacher: Teacher?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_teacher, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val isEdit = teacher != null

        dialogView.findViewById<TextView>(R.id.tvTitle).text =
            if (isEdit) "Edit Teacher" else "Add New Teacher"
        dialogView.findViewById<TextView>(R.id.tvSubtitle).text =
            if (isEdit) "Update teacher information" else "Fill in the teacher information below"
        dialogView.findViewById<MaterialButton>(R.id.btnSave).text =
            if (isEdit) "Update Teacher" else "Add Teacher"

        if (isEdit) {
            dialogView.findViewById<TextInputEditText>(R.id.etFirstName).setText(teacher!!.firstName)
            dialogView.findViewById<TextInputEditText>(R.id.etLastName).setText(teacher.lastName)
            dialogView.findViewById<TextInputEditText>(R.id.etEmail).setText(teacher.email)
            dialogView.findViewById<TextInputEditText>(R.id.etPhone).setText(teacher.phone)
            dialogView.findViewById<TextInputEditText>(R.id.etDepartment).setText(teacher.department)
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val firstName = dialogView.findViewById<TextInputEditText>(R.id.etFirstName).text.toString().trim()
            val lastName = dialogView.findViewById<TextInputEditText>(R.id.etLastName).text.toString().trim()
            val email = dialogView.findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val phone = dialogView.findViewById<TextInputEditText>(R.id.etPhone).text.toString().trim()
            val department = dialogView.findViewById<TextInputEditText>(R.id.etDepartment).text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEdit) {
                db.collection("teachers").document(teacher!!.docId)
                    .update(
                        "firstName", firstName,
                        "lastName", lastName,
                        "email", email,
                        "phone", phone,
                        "department", department
                    )
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Teacher updated!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
            } else {
                val newId = "TCH${String.format("%03d", teacherList.size + 1)}"
                val newTeacher = hashMapOf(
                    "teacherId" to newId,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "phone" to phone,
                    "department" to department
                )
                db.collection("teachers").add(newTeacher)
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Teacher added!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteDialog(teacher: Teacher) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Teacher")
            .setMessage("Are you sure you want to delete ${teacher.firstName} ${teacher.lastName}?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Delete") { dialog, _ ->
                db.collection("teachers").document(teacher.docId)
                    .delete()
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Teacher deleted!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvTeachers.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}