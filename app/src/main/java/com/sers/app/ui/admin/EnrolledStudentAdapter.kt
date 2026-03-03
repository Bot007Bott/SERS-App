package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemStudentBinding
import com.sers.app.model.Enrollment
import com.sers.app.model.Student

class EnrolledStudentAdapter(
    private var enrollments: MutableList<Enrollment>,
    private val onRemoveClick: (Enrollment) -> Unit
) : RecyclerView.Adapter<EnrolledStudentAdapter.ViewHolder>() {

    private var studentList = mutableListOf<Student>()

    inner class ViewHolder(private val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(enrollment: Enrollment) {
            val student = studentList.find { it.studentId == enrollment.studentId }
            binding.tvStudentName.text = if (student != null)
                "${student.firstName} ${student.lastName}" else enrollment.studentId
            binding.tvStudentId.text = enrollment.studentId
            binding.tvStudentEmail.text = student?.email ?: ""
            binding.tvAvatar.text = student?.firstName?.firstOrNull()?.uppercase() ?: "S"
            binding.tvAccountStatus.visibility = android.view.View.GONE

            binding.root.setOnLongClickListener {
                onRemoveClick(enrollment)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(enrollments[position])
    }

    override fun getItemCount() = enrollments.size

    fun updateList(newEnrollments: MutableList<Enrollment>, students: List<Student>) {
        enrollments = newEnrollments
        studentList = students.toMutableList()
        notifyDataSetChanged()
    }
}