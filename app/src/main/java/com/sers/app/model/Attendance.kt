package com.sers.app.model

data class Attendance(
    val attendanceId: String = "",
    val studentId: String = "",
    val courseId: String = "",
    val session: String = "",
    val date: String = "",
    val status: String = "",
    val docId: String = ""
)
