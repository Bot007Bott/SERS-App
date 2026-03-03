package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemCourseBinding
import com.sers.app.model.Course
import com.sers.app.model.Teacher

class CourseAdapter(
    private var courses: MutableList<Course>,
    private val onEditClick: (Course) -> Unit,
    private val onDeleteClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    private var teacherList = mutableListOf<Teacher>()

    inner class CourseViewHolder(private val binding: ItemCourseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.tvCourseName.text = course.courseName
            binding.tvCourseCode.text = course.courseCode
            binding.tvSchedule.text = course.schedule
            val teacher = teacherList.find { it.teacherId == course.teacherId }
            binding.tvCreatedBy.text = if (teacher != null)
                "${teacher.firstName} ${teacher.lastName}"
            else if (course.teacherId.isNotEmpty()) course.teacherId
            else "No Teacher Assigned"

            binding.root.setOnClickListener { onEditClick(course) }
            binding.root.setOnLongClickListener {
                onDeleteClick(course)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    fun setTeachers(teachers: List<Teacher>) {
        teacherList = teachers.toMutableList()
        notifyDataSetChanged()
    }
}