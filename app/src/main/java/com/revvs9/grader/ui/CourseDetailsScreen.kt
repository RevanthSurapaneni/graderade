package com.revvs9.grader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revvs9.grader.model.Assignment
import com.revvs9.grader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(
    viewModel: CourseDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    GraderTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.courseTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = AppPrimaryText
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AppIconColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppDarkBackground,
                        titleContentColor = AppPrimaryText,
                        navigationIconContentColor = AppIconColor
                    )
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
                uiState.overallCourseScore?.let { score ->
                    OverallGradeDisplay(grade = score, courseName = uiState.courseTitle)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when {
                    uiState.isLoading && uiState.assessmentOfLearningAssignments.isEmpty() && uiState.otherAssignments.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 50.dp),
                            color = AppPrimaryText
                        )
                    }
                    uiState.errorMessage != null -> {
                        Text(
                            text = uiState.errorMessage ?: "An unknown error occurred.",
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    uiState.assessmentOfLearningAssignments.isEmpty() && uiState.otherAssignments.isEmpty() -> {
                        Text(
                            text = "No assignments found for this course.",
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = AppSecondaryText
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.assessmentOfLearningAssignments.isNotEmpty()) {
                                item {
                                    AssignmentSectionHeader("Assessment of Learning")
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(
                                    items = uiState.assessmentOfLearningAssignments,
                                    key = { assignment -> assignment.name + assignment.category } // Added key, combining name and category for uniqueness
                                ) { assignment ->
                                    AssignmentCard(assignment = assignment)
                                    Spacer(modifier = Modifier.height(8.dp)) // Space between cards
                                }
                            }

                            if (uiState.otherAssignments.isNotEmpty()) {
                                item {
                                    if (uiState.assessmentOfLearningAssignments.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp)) // More space between sections
                                    }
                                    AssignmentSectionHeader("Other Assignments")
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(
                                    items = uiState.otherAssignments,
                                    key = { assignment -> assignment.name + assignment.category } // Added key
                                ) { assignment ->
                                    AssignmentCard(assignment = assignment)
                                    Spacer(modifier = Modifier.height(8.dp)) // Space between cards
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverallGradeDisplay(grade: String, courseName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Overall Grade: $grade",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = getGradeColor(grade = grade)
        )
    }
}


@Composable
fun AssignmentSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        color = AppPrimaryText
    )
}

@Composable
fun AssignmentCard(assignment: Assignment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppCardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = assignment.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppPrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Category: ${assignment.category}",
                    fontSize = 12.sp,
                    color = AppSecondaryText
                )
                Text(
                    text = "Due: ${assignment.dateDue ?: "N/A"}",
                    fontSize = 12.sp,
                    color = AppSecondaryText
                )
            }
            val score = assignment.score ?: "N/A"
            val scoreColor = getGradeColor(grade = score)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(scoreColor.copy(alpha = 0.20f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = score,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun getGradeColor(grade: String): Color {
    val numericGrade = grade.replace("%", "").toDoubleOrNull()
    return when {
        numericGrade == null -> AppSecondaryText.copy(alpha = 0.8f) // Theme color for N/A
        numericGrade >= 90 -> ScoreGood
        numericGrade >= 80 -> ScoreOkay
        numericGrade >= 70 -> ScoreWarning // Use ScoreWarning for 70-79 range
        numericGrade >= 60 -> ScoreWarning // Also use ScoreWarning for 60-69 range, or define a new color if needed
        else -> ScoreLow
    }
}
