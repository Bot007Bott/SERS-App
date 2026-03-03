package com.sers.app.ui.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemStudentAttendanceBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course

class StudentAttendanceAdapter(
    private var attendanceList: MutableList<Attendance>,
    private var courses: MutableList<Course>
) : RecyclerView.Adapter<StudentAttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(val binding: ItemStudentAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemStudentAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val attendance = attendanceList[position]
        val course = courses.find { it.courseId == attendance.courseId }

        holder.binding.tvCourse.text = course?.courseName ?: attendance.courseId
        holder.binding.tvDate.text = attendance.date
        holder.binding.tvSession.text = attendance.session
        holder.binding.tvStatus.text = attendance.status

        val color = when (attendance.status) {
            "Present" -> "#43A047"
            "Absent" -> "#E53935"
            "Late" -> "#FB8C00"
            else -> "#757575"
        }
        holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor(color))
        holder.binding.statusIndicator.setBackgroundColor(android.graphics.Color.parseColor(color))
    }

    override fun getItemCount() = attendanceList.size

    fun updateList(newList: MutableList<Attendance>, newCourses: MutableList<Course>) {
        attendanceList = newList
        courses = newCourses
        notifyDataSetChanged()
    }
}