package com.jubayerhossain.sobjidokan.ui

import com.jubayerhossain.sobjidokan.data.prefs.AppThemeMode
import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferencesRepository
import com.jubayerhossain.sobjidokan.data.repository.ShopRepository
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** Handles shop profile, display preferences, PIN configuration, and app lock lifecycle actions. */
internal class ShopPreferencesActions(
  private val prefsRepo: ShopPreferencesRepository,
  private val repo: ShopRepository,
  private val pinLock: PinLockController,
  private val uiStateProvider: () -> ShopUiState,
  private val snackbarEvents: MutableSharedFlow<String>,
  private val scope: CoroutineScope,
  private val launchSerializedSave: (suspend () -> Unit) -> Unit,
) {
  fun onAppMovedToBackground() {
    pinLock.onAppMovedToBackground(uiStateProvider().preferences.pinCode.isNotBlank())
  }

  fun onAppReturnedToForeground() {
    pinLock.onAppReturnedToForeground(uiStateProvider().preferences.pinCode.isNotBlank())
  }

  fun unlockWithPin(pin: String): Boolean = pinLock.unlockWithPin(pin, uiStateProvider().preferences)

  fun completeInitialSetup(
    shopName: String,
    ownerName: String,
    useBengaliDigits: Boolean,
    seedDefaults: Boolean,
  ) {
    val cleanShopName = shopName.trim()
    if (cleanShopName.isEmpty()) {
      snackbarEvents.tryEmit("অনুগ্রহ করে দোকানের নাম লিখুন।")
      return
    }
    launchSerializedSave {
      try {
        prefsRepo.saveInitialSetup(cleanShopName, ownerName.trim(), useBengaliDigits)
        if (seedDefaults) repo.seedDefaultVegetablesIfEmpty()
        pinLock.markUnlocked()
      } catch (_: Exception) {
        snackbarEvents.tryEmit("দোকান সেটআপ সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।")
      }
    }
  }

  fun updateShopProfile(
    shopName: String,
    ownerName: String,
  ) {
    val cleanShopName = shopName.trim()
    if (cleanShopName.isEmpty()) {
      snackbarEvents.tryEmit("দোকানের নাম ফাঁকা রাখা যাবে না।")
      return
    }
    scope.launch {
      try {
        prefsRepo.updateShopProfile(cleanShopName, ownerName.trim())
        snackbarEvents.tryEmit("দোকানের তথ্য সংরক্ষিত হয়েছে")
      } catch (_: Exception) {
        snackbarEvents.tryEmit("দোকানের তথ্য সংরক্ষণ করা যায়নি।")
      }
    }
  }

  fun setUseBengaliDigits(enabled: Boolean) {
    scope.launch {
      try {
        prefsRepo.setUseBengaliDigits(enabled)
      } catch (_: Exception) {
        snackbarEvents.tryEmit("সেটিং পরিবর্তন করা যায়নি।")
      }
    }
  }

  fun setShowCreditLedger(enabled: Boolean) {
    scope.launch {
      try {
        prefsRepo.setShowCreditLedger(enabled)
      } catch (_: Exception) {
        snackbarEvents.tryEmit("সেটিং পরিবর্তন করা যায়নি।")
      }
    }
  }

  fun setThemeMode(mode: AppThemeMode) {
    scope.launch {
      try {
        prefsRepo.setThemeMode(mode)
      } catch (_: Exception) {
        snackbarEvents.tryEmit("থিম পরিবর্তন করা যায়নি।")
      }
    }
  }

  fun setPinCode(pin: String) {
    val isInvalidPin =
      pin.isNotEmpty() &&
        (pin.length != BusinessCalculator.PIN_LENGTH || !pin.all { it.isDigit() })
    if (isInvalidPin) {
      snackbarEvents.tryEmit("৪ সংখ্যার সঠিক PIN লিখুন।")
      return
    }
    pinLock.resetOnPinUpdated()
    scope.launch {
      try {
        prefsRepo.setPinCode(pin)
        if (pin.isEmpty()) {
          snackbarEvents.tryEmit("PIN লক বন্ধ করা হয়েছে")
        } else {
          pinLock.markUnlocked()
          snackbarEvents.tryEmit("৪-ডিজিট PIN লক চালু হয়েছে")
        }
      } catch (_: Exception) {
        snackbarEvents.tryEmit("PIN সংরক্ষণ করা যায়নি।")
      }
    }
  }
}
