package com.sers.app.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.FragmentUserManagementBinding
import com.sers.app.model.Student
import com.sers.app.model.Teacher
import com.sers.app.model.User

class UserManagementFragment : Fragment() {

    private lateinit var binding: FragmentUserManagementBinding
    private lateinit var adapter: UserAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userList = mutableListOf<User>()
    private val studentList = mutableListOf<Student>()
    private val teacherList = mutableListOf<Teacher>()
    private var currentFilterRole = "All"
    private var currentSortOrder = "Default"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserAdapter(
            mutableListOf(),
            onEditClick = { user -> showEditUserDialog(user) },
            onDeleteClick = { user -> showDeleteUserDialog(user) }
        )

        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        loadAllData()

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
                            it.role.contains(query, ignoreCase = true)
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

        binding.btnFilter.setOnClickListener {
            val roles = arrayOf("All", "Admin", "Teacher", "Student")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter by Role")
                .setSingleChoiceItems(roles, roles.indexOf(currentFilterRole)) { dialog, which ->
                    currentFilterRole = roles[which]
                    val list = getFilteredSortedList()
                    adapter.updateList(list)
                    updateEmptyState(list)
                    binding.btnFilter.text = if (currentFilterRole == "All") "Filter" else "Filter •"
                    binding.btnFilter.setBackgroundColor(
                        if (currentFilterRole == "All") android.graphics.Color.TRANSPARENT
                        else android.graphics.Color.parseColor("#1976D2"))
                    binding.btnFilter.setTextColor(
                        if (currentFilterRole == "All") android.graphics.Color.parseColor("#1976D2")
                        else android.graphics.Color.WHITE)
                    binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(
                        if (currentFilterRole == "All") android.graphics.Color.parseColor("#1976D2")
                        else android.graphics.Color.WHITE)
                    dialog.dismiss()
                }
                .show()
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

        binding.btnCreateUser.setOnClickListener { showCreateUserDialog() }
    }

