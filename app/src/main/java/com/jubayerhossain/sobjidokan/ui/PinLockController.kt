package com.jubayerhossain.sobjidokan.ui

import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferences
import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages app PIN lock/unlock state, failed-attempt lockout windows, and background relock grace
 * timing.
 */
internal class PinLockController(
  private val prefsRepo: ShopPreferencesRepository,
  private val scope: CoroutineScope,
  private val clock: () -> Long = { System.currentTimeMillis() },
) {
  private val _isUnlocked = MutableStateFlow(false)
  val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

  private val failedAttempts = MutableStateFlow(0)
  private val _lockoutUntilMs = MutableStateFlow(0L)
  val lockoutUntilMs: StateFlow<Long> = _lockoutUntilMs.asStateFlow()

  private var lastBackgroundTimestampMs: Long = 0L

  fun markUnlocked() {
    _isUnlocked.value = true
  }

  fun resetOnPinUpdated() {
    failedAttempts.value = 0
    _lockoutUntilMs.value = 0L
  }

  fun onAppMovedToBackground(pinConfigured: Boolean) {
    if (pinConfigured && _isUnlocked.value) {
      lastBackgroundTimestampMs = clock()
    }
  }

  fun onAppReturnedToForeground(pinConfigured: Boolean) {
    val bgTime = lastBackgroundTimestampMs
    lastBackgroundTimestampMs = 0L
    if (bgTime > 0L && pinConfigured) {
      if (clock() - bgTime >= ShopViewModel.PIN_RELOCK_GRACE_MS) {
        _isUnlocked.value = false
      }
    }
  }

  fun unlockWithPin(
    pin: String,
    prefs: ShopPreferences,
  ): Boolean {
    val now = clock()
    val currentLockout = maxOf(_lockoutUntilMs.value, prefs.pinLockoutUntilMs)
    if (currentLockout > now) {
      return false
    }
    val valid = ShopPreferencesRepository.verifyPin(pin, prefs.pinCode)
    if (valid) {
      failedAttempts.value = 0
      _lockoutUntilMs.value = 0L
      _isUnlocked.value = true
      scope.launch { prefsRepo.clearPinAttempts() }
      return true
    }

    val prevAttempts =
      if (currentLockout in 1L..now) 0 else maxOf(failedAttempts.value, prefs.failedPinAttempts)
    val nextAttempts = prevAttempts + 1
    val nextLockout =
      if (nextAttempts >= ShopViewModel.MAX_PIN_ATTEMPTS) {
        now + ShopViewModel.PIN_LOCKOUT_DURATION_MS
      } else {
        0L
      }
    failedAttempts.value = nextAttempts
    _lockoutUntilMs.value = nextLockout
    scope.launch { prefsRepo.updatePinAttemptState(nextAttempts, nextLockout) }
    return false
  }
}
