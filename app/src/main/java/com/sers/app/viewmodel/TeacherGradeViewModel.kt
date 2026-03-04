package com.sers.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sers.app.model.*

/**
 * TeacherGradeViewModel — MVVM ViewModel for Teacher's grading.
 */
class TeacherGradeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var gradesListener: ListenerRegistration? = null

    private val _grades = MutableLiveData<List<Grade>>()
    val grades: LiveData<List<Grade>> = _grades

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _enrollments = MutableLiveData<List<Enrollment>>()
    val enrollments: LiveData<List<Enrollment>> = _enrollments

    private val _teacherId = MutableLiveData<String>()
    val teacherId: LiveData<String> = _teacherId

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadAllData() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                val tid = docs.documents[0].getString("teacherId") ?: ""
                _teacherId.value = tid
                loadRelatedData(tid)
            }
    }

    private fun loadRelatedData(tid: String) {
        db.collection("students").get().addOnSuccessListener { sDocs ->
            _students.value = sDocs.map { doc ->
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
            db.collection("courses").whereEqualTo("teacherId", tid).get().addOnSuccessListener { cDocs ->
                val cList = cDocs.map { doc ->
                    Course(
                        courseId = doc.getString("courseId") ?: "",
                        courseName = doc.getString("courseName") ?: "",
                        courseCode = doc.getString("courseCode") ?: "",
                        schedule = doc.getString("schedule") ?: "",
                        teacherId = tid,
                        docId = doc.id
                    )
                }
                _courses.value = cList
                val cIds = cList.map { it.courseId }
                
                db.collection("enrollments").get().addOnSuccessListener { eDocs ->
                    _enrollments.value = eDocs.map { doc ->
                        Enrollment(
                            enrollmentId = doc.getString("enrollmentId") ?: "",
                            studentId = doc.getString("studentId") ?: "",
                            courseId = doc.getString("courseId") ?: "",
                            docId = doc.id
                        )
                    }
                    startGradesListener(cIds)
                }
            }
        }
    }

    private fun startGradesListener(cIds: List<String>) {
        if (cIds.isEmpty()) { _grades.value = emptyList(); _isLoading.value = false; return }
        gradesListener?.remove()
        gradesListener = db.collection("grades").addSnapshotListener { snapshot, error ->
            _isLoading.value = false
            if (error != null) { _message.value = "ERROR:${error.message}"; return@addSnapshotListener }
            _grades.value = snapshot?.documents?.filter { it.getString("courseId") in cIds }?.map { doc ->
                Grade(
                    gradeId = doc.getString("gradeId") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    gradeType = doc.getString("gradeType") ?: "",
                    title = doc.getString("title") ?: "",
                    score = (doc.getLong("score") ?: 0).toInt(),
                    totalMarks = (doc.getLong("totalMarks") ?: 100).toInt(),
                    docId = doc.id
                )
            } ?: emptyList()
        }
    }

    fun saveGrade(docId: String?, data: Map<String, Any>) {
        if (docId != null) {
            db.collection("grades").document(docId).update(data)
                .addOnSuccessListener { _message.value = "Grade updated!" }
                .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
        } else {
            val newData = data.toMutableMap()
            newData["gradeId"] = "G${System.currentTimeMillis()}"
            db.collection("grades").add(newData)
                .addOnSuccessListener { _message.value = "Grade added!" }
                .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
        }
    }

    fun deleteGrade(docId: String) {
        db.collection("grades").document(docId).delete()
            .addOnSuccessListener { _message.value = "Grade deleted!" }
            .addOnFailureListener { e -> _message.value = "ERROR:${e.message}" }
    }

    override fun onCleared() {
        super.onCleared()
        gradesListener?.remove()
    }
}
