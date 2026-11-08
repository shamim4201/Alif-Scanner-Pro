package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.UserPreferences
import com.example.scanner.ParsedResult
import com.example.scanner.QrBarcodeGenerator
import com.example.scanner.QrBarcodeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Scanner Pro", appName)
    }

    @Test
    fun `test UserPreferences premium and pin logic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)

        // Initial default
        assertEquals(false, prefs.isPremium.value)

        // Set Premium
        prefs.setPremium(true, "Yearly")
        assertEquals(true, prefs.isPremium.value)
        assertEquals("Yearly", prefs.premiumPlan.value)

        // App Lock PIN
        prefs.setPinCode("1234")
        prefs.setAppLockEnabled(true)
        assertEquals(true, prefs.isAppLockEnabled.value)
        assertEquals("1234", prefs.pinCode.value)
    }

    @Test
    fun `test QrBarcodeParser handles URL and WiFi`() {
        val urlResult = QrBarcodeParser.parse("https://example.com/scanner", "QR_CODE")
        assertTrue(urlResult is ParsedResult.Url)
        assertEquals("https://example.com/scanner", (urlResult as ParsedResult.Url).url)

        val wifiResult = QrBarcodeParser.parse("WIFI:S:MyOfficeWiFi;T:WPA;P:secretpass123;;", "QR_CODE")
        assertTrue(wifiResult is ParsedResult.Wifi)
        val wifi = wifiResult as ParsedResult.Wifi
        assertEquals("MyOfficeWiFi", wifi.ssid)
        assertEquals("secretpass123", wifi.password)
        assertEquals("WPA", wifi.security)
    }

    @Test
    fun `test QrBarcodeParser handles Phone and Barcode`() {
        val phoneResult = QrBarcodeParser.parse("tel:+1234567890", "QR_CODE")
        assertTrue(phoneResult is ParsedResult.Phone)
        assertEquals("+1234567890", (phoneResult as ParsedResult.Phone).number)

        val barcodeResult = QrBarcodeParser.parse("8901030383451", "EAN-13")
        assertTrue(barcodeResult is ParsedResult.ProductBarcode)
        assertEquals("8901030383451", (barcodeResult as ParsedResult.ProductBarcode).code)
    }

    @Test
    fun `test QR bitmap generation`() {
        val bmp = QrBarcodeGenerator.generateQrBitmap("Scanner Pro Test", 256, 256)
        assertNotNull(bmp)
        assertEquals(256, bmp!!.width)
        assertEquals(256, bmp.height)
    }
}
