package com.peter1323.litrolow.worker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.peter1323.litrolow.MainActivity
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.repository.TriggeredAlert
import java.util.Locale

private const val CHANNEL_ID = "price_alerts"

/** Lanza las notificaciones de las alertas de precio. */
class AlertNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    /** Indica si el usuario permite notificaciones; sin permiso no tiene sentido comprobar precios. */
    fun canNotify(): Boolean = manager.areNotificationsEnabled()

    /** Muestra el aviso de una alerta cumplida; al pulsarlo se abre la app. */
    // El permiso ya se comprueba en canNotify antes de llegar aquí.
    @SuppressLint("MissingPermission")
    fun show(triggered: TriggeredAlert) {
        createChannel()
        val alert = triggered.alert
        val price = String.format(
            Locale.forLanguageTag("es-ES"),
            context.getString(R.string.price_format),
            triggered.price,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.alert_notification_title, alert.stationName))
            .setContentText(context.getString(alert.fuel.label) + ": " + price)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        manager.notify(alert.id.toInt(), notification)
    }

    /** Registra el canal de notificaciones, obligatorio desde Android 8; repetirlo no tiene efecto. */
    private fun createChannel() {
        val name = context.getString(R.string.alert_channel_name)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    /** Crea la acción que abre la pantalla principal de la app. */
    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
