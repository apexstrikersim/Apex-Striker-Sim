package com.example.ui.tutorial

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

data class TutorialStep(
    val targetId: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

val STREET_TUTORIAL_STEPS = listOf(
    TutorialStep(
        targetId = "nav_home",
        title = "Welcome!",
        description = "This is your Home tab — check here for your latest career updates and events.",
        icon = Icons.Default.Home
    ),
    TutorialStep(
        targetId = "nav_calendar",
        title = "Your Calendar",
        description = "Track upcoming matches and events here, and advance through the months.",
        icon = Icons.Default.RssFeed
    ),
    TutorialStep(
        targetId = "nav_club",
        title = "Your Squad",
        description = "View your current team and player info here.",
        icon = Icons.Default.Shield
    )
)

val PRO_TUTORIAL_STEPS = listOf(
    TutorialStep(
        targetId = "nav_league",
        title = "Competitions",
        description = "Follow league standings and cup competitions from here.",
        icon = Icons.Default.Public
    ),
    TutorialStep(
        targetId = "nav_trophies",
        title = "Trophy Cabinet",
        description = "Every trophy you win throughout your career is tracked here.",
        icon = Icons.Default.EmojiEvents
    )
)
