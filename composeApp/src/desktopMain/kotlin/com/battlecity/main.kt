package com.battlecity

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

fun main() = application {
    val state = rememberWindowState(size = DpSize(640.dp, 480.dp))
    Window(onCloseRequest = ::exitApplication, state = state, title = "Battle City") {
        App()
    }
}
