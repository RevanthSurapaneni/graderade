package com.revvs9.grader.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revvs9.grader.HacRepository
import com.revvs9.grader.model.CourseGrades
import com.revvs9.grader.model.MarkingPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GradesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val courses: List<CourseGrades> = emptyList(),
    val markingPeriods: List<MarkingPeriod> = emptyList() // Kept to know the default MP's name, not for selection
    // selectedMarkingPeriodValue removed
)

open class GradesViewModel(private val hacRepository: HacRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GradesUiState(isLoading = true))
    open val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    init {
        hacRepository.logInstanceId("GradesViewModel")
        observeMarkingPeriods() // Still observe to get the list (and default selected name)
        observeGrades()
        initialLoad()
    }

    private fun observeMarkingPeriods() {
        viewModelScope.launch {
            hacRepository.markingPeriodsFlow
                .collectLatest { result: Result<List<MarkingPeriod>>? ->
                    if (result == null) {
                        Log.d("GradesViewModel", "MarkingPeriodsFlow emitted null.")
                        return@collectLatest
                    }

                    result.fold(
                        onSuccess = { periods: List<MarkingPeriod> ->
                            Log.d("GradesViewModel", "Successfully collected marking periods: ${periods.size} items. Default HTML selected: ${periods.find{it.isSelected}?.name}")
                            _uiState.update { currentState ->
                                currentState.copy(
                                    markingPeriods = periods
                                    // No selectedMarkingPeriodValue to update here
                                )
                            }
                        },
                        onFailure = { error: Throwable ->
                            Log.e("GradesViewModel", "Failure collecting markingPeriodsFlow", error)
                            _uiState.update { currentState ->
                                currentState.copy(
                                    error = currentState.error ?: "Failed to load marking periods: ${error.message}",
                                    markingPeriods = emptyList(),
                                    isLoading = if (currentState.courses.isEmpty()) false else currentState.isLoading
                                )
                            }
                        }
                    )
                }
        }
    }

    private fun observeGrades() {
        viewModelScope.launch {
            hacRepository.gradesDataFlow
                .collectLatest { result: Result<List<CourseGrades>>? ->
                    Log.d("GradesViewModel", "Collected from gradesDataFlow: ${if (result?.isSuccess == true) "Success (${result.getOrNull()?.size} courses)" else "Failure or Null"}")
                    if (result == null) {
                        Log.d("GradesViewModel", "GradesDataFlow emitted null.")
                        _uiState.update { it.copy(isLoading = true, courses = emptyList()) }
                        return@collectLatest
                    }
                    result.fold(
                        onSuccess = { courses: List<CourseGrades> ->
                            _uiState.update { currentState ->
                                currentState.copy(isLoading = false, courses = courses, error = null)
                            }
                            Log.d("GradesViewModel", "Updated uiState with ${courses.size} courses.")
                        },
                        onFailure = { error: Throwable ->
                            Log.e("GradesViewModel", "Error from gradesDataFlow", error)
                            _uiState.update { currentState ->
                                currentState.copy(isLoading = false, error = "Failed to load grades: ${error.message}", courses = emptyList())
                            }
                        }
                    )
                }
        }
    }

    open fun initialLoad(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            Log.d("GradesViewModel", "initialLoad called with forceRefresh: $forceRefresh")

            if (!forceRefresh) {
                val currentGrades = _uiState.value.courses
                val currentMarkingPeriods = _uiState.value.markingPeriods

                // Simplified check: if data exists and no error, assume it's fine for default view
                if (currentGrades.isNotEmpty() && currentMarkingPeriods.isNotEmpty() && _uiState.value.error == null) {
                    Log.d("GradesViewModel", "Initial load: Data already available in UI State for default view.")
                     _uiState.update { currentState ->
                        currentState.copy(isLoading = false) // Data is present
                    }
                    return@launch
                }
            }

            Log.d("GradesViewModel", "Initial load: Proceeding with data fetch. forceRefresh=$forceRefresh.")
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    error = null,
                    courses = if (forceRefresh) emptyList() else currentState.courses,
                    markingPeriods = if (forceRefresh) emptyList() else currentState.markingPeriods
                )
            }
            // No explicitMarkingPeriodValue is passed anymore
            hacRepository.loadMarkingPeriodsAndDefaultGrades(forceRefresh = forceRefresh)
        }
    }

    // selectMarkingPeriod function REMOVED

    open fun refreshData() {
        Log.d("GradesViewModel", "refreshData called. Forcing initialLoad.")
        initialLoad(forceRefresh = true)
    }

    // selectMarkingPeriod function removed
}
