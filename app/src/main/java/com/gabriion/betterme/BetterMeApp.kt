package com.gabriion.betterme

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gabriion.betterme.data.prefs.TipsPrefs
import com.gabriion.betterme.work.MiddayTipScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BetterMeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var tipsPrefs: TipsPrefs

    @Inject
    lateinit var middayScheduler: MiddayTipScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Restore the midday-tip schedule if the user previously enabled it.
        appScope.launch {
            runCatching {
                if (tipsPrefs.notifyEnabledFlow.first()) {
                    middayScheduler.schedule(this@BetterMeApp)
                }
            }
        }
    }
}
