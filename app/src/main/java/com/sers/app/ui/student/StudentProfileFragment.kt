package com.sers.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.databinding.FragmentStudentProfileBinding

class StudentProfileFragment : Fragment() {

    private lateinit var binding: FragmentStudentProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false)
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
                val studentId = userDoc.getString("studentId") ?: ""

                binding.tvName.text = "$firstName $lastName"
                binding.tvStudentId.text = studentId
                binding.tvAvatar.text = firstName.firstOrNull()?.uppercase() ?: "S"
                binding.tvEmail.text = email
                binding.tvPhone.text = phone.ifEmpty { "Not set" }
                binding.tvStudentIdInfo.text = studentId.ifEmpty { "Not set" }

                // Load enrolled courses
                if (studentId.isNotEmpty()) {
                    db.collection("enrollments").whereEqualTo("studentId", studentId).get()
                        .addOnSuccessListener { enrollDocs ->
                            val courseIds = enrollDocs.documents.mapNotNull { it.getString("courseId") }
                            if (courseIds.isEmpty()) {
                                binding.tvCourseHeader.text = "Enrolled in 0 courses"
                                binding.tvCourseList.text = "No courses enrolled"
                                return@addOnSuccessListener
                            }
                            db.collection("courses").whereIn("courseId", courseIds).get()
                                .addOnSuccessListener { courseDocs ->
                                    binding.tvCourseHeader.text = "Enrolled in ${courseDocs.size()} courses"
                                    binding.tvCourseList.text = courseDocs.documents.joinToString("\n") {
                                        "• ${it.getString("courseName") ?: ""}"
                                    }
                                }
                        }
                }
            }
    }
}