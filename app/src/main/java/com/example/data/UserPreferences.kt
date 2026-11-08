package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("scanner_pro_prefs", Context.MODE_PRIVATE)

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_IS_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _premiumPlan = MutableStateFlow(prefs.getString(KEY_PREMIUM_PLAN, "Free") ?: "Free")
    val premiumPlan: StateFlow<String> = _premiumPlan.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _pinCode = MutableStateFlow(prefs.getString(KEY_PIN_CODE, null))
    val pinCode: StateFlow<String?> = _pinCode.asStateFlow()

    private val _vibrateOnScan = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE, true))
    val vibrateOnScan: StateFlow<Boolean> = _vibrateOnScan.asStateFlow()

    private val _beepOnScan = MutableStateFlow(prefs.getBoolean(KEY_BEEP, true))
    val beepOnScan: StateFlow<Boolean> = _beepOnScan.asStateFlow()

    private val _autoCopy = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COPY, false))
    val autoCopy: StateFlow<Boolean> = _autoCopy.asStateFlow()

    private val _defaultFormat = MutableStateFlow(prefs.getString(KEY_DEFAULT_FORMAT, "PDF") ?: "PDF")
    val defaultFormat: StateFlow<String> = _defaultFormat.asStateFlow()

    private val _pdfPageSize = MutableStateFlow(prefs.getString(KEY_PDF_PAGE_SIZE, "A4") ?: "A4")
    val pdfPageSize: StateFlow<String> = _pdfPageSize.asStateFlow()

    private val _cloudBackupEnabled = MutableStateFlow(prefs.getBoolean(KEY_CLOUD_BACKUP, false))
    val cloudBackupEnabled: StateFlow<Boolean> = _cloudBackupEnabled.asStateFlow()

    private val _cloudProvider = MutableStateFlow(prefs.getString(KEY_CLOUD_PROVIDER, "Google Drive") ?: "Google Drive")
    val cloudProvider: StateFlow<String> = _cloudProvider.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString(KEY_APP_LANGUAGE, "English") ?: "English")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setPremium(premium: Boolean, plan: String = "Yearly") {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, premium).putString(KEY_PREMIUM_PLAN, plan).apply()
        _isPremium.value = premium
        _premiumPlan.value = plan
    }

    fun setPremiumPlan(plan: String) {
        prefs.edit().putString(KEY_PREMIUM_PLAN, plan).apply()
        _premiumPlan.value = plan
    }

    fun setAppLock(enabled: Boolean, pin: String? = null) {
        val editor = prefs.edit().putBoolean(KEY_APP_LOCK, enabled)
        if (pin != null) {
            editor.putString(KEY_PIN_CODE, pin)
            _pinCode.value = pin
        }
        editor.apply()
        _isAppLockEnabled.value = enabled
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _isAppLockEnabled.value = enabled
    }

    fun setPinCode(pin: String?) {
        prefs.edit().putString(KEY_PIN_CODE, pin).apply()
        _pinCode.value = pin
    }

    fun setVibrate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        _vibrateOnScan.value = enabled
    }

    fun setVibrateOnScan(enabled: Boolean) = setVibrate(enabled)

    fun setBeep(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BEEP, enabled).apply()
        _beepOnScan.value = enabled
    }

    fun setBeepOnScan(enabled: Boolean) = setBeep(enabled)

    fun setAutoCopy(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COPY, enabled).apply()
        _autoCopy.value = enabled
    }

    fun setDefaultFormat(format: String) {
        prefs.edit().putString(KEY_DEFAULT_FORMAT, format).apply()
        _defaultFormat.value = format
    }

    fun setPdfPageSize(size: String) {
        prefs.edit().putString(KEY_PDF_PAGE_SIZE, size).apply()
        _pdfPageSize.value = size
    }

    fun setCloudBackup(enabled: Boolean, provider: String = "Google Drive") {
        prefs.edit().putBoolean(KEY_CLOUD_BACKUP, enabled).putString(KEY_CLOUD_PROVIDER, provider).apply()
        _cloudBackupEnabled.value = enabled
        _cloudProvider.value = provider
    }

    fun setCloudBackupEnabled(enabled: Boolean) = setCloudBackup(enabled)

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
        _appLanguage.value = lang
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
        _isPremium.value = false
        _premiumPlan.value = "Free"
        _isAppLockEnabled.value = false
        _pinCode.value = null
        _vibrateOnScan.value = true
        _beepOnScan.value = true
        _autoCopy.value = false
        _cloudBackupEnabled.value = false
        _pdfPageSize.value = "A4"
    }

    companion object {
        private const val KEY_IS_PREMIUM = "key_is_premium"
        private const val KEY_PREMIUM_PLAN = "key_premium_plan"
        private const val KEY_APP_LOCK = "key_app_lock"
        private const val KEY_PIN_CODE = "key_pin_code"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_BEEP = "key_beep"
        private const val KEY_AUTO_COPY = "key_auto_copy"
        private const val KEY_DEFAULT_FORMAT = "key_default_format"
        private const val KEY_PDF_PAGE_SIZE = "key_pdf_page_size"
        private const val KEY_CLOUD_BACKUP = "key_cloud_backup"
        private const val KEY_CLOUD_PROVIDER = "key_cloud_provider"
        private const val KEY_APP_LANGUAGE = "key_app_language"
    }
}
