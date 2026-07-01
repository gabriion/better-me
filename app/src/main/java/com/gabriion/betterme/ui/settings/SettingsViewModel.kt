package com.gabriion.betterme.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriion.betterme.health.HealthConnectSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val healthConnectAvailable: Boolean = false,
    val healthConnectReady: Boolean = false,
    val grantedCount: Int = 0,
    val totalPermissionsCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val healthConnect: HealthConnectSource
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    val requiredPermissions: Set<String> get() = healthConnect.requiredReadPermissions
    val providerPackage: String get() = healthConnect.providerPackageName

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val available = healthConnect.isSdkAvailable
            val ready = healthConnect.isReady()
            val granted = healthConnect.grantedCount()
            _state.value = SettingsUiState(
                healthConnectAvailable = available,
                healthConnectReady = ready,
                grantedCount = granted,
                totalPermissionsCount = healthConnect.requiredReadPermissions.size
            )
        }
    }
}
