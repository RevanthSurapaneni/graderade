package com.revvs9.grader.ui

import android.content.Context // Added import
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext // Added import
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.revvs9.grader.HacParser
import com.revvs9.grader.HacRepository
import com.revvs9.grader.NetworkModule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.revvs9.grader.ui.CourseDetailsScreen // Added this import

// Destinations
object AppDestinations {
    const val LOGIN_ROUTE = "login"
    const val HOME_ROUTE = "home"
    const val CALENDAR_ROUTE = "calendar"
    const val SETTINGS_ROUTE = "settings"
    const val COURSE_DETAILS_ROUTE = "course_details" // Base route name
    const val COURSE_DETAILS_ARG_COURSE_NAME = "courseName" // Argument key
    val COURSE_DETAILS_FULL_ROUTE = "$COURSE_DETAILS_ROUTE/{$COURSE_DETAILS_ARG_COURSE_NAME}" // Full route with argument
}

// Bottom Navigation Items
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val BottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, AppDestinations.HOME_ROUTE),
    BottomNavItem("Calendar", Icons.Filled.DateRange, AppDestinations.CALENDAR_ROUTE),
    BottomNavItem("Settings", Icons.Filled.Settings, AppDestinations.SETTINGS_ROUTE)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = remember(currentDestination) {
        currentDestination?.route != AppDestinations.LOGIN_ROUTE
    }

    // Get application context
    val appContext = LocalContext.current.applicationContext

    // ViewModels will now get the singleton HacRepository via their factories
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(appContext))
    val gradesViewModel: GradesViewModel = viewModel(factory = GradesViewModelFactory(appContext))
    // CourseDetailsViewModel will be created within the NavHost composable for its specific route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.LOGIN_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.LOGIN_ROUTE) {
                LoginScreen(
                    loginViewModel = loginViewModel,
                    onLoginSuccess = {
                        navController.navigate(AppDestinations.HOME_ROUTE) {
                            popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestinations.HOME_ROUTE) {
                HomeScreen(
                    gradesViewModel = gradesViewModel,
                    onNavigateToCourseDetails = { courseName ->
                        // URL encode courseName to handle special characters safely
                        val encodedCourseName = java.net.URLEncoder.encode(courseName, "UTF-8")
                        navController.navigate("${AppDestinations.COURSE_DETAILS_ROUTE}/$encodedCourseName")
                    }
                )
            }
            composable(AppDestinations.CALENDAR_ROUTE) {
                CalendarScreen()
            }
            composable(AppDestinations.SETTINGS_ROUTE) {
                SettingsScreen()
            }
            // Add composable for COURSE_DETAILS_ROUTE
            composable(
                route = AppDestinations.COURSE_DETAILS_FULL_ROUTE, // Use the constant
                arguments = listOf(navArgument(AppDestinations.COURSE_DETAILS_ARG_COURSE_NAME) { type = NavType.StringType })
            ) { backStackEntry ->
                val courseNameArg = backStackEntry.arguments?.getString(AppDestinations.COURSE_DETAILS_ARG_COURSE_NAME)
                val decodedCourseName = URLDecoder.decode(courseNameArg, StandardCharsets.UTF_8.toString())
                
                val courseDetailsViewModel: CourseDetailsViewModel = viewModel(
                    // Corrected: Pass decodedCourseName and appContext to the factory
                    factory = CourseDetailsViewModelFactory(decodedCourseName, appContext)
                )
                CourseDetailsScreen(
                    viewModel = courseDetailsViewModel, // Pass the ViewModel
                    // courseName = decodedCourseName, // Removed as ViewModel now handles the title
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        BottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// Basic ViewModel Factories (Consider Hilt/Koin for a real app)
class LoginViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory { // Changed to context
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Use NetworkModule.hacApiService directly
            return LoginViewModel(HacRepository.getInstance(NetworkModule.hacApiService, HacParser)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class GradesViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory { // Changed to context
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GradesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Use NetworkModule.hacApiService directly
            return GradesViewModel(HacRepository.getInstance(NetworkModule.hacApiService, HacParser)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
