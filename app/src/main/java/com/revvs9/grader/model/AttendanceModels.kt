package com.revvs9.grader.model

// Using String for dates initially, can be parsed to LocalDate in ViewModel or UI if needed.
data class AttendanceRecord(
    val date: String,          // e.g., "MM/DD/YYYY"
    val period: String,        // e.g., "1", "All Day"
    val courseName: String?,   // Name of the course, if applicable
    val attendanceCode: String,// e.g., "UA", "T", "E" (Unexcused Absence, Tardy, Excused)
    val description: String    // Full description, e.g., "Unexcused Absence Period 1 - English"
)

// Represents the overall state for attendance data
data class AttendanceData(
    val records: List<AttendanceRecord> = emptyList(),
    val summaryMessage: String? = null // e.g., "No absences or tardies this month."
)
