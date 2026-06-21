package com.gabriion.betterme.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gabriion.betterme.MainActivity
import com.gabriion.betterme.R
import com.gabriion.betterme.data.repository.TipsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MiddayTipWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tipsRepository: TipsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tip = runCatching { tipsRepository.getTodayTips().firstOrNull() }.getOrNull()
            ?: return Result.success()

        val notificationsAllowed = NotificationManagerCompat.from(applicationContext)
            .areNotificationsEnabled()
        if (!notificationsAllowed) return Result.success()

        TipNotificationChannel.ensure(applicationContext)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, TipNotificationChannel.ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("A small nudge for today")
            .setContentText(tip.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tip.message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
