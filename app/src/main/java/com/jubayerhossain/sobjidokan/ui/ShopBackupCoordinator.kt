package com.jubayerhossain.sobjidokan.ui

import android.net.Uri
import com.jubayerhossain.sobjidokan.data.backup.BackupManager
import com.jubayerhossain.sobjidokan.data.backup.SobjiKhataBackupPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Coordinates JSON backup export, restore validation, and confirmation execution. */
internal class ShopBackupCoordinator(
  private val backupManager: BackupManager,
  private val snackbarEvents: MutableSharedFlow<String>,
  private val launchSerialized: (suspend () -> Unit) -> Unit,
  private val scope: CoroutineScope,
  private val onRestored: () -> Unit,
) {
  private val _pendingRestorePayload = MutableStateFlow<SobjiKhataBackupPayload?>(null)
  val pendingRestorePayload: StateFlow<SobjiKhataBackupPayload?> =
    _pendingRestorePayload.asStateFlow()

  fun exportBackup(uri: Uri) {
    launchSerialized {
      try {
        val result = backupManager.exportToUri(uri)
        if (result.isSuccess) {
          snackbarEvents.tryEmit("ব্যাকআপ ফাইল সফলভাবে সেভ হয়েছে")
        } else {
          snackbarEvents.tryEmit("ব্যাকআপ তৈরি ব্যর্থ হয়েছে। আবার চেষ্টা করুন।")
        }
      } catch (_: Exception) {
        snackbarEvents.tryEmit("ব্যাকআপ তৈরি ব্যর্থ হয়েছে। আবার চেষ্টা করুন।")
      }
    }
  }

  fun prepareRestoreFromUri(uri: Uri) {
    scope.launch {
      try {
        val result = backupManager.validateBackupUri(uri)
        result
          .onSuccess { payload -> _pendingRestorePayload.value = payload }
          .onFailure {
            snackbarEvents.tryEmit(
              "ফাইলটি সঠিক সবজি খাতা ব্যাকআপ নয় বা ক্ষতিগ্রস্ত। আপনার বর্তমান তথ্য নিরাপদ আছে।",
            )
          }
      } catch (_: Exception) {
        snackbarEvents.tryEmit("ফাইলটি পড়া সম্ভব হয়নি। আপনার বর্তমান তথ্য নিরাপদ আছে।")
      }
    }
  }

  fun dismissPendingRestore() {
    _pendingRestorePayload.value = null
  }

  fun confirmRestore() {
    if (_pendingRestorePayload.value == null) return
    launchSerialized {
      val currentPayload = _pendingRestorePayload.value ?: return@launchSerialized
      try {
        val res = backupManager.restoreFromPayload(currentPayload)
        _pendingRestorePayload.value = null
        if (res.isSuccess) {
          onRestored()
          snackbarEvents.tryEmit("ব্যাকআপ থেকে সব তথ্য সফলভাবে ফিরে এসেছে")
        } else {
          snackbarEvents.tryEmit("পুনরুদ্ধার ব্যর্থ হয়েছে।")
        }
      } catch (_: Exception) {
        _pendingRestorePayload.value = null
        snackbarEvents.tryEmit("পুনরুদ্ধার ব্যর্থ হয়েছে।")
      }
    }
  }
}
