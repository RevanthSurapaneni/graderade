package com.revvs9.grader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.revvs9.grader.model.CourseGrades
import com.revvs9.grader.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.revvs9.grader.HacRepository
import com.revvs9.grader.NetworkModule
import com.revvs9.grader.HacParser
import androidx.compose.ui.text.style.TextAlign

// Helper function to clean up course names
fun getCleanCourseName(fullCourseName: String): String {
    val parts = fullCourseName.split(" - ", limit = 2)
    if (parts.size == 2) {
        return parts[1].replaceFirst(Regex("^\\d+\\s*"), "").trim()
    }
    return fullCourseName.trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    gradesViewModel: GradesViewModel,
    onNavigateToCourseDetails: (String) -> Unit
) {
    val uiState by gradesViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        gradesViewModel.initialLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Courses", color = AppPrimaryText) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppDarkBackground,
                    titleContentColor = AppPrimaryText,
                    actionIconContentColor = AppIconColor
                ),
                actions = {
                    IconButton(onClick = { gradesViewModel.refreshData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Grades")
                    }
                }
            )
        },
        containerColor = AppDarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp)) // Keep spacer for layout

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AppPrimaryText)
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    // Simplified retry logic
                                    gradesViewModel.initialLoad(forceRefresh = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppIconColor)
                            ) {
                                Text("Retry", color = AppDarkBackground)
                            }
                        }
                    }
                    // Updated conditions for empty courses
                    uiState.courses.isEmpty() && !uiState.isLoading && uiState.error == null -> {
                        Text(
                            "No courses found. Try refreshing.", // Simplified message
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            color = AppPrimaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            itemsIndexed(
                                uiState.courses,
                                key = { index, course -> // Key no longer includes marking period
                                    "${course.courseName}_${course.overallScore ?: "NOGRADE"}_$index"
                                }
                            ) { index, course ->
                                CourseItem(course = course, onClick = { onNavigateToCourseDetails(course.courseName) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreBadge(score: String?) {
    val scoreValue = score?.replace("%", "")?.toDoubleOrNull()
    val backgroundColor = when {
        scoreValue == null -> ScoreUnspecified
        scoreValue >= 90 -> ScoreGood
        scoreValue >= 80 -> ScoreOkay
        scoreValue >= 70 -> ScoreWarning
        else -> ScoreLow
    }
    val scoreDisplay = scoreValue?.let { String.format("%.2f", it) } ?: score ?: "N/A"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = scoreDisplay,
            color = ScoreText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            fontSize = 13.sp
        )
    }
}

@Composable
fun CourseItem(course: CourseGrades, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardBackground),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = getCleanCourseName(course.courseName),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                color = AppPrimaryText,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 2
            )
            ScoreBadge(score = course.overallScore)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1C20)
@Composable
fun HomeScreenPreview() {
    val previewCourses = listOf(
        CourseGrades("CATE03742B - 13 PLTW Intro Engr Des S2", "98.60", emptyList()),
        CourseGrades("CATE24300 - 2 Dollars & Sense", "100.00", emptyList()),
        CourseGrades("ELA13300B - 5 AP English Language S2", "90.19", emptyList())
    )
    // Marking periods removed from preview state
    val mockUiState = GradesUiState(
        isLoading = false,
        error = null,
        courses = previewCourses
        // markingPeriods and selectedMarkingPeriodValue removed
    )
    
    val mockViewModel = MockGradesViewModel(
        mockUiState, 
        HacRepository.getInstance(NetworkModule.hacApiService, HacParser)
    )

    GraderTheme {
        HomeScreen(gradesViewModel = mockViewModel, onNavigateToCourseDetails = {})
    }
}

// Define MockGradesViewModel for preview purposes
class MockGradesViewModel(
    private val initialStateForMock: GradesUiState, // Renamed to avoid conflict
    hacRepository: HacRepository
) : GradesViewModel(hacRepository) {
    private val _mockUiState = MutableStateFlow(initialStateForMock)
    override val uiState: StateFlow<GradesUiState> = _mockUiState.asStateFlow()

    override fun initialLoad(forceRefresh: Boolean) {
        _mockUiState.update { it.copy(isLoading = true) }
        // Simulate a delay or just directly set the state using the passed initial state
        _mockUiState.update { 
            initialStateForMock.copy(isLoading = false) // Use the renamed initialStateForMock
        }
    }

    override fun refreshData() {
        initialLoad(forceRefresh = true)
    }
    // selectMarkingPeriod is removed from the base ViewModel
}
