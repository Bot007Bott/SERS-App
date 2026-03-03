package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemTeacherBinding
import com.sers.app.model.Teacher

class TeacherAdapter(
    private var teachers: MutableList<Teacher>,
    private val onEditClick: (Teacher) -> Unit,
    private val onDeleteClick: (Teacher) -> Unit
) : RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {

    inner class TeacherViewHolder(private val binding: ItemTeacherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(teacher: Teacher) {
            binding.tvTeacherName.text = "${teacher.firstName} ${teacher.lastName}"
            binding.tvTeacherId.text = teacher.teacherId
            binding.tvTeacherEmail.text = teacher.email
            binding.tvTeacherDepartment.text = teacher.department
            binding.tvAvatar.text = teacher.firstName.firstOrNull()?.uppercase() ?: "T"

            if (teacher.userId.isNotEmpty()) {
                binding.tvAccountStatus.visibility = android.view.View.VISIBLE
                binding.tvAccountStatus.text = "Has Account"
                binding.tvAccountStatus.setTextColor(android.graphics.Color.parseColor("#43A047"))
            } else {
                binding.tvAccountStatus.visibility = android.view.View.VISIBLE
                binding.tvAccountStatus.text = "No Account"
                binding.tvAccountStatus.setTextColor(android.graphics.Color.parseColor("#E53935"))
            }

            binding.root.setOnClickListener { onEditClick(teacher) }
            binding.root.setOnLongClickListener {
                onDeleteClick(teacher)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val binding = ItemTeacherBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TeacherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        holder.bind(teachers[position])
    }

    override fun getItemCount() = teachers.size

    fun updateList(newList: List<Teacher>) {
        teachers = newList.toMutableList()
        notifyDataSetChanged()
    }
}