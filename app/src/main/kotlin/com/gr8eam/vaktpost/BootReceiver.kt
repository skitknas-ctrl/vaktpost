package com.gr8eam.vaktpost

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Enhetsadministratören överlever en omstart av sig själv, men räknaren för misslyckade försök nollställs
 * så att gränsen "N försök i rad" börjar om efter varje omstart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Store.setFails(context, 0)
        }
    }
}
