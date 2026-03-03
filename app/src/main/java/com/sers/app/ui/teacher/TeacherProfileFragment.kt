package com.sers.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.databinding.FragmentTeacherProfileBinding

class TeacherProfileFragment : Fragment() {

    private lateinit var binding: FragmentTeacherProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { userDocs ->
                if (userDocs.isEmpty) return@addOnSuccessListener
                val userDoc = userDocs.documents[0]
                val firstName = userDoc.getString("firstName") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                val email = userDoc.getString("email") ?: ""
                val phone = userDoc.getString("phone") ?: ""
                val teacherId = userDoc.getString("teacherId") ?: ""

                binding.tvName.text = "$firstName $lastName"
                binding.tvTeacherId.text = teacherId
                binding.tvAvatar.text = firstName.firstOrNull()?.uppercase() ?: "T"
                binding.tvEmail.text = email
                binding.tvPhone.text = phone.ifEmpty { "Not set" }

                // Load teacher's courses
                if (teacherId.isNotEmpty()) {
                    db.collection("teachers").whereEqualTo("teacherId", teacherId).get()
                        .addOnSuccessListener { teacherDocs ->
                            if (!teacherDocs.isEmpty) {
                                val dept = teacherDocs.documents[0].getString("department") ?: ""
                                binding.tvDepartment.text = dept.ifEmpty { "Not set" }
                            }
                        }
                    db.collection("courses").whereEqualTo("teacherId", teacherId).get()
                        .addOnSuccessListener { courseDocs ->
                            binding.tvCourseHeader.text = "Teaching ${courseDocs.size()} courses"
                            binding.tvCourseList.text = if (courseDocs.isEmpty)
                                "No courses assigned"
                            else
                                courseDocs.documents.joinToString("\n") {
                                    "• ${it.getString("courseName") ?: ""}"
                                }
                        }
                }
            }
    }
}