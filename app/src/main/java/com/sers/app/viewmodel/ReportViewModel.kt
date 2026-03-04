package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.model.*

/**
 * ReportViewModel — MVVM ViewModel for reporting.
 * Handles loading of all data required for Admin and Student reports.
 */
class ReportViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _teachers = MutableLiveData<List<Teacher>>()
    val teachers: LiveData<List<Teacher>> = _teachers

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _enrollments = MutableLiveData<List<Enrollment>>()
    val enrollments: LiveData<List<Enrollment>> = _enrollments

    private val _grades = MutableLiveData<List<Grade>>()
    val grades: LiveData<List<Grade>> = _grades

    private val _attendance = MutableLiveData<List<Attendance>>()
    val attendance: LiveData<List<Attendance>> = _attendance

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadAllData(studentIdFilter: String? = null) {
        _isLoading.value = true

        // Use a simple chain or parallel fetch. For simplicity in this env, we fetch sequentially or just use multiple listeners if real-time needed.
        // But for Reports, fetching ONCE is often enough, or real-time. Let's do a one-time fetch for all.

        db.collection("students").get().addOnSuccessListener { sDocs ->
            val sList = sDocs.map { doc ->
                Student(
                    studentId = doc.getString("studentId") ?: "",
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    program = doc.getString("program") ?: "",
                    phone = doc.getString("phone") ?: "",
                    docId = doc.id
                )
            }
            _students.value = sList

            db.collection("teachers").get().addOnSuccessListener { tDocs ->
                val tList = tDocs.map { doc ->
                    Teacher(
                        teacherId = doc.getString("teacherId") ?: "",
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        email = doc.getString("email") ?: "",
                        department = doc.getString("department") ?: "",
                        phone = doc.getString("phone") ?: "",
                        docId = doc.id
                    )
                }
                _teachers.value = tList

                db.collection("courses").get().addOnSuccessListener { cDocs ->
                    val cList = cDocs.map { doc ->
                        Course(
                            courseId = doc.getString("courseId") ?: "",
                            courseName = doc.getString("courseName") ?: "",
                            courseCode = doc.getString("courseCode") ?: "",
                            schedule = doc.getString("schedule") ?: "",
                            teacherId = doc.getString("teacherId") ?: "",
                            createdBy = doc.getString("createdBy") ?: "",
                            docId = doc.id
                        )
                    }
                    _courses.value = cList

                    db.collection("enrollments").get().addOnSuccessListener { eDocs ->
                        val eList = eDocs.map { doc ->
                            Enrollment(
                                enrollmentId = doc.getString("enrollmentId") ?: "",
                                studentId = doc.getString("studentId") ?: "",
                                courseId = doc.getString("courseId") ?: "",
                                docId = doc.id
                            )
                        }
                        _enrollments.value = eList

                        val gradeQuery = if (studentIdFilter != null) db.collection("grades").whereEqualTo("studentId", studentIdFilter) else db.collection("grades")
                        gradeQuery.get().addOnSuccessListener { gDocs ->
                            val gList = gDocs.map { doc ->
                                Grade(
                                    gradeId = doc.getString("gradeId") ?: "",
                                    studentId = doc.getString("studentId") ?: "",
                                    courseId = doc.getString("courseId") ?: "",
                                    score = (doc.getLong("score") ?: 0).toInt(),
                                    totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(),
                                    gradeType = doc.getString("gradeType") ?: "",
                                    title = doc.getString("title") ?: "",
                                    docId = doc.id
                                )
                            }
                            _grades.value = gList

                            val attQuery = if (studentIdFilter != null) db.collection("attendance").whereEqualTo("studentId", studentIdFilter) else db.collection("attendance")
                            attQuery.get().addOnSuccessListener { aDocs ->
                                val aList = aDocs.map { doc ->
                                    Attendance(
                                        attendanceId = doc.getString("attendanceId") ?: "",
                                        studentId = doc.getString("studentId") ?: "",
                                        courseId = doc.getString("courseId") ?: "",
                                        session = doc.getString("session") ?: doc.getString("timeSlot") ?: "",
                                        date = doc.getString("date") ?: "",
                                        status = doc.getString("status") ?: "",
                                        docId = doc.id
                                    )
                                }
                                _attendance.value = aList
                                _isLoading.value = false
                            }.addOnFailureListener { _message.value = it.message; _isLoading.value = false }
                        }.addOnFailureListener { _message.value = it.message; _isLoading.value = false }
                    }.addOnFailureListener { _message.value = it.message; _isLoading.value = false }
                }.addOnFailureListener { _message.value = it.message; _isLoading.value = false }
            }.addOnFailureListener { _message.value = it.message; _isLoading.value = false }
        }.addOnFailureListener { _message.value = it.message; _isLoading.value = false }
    }
}
