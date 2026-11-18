package com.gr8eam.vaktpost

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Enhetsadministratören överlever en omstart av sig själv, men räknaren för misslyckade försök nollställs
 * så att gränsen "N försök i rad" börjar om efter varje omstart. Om skyddet var påslaget startas även
 * WatchService igen (vissa tillverkare kan neka detta från bakgrunden efter omstart; då räcker det att
 * öppna appen en gång).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Store.setFails(context, 0)
            if (Store.armed(context)) WatchService.start(context)
        }
    }
}
