package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ArenaNotificationHelper {
    const val CHANNEL_ALERTS = "ahxan_arena_alerts"
    const val CHANNEL_MATCHES = "ahxan_arena_matches"
    const val EXTRA_ROUTE = "extra_arena_route"

    private val imgClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Ahxan Arena Broadcasts & Offers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Admin broadcasts, deposit confirmations, and support replies"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }
            val matchesChannel = NotificationChannel(
                CHANNEL_MATCHES,
                "Match Room ID & Prize Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Room ID & Password reveals, prize payouts, and match refunds"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 120, 250)
            }
            nm.createNotificationChannel(alertsChannel)
            nm.createNotificationChannel(matchesChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    suspend fun showShutterNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        imageUrl: String = "",
        route: String = "home",
        isMatchAlert: Boolean = false
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val bannerBitmap: Bitmap? = if (imageUrl.isNotBlank()) {
            fetchBitmap(imageUrl)
        } else {
            null
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, route)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isMatchAlert) CHANNEL_MATCHES else CHANNEL_ALERTS
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setContentIntent(pendingIntent)
            .addAction(0, "Open Now", pendingIntent)

        if (bannerBitmap != null) {
            builder.setLargeIcon(bannerBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bannerBitmap)
                    .setBigContentTitle(title)
                    .setSummaryText(body)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }

    suspend fun sendTestShutterNotification(context: Context) {
        val fallbackBmp = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_banner_bgmi)
        }.getOrNull()

        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, "tournaments")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9091,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ahxan Arena • Cyber Scrims Live!")
            .setContentText("Mobile Shutter Push is active. Tap Open Now to enter the Arena!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setContentIntent(pendingIntent)
            .addAction(0, "Open Now", pendingIntent)

        if (fallbackBmp != null) {
            builder.setLargeIcon(fallbackBmp)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(fallbackBmp)
                    .setBigContentTitle("Ahxan Arena • Cyber Scrims Live!")
                    .setSummaryText("Mobile Shutter Push is active. Tap Open Now to enter the Arena!")
            )
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(9091, builder.build())
        }
    }

    private suspend fun fetchBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).get().build()
            imgClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bytes = resp.body?.bytes() ?: return@withContext null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (_: Exception) {
            null
        }
    }
}
