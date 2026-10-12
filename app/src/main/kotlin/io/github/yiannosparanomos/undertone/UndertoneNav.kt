package io.github.yiannosparanomos.undertone

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.yiannosparanomos.undertone.ui.apps.AppDetailScreen
import io.github.yiannosparanomos.undertone.ui.apps.AppsScreen
import io.github.yiannosparanomos.undertone.ui.calls.CallsScreen
import io.github.yiannosparanomos.undertone.ui.health.HealthCheckScreen
import io.github.yiannosparanomos.undertone.ui.held.HeldAndLogScreen
import io.github.yiannosparanomos.undertone.ui.home.HomeScreen
import io.github.yiannosparanomos.undertone.ui.rules.RulesScreen
import io.github.yiannosparanomos.undertone.ui.sounds.SoundsScreen

/**
 * The seven screens of v1 (spec §11), reached from Home.
 *
 * Flat on purpose: principle 1 of the UI direction is that every setting is one tap away. The
 * only nested destination is one app's own rule, which belongs to the app it configures.
 */
object Destinations {
    const val HOME = "home"
    const val APPS = "apps"
    const val APP_DETAIL = "apps/{packageName}"
    const val CALLS = "calls"
    const val RULES = "rules"
    const val HELD = "held"
    const val SOUNDS = "sounds"
    const val HEALTH = "health"

    fun appDetail(packageName: String) = "apps/$packageName"
}

@Composable
fun UndertoneNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) {
            HomeScreen(onOpen = { route -> navController.navigate(route) })
        }
        composable(Destinations.APPS) {
            AppsScreen(
                onBack = { navController.popBackStack() },
                onOpenApp = { packageName -> navController.navigate(Destinations.appDetail(packageName)) },
            )
        }
        composable(Destinations.APP_DETAIL) { entry ->
            AppDetailScreen(
                packageName = entry.arguments?.getString("packageName").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Destinations.CALLS) { CallsScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.RULES) { RulesScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.HELD) { HeldAndLogScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.SOUNDS) { SoundsScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.HEALTH) { HealthCheckScreen(onBack = { navController.popBackStack() }) }
    }
}
