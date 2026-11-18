package com.gr8eam.vaktpost

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Inställningar: hur många misslyckade försök i rad innan ett kort tas, antal kort per utlösning och den
 * valfria e-postsändningen (SMTP). Byggs i kod, sparas i Store (appens privata lagring).
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var threshold: EditText
    private lateinit var burst: EditText
    private lateinit var mailOn: EditText
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var user: EditText
    private lateinit var pass: EditText
    private lateinit var to: EditText

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#13161F")) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        head(root, "Inställningar", 24f, true)

        label(root, "Antal fel i rad innan ett kort tas (1–5)")
        threshold = field(root, Store.threshold(this).toString(), InputType.TYPE_CLASS_NUMBER)

        label(root, "Antal kort per utlösning (1–3)")
        burst = field(root, Store.burst(this).toString(), InputType.TYPE_CLASS_NUMBER)

        head(root, "Skicka korten via e-post (valfritt)", 17f, true)
        label(root, "Lämna tomt för att bara spara korten i appen. För Gmail: host smtp.gmail.com, port 587, och ett app-lösenord (inte ditt vanliga lösenord).")

        label(root, "Skicka e-post? Skriv ja eller nej")
        mailOn = field(root, if (Store.mailOn(this)) "ja" else "nej", InputType.TYPE_CLASS_TEXT)

        label(root, "SMTP-server (host)")
        host = field(root, Store.mailHost(this), InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_CLASS_TEXT)

        label(root, "Port (oftast 587, eller 465)")
        port = field(root, Store.mailPort(this).toString(), InputType.TYPE_CLASS_NUMBER)

        label(root, "Användarnamn (din e-postadress)")
        user = field(root, Store.mailUser(this), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or InputType.TYPE_CLASS_TEXT)

        label(root, "Lösenord / app-lösenord")
        pass = field(root, Store.mailPass(this), InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT)

        label(root, "Skicka till (mottagarens e-postadress)")
        to = field(root, Store.mailTo(this), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or InputType.TYPE_CLASS_TEXT)

        val save = Button(this).apply {
            text = "Spara"
            isAllCaps = false
            setTextColor(Color.parseColor("#0C1A16"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setBackgroundColor(Color.parseColor("#49C0A8"))
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener { save() }
        }
        root.addView(save, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(20), 0, 0) })
    }

    private fun head(parent: LinearLayout, text: String, size: Float, bold: Boolean) {
        parent.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#ECEFF4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(8))
        })
    }

    private fun label(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#95A0B5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(10), 0, dp(4))
        })
    }

    private fun field(parent: LinearLayout, value: String, type: Int): EditText {
        val e = EditText(this).apply {
            setText(value)
            inputType = type
            setTextColor(Color.parseColor("#ECEFF4"))
            setHintTextColor(Color.parseColor("#5A6478"))
            setBackgroundColor(Color.parseColor("#1E2330"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        parent.addView(e, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return e
    }

    private fun save() {
        Store.setThreshold(this, threshold.text.toString().toIntOrNull() ?: 1)
        Store.setBurst(this, burst.text.toString().toIntOrNull() ?: 1)
        val on = mailOn.text.toString().trim().lowercase().let { it == "ja" || it == "yes" || it == "j" }
        Store.setMailOn(this, on)
        Store.saveMail(
            this,
            host.text.toString(),
            port.text.toString().toIntOrNull() ?: 587,
            user.text.toString(),
            pass.text.toString(),
            to.text.toString()
        )
        android.widget.Toast.makeText(this, "Sparat", android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }
}
