package com.moorthy.statussaver.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.ui.onboarding.OnboardingScreen
import com.moorthy.statussaver.ui.settings.SettingsScreen
import com.moorthy.statussaver.ui.statuses.StatusesScreen
import com.moorthy.statussaver.ui.statuses.StatusesViewModel
import com.moorthy.statussaver.ui.viewer.ViewerScreen

/** Navigation routes (design.md §2). */
object Routes {
    const val ONBOARDING = "onboarding"
    const val STATUSES = "statuses"
    const val VIEWER_TAB = "tab"
    const val VIEWER_INDEX = "index"
    const val VIEWER = "viewer/{$VIEWER_TAB}/{$VIEWER_INDEX}"
    const val SETTINGS = "settings"

    /** The viewer opened at item [index] of the [tab] list. */
    fun viewer(tab: MediaType, index: Int): String = "viewer/${tab.name}/$index"
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
                onOpenItem = { tab, index ->
                    navController.navigate(Routes.viewer(tab, index))
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = {
                    // Pops only the settings screen, even if asked twice.
                    navController.popBackStack(Routes.SETTINGS, inclusive = true)
                },
            )
        }
        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument(Routes.VIEWER_TAB) { type = NavType.StringType },
                navArgument(Routes.VIEWER_INDEX) { type = NavType.IntType },
            ),
        ) { entry ->
            val arguments = requireNotNull(entry.arguments)
            val tab = MediaType.valueOf(requireNotNull(arguments.getString(Routes.VIEWER_TAB)))
            val index = arguments.getInt(Routes.VIEWER_INDEX)
            // The viewer shares the list's ViewModel, so the index means the same item the
            // user tapped (D-023).
            val statusesEntry = remember(entry) { navController.getBackStackEntry(Routes.STATUSES) }
            ViewerScreen(
                tab = tab,
                startIndex = index,
                viewModel = viewModel(statusesEntry, factory = StatusesViewModel.Factory),
                onClose = {
                    // Pops only the viewer, even if asked twice.
                    navController.popBackStack(Routes.VIEWER, inclusive = true)
                },
            )
        }
    }
}
