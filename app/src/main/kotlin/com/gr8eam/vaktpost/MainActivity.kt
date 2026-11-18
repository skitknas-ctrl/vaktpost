package com.gr8eam.vaktpost

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Enda huvudskärmen. Visar status (kamera, administratör, skydd), knappar för att slå på skyddet, ta ett
 * testkort och öppna inställningar, samt ett galleri med de kort som tagits vid misslyckade upplåsningsförsök.
 * Hela gränssnittet byggs i kod, så appen har inga layout-XML-filer.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var armBtn: Button
    private lateinit var camBtn: Button
    private lateinit var adminBtn: Button
    private lateinit var gallery: LinearLayout
    private lateinit var galleryHead: TextView

    private val dpm by lazy { getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }

    private val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val adminFlow = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askNotif.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    // ---------------------------------------------------------------- UI
    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#13161F")) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "Vaktpost"
            setTextColor(Color.parseColor("#ECEFF4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Tar ett foto med framkameran när någon skriver fel upplåsningskod."
            setTextColor(Color.parseColor("#95A0B5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(4), 0, dp(18))
        })

        status = TextView(this).apply {
            setTextColor(Color.parseColor("#ECEFF4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.parseColor("#1E2330"))
        }
        root.addView(status, lp(top = 0, bottom = 16))

        armBtn = primaryButton("") { toggleArmed() }
        root.addView(armBtn, lp())

        camBtn = plainButton("") {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) askCamera.launch(android.Manifest.permission.CAMERA)
        }
        root.addView(camBtn, lp())

        adminBtn = plainButton("") {
            if (dpm.isAdminActive(AdminReceiver.component(this))) openAdminSettings()
            else requestAdmin()
        }
        root.addView(adminBtn, lp())

        root.addView(plainButton("Ta ett testkort nu") { testShot() }, lp())
        root.addView(plainButton("Inställningar") { startActivity(Intent(this, SettingsActivity::class.java)) }, lp())

        galleryHead = TextView(this).apply {
            setTextColor(Color.parseColor("#ECEFF4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(10))
        }
        root.addView(galleryHead)

        gallery = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(gallery)

        root.addView(plainButton("Radera alla bilder") { confirmClear() }, lp(top = 16))

        return scroll
    }

    private fun lp(top: Int = 10, bottom: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, dp(top), 0, dp(bottom)) }

    private fun primaryButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextColor(Color.parseColor("#0C1A16"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setBackgroundColor(Color.parseColor("#49C0A8"))
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setOnClickListener { onClick() }
    }

    private fun plainButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextColor(Color.parseColor("#ECEFF4"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setBackgroundColor(Color.parseColor("#1E2330"))
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setOnClickListener { onClick() }
    }

    // ---------------------------------------------------------------- status
    private fun refresh() {
        val cam = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val admin = dpm.isAdminActive(AdminReceiver.component(this))
        val armed = Store.armed(this) && cam && admin

        val sb = StringBuilder()
        sb.append(if (armed) "● Skyddet är PÅ\n" else "○ Skyddet är av\n")
        sb.append(if (cam) "✓ Kamera-tillstånd\n" else "✗ Kamera-tillstånd saknas\n")
        sb.append(if (admin) "✓ Enhetsadministratör aktiv" else "✗ Enhetsadministratör ej aktiv")
        status.text = sb.toString()
        status.setTextColor(Color.parseColor(if (armed) "#49C0A8" else "#ECEFF4"))

        armBtn.text = if (Store.armed(this)) "Stäng av skyddet" else "Slå på skyddet"
        camBtn.text = if (cam) "Kamera-tillstånd: beviljat ✓" else "Ge kamera-tillstånd"
        camBtn.isEnabled = !cam
        adminBtn.text = if (admin) "Enhetsadministratör: aktiv ✓ (tryck för att ändra)" else "Aktivera enhetsadministratör"

        refreshGallery()
    }

    private fun refreshGallery() {
        gallery.removeAllViews()
        val shots = Store.photos(this)
        galleryHead.text = if (shots.isEmpty()) "Inga foton än" else "Foton (${shots.size})"
        val cols = 3
        var row: LinearLayout? = null
        shots.forEachIndexed { i, f ->
            if (i % cols == 0) {
                row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                gallery.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(6)) })
            }
            row!!.addView(thumbCell(f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        // fyll ut sista raden så att kolumnerna håller bredden
        row?.let { r -> val rest = cols - (shots.size % cols); if (rest in 1 until cols) repeat(rest) { r.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) } }
    }

    private fun thumbCell(f: File): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val img = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(decodeThumb(f, dp(110)))
            setBackgroundColor(Color.parseColor("#1E2330"))
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ViewerActivity::class.java).putExtra("path", f.absolutePath))
            }
        }
        box.addView(img, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(110)))
        box.addView(TextView(this).apply {
            text = Store.labelOf(f)
            setTextColor(Color.parseColor("#95A0B5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, 0)
        })
        return box
    }

    private fun decodeThumb(f: File, target: Int): android.graphics.Bitmap? {
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, o)
            var s = 1
            while (o.outWidth / s > target * 2 || o.outHeight / s > target * 2) s *= 2
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = s })
        } catch (e: Exception) { null }
    }

    // ---------------------------------------------------------------- åtgärder
    private fun toggleArmed() {
        if (Store.armed(this)) { Store.setArmed(this, false); refresh(); return }
        // slå på: se till att kamera och administratör finns, annars guida dit
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            toast("Ge först kamera-tillstånd"); askCamera.launch(android.Manifest.permission.CAMERA); return
        }
        if (!dpm.isAdminActive(AdminReceiver.component(this))) {
            toast("Aktivera först enhetsadministratör"); requestAdmin(); return
        }
        Store.setArmed(this, true); Store.setFails(this, 0); refresh()
        toast("Skyddet är på")
    }

    private fun requestAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(this@MainActivity))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Vaktpost behöver detta för att få veta när någon skriver fel upplåsningskod. Appen får inga andra rättigheter över telefonen."
            )
        }
        adminFlow.launch(intent)
    }

    private fun openAdminSettings() {
        try { startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)) } catch (e: Exception) {}
    }

    private fun testShot() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            toast("Ge först kamera-tillstånd"); askCamera.launch(android.Manifest.permission.CAMERA); return
        }
        val svc = Intent(this, CaptureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)
        toast("Tar ett testkort …")
        status.postDelayed({ refresh() }, 2500)
    }

    private fun confirmClear() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Radera alla bilder?")
            .setMessage("Alla sparade foton tas bort permanent.")
            .setPositiveButton("Radera") { _, _ -> Store.deleteAll(this); refresh() }
            .setNegativeButton("Avbryt", null)
            .show()
    }

    private fun toast(s: String) = android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show()
}
