package com.gabriion.betterme.ui.calories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriion.betterme.core.db.FoodCacheEntity
import com.gabriion.betterme.core.db.FoodEntryEntity
import com.gabriion.betterme.data.prefs.CaloriesPrefs
import com.gabriion.betterme.data.repository.CaloriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CaloriesUiState(
    val date: LocalDate = LocalDate.now(),
    val dailyTarget: Int = CaloriesPrefs.DEFAULT_TARGET,
    val totalKcal: Double = 0.0,
    val entries: List<FoodEntryEntity> = emptyList(),
    val searchResults: List<FoodCacheEntity> = emptyList(),
    val favourites: List<FoodCacheEntity> = emptyList(),
    val recent: List<FoodCacheEntity> = emptyList(),
    val query: String = "",
    val isSearching: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CaloriesViewModel @Inject constructor(
    private val repo: CaloriesRepository,
    private val prefs: CaloriesPrefs
) : ViewModel() {

    private val dateFlow = MutableStateFlow(LocalDate.now())
    private val queryFlow = MutableStateFlow("")
    private val searchResultsFlow = MutableStateFlow<List<FoodCacheEntity>>(emptyList())
    private val isSearchingFlow = MutableStateFlow(false)

    private val entriesFlow = dateFlow.flatMapLatest { repo.observeEntriesFor(it) }
    private val totalFlow = dateFlow.flatMapLatest { repo.observeTotalKcal(it) }

    private data class CoreState(
        val date: LocalDate,
        val target: Int,
        val totalKcal: Double,
        val entries: List<FoodEntryEntity>
    )

    private data class ExtraState(
        val favourites: List<FoodCacheEntity>,
        val recent: List<FoodCacheEntity>,
        val query: String,
        val searchResults: List<FoodCacheEntity>,
        val isSearching: Boolean
    )

    val state: StateFlow<CaloriesUiState> = combine(
        combine(dateFlow, prefs.dailyTargetFlow, totalFlow, entriesFlow) { d, t, kcal, entries ->
            CoreState(d, t, kcal, entries)
        },
        combine(
            repo.observeFavourites(),
            repo.observeRecent(),
            queryFlow,
            searchResultsFlow,
            isSearchingFlow
        ) { favs, rec, q, results, searching ->
            ExtraState(favs, rec, q, results, searching)
        }
    ) { core, extras ->
        CaloriesUiState(
            date = core.date,
            dailyTarget = core.target,
            totalKcal = core.totalKcal,
            entries = core.entries,
            searchResults = extras.searchResults,
            favourites = extras.favourites,
            recent = extras.recent,
            query = extras.query,
            isSearching = extras.isSearching
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaloriesUiState())

    fun goPreviousDay() = dateFlow.update { it.minusDays(1) }
    fun goNextDay() = dateFlow.update { it.plusDays(1) }
    fun goToday() = dateFlow.update { LocalDate.now() }

    fun onQueryChange(q: String) {
        queryFlow.value = q
        if (q.isBlank()) {
            searchResultsFlow.value = emptyList()
            isSearchingFlow.value = false
            return
        }
        viewModelScope.launch {
            isSearchingFlow.value = true
            val results = runCatching { repo.search(q) }.getOrDefault(emptyList())
            if (queryFlow.value == q) {
                searchResultsFlow.value = results
            }
            isSearchingFlow.value = false
        }
    }

    fun logEntry(cache: FoodCacheEntity, grams: Double, slot: String) {
        viewModelScope.launch { repo.logEntry(cache, grams, slot, dateFlow.value) }
    }

    fun logManual(name: String, kcal: Double, grams: Double, slot: String) {
        viewModelScope.launch { repo.logManual(name, kcal, grams, slot, dateFlow.value) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun toggleFavourite(foodId: String, fav: Boolean) {
        viewModelScope.launch { repo.toggleFavourite(foodId, fav) }
    }

    fun setDailyTarget(value: Int) {
        viewModelScope.launch { prefs.setDailyTarget(value) }
    }
}
