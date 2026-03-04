package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.Student
import com.sers.app.model.Teacher
import com.sers.app.model.User

/**
 * UserManagementViewModel — MVVM ViewModel
 * Handles user list loading and CRUD operations for UserManagementFragment.
 */
class UserManagementViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userListener: ListenerRegistration? = null

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _teachers = MutableLiveData<List<Teacher>>()
    val teachers: LiveData<List<Teacher>> = _teachers

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadAllData() {
        db.collection("students").get().addOnSuccessListener { studentDocs ->
            val studentList = studentDocs.map { doc ->
                Student(
                    studentId = doc.getString("studentId") ?: "",
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    program = doc.getString("program") ?: "",
                    phone = doc.getString("phone") ?: "",
                    docId = doc.id
                )
            }
            _students.value = studentList
            db.collection("teachers").get().addOnSuccessListener { teacherDocs ->
                val teacherList = teacherDocs.map { doc ->
                    Teacher(
                        teacherId = doc.getString("teacherId") ?: "",
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        email = doc.getString("email") ?: "",
                        department = doc.getString("department") ?: "",
                        phone = doc.getString("phone") ?: "",
                        docId = doc.id
                    )
                }
                _teachers.value = teacherList
                startUserListener()
            }
        }
    }

    private fun startUserListener() {
        userListener = db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) { _message.value = "ERROR:${error.message}"; return@addSnapshotListener }
            val list = snapshot?.documents?.map { doc ->
                User(
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
                )
            } ?: emptyList()
            _users.value = list
        }
    }

    fun createUser(email: String, password: String, firstName: String, lastName: String,
                   username: String, role: String, profileId: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                val newUser = hashMapOf(
                    "firstName" to firstName, "lastName" to lastName, "username" to username,
                    "email" to email, "role" to role, "uid" to uid,
                    "studentId" to if (role == "Student") profileId else "",
                    "teacherId" to if (role == "Teacher") profileId else ""
                )
                db.collection("users").add(newUser)
                    .addOnSuccessListener { _message.value = "User created successfully!" }
                    .addOnFailureListener { e -> _message.value = "ERROR:Error saving user: ${e.message}" }
            }
            .addOnFailureListener { e -> _message.value = "ERROR:Auth error: ${e.message}" }
    }

    fun updateUser(docId: String, firstName: String, lastName: String, username: String,
                   email: String, role: String, profileId: String) {
        db.collection("users").document(docId)
            .update("firstName", firstName, "lastName", lastName, "username", username,
                "email", email, "role", role,
                "studentId", if (role == "Student") profileId else "",
                "teacherId", if (role == "Teacher") profileId else "")
            .addOnSuccessListener { _message.value = "User updated successfully!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    fun deleteUser(docId: String) {
        db.collection("users").document(docId).delete()
            .addOnSuccessListener { _message.value = "User deleted!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
