package com.gabriion.betterme.ui.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriion.betterme.data.repository.MealsRepository
import com.gabriion.betterme.domain.meals.MealPlan
import com.gabriion.betterme.domain.meals.MealPlanEngine
import com.gabriion.betterme.domain.meals.ShoppingItem
import com.gabriion.betterme.domain.meals.ShoppingList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class PlanSpan(val days: Int, val label: String) {
    TODAY(1, "Today"),
    WEEK(7, "This week"),
}

data class MealsUiState(
    val ingredients: List<String> = emptyList(),
    val selected: Set<String> = emptySet(),
    val span: PlanSpan = PlanSpan.TODAY,
    val plan: MealPlan? = null,
    val shoppingList: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class MealsViewModel @Inject constructor(
    private val mealsRepository: MealsRepository,
) : ViewModel() {

    private val engine = MealPlanEngine(baseSeed = 1_000L)

    private val _state = MutableStateFlow(MealsUiState())
    val state: StateFlow<MealsUiState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(ingredients = mealsRepository.allIngredients())
    }

    fun toggleIngredient(name: String) {
        val current = _state.value.selected
        val next = if (name in current) current - name else current + name
        _state.value = _state.value.copy(selected = next)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selected = emptySet())
    }

    fun setSpan(span: PlanSpan) {
        _state.value = _state.value.copy(span = span)
    }

    fun generatePlan() {
        val snapshot = _state.value
        _state.value = snapshot.copy(isLoading = true)
        viewModelScope.launch {
            val plan = withContext(Dispatchers.Default) {
                engine.build(
                    allMeals = mealsRepository.allMeals(),
                    selectedIngredients = snapshot.selected,
                    days = snapshot.span.days,
                )
            }
            val shopping = ShoppingList.fromPlan(plan)
            _state.value = snapshot.copy(
                plan = plan,
                shoppingList = shopping,
                isLoading = false,
            )
        }
    }
}