    private fun loadAllData() {
        binding.progressBar.visibility = View.VISIBLE
        // Load students and teachers first, then users
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
            db.collection("teachers").get().addOnSuccessListener { teacherDocs ->
                teacherList.clear()
                teacherDocs.forEach { doc ->
                    teacherList.add(Teacher(
                        teacherId = doc.getString("teacherId") ?: "",
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        email = doc.getString("email") ?: "",
                        department = doc.getString("department") ?: "",
                        phone = doc.getString("phone") ?: "",
                        docId = doc.id
                    ))
                }
                loadUsers()
            }
        }
    }

    private fun loadUsers() {
        db.collection("users")
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                if (error != null) {
                    Snackbar.make(binding.root, "Error: ${error.message}", Snackbar.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                userList.clear()
                snapshot?.documents?.forEach { doc ->
                    userList.add(User(
                        userId = doc.id,
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        username = doc.getString("username") ?: "",
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        teacherId = doc.getString("teacherId") ?: "",
                        uid = doc.getString("uid") ?: "",
                        docId = doc.id
                    ))
                }
                val list = getFilteredSortedList()
                adapter.updateList(list)
                updateEmptyState(list)
            }
    }

    private fun getFilteredSortedList(): MutableList<User> {
        var list = userList.toMutableList()
        if (currentFilterRole != "All") {
            list = list.filter { it.role == currentFilterRole }.toMutableList()
        }
        list = when (currentSortOrder) {
            "Name A-Z" -> list.sortedBy { it.firstName }.toMutableList()
            "Name Z-A" -> list.sortedByDescending { it.firstName }.toMutableList()
            else -> list
        }
        return list
    }

    private fun showProfilePickerSheet(
        title: String,
        options: List<Pair<String, String>>,
        onSelect: (String, String) -> Unit
    ) {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<android.widget.TextView>(R.id.tvFilterTitle).text = title

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
                holder.b.root.setOnClickListener {
                    onSelect(option.first, option.second)
                    bottomSheet.dismiss()
                }
            }
            override fun getItemCount() = filteredOptions.size
            fun updateList(newList: MutableList<Pair<String, String>>) {
                filteredOptions = newList
                notifyDataSetChanged()
            }
        }

        rvFilterOptions.adapter = filterAdapter
        sheetView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFilterSearch)
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

    private fun showCreateUserDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_user, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val actvRole = dialogView.findViewById<AutoCompleteTextView>(R.id.actvRole)
        actvRole.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, listOf("Teacher", "Student")))

        val btnLinkProfile = dialogView.findViewById<MaterialButton>(R.id.btnLinkProfile)
        var selectedProfileId = ""

        actvRole.setOnItemClickListener { _, _, _, _ ->
            when (actvRole.text.toString()) {
                "Student" -> {
                    btnLinkProfile.visibility = View.VISIBLE
                    btnLinkProfile.text = "Link Student Profile (Optional)"
                    selectedProfileId = ""
                }
                "Teacher" -> {
                    btnLinkProfile.visibility = View.VISIBLE
                    btnLinkProfile.text = "Link Teacher Profile (Optional)"
                    selectedProfileId = ""
                }
                else -> {
                    btnLinkProfile.visibility = View.GONE
                    selectedProfileId = ""
                }
            }
        }

        btnLinkProfile.setOnClickListener {
            when (actvRole.text.toString()) {
                "Student" -> showProfilePickerSheet(
                    "Select Student Profile",
                    listOf(Pair("Unlink Profile", "")) + studentList.map {
                        Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId)
                    }
                ) { name, id ->
                    selectedProfileId = id
                    btnLinkProfile.text = if (id.isEmpty()) "Link Student Profile (Optional)" else name
                }
                "Teacher" -> showProfilePickerSheet(
                    "Select Teacher Profile",
                    listOf(Pair("Unlink Profile", "")) + teacherList.map {
                        Pair("${it.firstName} ${it.lastName} (${it.teacherId})", it.teacherId)
                    }
                ) { name, id ->
                    selectedProfileId = id
                    btnLinkProfile.text = if (id.isEmpty()) "Link Teacher Profile (Optional)" else name
                }
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener {
            val firstName = dialogView.findViewById<TextInputEditText>(R.id.etFirstName).text.toString().trim()
            val lastName = dialogView.findViewById<TextInputEditText>(R.id.etLastName).text.toString().trim()
            val username = dialogView.findViewById<TextInputEditText>(R.id.etUsername).text.toString().trim()
            val email = dialogView.findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val role = actvRole.text.toString().trim()
            val password = dialogView.findViewById<TextInputEditText>(R.id.etPassword).text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                || email.isEmpty() || role.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Snackbar.make(binding.root, "Password must be at least 6 characters", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create in Firebase Auth first
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""
                    val newUser = hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "username" to username,
                        "email" to email,
                        "role" to role,
                        "uid" to uid,
                        "studentId" to if (role == "Student") selectedProfileId else "",
                        "teacherId" to if (role == "Teacher") selectedProfileId else ""
                    )
                    db.collection("users").add(newUser)
                        .addOnSuccessListener {
                            Snackbar.make(binding.root, "User created successfully!", Snackbar.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Snackbar.make(binding.root, "Error saving user: ${e.message}", Snackbar.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Snackbar.make(binding.root, "Auth error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_user, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val actvRole = dialogView.findViewById<AutoCompleteTextView>(R.id.actvRole)

        if (user.role == "Admin") {
            actvRole.isEnabled = false
            actvRole.isFocusable = false
        } else {
            actvRole.setAdapter(ArrayAdapter(requireContext(),
                android.R.layout.simple_dropdown_item_1line, listOf("Teacher", "Student")))
        }

        dialogView.findViewById<TextView>(R.id.tvTitle).text = "Edit User"
        dialogView.findViewById<TextView>(R.id.tvSubtitle).text = "Update user account information"
        dialogView.findViewById<MaterialButton>(R.id.btnCreate).text = "Update User"

        dialogView.findViewById<TextInputEditText>(R.id.etFirstName).setText(user.firstName)
        dialogView.findViewById<TextInputEditText>(R.id.etLastName).setText(user.lastName)
        dialogView.findViewById<TextInputEditText>(R.id.etUsername).setText(user.username)
        dialogView.findViewById<TextInputEditText>(R.id.etEmail).setText(user.email)
        dialogView.findViewById<TextInputEditText>(R.id.etPassword).setText("")
        actvRole.setText(user.role, false)

        val btnLinkProfile = dialogView.findViewById<MaterialButton>(R.id.btnLinkProfile)
        var selectedProfileId = ""

        when (user.role) {
            "Student" -> {
                btnLinkProfile.visibility = View.VISIBLE
                val linked = studentList.find { it.studentId == user.studentId }
                btnLinkProfile.text = if (linked != null)
                    "${linked.firstName} ${linked.lastName} (${linked.studentId})"
                else "Link Student Profile (Optional)"
                selectedProfileId = user.studentId
            }
            "Teacher" -> {
                btnLinkProfile.visibility = View.VISIBLE
                val linked = teacherList.find { it.teacherId == user.teacherId }
                btnLinkProfile.text = if (linked != null)
                    "${linked.firstName} ${linked.lastName} (${linked.teacherId})"
                else "Link Teacher Profile (Optional)"
                selectedProfileId = user.teacherId
            }
            else -> btnLinkProfile.visibility = View.GONE
        }

        actvRole.setOnItemClickListener { _, _, _, _ ->
            when (actvRole.text.toString()) {
                "Student" -> {
                    btnLinkProfile.visibility = View.VISIBLE
                    btnLinkProfile.text = "Link Student Profile (Optional)"
                    selectedProfileId = ""
                }
                "Teacher" -> {
                    btnLinkProfile.visibility = View.VISIBLE
                    btnLinkProfile.text = "Link Teacher Profile (Optional)"
                    selectedProfileId = ""
                }
                else -> {
                    btnLinkProfile.visibility = View.GONE
                    selectedProfileId = ""
                }
            }
        }

        btnLinkProfile.setOnClickListener {
            when (actvRole.text.toString()) {
                "Student" -> showProfilePickerSheet(
                    "Select Student Profile",
                    listOf(Pair("Unlink Profile", "")) + studentList.map {
                        Pair("${it.firstName} ${it.lastName} (${it.studentId})", it.studentId)
                    }
                ) { name, id ->
                    selectedProfileId = id
                    btnLinkProfile.text = if (id.isEmpty()) "Link Student Profile (Optional)" else name
                }
                "Teacher" -> showProfilePickerSheet(
                    "Select Teacher Profile",
                    listOf(Pair("Unlink Profile", "")) + teacherList.map {
                        Pair("${it.firstName} ${it.lastName} (${it.teacherId})", it.teacherId)
                    }
                ) { name, id ->
                    selectedProfileId = id
                    btnLinkProfile.text = if (id.isEmpty()) "Link Teacher Profile (Optional)" else name
                }
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener {
            val firstName = dialogView.findViewById<TextInputEditText>(R.id.etFirstName).text.toString().trim()
            val lastName = dialogView.findViewById<TextInputEditText>(R.id.etLastName).text.toString().trim()
            val username = dialogView.findViewById<TextInputEditText>(R.id.etUsername).text.toString().trim()
            val email = dialogView.findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val role = actvRole.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                || email.isEmpty() || role.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users").document(user.docId)
                .update(
                    "firstName", firstName,
                    "lastName", lastName,
                    "username", username,
                    "email", email,
                    "role", role,
                    "studentId", if (role == "Student") selectedProfileId else "",
                    "teacherId", if (role == "Teacher") selectedProfileId else ""
                )
                .addOnSuccessListener {
                    Snackbar.make(binding.root, "User updated successfully!", Snackbar.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteUserDialog(user: User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.firstName} ${user.lastName}?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Delete") { dialog, _ ->
                db.collection("users").document(user.docId)
                    .delete()
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "User deleted!", Snackbar.LENGTH_SHORT).show()
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
        binding.rvUsers.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}