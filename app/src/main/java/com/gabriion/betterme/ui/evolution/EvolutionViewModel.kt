package com.gabriion.betterme.ui.evolution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriion.betterme.core.db.WeightEntity
import com.gabriion.betterme.data.repository.EvolutionRepository
import com.gabriion.betterme.domain.model.Highlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TimeWindow(val days: Int, val label: String) {
    M1(30, "1M"),
    M3(90, "3M"),
    M6(180, "6M"),
    Y1(365, "1Y")
}

data class EvolutionUiState(
    val window: TimeWindow = TimeWindow.M3,
    val weights: List<WeightEntity> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val latestKg: Double? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EvolutionViewModel @Inject constructor(
    private val repo: EvolutionRepository
) : ViewModel() {

    private val window = MutableStateFlow(TimeWindow.M3)

    val state: StateFlow<EvolutionUiState> = window
        .flatMapLatest { w ->
            combine(
                repo.observeWeightsSince(w.days),
                repo.observeWeights(),
                repo.observeHighlights()
            ) { windowed, all, highlights ->
                EvolutionUiState(
                    window = w,
                    weights = windowed,
                    highlights = highlights,
                    latestKg = all.maxByOrNull { it.date }?.kg
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EvolutionUiState())

    fun setWindow(w: TimeWindow) {
        window.value = w
    }

    fun addWeight(date: LocalDate, kg: Double, note: String?) {
        viewModelScope.launch { repo.addWeight(date, kg, note) }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch { repo.deleteWeight(id) }
    }
}
