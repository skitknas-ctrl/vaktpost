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

    fun newPhotoFile(c: Context): File {
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        return File(dir(c), "$ts.jpg")
    }

    /** Alla kort, nyast först. */
    fun photos(c: Context): List<File> =
        (dir(c).listFiles { f -> f.isFile && f.name.endsWith(".jpg") } ?: emptyArray())
            .sortedByDescending { it.name }

    fun deleteAll(c: Context) {
        dir(c).listFiles()?.forEach { it.delete() }
    }

    fun delete(f: File) {
        f.delete()
    }

    /** Läsbar tidsstämpel ur filnamnet (yyyyMMdd-HHmmss-SSS.jpg). */
    fun labelOf(f: File): String {
        return try {
            val d = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).parse(f.nameWithoutExtension)
            SimpleDateFormat("d MMM HH:mm:ss", Locale.getDefault()).format(d ?: Date(f.lastModified()))
        } catch (e: Exception) {
            SimpleDateFormat("d MMM HH:mm:ss", Locale.getDefault()).format(Date(f.lastModified()))
        }
    }
}
