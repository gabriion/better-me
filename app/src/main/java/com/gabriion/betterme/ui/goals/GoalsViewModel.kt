package com.gabriion.betterme.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriion.betterme.data.repository.GoalTemplate
import com.gabriion.betterme.data.repository.GoalsRepository
import com.gabriion.betterme.domain.goals.GoalProgressEngine
import com.gabriion.betterme.domain.goals.GoalSummary
import com.gabriion.betterme.domain.model.Goal
import com.gabriion.betterme.domain.model.GoalType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GoalWithProgress(
    val goal: Goal,
    val total: Double,
    val summary: GoalSummary
)

data class GoalsUiState(
    val goals: List<GoalWithProgress> = emptyList(),
    val templates: List<GoalTemplate> = emptyList(),
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repo: GoalsRepository
) : ViewModel() {

    private val templates = MutableStateFlow<List<GoalTemplate>>(emptyList())

    val state: StateFlow<GoalsUiState> = repo.activeGoals()
        .flatMapLatest { goals ->
            if (goals.isEmpty()) {
                flowOf(emptyList<GoalWithProgress>())
            } else {
                val totalsFlows = goals.map { goal ->
                    repo.totalProgress(goal.id)
                }
                combine(totalsFlows) { totals ->
                    goals.mapIndexed { idx, goal ->
                        val total = totals[idx]
                        GoalWithProgress(
                            goal = goal,
                            total = total,
                            summary = GoalProgressEngine.summary(goal, total, LocalDate.now())
                        )
                    }
                }
            }
        }
        .combine(templates) { goals, tpl ->
            GoalsUiState(goals = goals, templates = tpl, loading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            templates.value = repo.loadTemplates()
        }
    }

    fun addGoal(
        title: String,
        type: GoalType,
        target: Double,
        unit: String,
        deadline: LocalDate?,
        isSportTracked: Boolean
    ) {
        viewModelScope.launch {
            repo.addGoal(
                Goal(
                    title = title,
                    type = type,
                    target = target,
                    unit = unit,
                    deadline = deadline ?: defaultDeadline(),
                    createdAt = LocalDate.now(),
                    archived = false,
                    isSportTracked = isSportTracked
                )
            )
        }
    }

    fun addFromTemplate(template: GoalTemplate) {
        addGoal(
            title = template.title,
            type = template.goalType(),
            target = template.target,
            unit = template.unit,
            deadline = defaultDeadline(),
            isSportTracked = template.isSportTracked
        )
    }

    fun logProgress(goalId: Long, amount: Double, note: String? = null) {
        viewModelScope.launch { repo.logProgress(goalId, amount, note) }
    }

    fun archive(goalId: Long) {
        viewModelScope.launch { repo.archiveGoal(goalId) }
    }

    fun eventsFor(goalId: Long) = repo.eventsForGoal(goalId)

    private fun defaultDeadline(): LocalDate =
        LocalDate.of(LocalDate.now().year, 12, 31)
}
