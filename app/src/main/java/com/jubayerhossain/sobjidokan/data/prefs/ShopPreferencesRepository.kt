package com.jubayerhossain.sobjidokan.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sobji_khata_prefs")

enum class AppThemeMode(
  val labelBn: String,
) {
  SYSTEM("সিস্টেম"),
  LIGHT("হালকা (দিন)"),
  DARK("গাঢ় (রাত)"),
}

data class ShopPreferences(
  val isSetupComplete: Boolean = false,
  val shopName: String = "",
  val ownerName: String = "",
  val useBengaliDigits: Boolean = true,
  val showCreditLedger: Boolean = true,
  val pinCode: String = "",
  val lastBackupTimestamp: Long = 0L,
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val failedPinAttempts: Int = 0,
  val pinLockoutUntilMs: Long = 0L,
)

class ShopPreferencesRepository(
  private val context: Context,
) {
  private object Keys {
    val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
    val SHOP_NAME = stringPreferencesKey("shop_name")
    val OWNER_NAME = stringPreferencesKey("owner_name")
    val USE_BENGALI_DIGITS = booleanPreferencesKey("use_bengali_digits")
    val SHOW_CREDIT_LEDGER = booleanPreferencesKey("show_credit_ledger")
    val PIN_CODE = stringPreferencesKey("pin_code")
    val LAST_BACKUP = longPreferencesKey("last_backup")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
    val PIN_LOCKOUT_UNTIL_MS = longPreferencesKey("pin_lockout_until_ms")
  }

  val preferencesFlow: Flow<ShopPreferences> =
    context.dataStore.data.map { prefs ->
      val modeName = prefs[Keys.THEME_MODE]
      val parsedMode =
        AppThemeMode.entries.firstOrNull { it.name == modeName } ?: AppThemeMode.SYSTEM
      ShopPreferences(
        isSetupComplete = prefs[Keys.IS_SETUP_COMPLETE] ?: false,
        shopName = prefs[Keys.SHOP_NAME] ?: "",
        ownerName = prefs[Keys.OWNER_NAME] ?: "",
        useBengaliDigits = prefs[Keys.USE_BENGALI_DIGITS] ?: true,
        showCreditLedger = prefs[Keys.SHOW_CREDIT_LEDGER] ?: true,
        pinCode = prefs[Keys.PIN_CODE] ?: "",
        lastBackupTimestamp = prefs[Keys.LAST_BACKUP] ?: 0L,
        themeMode = parsedMode,
        failedPinAttempts = prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0,
        pinLockoutUntilMs = prefs[Keys.PIN_LOCKOUT_UNTIL_MS] ?: 0L,
      )
    }

  suspend fun saveInitialSetup(
    shopName: String,
    ownerName: String,
    useBengaliDigits: Boolean,
  ) {
    context.dataStore.edit { prefs ->
      prefs[Keys.IS_SETUP_COMPLETE] = true
      prefs[Keys.SHOP_NAME] = shopName.trim()
      prefs[Keys.OWNER_NAME] = ownerName.trim()
      prefs[Keys.USE_BENGALI_DIGITS] = useBengaliDigits
    }
  }

  suspend fun updateShopProfile(
    shopName: String,
    ownerName: String,
  ) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SHOP_NAME] = shopName.trim()
      prefs[Keys.OWNER_NAME] = ownerName.trim()
    }
  }

  suspend fun setUseBengaliDigits(enabled: Boolean) {
    context.dataStore.edit { it[Keys.USE_BENGALI_DIGITS] = enabled }
  }

  suspend fun setShowCreditLedger(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SHOW_CREDIT_LEDGER] = enabled }
  }

  suspend fun setThemeMode(mode: AppThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setPinCode(pin: String) {
    context.dataStore.edit {
      it[Keys.PIN_CODE] = if (pin.isBlank()) "" else hashPin(pin)
      it[Keys.FAILED_PIN_ATTEMPTS] = 0
      it[Keys.PIN_LOCKOUT_UNTIL_MS] = 0L
    }
  }

  suspend fun updatePinAttemptState(
    failedAttempts: Int,
    lockoutUntilMs: Long,
  ) {
    context.dataStore.edit {
      it[Keys.FAILED_PIN_ATTEMPTS] = failedAttempts
      it[Keys.PIN_LOCKOUT_UNTIL_MS] = lockoutUntilMs
    }
  }

  suspend fun clearPinAttempts() {
    context.dataStore.edit {
      it[Keys.FAILED_PIN_ATTEMPTS] = 0
      it[Keys.PIN_LOCKOUT_UNTIL_MS] = 0L
    }
  }

  suspend fun updateLastBackup(timestamp: Long) {
    context.dataStore.edit { it[Keys.LAST_BACKUP] = timestamp }
  }

  @androidx.annotation.VisibleForTesting
  internal suspend fun resetForTest() {
    context.dataStore.edit { it.clear() }
  }

  companion object {
    private const val HASH_PREFIX = "sha256:"

    fun hashPin(pin: String): String {
      val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
      val saltHex = salt.joinToString("") { "%02x".format(it) }
      val digest = sha256Hex("$saltHex:$pin")
      return "$HASH_PREFIX$saltHex:$digest"
    }

    fun verifyPin(
      inputPin: String,
      storedValue: String,
    ): Boolean {
      if (storedValue.isBlank()) return true
      if (!storedValue.startsWith(HASH_PREFIX)) {
        return java.security.MessageDigest.isEqual(
          inputPin.toByteArray(Charsets.UTF_8),
          storedValue.toByteArray(Charsets.UTF_8),
        )
      }
      val parts = storedValue.removePrefix(HASH_PREFIX).split(":")
      if (parts.size != 2) return false
      val (saltHex, expectedDigest) = parts
      val computed = sha256Hex("$saltHex:$inputPin")
      return java.security.MessageDigest.isEqual(
        computed.toByteArray(Charsets.UTF_8),
        expectedDigest.toByteArray(Charsets.UTF_8),
      )
    }

    private fun sha256Hex(value: String): String {
      val bytes =
        java.security.MessageDigest
          .getInstance("SHA-256")
          .digest(value.toByteArray(Charsets.UTF_8))
      return bytes.joinToString("") { "%02x".format(it) }
    }
  }
}
