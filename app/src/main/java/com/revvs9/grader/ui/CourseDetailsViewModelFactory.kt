package com.revvs9.grader.ui

import android.content.Context // Keep this if other parts of the factory might need it in the future
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import com.revvs9.grader.HacParser
import com.revvs9.grader.HacRepository
import com.revvs9.grader.NetworkModule

class CourseDetailsViewModelFactory(
    private val courseName: String,
    // Context is no longer strictly needed here for HacApiService if NetworkModule.hacApiService is used directly
    // However, keeping it in case other context-dependent initializations are needed in the future.
    // If not, it can be removed from constructor and call site.
    @Suppress("UNUSED_PARAMETER") private val context: Context 
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Use NetworkModule.hacApiService directly
            return CourseDetailsViewModel(
                HacRepository.getInstance(NetworkModule.hacApiService, HacParser),
                SavedStateHandle(mapOf(CourseDetailsViewModel.COURSE_NAME_KEY to courseName))
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
