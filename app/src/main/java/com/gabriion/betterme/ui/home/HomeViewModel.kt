package com.gabriion.betterme.ui.home

import androidx.lifecycle.ViewModel
import com.gabriion.betterme.data.repository.ContentRepository
import com.gabriion.betterme.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class HomeUiState(
    val firstName: String = "friend",
    val quote: String = "",
    val heroAsset: String = "concept_art/zen.webp"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val content: ContentRepository,
    private val user: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        _state.value = HomeUiState(
            firstName = user.firstNameOrDefault(),
            quote = content.quoteOfTheDay(),
            heroAsset = content.heroAssetOfTheDay()
        )
    }
}
