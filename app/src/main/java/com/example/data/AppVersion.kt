package com.example.data

/**
 * Single source of truth for the app's version and migration number.
 * This MUST always equal the current AppDatabase @Database(version = ...) value.
 */
object AppVersion {
    const val CURRENT = 2
    const val DISPLAY = "DEMO"
}

