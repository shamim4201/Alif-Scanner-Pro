package com.example.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

sealed class ParsedResult {
    data class Url(val url: String) : ParsedResult()
    data class Wifi(val ssid: String, val password: String, val security: String, val isHidden: Boolean) : ParsedResult()
    data class Phone(val number: String) : ParsedResult()
    data class Email(val address: String, val subject: String?, val body: String?) : ParsedResult()
    data class Contact(val name: String, val phone: String?, val email: String?, val org: String?) : ParsedResult()
    data class Geo(val latitude: Double, val longitude: Double, val query: String?) : ParsedResult()
    data class ProductBarcode(val code: String, val format: String) : ParsedResult()
    data class PlainText(val text: String) : ParsedResult()
}

object QrBarcodeParser {

    fun parse(rawText: String, format: String = "QR_CODE"): ParsedResult {
        val trimmed = rawText.trim()

        // Check if barcode format (not QR)
        val isBarcode = format != "QR_CODE" && format.isNotEmpty() && !format.contains("QR", ignoreCase = true)
        if (isBarcode) {
            return ParsedResult.ProductBarcode(trimmed, format)
        }

        // Wi-Fi check
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val ssid = extractRegex(trimmed, "S:([^;]+);")
            val pass = extractRegex(trimmed, "P:([^;]+);")
            val type = extractRegex(trimmed, "T:([^;]+);").ifEmpty { "WPA" }
            val hidden = extractRegex(trimmed, "H:([^;]+);").equals("true", ignoreCase = true)
            return ParsedResult.Wifi(ssid, pass, type, hidden)
        }

        // URL check
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true)
        ) {
            val validUrl = if (trimmed.startsWith("www.", ignoreCase = true)) "https://$trimmed" else trimmed
            return ParsedResult.Url(validUrl)
        }

        // Phone check
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val phone = trimmed.substring(4)
            return ParsedResult.Phone(phone)
        }

        // Email check
        if (trimmed.startsWith("mailto:", ignoreCase = true)) {
            val email = trimmed.substring(7).substringBefore("?")
            val subject = extractParam(trimmed, "subject")
            val body = extractParam(trimmed, "body")
            return ParsedResult.Email(email, subject, body)
        } else if (trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            val to = extractRegex(trimmed, "TO:([^;]+);")
            val sub = extractRegex(trimmed, "SUB:([^;]+);")
            val body = extractRegex(trimmed, "BODY:([^;]+);")
            return ParsedResult.Email(to, sub.ifEmpty { null }, body.ifEmpty { null })
        }

        // vCard check
        if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
            val name = extractRegex(trimmed, "FN:([^\r\n]+)").ifEmpty {
                extractRegex(trimmed, "N:([^\r\n]+)")
            }
            val phone = extractRegex(trimmed, "TEL[^:]*:([^\r\n]+)")
            val email = extractRegex(trimmed, "EMAIL[^:]*:([^\r\n]+)")
            val org = extractRegex(trimmed, "ORG:([^\r\n]+)")
            return ParsedResult.Contact(
                name = name.ifEmpty { "Contact" },
                phone = phone.ifEmpty { null },
                email = email.ifEmpty { null },
                org = org.ifEmpty { null }
            )
        }

        // Geo check
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            val coords = trimmed.substring(4).substringBefore("?")
            val parts = coords.split(",")
            if (parts.size >= 2) {
                val lat = parts[0].toDoubleOrNull() ?: 0.0
                val lng = parts[1].toDoubleOrNull() ?: 0.0
                val q = extractParam(trimmed, "q")
                return ParsedResult.Geo(lat, lng, q)
            }
        }

        // Digits-only barcode check (UPC / EAN / Code 128)
        if (trimmed.all { it.isDigit() } && trimmed.length in 8..14) {
            val detectedFormat = when (trimmed.length) {
                8 -> "EAN-8"
                12 -> "UPC-A"
                13 -> "EAN-13"
                14 -> "ITF-14"
                else -> "BARCODE"
            }
            return ParsedResult.ProductBarcode(trimmed, detectedFormat)
        }

        return ParsedResult.PlainText(trimmed)
    }

    private fun extractRegex(input: String, patternStr: String): String {
        val regex = Regex(patternStr, RegexOption.IGNORE_CASE)
        val match = regex.find(input)
        return match?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractParam(input: String, param: String): String? {
        val qIndex = input.indexOf("?")
        if (qIndex == -1) return null
        val query = input.substring(qIndex + 1)
        val pairs = query.split("&")
        for (pair in pairs) {
            val kv = pair.split("=")
            if (kv.isNotEmpty() && kv[0].equals(param, ignoreCase = true)) {
                return if (kv.size > 1) Uri.decode(kv[1]) else ""
            }
        }
        return null
    }

    // Interactive Action Helpers
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, text: String, title: String = "Share Scanned Data") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(shareIntent)
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun dialPhone(context: Context, phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(context: Context, email: String, subject: String? = null, body: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                body?.let { putExtra(Intent.EXTRA_TEXT, it) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open email client", Toast.LENGTH_SHORT).show()
        }
    }

    fun searchProductOnline(context: Context, barcode: String) {
        val url = "https://www.google.com/search?q=$barcode"
        openUrl(context, url)
    }

    fun openInMaps(context: Context, lat: Double, lng: Double, query: String? = null) {
        val uri = if (query.isNullOrEmpty()) {
            Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        } else {
            Uri.parse("geo:$lat,$lng?q=${Uri.encode(query)}")
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openUrl(context, "https://maps.google.com/?q=$lat,$lng")
        }
    }
}
