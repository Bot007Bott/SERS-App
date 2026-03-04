package com.sers.app.utils

/**
 * Utils — Shared utility functions used across the app.
 * Part of the Model layer in MVC — provides reusable business logic
 * so Fragments (Controllers) don't repeat the same calculations.
 */
object Utils {

    /**
     * Convert a percentage score to a letter grade.
     * Used in GradesFragment, StudentGradesFragment, and Reports.
     */
    fun getLetterGrade(percent: Int): String {
        return when {
            percent >= 90 -> "A"
            percent >= 80 -> "B"
            percent >= 70 -> "C"
            percent >= 60 -> "D"
            else -> "F"
        }
    }

    /**
     * Calculate percentage from score and total marks.
     * Returns 0 if total is zero to avoid division by zero.
     */
    fun getPercentage(score: Int, totalMarks: Int): Int {
        return if (totalMarks > 0) (score.toFloat() / totalMarks * 100).toInt() else 0
    }

    /**
     * Get a color resource ID based on percentage for grade display.
     * Green = good, Orange = average, Red = poor.
     */
    fun getGradeColor(percent: Int): Int {
        return when {
            percent >= 70 -> android.graphics.Color.parseColor("#388E3C") // Green
            percent >= 50 -> android.graphics.Color.parseColor("#F57C00") // Orange
            else -> android.graphics.Color.parseColor("#D32F2F")          // Red
        }
    }

    /**
     * Format a date string from "YYYY-MM-DD" to "DD MMM YYYY".
     * Returns original string if format is unrecognized.
     */
    fun formatDate(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val month = parts[1].toIntOrNull() ?: return dateStr
                "${parts[2]} ${months[month]} ${parts[0]}"
            } else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Generate a unique ID with a given prefix and timestamp.
     * Used for creating new records in Firestore.
     */
    fun generateId(prefix: String): String {
        return "$prefix${System.currentTimeMillis()}"
    }

    /**
     * Validate that an email address has a proper format.
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Get attendance status color for display.
     */
    fun getAttendanceColor(status: String): Int {
        return when (status) {
            "Present" -> android.graphics.Color.parseColor("#388E3C")  // Green
            "Late" -> android.graphics.Color.parseColor("#F57C00")     // Orange
            "Absent" -> android.graphics.Color.parseColor("#D32F2F")   // Red
            else -> android.graphics.Color.parseColor("#757575")        // Grey
        }
    }
}
