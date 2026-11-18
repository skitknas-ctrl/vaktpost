package com.gr8eam.vaktpost

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kvarvarande förgrundstjänst som bevakar telefonen medan skyddet är på. Den gör två saker:
 *
 *  1. FEL KOD (åtgärd FAIL, skickas av AdminReceiver): tar en kort serie "fail"-kort. Dessa sparas alltid.
 *
 *  2. SKÄRMEN TÄNDS medan telefonen är låst (ACTION_SCREEN_ON + låst keyguard): tar ETT "wake"-kort.
 *     Det är enda sättet att fånga fel fingeravtryck/ansikte, eftersom Android inte berättar för appar
 *     om misslyckade biometriska försök på låsskärmen. När telefonen sedan låses upp (ACTION_USER_PRESENT)
 *     raderas "wake"-kort som är yngre än Store.WAKE_KEEP_MS – så försvinner dina egna selfies, medan en
 *     inkräktare som inte lyckas låsa upp finns kvar på bild.
 *
 * Tjänsten är redan en aktiv kamera-förgrundstjänst, vilket gör att den får öppna kameran vid skärmtändning
 * utan att starta en ny tjänst från bakgrunden (annars blockerat på Android 14+).
 */
class WatchService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private val capturing = AtomicBoolean(false)
    private var lastWake = 0L
    private var registered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> onScreenOn()
                Intent.ACTION_USER_PRESENT -> Store.pruneRecentWake(this@WatchService)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!registered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        if (intent?.action == ACTION_FAIL) fireFail()
        return START_STICKY
    }

    override fun onDestroy() {
        if (registered) { try { unregisterReceiver(screenReceiver) } catch (e: Exception) {} ; registered = false }
        super.onDestroy()
    }

    // ---- skärmtändning medan låst -> ett "wake"-kort --------------------------------------------
    private fun onScreenOn() {
        if (!Store.armed(this) || !Store.screenTrigger(this)) return
        if (!hasCamera()) return
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isKeyguardLocked) return                     // olåst = du, inget kort
        val now = System.currentTimeMillis()
        if (now - lastWake < DEBOUNCE_MS) return             // undvik upprepade kort vid samma väckning
        if (!capturing.compareAndSet(false, true)) return    // en kamera i taget
        lastWake = now
        CameraSnap(this).capture { bytes ->
            saveBytes(bytes, Store.KIND_WAKE)
            capturing.set(false)
        }
    }

    // ---- fel kod -> serie "fail"-kort ------------------------------------------------------------
    private fun fireFail() {
        if (!hasCamera()) return
        if (!capturing.compareAndSet(false, true)) return
        val saved = ArrayList<File>()
        var remaining = Store.burst(this).coerceIn(1, 3)
        fun next() {
            if (remaining <= 0) {
                capturing.set(false)
                maybeMail(saved)
                return
            }
            remaining--
            CameraSnap(this).capture { bytes ->
                saveBytes(bytes, Store.KIND_FAIL)?.let { saved.add(it) }
                main.postDelayed({ next() }, 450)            // kort paus så kameran hinner återställa sig
            }
        }
        next()
    }

    private fun maybeMail(saved: List<File>) {
        if (Store.mailOn(this) && saved.isNotEmpty() &&
            Store.mailHost(this).isNotEmpty() && Store.mailTo(this).isNotEmpty()
        ) {
            val files = ArrayList(saved)
            Thread { try { Mailer.send(this, files) } catch (e: Exception) {} }.start()
        }
    }

    private fun saveBytes(bytes: ByteArray?, kind: String): File? {
        if (bytes == null) return null
        return try {
            val f = Store.newPhotoFile(this, kind)
            f.writeBytes(bytes)
            f
        } catch (e: Exception) { null }
    }

    private fun hasCamera() = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

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
            .setContentText(getString(R.string.watch_running))
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
        private const val NOTE_ID = 43
        private const val DEBOUNCE_MS = 8_000L
        const val ACTION_FAIL = "com.gr8eam.vaktpost.FAIL"

        /** Starta/försäkra att vakttjänsten kör (kräver att skyddet är påslaget). */
        fun start(c: Context, action: String? = null) {
            val i = Intent(c, WatchService::class.java)
            if (action != null) i.action = action
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i)
                else c.startService(i)
            } catch (e: Exception) {
                // Android 14+ kan neka start av en kamera-förgrundstjänst från bakgrunden (t.ex. efter omstart).
                // Skyddet slås då på igen nästa gång appen öppnas.
            }
        }

        fun stop(c: Context) {
            try { c.stopService(Intent(c, WatchService::class.java)) } catch (e: Exception) {}
        }
    }
}
