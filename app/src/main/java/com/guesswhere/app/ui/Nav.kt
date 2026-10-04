package com.guesswhere.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.guesswhere.app.data.LocalStore
import com.guesswhere.app.game.GameViewModel
import com.guesswhere.app.ui.screens.GameScreen
import com.guesswhere.app.ui.screens.HomeScreen
import com.guesswhere.app.ui.screens.LeaderboardScreen

object Routes {
    const val HOME = "home"
    const val GAME = "game"
    const val LEADERBOARD = "leaderboard"
}

@Composable
fun GuessWhereRoot(gameViewModel: GameViewModel, store: LocalStore) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInVertically(tween(260)) { it / 5 } + fadeIn(tween(260))
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = {
            slideOutVertically(tween(260)) { it / 5 } + fadeOut(tween(220))
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                store = store,
                onPlay = { config ->
                    gameViewModel.start(config)
                    navController.navigate(Routes.GAME) { launchSingleTop = true }
                },
                onOpenLeaderboard = {
                    navController.navigate(Routes.LEADERBOARD) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.GAME) {
            GameScreen(
                gameViewModel = gameViewModel,
                onExit = {
                    gameViewModel.reset()
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(
                store = store,
                onBack = { navController.popBackStack() },
            )
        }
    }
}