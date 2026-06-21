package com.gabriion.betterme.domain.tips

import com.gabriion.betterme.garmin.GarminSnapshot
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class TipTemplate(
    val kind: String,
    val priority: Int,
    val messages: List<String>
)

data class ComposedTip(
    val kind: String,
    val message: String,
    val priority: Int
)

/**
 * Resolves the rule-based [TipEngine] output against a content pack of
 * message templates, interpolating snapshot values and rotating message
 * variants deterministically by date.
 */
class TipComposer(private val templates: List<TipTemplate>) {

    private val byKind: Map<String, TipTemplate> = templates.associateBy { it.kind }

    fun compose(
        snapshot: GarminSnapshot?,
        rhr7dAvg: Int?,
        today: LocalDate
    ): List<ComposedTip> {
        val raw = TipEngine().generate(snapshot, rhr7dAvg)
        val epochDay = today.toEpochDay()

        // TipEngine emits kind="default" twice when there is no snapshot/signal.
        // Translate that into the mindfulness + hydration defaults pack.
        val defaultsOnly = raw.isNotEmpty() && raw.all { it.kind == "default" }
        val kinds: List<String> = if (defaultsOnly) {
            listOf("hydration", "mindfulness")
        } else {
            raw.map { it.kind }.distinct()
        }

        return kinds.mapNotNull { kind ->
            val template = byKind[kind] ?: return@mapNotNull null
            if (template.messages.isEmpty()) return@mapNotNull null
            val idx = pickVariant(epochDay, template.messages.size)
            val message = interpolate(template.messages[idx], snapshot)
            ComposedTip(kind = kind, message = message, priority = template.priority)
        }.sortedByDescending { it.priority }
    }

    private fun pickVariant(epochDay: Long, size: Int): Int {
        val m = (epochDay % size).toInt()
        return if (m < 0) m + size else m
    }

    private fun interpolate(template: String, snapshot: GarminSnapshot?): String {
        if (!template.contains('{')) return template
        var out = template
        snapshot?.sleepHours?.let { out = out.replace("{hours}", "%.1f".format(it)) }
        snapshot?.steps?.let { out = out.replace("{steps}", it.toString()) }
        snapshot?.restingHeartRate?.let { out = out.replace("{rhr}", it.toString()) }
        snapshot?.hrv?.let { out = out.replace("{hrv}", it.toString()) }
        snapshot?.stressAvg?.let { out = out.replace("{stress}", it.toString()) }
        snapshot?.lastActivityHrAvg?.let { out = out.replace("{activityHr}", it.toString()) }
        val activity = snapshot?.lastActivityType ?: "last activity"
        out = out.replace("{activity}", activity)
        return out
    }
}
