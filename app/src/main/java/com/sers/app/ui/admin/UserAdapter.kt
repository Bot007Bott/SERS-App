package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemUserBinding
import com.sers.app.model.User
import android.view.View

class UserAdapter(
    private var users: MutableList<User>,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            val fullName = "${user.firstName} ${user.lastName}"
            binding.tvUserName.text = fullName
            binding.tvUserEmail.text = user.email
            binding.tvUserRole.text = user.role
            binding.tvAvatar.text = user.firstName.firstOrNull()?.uppercase() ?: "U"

            // Role badge color
            val badgeColor = when (user.role.lowercase()) {
                "admin" -> "#E53935"
                "teacher" -> "#1976D2"
                else -> "#43A047"
            }

            // Show linked profile status
            when (user.role) {
                "Student" -> {
                    binding.tvLinkedProfile.visibility = View.VISIBLE
                    binding.tvLinkedProfile.text = if (user.studentId.isNotEmpty())
                        "Linked: ${user.studentId}" else "No Profile Linked"
                    binding.tvLinkedProfile.setTextColor(
                        android.graphics.Color.parseColor(
                            if (user.studentId.isNotEmpty()) "#43A047" else "#E53935"))
                }
                "Teacher" -> {
                    binding.tvLinkedProfile.visibility = View.VISIBLE
                    binding.tvLinkedProfile.text = if (user.teacherId.isNotEmpty())
                        "Linked: ${user.teacherId}" else "No Profile Linked"
                    binding.tvLinkedProfile.setTextColor(
                        android.graphics.Color.parseColor(
                            if (user.teacherId.isNotEmpty()) "#43A047" else "#E53935"))
                }
                else -> binding.tvLinkedProfile.visibility = View.GONE
            }

            binding.tvUserRole.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(badgeColor)
                )

            // Click listeners
            binding.root.setOnClickListener { onEditClick(user) }
            binding.root.setOnLongClickListener {
                if (user.role.lowercase() != "admin") {
                    onDeleteClick(user)
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    // Filter for search
    fun filter(query: String, originalList: List<User>) {
        users = if (query.isEmpty()) {
            originalList.toMutableList()
        } else {
            originalList.filter {
                it.firstName.contains(query, ignoreCase = true) ||
                        it.lastName.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true) ||
                        it.role.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun updateList(newList: List<User>) {
        users = newList.toMutableList()
        notifyDataSetChanged()
    }
}