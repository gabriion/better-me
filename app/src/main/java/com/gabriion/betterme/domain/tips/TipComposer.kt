package com.gabriion.betterme.domain.tips

import com.gabriion.betterme.health.AppSignals
import com.gabriion.betterme.health.HealthSnapshot
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
 * message templates, interpolating snapshot/signal values and rotating
 * message variants deterministically by date.
 */
class TipComposer(private val templates: List<TipTemplate>) {

    private val byKind: Map<String, TipTemplate> = templates.associateBy { it.kind }

    fun compose(
        snapshot: HealthSnapshot?,
        signals: AppSignals = AppSignals.EMPTY,
        rhr7dAvg: Int? = null,
        today: LocalDate = LocalDate.now()
    ): List<ComposedTip> {
        val raw = TipEngine().generate(snapshot, signals, rhr7dAvg)
        val epochDay = today.toEpochDay()

        return raw.mapNotNull { tip ->
            val template = byKind[tip.kind] ?: return@mapNotNull null
            if (template.messages.isEmpty()) return@mapNotNull null
            val idx = pickVariant(epochDay, template.messages.size)
            val message = interpolate(template.messages[idx], snapshot, signals)
            ComposedTip(kind = tip.kind, message = message, priority = template.priority)
        }.sortedByDescending { it.priority }
    }

    private fun pickVariant(epochDay: Long, size: Int): Int {
        val m = (epochDay % size).toInt()
        return if (m < 0) m + size else m
    }

    private fun interpolate(template: String, snapshot: HealthSnapshot?, signals: AppSignals): String {
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

        signals.daysSinceLastWorkout?.let { out = out.replace("{daysSinceWorkout}", it.toString()) }
        out = out.replace("{streak}", signals.workoutStreakDays.toString())
        signals.weightTrendKgPerWeek?.let {
            val abs = kotlin.math.abs(it)
            out = out.replace("{trendKg}", "%.1f".format(abs))
        }
        signals.daysSinceLastWeighIn?.let { out = out.replace("{daysSinceWeighIn}", it.toString()) }
        return out
    }
}
