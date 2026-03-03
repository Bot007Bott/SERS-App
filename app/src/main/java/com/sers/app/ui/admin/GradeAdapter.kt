package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemGradeBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade
import com.sers.app.model.Student

class GradeAdapter(
    private var grades: MutableList<Grade>,
    private val onEditClick: (Grade) -> Unit,
    private val onDeleteClick: (Grade) -> Unit
) : RecyclerView.Adapter<GradeAdapter.GradeViewHolder>() {

    private var studentList = mutableListOf<Student>()
    private var courseList = mutableListOf<Course>()

    inner class GradeViewHolder(private val binding: ItemGradeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(grade: Grade) {
            val student = studentList.find { it.studentId == grade.studentId }
            val course = courseList.find { it.courseId == grade.courseId }

            binding.tvStudentName.text = if (student != null)
                "${student.firstName} ${student.lastName}" else grade.studentId
            binding.tvCourse.text = course?.courseName ?: grade.courseId
            binding.tvScore.text = "${grade.score}/${grade.totalMarks}"

            // Show grade type and title
            val label = buildString {
                if (grade.gradeType.isNotEmpty()) append(grade.gradeType)
                if (grade.title.isNotEmpty()) append(if (grade.gradeType.isNotEmpty()) " — ${grade.title}" else grade.title)
            }
            if (label.isNotEmpty()) {
                binding.tvGradeLabel.text = label
                binding.tvGradeLabel.visibility = android.view.View.VISIBLE
            } else {
                binding.tvGradeLabel.visibility = android.view.View.GONE
            }

            val percent = if (grade.totalMarks > 0) (grade.score.toFloat() / grade.totalMarks * 100).toInt() else 0
            binding.tvPercent.text = "$percent%"

            val color = when {
                percent >= 80 -> "#43A047"
                percent >= 60 -> "#FB8C00"
                else -> "#E53935"
            }
            binding.tvPercent.setTextColor(android.graphics.Color.parseColor(color))

            binding.root.setOnClickListener { onEditClick(grade) }
            binding.root.setOnLongClickListener { onDeleteClick(grade); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val binding = ItemGradeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GradeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        holder.bind(grades[position])
    }

    override fun getItemCount() = grades.size

    fun updateList(newList: MutableList<Grade>) {
        grades = newList
        notifyDataSetChanged()
    }

    fun setStudentsAndCourses(students: List<Student>, courses: List<Course>) {
        studentList = students.toMutableList()
        courseList = courses.toMutableList()
        notifyDataSetChanged()
    }
}