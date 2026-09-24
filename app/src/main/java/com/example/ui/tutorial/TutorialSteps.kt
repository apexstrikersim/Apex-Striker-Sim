package com.example.ui.tutorial

data class TutorialStep(
    val targetId: String,
    val title: String,
    val description: String
)

val STREET_TUTORIAL_STEPS = listOf(
    TutorialStep(
        targetId = "nav_home",
        title = "Welcome!",
        description = "This is your Home tab — check here for your latest career updates and events."
    ),
    TutorialStep(
        targetId = "nav_calendar",
        title = "Your Calendar",
        description = "Track upcoming matches and events here, and advance through the months."
    ),
    TutorialStep(
        targetId = "nav_club",
        title = "Your Squad",
        description = "View your current team and player info here."
    )
)

val PRO_TUTORIAL_STEPS = listOf(
    TutorialStep(
        targetId = "nav_league",
        title = "Competitions",
        description = "Follow league standings and cup competitions from here."
    ),
    TutorialStep(
        targetId = "nav_trophies",
        title = "Trophy Cabinet",
        description = "Every trophy you win throughout your career is tracked here."
    )
)
