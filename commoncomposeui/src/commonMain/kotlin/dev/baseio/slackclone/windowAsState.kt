package dev.baseio.slackclone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
internal expect fun rememberComposeWindow(): State<WindowInfo>