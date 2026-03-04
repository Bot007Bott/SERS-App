package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sers.app.databinding.FragmentAdminProfileBinding
import com.sers.app.viewmodel.ProfileViewModel

/**
 * AdminProfileFragment — MVVM View
 * Observes ProfileViewModel to display admin profile info.
 */
class AdminProfileFragment : Fragment() {

    private lateinit var binding: FragmentAdminProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.tvName.text = "${user.firstName} ${user.lastName}"
            binding.tvRole.text = "Administrator"
            binding.tvAvatar.text = user.firstName.firstOrNull()?.uppercase() ?: "A"
            binding.tvEmail.text = user.email
            binding.tvPhone.text = "Not set"
            binding.tvRoleInfo.text = "Administrator"
        }

        viewModel.loadCurrentUser()
    }
}