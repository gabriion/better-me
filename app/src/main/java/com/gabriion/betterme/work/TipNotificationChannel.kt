package com.gabriion.betterme.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

object TipNotificationChannel {
    const val ID = "tips_midday"
    private const val NAME = "Daily Tips"
    private const val DESCRIPTION = "A small midday nudge based on your day so far."

    /** Idempotent: safe to call from every worker run. */
    fun ensure(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(ID) != null) return
        val channel = NotificationChannel(
            ID,
            NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }
}
