package com.tick.app.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tick.app.notify.TickNotifications
import java.util.concurrent.TimeUnit

class UpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!UpdateConfig.isConfigured) return Result.success()
        val check = UpdateChecker.check()
        check.onSuccess { info ->
            if (info != null && info.versionCode > UpdateChecker.currentVersionCode(applicationContext)) {
                TickNotifications.showUpdateAvailable(applicationContext, info.versionName)
            }
        }
        return if (check.isSuccess) Result.success() else Result.retry()
    }
}

object UpdateScheduler {
    private const val WORK_NAME = "tick_update_check"

    fun setEnabled(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (!enabled || !UpdateConfig.isConfigured) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        manager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
