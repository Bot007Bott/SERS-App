package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemStudentBinding
import com.sers.app.model.Student

class StudentAdapter(
    private var students: MutableList<Student>,
    private val onEditClick: (Student) -> Unit,
    private val onDeleteClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(private val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(student: Student) {
            binding.tvStudentName.text = "${student.firstName} ${student.lastName}"
            binding.tvStudentId.text = student.studentId
            binding.tvStudentEmail.text = student.email
            binding.tvAvatar.text = student.firstName.firstOrNull()?.uppercase() ?: "S"
            if (student.userId.isNotEmpty()) {
                binding.tvAccountStatus.visibility = android.view.View.VISIBLE
                binding.tvAccountStatus.text = "Has Account"
                binding.tvAccountStatus.setTextColor(android.graphics.Color.parseColor("#43A047"))
            } else {
                binding.tvAccountStatus.visibility = android.view.View.VISIBLE
                binding.tvAccountStatus.text = "No Account"
                binding.tvAccountStatus.setTextColor(android.graphics.Color.parseColor("#E53935"))
            }

            binding.root.setOnClickListener { onEditClick(student) }
            binding.root.setOnLongClickListener {
                onDeleteClick(student)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount() = students.size

    fun filter(query: String, originalList: List<Student>) {
        students = if (query.isEmpty()) {
            originalList.toMutableList()
        } else {
            originalList.filter {
                it.firstName.contains(query, ignoreCase = true) ||
                        it.lastName.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true) ||
                        it.studentId.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun updateList(newList: List<Student>) {
        students = newList.toMutableList()
        notifyDataSetChanged()
    }
}