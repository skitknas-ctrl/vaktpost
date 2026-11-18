package com.gr8eam.vaktpost

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Kortlivad förgrundstjänst som tar ett eller flera kort med framkameran och sedan stänger sig själv.
 * Android kräver att en förgrundstjänst visar en avisering; den hålls så diskret som möjligt och försvinner
 * när kortet är taget. Tjänsten startas av AdminReceiver vid misslyckad upplåsning.
 */
class CaptureService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private var remaining = 0
    private val saved = ArrayList<File>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopNow(); return START_NOT_STICKY
        }

        remaining = Store.burst(this).coerceIn(1, 3)
        shootNext()
        return START_NOT_STICKY
    }

    private fun shootNext() {
        if (remaining <= 0) { afterAll(); return }
        remaining--
        CameraSnap(this).capture { bytes ->
            if (bytes != null) {
                try {
                    val f = Store.newPhotoFile(this)
                    f.writeBytes(bytes)
                    saved.add(f)
                } catch (e: Exception) {}
            }
            // nästa kort efter en kort paus (låter kameran återställa sig)
            main.postDelayed({ shootNext() }, 450)
        }
    }

    private fun afterAll() {
        // Valfri e-post körs på en egen tråd; tjänsten stängs oavsett hur det går.
        if (Store.mailOn(this) && saved.isNotEmpty() && Store.mailHost(this).isNotEmpty() && Store.mailTo(this).isNotEmpty()) {
            val files = ArrayList(saved)
            Thread {
                try { Mailer.send(this, files) } catch (e: Exception) {}
                main.post { stopNow() }
            }.start()
        } else {
            stopNow()
        }
    }

    private fun stopNow() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "Vaktpost", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            mgr.createNotificationChannel(ch)
        }
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTE_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTE_ID, n)
        }
    }

    companion object {
        private const val CHANNEL = "vaktpost"
        private const val NOTE_ID = 42
    }
}
