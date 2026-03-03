package com.sers.app.ui.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemStudentGradeBinding
import com.sers.app.model.Course
import com.sers.app.model.Grade

class StudentGradeAdapter(
    private var grades: MutableList<Grade>,
    private var courses: MutableList<Course>
) : RecyclerView.Adapter<StudentGradeAdapter.GradeViewHolder>() {

    inner class GradeViewHolder(val binding: ItemStudentGradeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val binding = ItemStudentGradeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GradeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        val grade = grades[position]
        val course = courses.find { it.courseId == grade.courseId }

        holder.binding.tvCourse.text = course?.courseName ?: grade.courseId

        // Show grade label
        val label = buildString {
            if (grade.gradeType.isNotEmpty()) append(grade.gradeType)
            if (grade.title.isNotEmpty()) append(if (grade.gradeType.isNotEmpty()) " — ${grade.title}" else grade.title)
        }
        if (label.isNotEmpty()) {
            holder.binding.tvGradeLabel.text = label
            holder.binding.tvGradeLabel.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvGradeLabel.visibility = android.view.View.GONE
        }

        holder.binding.tvScore.text = "${grade.score}/${grade.totalMarks}"

        val percentage = (grade.score.toFloat() / grade.totalMarks * 100).toInt()
        holder.binding.tvPercentage.text = "$percentage%"
        holder.binding.progressGrade.progress = percentage

        holder.binding.tvGradeLetter.text = when {
            percentage >= 90 -> "A"
            percentage >= 80 -> "B"
            percentage >= 70 -> "C"
            percentage >= 60 -> "D"
            else -> "F"
        }

        holder.binding.tvGradeLetter.setTextColor(
            android.graphics.Color.parseColor(when {
                percentage >= 90 -> "#43A047"
                percentage >= 80 -> "#1976D2"
                percentage >= 70 -> "#FB8C00"
                else -> "#E53935"
            })
        )
    }

    override fun getItemCount() = grades.size

    fun updateList(newGrades: MutableList<Grade>, newCourses: MutableList<Course>) {
        grades = newGrades
        courses = newCourses
        notifyDataSetChanged()
    }
}