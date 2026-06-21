package com.gabriion.betterme.domain.model

/**
 * Lightweight summary card surfaced on the Evolution screen.
 *
 * @param icon symbolic key resolved by the UI to a concrete icon (e.g. "scale", "streak",
 *             "wave"). Unknown keys fall back to a sparkle icon.
 */
data class Highlight(
    val icon: String,
    val title: String,
    val body: String
)
