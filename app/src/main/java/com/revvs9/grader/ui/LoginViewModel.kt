package com.revvs9.grader.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revvs9.grader.HacRepository
import com.revvs9.grader.model.CourseGrades
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val loginSuccess: Boolean = false
)

class LoginViewModel(private val hacRepository: HacRepository) : ViewModel() {    var username by mutableStateOf("")
    var password by mutableStateOf("")

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login() {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(loginError = "Username and password cannot be empty.")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            try {
                val loginResult = hacRepository.login(username, password)
                // Match the exact string returned by HacRepository on success
                if (loginResult == "Success") { 
                    _uiState.value = LoginUiState(loginSuccess = true)
                } else {
                    _uiState.value = LoginUiState(loginError = loginResult)
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState(loginError = "Login failed: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(loginError = null)
    }
}
