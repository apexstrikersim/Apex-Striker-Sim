package com.example.data

/**
 * Single source of truth for the app's displayed version number.
 * This MUST always equal the current AppDatabase @Database(version = ...) value.
 * Bump this at the same time you bump the Room schema version — do not let them drift again.
 */
object AppVersion {
    const val CURRENT = 27
}
