package com.gr8eam.vaktpost

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

/** Visar ett kort i full storlek med knappar för att dela eller radera det. */
class ViewerActivity : AppCompatActivity() {

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("path")
        val file = if (path != null) File(path) else null
        if (file == null || !file.exists()) { finish(); return }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#13161F"))
            setPadding(dp(12), dp(16), dp(12), dp(16))
        }
        setContentView(root)

        root.addView(TextView(this).apply {
            text = Store.labelOf(file)
            setTextColor(Color.parseColor("#ECEFF4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, 0, 0, dp(10))
        })

        val img = ImageView(this).apply {
            adjustViewBounds = true
            setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        }
        root.addView(img, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, 0) }
        root.addView(btnRow)

        btnRow.addView(button("Dela") { share(file) }, rowLp())
        btnRow.addView(button("Radera") { confirmDelete(file) }, rowLp())
    }

    private fun rowLp() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4), 0, dp(4), 0) }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextColor(Color.parseColor("#ECEFF4"))
        setBackgroundColor(Color.parseColor("#1E2330"))
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setOnClickListener { onClick() }
    }

    private fun share(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Dela foto"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Kunde inte dela", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(file: File) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Radera fotot?")
            .setPositiveButton("Radera") { _, _ -> Store.delete(file); finish() }
            .setNegativeButton("Avbryt", null)
            .show()
    }
}
