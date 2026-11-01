package com.meditation.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.meditation.tracker.ui.screens.history.HistoryScreen
import com.meditation.tracker.ui.screens.home.HomeScreen
import com.meditation.tracker.ui.screens.interval.IntervalConfigScreen
import com.meditation.tracker.ui.screens.longduration.LongDurationConfigScreen
import com.meditation.tracker.ui.screens.session.SessionScreen
import com.meditation.tracker.ui.screens.settings.SettingsScreen

@Composable
fun MeditationNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToInterval = { navController.navigate(Screen.IntervalConfig.route) },
                onNavigateToLongDuration = { navController.navigate(Screen.LongDurationConfig.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.IntervalConfig.route) {
            IntervalConfigScreen(
                onBack = { navController.popBackStack() },
                onStartSession = { duration, step ->
                    navController.navigate(Screen.Session.createRoute("INTERVAL", duration, step))
                }
            )
        }

        composable(Screen.LongDurationConfig.route) {
            LongDurationConfigScreen(
                onBack = { navController.popBackStack() },
                onStartSession = { duration, milestoneStep ->
                    navController.navigate(Screen.Session.createRoute("LONG_DURATION", duration, milestoneStep))
                }
            )
        }

        composable(
            route = Screen.Session.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("durationSeconds") { type = NavType.IntType },
                navArgument("intervalStepSeconds") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "LONG_DURATION"
            val durationSeconds = backStackEntry.arguments?.getInt("durationSeconds") ?: 600
            val intervalStepSeconds = backStackEntry.arguments?.getInt("intervalStepSeconds") ?: 0

            SessionScreen(
                modeString = mode,
                targetDurationSeconds = durationSeconds,
                intervalStepSeconds = intervalStepSeconds,
                onSessionFinished = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onCancel = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
