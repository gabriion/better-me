package com.gabriion.betterme.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriion.betterme.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val user: UserRepository
) : ViewModel() {

    fun completeOnboarding(firstName: String) {
        viewModelScope.launch {
            user.setFirstName(firstName)
            user.setOnboarded(true)
        }
    }
}
