package com.dcapps.spese.core.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dcapps.spese.MainActivity
import com.dcapps.spese.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mostra la notifica di sistema "aggiornamento disponibile" (usata dal worker periodico, anche ad
 * app chiusa). Al tap apre [MainActivity] con l'extra [EXTRA_OPEN_UPDATE], così l'app va diretta
 * sulla schermata di aggiornamento senza mostrare anche il dialog (sarebbe un doppione).
 */
@Singleton
class UpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** True se possiamo mostrare notifiche (canale abilitato + permesso runtime su Android 13+). */
    fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notifyUpdate(version: String) {
        ensureChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UPDATE, true)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(context.getString(R.string.update_title))
            .setContentText(context.getString(R.string.update_notification_text, version))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .build()

        // Guardia: su Android 13+ notify richiede il permesso; se non c'è, canNotify() è già
        // false a monte, ma il linter vuole comunque il check esplicito prima di notify().
        if (!canNotify()) return
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.update_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.update_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        /** Extra su MainActivity: se true, l'app apre direttamente la schermata di aggiornamento. */
        const val EXTRA_OPEN_UPDATE = "open_update"

        private const val CHANNEL_ID = "updates"
        private const val NOTIFICATION_ID = 4001
    }
}
