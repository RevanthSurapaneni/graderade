package com.revvs9.grader.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revvs9.grader.HacRepository
import com.revvs9.grader.model.Assignment
import com.revvs9.grader.model.CourseGrades
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

data class CourseDetailsUiState(
    val isLoading: Boolean = false,
    val assessmentOfLearningAssignments: List<Assignment> = emptyList(),
    val otherAssignments: List<Assignment> = emptyList(),
    val overallCourseScore: String? = null,
    val courseTitle: String = "",
    val errorMessage: String? = null
)

class CourseDetailsViewModel(
    private val hacRepository: HacRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val COURSE_NAME_KEY = "courseName"
    }

    private val _uiState = MutableStateFlow(CourseDetailsUiState(isLoading = true))
    val uiState: StateFlow<CourseDetailsUiState> = _uiState.asStateFlow()

    private val courseName: String = savedStateHandle.get<String>(COURSE_NAME_KEY) ?: ""

    init {
        Log.d("CourseDetailsViewModel", "--- ViewModel CREATED for course: '$courseName' ---")
        hacRepository.logInstanceId("CourseDetailsViewModel Init")

        if (courseName.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(courseTitle = getCleanCourseName(this.courseName))
            observeCourseDetails()
        } else {
            _uiState.value = CourseDetailsUiState(
                isLoading = false,
                errorMessage = "Course name not provided.",
                courseTitle = "Error"
            )
            Log.e("CourseDetailsViewModel", "Course name not found in SavedStateHandle.")
        }
    }

    private fun processDataResult(result: Result<List<CourseGrades>>) {
        val currentTitle = _uiState.value.courseTitle
        result.fold(
            onSuccess = { courses ->
                val targetCourse = courses.find { it.courseName == courseName }
                if (targetCourse != null) {
                    val groupedAssignments = targetCourse.assignments.groupBy {
                        if (it.category.equals("Assessment of Learning", ignoreCase = true)) {
                            "AOL"
                        } else {
                            "Other"
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        assessmentOfLearningAssignments = groupedAssignments["AOL"] ?: emptyList(),
                        otherAssignments = groupedAssignments["Other"] ?: emptyList(),
                        overallCourseScore = targetCourse.overallScore,
                        courseTitle = currentTitle,
                        errorMessage = null
                    )
                    Log.d("CourseDetailsViewModel", "Processed data: Found course '${targetCourse.courseName}', AOL: ${groupedAssignments["AOL"]?.size ?: 0}, Other: ${groupedAssignments["Other"]?.size ?: 0}, Overall: ${targetCourse.overallScore}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        assessmentOfLearningAssignments = emptyList(),
                        otherAssignments = emptyList(),
                        overallCourseScore = null,
                        courseTitle = currentTitle,
                        errorMessage = "Details for course '${getCleanCourseName(courseName)}' not found."
                    )
                    Log.w("CourseDetailsViewModel", "Processed data: Course '$courseName' not found.")
                }
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    assessmentOfLearningAssignments = emptyList(),
                    otherAssignments = emptyList(),
                    overallCourseScore = null,
                    courseTitle = currentTitle,
                    errorMessage = "Error processing course data: ${error.message}"
                )
                Log.e("CourseDetailsViewModel", "Processed data: Error for $courseName", error)
            }
        )
    }

    private fun observeCourseDetails() {
        viewModelScope.launch {
            Log.d("CourseDetailsViewModel", "observeCourseDetails: Subscribing to gradesDataFlow for '$courseName'.")
            hacRepository.gradesDataFlow
                .collect { resultFromFlow ->
                    Log.d("CourseDetailsViewModel", "observeCourseDetails: gradesDataFlow emitted for '$courseName'")
                    if (resultFromFlow == null) {
                        if (!_uiState.value.isLoading) {
                             _uiState.value = _uiState.value.copy(
                                isLoading = true,
                                assessmentOfLearningAssignments = emptyList(),
                                otherAssignments = emptyList(),
                                overallCourseScore = null,
                                errorMessage = null
                            )
                            Log.d("CourseDetailsViewModel", "observeCourseDetails: Data became null. Setting isLoading=true.")
                        } else {
                            Log.d("CourseDetailsViewModel", "observeCourseDetails: Data is null, isLoading already true.")
                        }
                    } else {
                        processDataResult(resultFromFlow)
                    }
                }
        }
    }
}
