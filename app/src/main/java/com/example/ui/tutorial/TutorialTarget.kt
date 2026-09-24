package com.example.ui.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot

// Global registry of tutorial target IDs to their current on-screen 
// bounds (in root coordinates). Updated live as composables 
// reposition/recompose. Read by TutorialOverlay to know where to cut 
// out the spotlight window.
object TutorialTargetRegistry {
    val bounds = mutableStateMapOf<String, Rect>()
}

fun Modifier.tutorialTarget(id: String): Modifier = this.onGloballyPositioned { coordinates ->
    TutorialTargetRegistry.bounds[id] = coordinates.boundsInRoot()
}
