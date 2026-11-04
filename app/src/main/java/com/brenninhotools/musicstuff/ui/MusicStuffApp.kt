package com.brenninhotools.musicstuff.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.brenninhotools.musicstuff.ui.about.AboutScreen
import com.brenninhotools.musicstuff.ui.converter.ConverterScreen
import com.brenninhotools.musicstuff.ui.menu.MenuScreen

private enum class Screen { MENU, CONVERTER, ABOUT }

private const val TRANSITION_MILLIS = 220

@Composable
fun MusicStuffApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.MENU) }
    val goToMenu = { screen = Screen.MENU }

    BackHandler(enabled = screen != Screen.MENU, onBack = goToMenu)

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                val forward = targetState != Screen.MENU
                val direction = if (forward) 1 else -1
                val spec = tween<IntOffset>(TRANSITION_MILLIS)
                (slideInHorizontally(spec) { it / 3 * direction } + fadeIn(tween(TRANSITION_MILLIS)))
                    .togetherWith(slideOutHorizontally(spec) { -it / 3 * direction } + fadeOut(tween(TRANSITION_MILLIS)))
            },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.MENU -> MenuScreen(
                    onOpenConverter = { screen = Screen.CONVERTER },
                    onOpenAbout = { screen = Screen.ABOUT },
                )
                Screen.CONVERTER -> ConverterScreen(onBack = goToMenu)
                Screen.ABOUT -> AboutScreen(onBack = goToMenu)
            }
        }
    }
}
