package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemAttendanceBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.model.Student

class AttendanceAdapter(
    private var attendances: MutableList<Attendance>,
    private val onEditClick: (Attendance) -> Unit,
    private val onDeleteClick: (Attendance) -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    private var studentList = mutableListOf<Student>()
    private var courseList = mutableListOf<Course>()

    inner class AttendanceViewHolder(private val binding: ItemAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(attendance: Attendance) {
            val student = studentList.find { it.studentId == attendance.studentId }
            val course = courseList.find { it.courseId == attendance.courseId }

            binding.tvStudentName.text = if (student != null)
                "${student.firstName} ${student.lastName}" else attendance.studentId
            binding.tvCourse.text = course?.courseName ?: attendance.courseId
            binding.tvSession.text = attendance.session
            binding.tvDate.text = attendance.date
            binding.tvStatus.text = attendance.status

            val color = when (attendance.status) {
                "Present" -> "#43A047"
                "Absent" -> "#E53935"
                "Late" -> "#FB8C00"
                else -> "#757575"
            }
            binding.tvStatus.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))

            binding.root.setOnClickListener { onEditClick(attendance) }
            binding.root.setOnLongClickListener {
                onDeleteClick(attendance)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(attendances[position])
    }

    override fun getItemCount() = attendances.size

    fun updateList(newList: MutableList<Attendance>) {
        attendances = newList
        notifyDataSetChanged()
    }

    fun setStudentsAndCourses(students: List<Student>, courses: List<Course>) {
        studentList = students.toMutableList()
        courseList = courses.toMutableList()
        notifyDataSetChanged()
    }
}