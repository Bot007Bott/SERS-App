package com.sers.app.model

data class Grade(
    val gradeId: String = "",
    val studentId: String = "",
    val courseId: String = "",
    val gradeType: String = "",
    val title: String = "",
    val score: Int = 0,
    val totalMarks: Int = 100,
    val docId: String = ""
)
