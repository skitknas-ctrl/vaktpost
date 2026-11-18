package com.gr8eam.vaktpost

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Inställningar (SharedPreferences) och lagring av de tagna korten. Allt ligger i appens privata lagring
 * (filesDir/shots), så inga andra appar kommer åt bilderna och de syns inte i telefonens galleri.
 */
object Store {
    private const val PREFS = "vaktpost"

    private fun p(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Är skyddet påslaget i appen? (Kräver dessutom att enhetsadministratören är aktiverad i systemet.)
    fun armed(c: Context) = p(c).getBoolean("armed", false)
    fun setArmed(c: Context, v: Boolean) = p(c).edit().putBoolean("armed", v).apply()

    // Hur många misslyckade försök i rad innan ett kort tas (1 = varje försök).
    fun threshold(c: Context) = p(c).getInt("threshold", 1).coerceIn(1, 5)
    fun setThreshold(c: Context, v: Int) = p(c).edit().putInt("threshold", v.coerceIn(1, 5)).apply()

    // Antal kort per utlösning.
    fun burst(c: Context) = p(c).getInt("burst", 1).coerceIn(1, 3)
    fun setBurst(c: Context, v: Int) = p(c).edit().putInt("burst", v.coerceIn(1, 3)).apply()

    // Ta även ett kort när skärmen tänds medan telefonen är låst. Detta är enda sättet att fånga fel
    // fingeravtryck/ansikte, eftersom Android inte berättar för appar om misslyckade biometriska försök på
    // låsskärmen. Kortet taggas "wake-" och raderas automatiskt om du själv låser upp inom 30 sekunder.
    fun screenTrigger(c: Context) = p(c).getBoolean("screenTrigger", true)
    fun setScreenTrigger(c: Context, v: Boolean) = p(c).edit().putBoolean("screenTrigger", v).apply()

    // Hur länge efter ett "wake"-kort en lyckad upplåsning raderar det (din egen selfie).
    const val WAKE_KEEP_MS = 30_000L

    // Valfri e-post. Skickas bara om host och mottagare är ifyllda.
    fun mailOn(c: Context) = p(c).getBoolean("mailOn", false)
    fun setMailOn(c: Context, v: Boolean) = p(c).edit().putBoolean("mailOn", v).apply()
    fun mailHost(c: Context): String = p(c).getString("mailHost", "") ?: ""
    fun mailPort(c: Context): Int = p(c).getInt("mailPort", 587)
    fun mailUser(c: Context): String = p(c).getString("mailUser", "") ?: ""
    fun mailPass(c: Context): String = p(c).getString("mailPass", "") ?: ""
    fun mailTo(c: Context): String = p(c).getString("mailTo", "") ?: ""
    fun saveMail(c: Context, host: String, port: Int, user: String, pass: String, to: String) =
        p(c).edit()
            .putString("mailHost", host.trim())
            .putInt("mailPort", if (port in 1..65535) port else 587)
            .putString("mailUser", user.trim())
            .putString("mailPass", pass)
            .putString("mailTo", to.trim())
            .apply()

    // Räknare för misslyckade försök sedan senaste lyckade upplåsning.
    fun fails(c: Context) = p(c).getInt("fails", 0)
    fun setFails(c: Context, v: Int) = p(c).edit().putInt("fails", v).apply()

    // ---- bilder ----
    private fun dir(c: Context): File = File(c.filesDir, "shots").apply { mkdirs() }

    // Kortets ursprung kodas i filnamnet: "fail-" (fel kod / testkort, sparas alltid) eller
    // "wake-" (skärmen tändes medan låst, raderas vid egen upplåsning inom WAKE_KEEP_MS).
    const val KIND_FAIL = "fail"
    const val KIND_WAKE = "wake"

    fun newPhotoFile(c: Context, kind: String = KIND_FAIL): File {
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        return File(dir(c), "$kind-$ts.jpg")
    }

    /** Alla kort, nyast först. */
    fun photos(c: Context): List<File> =
        (dir(c).listFiles { f -> f.isFile && f.name.endsWith(".jpg") } ?: emptyArray())
            .sortedByDescending { it.name.substringAfter('-', it.name) }

    /**
     * Raderar "wake"-kort som togs för mindre än WAKE_KEEP_MS sedan. Anropas när telefonen låses upp:
     * vaknade skärmen av dig själv försvinner din egen selfie, men kort från en inkräktare som inte låser
     * upp (eller "fail"-kort från fel kod) rörs aldrig.
     */
    fun pruneRecentWake(c: Context) {
        val now = System.currentTimeMillis()
        dir(c).listFiles { f -> f.isFile && f.name.startsWith("$KIND_WAKE-") }?.forEach { f ->
            if (now - f.lastModified() <= WAKE_KEEP_MS) f.delete()
        }
    }

    fun deleteAll(c: Context) {
        dir(c).listFiles()?.forEach { it.delete() }
    }

    fun delete(f: File) {
        f.delete()
    }

    /** Läsbar tidsstämpel ur filnamnet ([kind-]yyyyMMdd-HHmmss-SSS.jpg). */
    fun labelOf(f: File): String {
        val stamp = f.nameWithoutExtension.let {
            if (it.startsWith("$KIND_FAIL-") || it.startsWith("$KIND_WAKE-")) it.substring(5) else it
        }
        return try {
            val d = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).parse(stamp)
            SimpleDateFormat("d MMM HH:mm:ss", Locale.getDefault()).format(d ?: Date(f.lastModified()))
        } catch (e: Exception) {
            SimpleDateFormat("d MMM HH:mm:ss", Locale.getDefault()).format(Date(f.lastModified()))
        }
    }
}
