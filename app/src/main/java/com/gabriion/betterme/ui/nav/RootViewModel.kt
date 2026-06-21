package com.gabriion.betterme.ui.nav

import androidx.lifecycle.ViewModel
import com.gabriion.betterme.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val user: UserRepository
) : ViewModel() {

    fun startDestination(): String =
        if (user.isOnboardedBlocking()) Dest.Home.route else Dest.Onboarding.route
}
