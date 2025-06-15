package com.revvs9.grader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.revvs9.grader.ui.theme.GraderTheme

@Composable
fun CalendarScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Calendar Screen (Placeholder)")
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings Screen (Placeholder)")
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    GraderTheme {
        CalendarScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    GraderTheme {
        SettingsScreen()
    }
}
