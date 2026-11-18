package com.gr8eam.vaktpost

import android.content.Context
import javax.activation.DataHandler
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

/**
 * Valfri e-postsändning via SMTP (STARTTLS, eller SSL på port 465). Körs bara när användaren fyllt i ett konto.
 * Loginuppgifterna ligger i appens privata lagring. Fungerar med vanliga leverantörer; för Gmail krävs ett app-lösenord.
 * Biblioteket com.sun.mail:android-mail använder paketnamnet javax.mail.
 */
object Mailer {

    fun send(ctx: Context, files: List<File>) {
        val host = Store.mailHost(ctx)
        val port = Store.mailPort(ctx)
        val user = Store.mailUser(ctx)
        val pass = Store.mailPass(ctx)
        val to = Store.mailTo(ctx)
        if (host.isEmpty() || to.isEmpty()) return

        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", if (user.isNotEmpty()) "true" else "false")
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
            if (port == 465) {
                put("mail.smtp.socketFactory.port", "465")
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.socketFactory.fallback", "false")
            } else {
                put("mail.smtp.starttls.enable", "true")
            }
        }
        val session = if (user.isNotEmpty())
            Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(user, pass)
            })
        else Session.getInstance(props)

        val from = if (user.contains("@")) user else to
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val msg = MimeMessage(session).apply {
            setFrom(InternetAddress(from, "Vaktpost"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            subject = "Vaktpost: foto vid upplasningsforsok ($stamp)"
        }
        val multipart = MimeMultipart()
        multipart.addBodyPart(MimeBodyPart().apply {
            setText("Nagon skrev fel upplasningskod $stamp. ${files.size} kort bifogas.")
        })
        for (f in files) {
            multipart.addBodyPart(MimeBodyPart().apply {
                dataHandler = DataHandler(ByteArrayDataSource(f.readBytes(), "image/jpeg"))
                fileName = f.name
            })
        }
        msg.setContent(multipart)
        Transport.send(msg)
    }
}
