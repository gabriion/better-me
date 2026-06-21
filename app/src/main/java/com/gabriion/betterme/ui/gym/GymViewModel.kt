package com.gabriion.betterme.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriion.betterme.core.db.TrainingPlanEntity
import com.gabriion.betterme.core.db.WorkoutSetEntity
import com.gabriion.betterme.data.repository.GymRepository
import com.gabriion.betterme.domain.gym.BuiltWorkoutDay
import com.gabriion.betterme.domain.gym.PlanBuilder
import com.gabriion.betterme.domain.model.Exercise
import com.gabriion.betterme.domain.model.WorkoutTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GymUiState(
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val plan: TrainingPlanEntity? = null,
    val builtDays: List<BuiltWorkoutDay> = emptyList(),
    val recentSets: List<WorkoutSetEntity> = emptyList(),
)

@HiltViewModel
class GymViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {

    private val exercises = repository.exercises()
    private val templates = repository.templates()

    private val planFlow = repository.observePlan()
    private val setsFlow = repository.observeRecentSets()

    val state: StateFlow<GymUiState> = combine(planFlow, setsFlow) { plan, sets ->
        val days = plan?.let { p ->
            val template = templates.firstOrNull { it.id == p.templateId }
                ?: templates.firstOrNull()
            if (template == null) emptyList() else PlanBuilder.build(template, exercises, p.level)
        } ?: emptyList()
        GymUiState(
            exercises = exercises,
            templates = templates,
            plan = plan,
            builtDays = days,
            recentSets = sets,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = GymUiState(exercises = exercises, templates = templates),
    )

    fun savePlan(
        templateId: String,
        daysPerWeek: Int,
        level: String,
        targetGroups: Set<String>,
    ) {
        val entity = TrainingPlanEntity(
            id = 0,
            daysPerWeek = daysPerWeek,
            targetGroupsCsv = targetGroups.sorted().joinToString(","),
            level = level,
            templateId = templateId,
        )
        viewModelScope.launch { repository.savePlan(entity) }
    }

    fun logSet(exercise: Exercise, reps: Int, weightKg: Double?) {
        val now = System.currentTimeMillis()
        val nextIndex = state.value.recentSets
            .count { it.exerciseId == exercise.id && it.date >= startOfDay(now) } + 1
        val entity = WorkoutSetEntity(
            date = now,
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            setIndex = nextIndex,
            reps = reps,
            weightKg = weightKg,
        )
        viewModelScope.launch { repository.logSet(entity) }
    }

    fun deleteSet(id: Long) {
        viewModelScope.launch { repository.deleteSet(id) }
    }

    private fun startOfDay(epochMs: Long): Long {
        val day = 24L * 60L * 60L * 1000L
        return (epochMs / day) * day
    }
}
