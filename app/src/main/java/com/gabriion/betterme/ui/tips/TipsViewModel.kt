package com.gabriion.betterme.ui.tips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gabriion.betterme.data.prefs.TipsPrefs
import com.gabriion.betterme.data.repository.TipsRepository
import com.gabriion.betterme.domain.tips.ComposedTip
import com.gabriion.betterme.work.MiddayTipScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TipsUiState(
    val tips: List<ComposedTip> = emptyList(),
    val loading: Boolean = true,
    val notificationsEnabled: Boolean = false
)

@HiltViewModel
class TipsViewModel @Inject constructor(
    app: Application,
    private val repository: TipsRepository,
    private val prefs: TipsPrefs,
    private val scheduler: MiddayTipScheduler
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(TipsUiState())
    val state: StateFlow<TipsUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            prefs.notifyEnabledFlow.collect { enabled ->
                _state.update { it.copy(notificationsEnabled = enabled) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val tips = repository.getTodayTips()
            _state.update { it.copy(tips = tips, loading = false) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotifyEnabled(enabled)
            val ctx = getApplication<Application>()
            if (enabled) scheduler.schedule(ctx) else scheduler.cancel(ctx)
        }
    }
}
