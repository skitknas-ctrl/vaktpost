package com.gr8eam.vaktpost

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Enhetsadministratören. Den enda rättigheten appen ber om är watch-login, så systemet skickar hit
 * onPasswordFailed varje gång någon skriver fel upplåsningskod. Då startas CaptureService som tar ett kort.
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        if (!Store.armed(context)) return

        // Räkna försöken; ta kort först när gränsen nåtts (minst 1).
        val fails = Store.fails(context) + 1
        Store.setFails(context, fails)
        if (fails < Store.threshold(context)) return
        Store.setFails(context, 0)

        val svc = Intent(context, CaptureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
        else context.startService(svc)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        // Rätt kod: nollställ räknaren.
        Store.setFails(context, 0)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Administratören togs bort i systeminställningarna: slå av skyddet så att appen visar rätt status.
        Store.setArmed(context, false)
    }

    companion object {
        fun component(c: Context) = ComponentName(c, AdminReceiver::class.java)
    }
}
