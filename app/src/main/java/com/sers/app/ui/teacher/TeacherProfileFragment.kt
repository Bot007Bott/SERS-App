package com.sers.app.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sers.app.databinding.FragmentTeacherProfileBinding
import com.sers.app.viewmodel.ProfileViewModel
import com.sers.app.viewmodel.TeacherCourseViewModel

/**
 * TeacherProfileFragment — MVVM View
 * Uses ProfileViewModel and TeacherCourseViewModel for teacher info and course stats.
 */
class TeacherProfileFragment : Fragment() {

    private lateinit var binding: FragmentTeacherProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    private val courseViewModel: TeacherCourseViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTeacherProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        profileViewModel.loadCurrentUser()
        courseViewModel.loadData()
    }

    private fun setupObservers() {
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.tvName.text = "${user.firstName} ${user.lastName}"
            binding.tvTeacherId.text = user.teacherId
            binding.tvAvatar.text = user.firstName.take(1).uppercase()
            binding.tvEmail.text = user.email
            binding.tvPhone.text = user.phone.ifEmpty { "Not set" }

            if (user.teacherId.isNotEmpty()) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("teachers")
                    .whereEqualTo("teacherId", user.teacherId)
                    .get()
                    .addOnSuccessListener { docs ->
                        val department = docs.documents.firstOrNull()?.getString("department") ?: ""
                        binding.tvDepartment.text = department.ifEmpty { "Not set" }
                    }
                    .addOnFailureListener {
                        binding.tvDepartment.text = "Not set"
                    }
            } else {
                binding.tvDepartment.text = "Not set"
            }
        }

        courseViewModel.courses.observe(viewLifecycleOwner) { courses ->
            binding.tvCourseHeader.text = "Teaching ${courses.size} courses"
            binding.tvCourseList.text = if (courses.isEmpty()) "No courses assigned" else courses.joinToString("\n") { "• ${it.courseName}" }
        }
    }
}
