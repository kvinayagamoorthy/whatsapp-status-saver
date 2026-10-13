package com.moorthy.statussaver.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.moorthy.statussaver.ui.onboarding.OnboardingScreen
import com.moorthy.statussaver.ui.statuses.StatusesScreen

/** Navigation routes (design.md §2). The viewer and settings routes arrive with M3 and M5. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val STATUSES = "statuses"
}

@Composable
fun KeepsakeNavHost(
    startViewModel: StartViewModel = viewModel(factory = StartViewModel.Factory),
) {
    // Nothing is drawn for the few milliseconds the stored grants take to read; the
    // window background already matches the theme, so there is no visible flash.
    val startRoute by startViewModel.startRoute.collectAsStateWithLifecycle()
    val resolvedStartRoute = startRoute ?: return

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = resolvedStartRoute) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    // Onboarding leaves the back stack, so Back from the list exits the app.
                    navController.navigate(Routes.STATUSES) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.STATUSES) {
            StatusesScreen(
                onNeedsAccess = {
                    // Back from onboarding then exits instead of returning to a list that
                    // cannot load (D-022).
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.STATUSES) { inclusive = true }
                    }
                },
            )
        }
    }
}
