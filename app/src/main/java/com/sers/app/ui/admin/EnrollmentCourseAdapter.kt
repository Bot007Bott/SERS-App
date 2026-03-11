package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemEnrollmentCourseBinding
import com.sers.app.model.Course
import com.sers.app.model.Enrollment

class EnrollmentCourseAdapter(
    private var courses: MutableList<Course>,
    private val enrollments: MutableList<Enrollment>,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<EnrollmentCourseAdapter.CourseViewHolder>() {

    private var teachers: List<com.sers.app.model.Teacher> = emptyList()

    fun setTeachers(list: List<com.sers.app.model.Teacher>) {
        teachers = list
        notifyDataSetChanged()
    }

    inner class CourseViewHolder(private val binding: ItemEnrollmentCourseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.tvCourseName.text = course.courseName
            binding.tvCourseCode.text = course.courseCode
            binding.tvSchedule.text = course.schedule

            val teacher = teachers.find { it.teacherId == course.teacherId }
            binding.tvTeacherName.text = if (teacher != null) "👤 ${teacher.firstName} ${teacher.lastName}" else "No teacher assigned"

            val count = enrollments.count { it.courseId == course.courseId }
            binding.tvStudentCount.text = "$count student${if (count != 1) "s" else ""}"

            binding.tvCourseIcon.text = course.courseName.firstOrNull()?.uppercase() ?: "C"

            binding.root.setOnClickListener { onCourseClick(course) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemEnrollmentCourseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount() = courses.size

    fun updateList(newList: MutableList<Course>) {
        courses = newList
        notifyDataSetChanged()
    }
}