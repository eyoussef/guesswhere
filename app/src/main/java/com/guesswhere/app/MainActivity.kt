package com.guesswhere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.guesswhere.app.game.GameViewModel
import com.guesswhere.app.ui.GuessWhereRoot
import com.guesswhere.app.ui.theme.GuessWhereTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as GuessWhereApp).container
        val gameViewModel: GameViewModel by viewModels {
            GameViewModel.factory(container.repository, container.localStore)
        }

        setContent {
            GuessWhereTheme {
                GuessWhereRoot(
                    gameViewModel = gameViewModel,
                    store = container.localStore,
                )
            }
        }
    }
}