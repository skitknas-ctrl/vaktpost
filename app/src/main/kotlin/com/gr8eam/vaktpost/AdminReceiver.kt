package com.gr8eam.vaktpost

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Enhetsadministratören. Den enda rättigheten appen ber om är watch-login, så systemet skickar hit
 * onPasswordFailed varje gång någon skriver fel upplåsningskod (PIN, mönster eller lösenord). Då ber vi
 * WatchService ta ett kort. OBS: fel fingeravtryck/ansikte på låsskärmen rapporteras INTE hit av Android –
 * det fångas i stället av WatchService via skärmtändning (se WatchService).
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        if (!Store.armed(context)) return

        // Räkna försöken; ta kort först när gränsen nåtts (minst 1).
        val fails = Store.fails(context) + 1
        Store.setFails(context, fails)
        if (fails < Store.threshold(context)) return
        Store.setFails(context, 0)

        WatchService.start(context, WatchService.ACTION_FAIL)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        // Rätt kod: nollställ räknaren.
        Store.setFails(context, 0)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Administratören togs bort i systeminställningarna: slå av skyddet så att appen visar rätt status.
        Store.setArmed(context, false)
        WatchService.stop(context)
    }

    companion object {
        fun component(c: Context) = ComponentName(c, AdminReceiver::class.java)
    }
}
