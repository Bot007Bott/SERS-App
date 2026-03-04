package com.sers.app.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.sers.app.R
import com.sers.app.databinding.FragmentTeacherManagementBinding
import com.sers.app.model.Teacher
import com.sers.app.viewmodel.TeacherManagementViewModel

/**
 * TeacherManagementFragment — MVVM View
 * Observes TeacherManagementViewModel and handles UI interactions.
 */
class TeacherManagementFragment : Fragment() {

    private lateinit var binding: FragmentTeacherManagementBinding
    private lateinit var adapter: TeacherAdapter
    private val viewModel: TeacherManagementViewModel by viewModels()

    private var currentSortOrder = "Default"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

        setupObservers()
        setupSearch()
        setupSort()

        binding.btnAddTeacher.setOnClickListener { showTeacherDialog(null) }

        viewModel.loadTeachers()
    }

    private fun setupObservers() {
        viewModel.teachers.observe(viewLifecycleOwner) { teachers ->
            binding.progressBar.visibility = View.GONE
            refreshList()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearch() {
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
                refreshList()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshList()
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
            refreshList()
        }
    }

    private fun setupSort() {
        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Name A-Z", "Name Z-A")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]
                    refreshList()
                    updateSortButtonUI()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun refreshList() {
        val query = binding.etSearch.text.toString().lowercase()
        val allTeachers = viewModel.teachers.value ?: emptyList()

        var list = allTeachers.filter {
            it.firstName.contains(query, ignoreCase = true) ||
                    it.lastName.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true) ||
                    it.department.contains(query, ignoreCase = true)
        }

        list = when (currentSortOrder) {
            "Name A-Z" -> list.sortedBy { it.firstName }
            "Name Z-A" -> list.sortedByDescending { it.firstName }
            else -> list
        }

        adapter.updateList(list.toMutableList())
        updateEmptyState(list)
    }

    private fun updateSortButtonUI() {
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

    private fun showTeacherDialog(teacher: Teacher?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_teacher, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        val isEdit = teacher != null

        dialogView.findViewById<TextView>(R.id.tvTitle).text = if (isEdit) "Edit Teacher" else "Add New Teacher"
        dialogView.findViewById<TextView>(R.id.tvSubtitle).text = if (isEdit) "Update teacher information" else "Fill in the teacher information below"
        dialogView.findViewById<MaterialButton>(R.id.btnSave).text = if (isEdit) "Update Teacher" else "Add Teacher"

        if (isEdit) {
            dialogView.findViewById<TextInputEditText>(R.id.etFirstName).setText(teacher!!.firstName)
            dialogView.findViewById<TextInputEditText>(R.id.etLastName).setText(teacher.lastName)
            dialogView.findViewById<TextInputEditText>(R.id.etEmail).setText(teacher.email)
            dialogView.findViewById<TextInputEditText>(R.id.etPhone).setText(teacher.phone)
            dialogView.findViewById<TextInputEditText>(R.id.etDepartment).setText(teacher.department)
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val firstName = dialogView.findViewById<TextInputEditText>(R.id.etFirstName).text.toString().trim()
            val lastName = dialogView.findViewById<TextInputEditText>(R.id.etLastName).text.toString().trim()
            val email = dialogView.findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val phone = dialogView.findViewById<TextInputEditText>(R.id.etPhone).text.toString().trim()
            val department = dialogView.findViewById<TextInputEditText>(R.id.etDepartment).text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(binding.root, "Please enter a valid email address", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (phone.isNotEmpty() && !phone.matches(Regex("^[+]?[0-9]{7,15}$"))) {
                Snackbar.make(binding.root, "Please enter a valid phone number", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }

            if (isEdit) {
                viewModel.updateTeacher(teacher!!.docId, firstName, lastName, email, phone, department)
            } else {
                val newId = "TCH${String.format("%03d", (viewModel.teachers.value?.size ?: 0) + 1)}"
                val newTeacher = Teacher(teacherId = newId, firstName = firstName, lastName = lastName, email = email, phone = phone, department = department, docId = "")
                viewModel.addTeacher(newTeacher)
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteDialog(teacher: Teacher) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Teacher")
            .setMessage("Are you sure you want to delete ${teacher.firstName} ${teacher.lastName}?")
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Delete") { d, _ ->
                viewModel.deleteTeacher(teacher.docId)
                d.dismiss()
            }.show()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvTeachers.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}
