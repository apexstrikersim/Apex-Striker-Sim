package com.example.data

import android.content.Context

data class SlotMetadata(
    val slotId: Int,
    val hasData: Boolean = false,
    val playerName: String = "",
    val generation: Int = 1,
    val ovr: Int = 60,
    val clubName: String = "",
    val lastPlayedTimestamp: Long = 0L
)

class SaveSlotManager(context: Context) {
    private val prefs = context.getSharedPreferences("save_slots", Context.MODE_PRIVATE)

    fun getLastActiveSlot(): Int {
        return prefs.getInt("last_active_slot", 0)
    }

    fun setLastActiveSlot(slotId: Int) {
        prefs.edit().putInt("last_active_slot", slotId).apply()
    }

    fun getSlotMetadata(slotId: Int): SlotMetadata {
        val hasData = prefs.getBoolean("slot_${slotId}_has_data", false)
        if (!hasData) return SlotMetadata(slotId = slotId, hasData = false)
        val name = prefs.getString("slot_${slotId}_name", "") ?: ""
        val gen = prefs.getInt("slot_${slotId}_gen", 1)
        val ovr = prefs.getInt("slot_${slotId}_ovr", 60)
        val club = prefs.getString("slot_${slotId}_club", "") ?: ""
        val timestamp = prefs.getLong("slot_${slotId}_timestamp", 0L)
        return SlotMetadata(
            slotId = slotId,
            hasData = true,
            playerName = name,
            generation = gen,
            ovr = ovr,
            clubName = club,
            lastPlayedTimestamp = timestamp
        )
    }

    fun updateSlotMetadata(slotId: Int, playerName: String, generation: Int, ovr: Int, clubName: String) {
        prefs.edit()
            .putBoolean("slot_${slotId}_has_data", true)
            .putString("slot_${slotId}_name", playerName)
            .putInt("slot_${slotId}_gen", generation)
            .putInt("slot_${slotId}_ovr", ovr)
            .putString("slot_${slotId}_club", clubName)
            .putLong("slot_${slotId}_timestamp", System.currentTimeMillis())
            .apply()
    }

    fun clearSlot(slotId: Int) {
        prefs.edit()
            .remove("slot_${slotId}_has_data")
            .remove("slot_${slotId}_name")
            .remove("slot_${slotId}_gen")
            .remove("slot_${slotId}_ovr")
            .remove("slot_${slotId}_club")
            .remove("slot_${slotId}_timestamp")
            .apply()
    }

    fun migrateLegacyDatabaseIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("save_slots", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("legacy_db_migrated", false)) {
            val oldDb = context.getDatabasePath("football_career_simulator_db")
            if (oldDb.exists()) {
                val newDb = context.getDatabasePath("apex_career_slot_1.db")
                try {
                    oldDb.renameTo(newDb)
                    val oldWal = context.getDatabasePath("football_career_simulator_db-wal")
                    if (oldWal.exists()) {
                        oldWal.renameTo(context.getDatabasePath("apex_career_slot_1.db-wal"))
                    }
                    val oldShm = context.getDatabasePath("football_career_simulator_db-shm")
                    if (oldShm.exists()) {
                        oldShm.renameTo(context.getDatabasePath("apex_career_slot_1.db-shm"))
                    }
                    updateSlotMetadata(
                        slotId = 1,
                        playerName = "Migrated Career",
                        generation = 1,
                        ovr = 70,
                        clubName = "Active Club"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            prefs.edit().putBoolean("legacy_db_migrated", true).apply()
        }
    }
}
