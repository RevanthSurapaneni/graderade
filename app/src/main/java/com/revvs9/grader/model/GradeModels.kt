package com.revvs9.grader.model

data class Assignment(
    val name: String,
    val category: String,
    val dateDue: String?,
    val dateAssigned: String?, // Uncommented and expecting String?
    val score: String?, // Expecting String?
    val totalPoints: String? // Expecting String?
)

data class CourseGrades(
    val courseName: String,
    val overallScore: String?, // This might be the average shown for the course
    val assignments: List<Assignment>
)

data class MarkingPeriod(
    val name: String, // e.g., "MP1", "Quarter 1"
    val value: String,  // The actual value used in the form submission, e.g., "1", "Q1"
    val isSelected: Boolean = false // New field to indicate if this period is selected by default
)
