package com.sers.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sers.app.databinding.FragmentStudentProfileBinding
import com.sers.app.viewmodel.StudentProfileViewModel

/**
 * StudentProfileFragment — MVVM View
 * Observes StudentProfileViewModel to display student profile and enrolled courses.
 */
class StudentProfileFragment : Fragment() {

    private lateinit var binding: FragmentStudentProfileBinding
    private val viewModel: StudentProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            profile ?: return@observe
            binding.tvName.text = "${profile.firstName} ${profile.lastName}"
            binding.tvStudentId.text = profile.studentId
            binding.tvAvatar.text = profile.firstName.firstOrNull()?.uppercase() ?: "S"
            binding.tvEmail.text = profile.email
            binding.tvPhone.text = profile.phone.ifEmpty { "Not set" }
            binding.tvStudentIdInfo.text = profile.studentId.ifEmpty { "Not set" }
        }

        viewModel.enrolledCourseCount.observe(viewLifecycleOwner) { count ->
            binding.tvCourseHeader.text = "Enrolled in $count courses"
        }

        viewModel.enrolledCourseNames.observe(viewLifecycleOwner) { names ->
            binding.tvCourseList.text = names
        }

        viewModel.loadProfile()
    }
}