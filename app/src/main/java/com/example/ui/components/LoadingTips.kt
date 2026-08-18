package com.example.ui.components

object LoadingTips {
    private val tips = listOf(
        "Tip: Players trained on a stat that matches their position develop faster.",
        "Tip: You can only train once per in-game month — plan your focus stat ahead of time.",
        "Tip: Club reputation affects which transfer offers you'll receive.",
        "Tip: Keeping your manager's trust high reduces your risk of losing your starting spot.",
        "Tip: European competition brackets reset each season — check standings after every matchday.",
        "Tip: Rival relationships can affect morale swings during head-to-head matches.",
        "Tip: Fan reputation grows fastest after high-profile match performances.",
        "Tip: Youth academy crests inherit their identity from the parent club.",
        "Tip: A new manager often means a reset relationship — a strong first impression matters."
    )

    // Remembers the index the last session started on (process-lifetime only,
    // not persisted to disk), so consecutive loads don't open on the same tip.
    private var lastSessionStartIndex: Int = -1

    /** Returns a shuffled order of tip indices for one loading session. */
    fun newSessionOrder(): List<Int> {
        var shuffled = tips.indices.shuffled()
        if (shuffled.isNotEmpty() && shuffled.first() == lastSessionStartIndex) {
            // avoid repeating the same opening tip twice in a row
            shuffled = shuffled.drop(1) + shuffled.first()
        }
        lastSessionStartIndex = shuffled.firstOrNull() ?: -1
        return shuffled
    }

    fun tipAt(index: Int): String = tips[index]
}
