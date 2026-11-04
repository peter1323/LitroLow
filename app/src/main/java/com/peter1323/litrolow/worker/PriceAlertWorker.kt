package com.peter1323.litrolow.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.peter1323.litrolow.LitroLowApplication
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "price_alert_check"
private const val CHECK_INTERVAL_HOURS = 3L

/** Tarea en segundo plano que comprueba las alertas aunque la app esté cerrada. */
class PriceAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    /** Comprueba los precios y avisa de las alertas cumplidas; si falla la red, WorkManager lo reintenta. */
    override suspend fun doWork(): Result {
        val notifier = AlertNotifier(applicationContext)
        if (!notifier.canNotify()) return Result.success()

        val app = applicationContext as LitroLowApplication
        val result = app.alertRepository.check()
        result.triggered.forEach(notifier::show)
        return if (result.error == null) Result.success() else Result.retry()
    }

    companion object {
        /** Programa la comprobación periódica; KEEP evita reiniciar el contador en cada arranque de la app. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(CHECK_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
