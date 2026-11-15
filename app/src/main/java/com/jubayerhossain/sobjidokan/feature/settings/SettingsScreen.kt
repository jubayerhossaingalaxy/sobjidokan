
@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.backup.SobjiKhataBackupPayload
import com.jubayerhossain.sobjidokan.data.prefs.AppThemeMode
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.ConfirmationDialog
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(
  uiState: ShopUiState,
  pendingRestore: SobjiKhataBackupPayload?,
  onBack: () -> Unit,
  onUpdateShopProfile: (String, String) -> Unit,
  onToggleBengaliDigits: (Boolean) -> Unit,
  onToggleCreditLedger: (Boolean) -> Unit,
  onSetPinCode: (String) -> Unit,
  onExportBackupUri: (android.net.Uri) -> Unit,
  onSelectRestoreUri: (android.net.Uri) -> Unit,
  onConfirmRestore: () -> Unit,
  onDismissRestore: () -> Unit,
  onSetThemeMode: (AppThemeMode) -> Unit = {},
) {
  val prefs = uiState.preferences
  val bn = prefs.useBengaliDigits

  var shopName by remember(prefs.shopName) { mutableStateOf(prefs.shopName) }
  var ownerName by remember(prefs.ownerName) { mutableStateOf(prefs.ownerName) }
  var shopNameError by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }
  var showDisablePinConfirm by remember { mutableStateOf(false) }

  val hasProfileChanges =
    shopName.trim() != prefs.shopName.trim() || ownerName.trim() != prefs.ownerName.trim()

  val createBackupLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
      if (uri != null) onExportBackupUri(uri)
    }

  val openRestoreLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) onSelectRestoreUri(uri)
    }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("সেটিংস ও ব্যাকআপ") },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = BazarDimens.MaxContentWidthExpanded)
            .imePadding()
            .navigationBarsPadding()
            .testTag("settings_list"),
        contentPadding = PaddingValues(BazarDimens.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // 1. Shop Information
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border =
              BorderStroke(
                BazarDimens.CardHairlineBorder,
                MaterialTheme.colorScheme.outlineVariant,
              ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("দোকানের তথ্য", style = MaterialTheme.typography.titleMedium)
                if (hasProfileChanges) {
                  StatusBadge(text = "অসংরক্ষিত পরিবর্তন", tone = BadgeTone.WARNING)
                }
              }
              OutlinedTextField(
                value = shopName,
                onValueChange = {
                  shopName = it
                  if (it.isNotBlank()) shopNameError = false
                },
                label = { Text("দোকানের নাম") },
                isError = shopNameError,
                supportingText = { if (shopNameError) Text("অনুগ্রহ করে দোকানের নাম লিখুন।") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_shop_name_input"),
              )
              OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("মালিকের নাম") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Button(
                onClick = {
                  if (shopName.isBlank()) {
                    shopNameError = true
                  } else {
                    onUpdateShopProfile(shopName, ownerName)
                  }
                },
                modifier =
                  Modifier
                    .align(Alignment.End)
                    .minimumInteractiveComponentSize()
                    .testTag("settings_update_profile_button"),
              ) {
                Text("আপডেট করুন")
              }
            }
          }
        }

        // 2. Display & Theme
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border =
              BorderStroke(
                BazarDimens.CardHairlineBorder,
                MaterialTheme.colorScheme.outlineVariant,
              ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text("প্রদর্শন ও ভাষা", style = MaterialTheme.typography.titleMedium)
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("রঙ ও থিম", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(AppThemeMode.entries) { mode ->
                    FilterChip(
                      selected = prefs.themeMode == mode,
                      onClick = { onSetThemeMode(mode) },
                      label = { Text(mode.labelBn) },
                      modifier =
                        Modifier
                          .minimumInteractiveComponentSize()
                          .testTag("theme_chip_${mode.name}"),
                    )
                  }
                }
              }
              SettingsSwitchRow(
                title = "বাংলা সংখ্যা (১২৩)",
                subtitle = "বন্ধ করলে ইংরেজি সংখ্যা (123) দেখাবে",
                checked = prefs.useBengaliDigits,
                onCheckedChange = onToggleBengaliDigits,
                testTag = "switch_bengali_digits",
              )
            }
          }
        }

        // 3. Business & Security
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border =
              BorderStroke(
                BazarDimens.CardHairlineBorder,
                MaterialTheme.colorScheme.outlineVariant,
              ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text("ব্যবসা ও নিরাপত্তা", style = MaterialTheme.typography.titleMedium)
              SettingsSwitchRow(
                title = "বাকি খাতা দেখান",
                subtitle = "ক্রেতার পাওনা ও পাইকারের দেনা পরিচালনা করুন",
                checked = prefs.showCreditLedger,
                onCheckedChange = onToggleCreditLedger,
                testTag = "switch_credit_ledger",
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("৪-ডিজিট PIN লক", style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = if (prefs.pinCode.isBlank()) "বন্ধ আছে" else "চালু আছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (prefs.pinCode.isNotBlank()) {
                    TextButton(
                      onClick = { showDisablePinConfirm = true },
                      modifier =
                        Modifier
                          .minimumInteractiveComponentSize()
                          .testTag("disable_pin_button"),
                    ) {
                      Text("বন্ধ করুন", color = MaterialTheme.colorScheme.error)
                    }
                  }
                  OutlinedButton(
                    onClick = { showPinDialog = true },
                    modifier =
                      Modifier.minimumInteractiveComponentSize().testTag("setup_pin_button"),
                  ) {
                    Text(if (prefs.pinCode.isBlank()) "সেট করুন" else "বদলান")
                  }
                }
              }
            }
          }
        }

        // 4. Offline Backup & Restore
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border =
              BorderStroke(
                BazarDimens.CardHairlineBorder,
                MaterialTheme.colorScheme.outlineVariant,
              ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text("অফলাইন ডেটা ব্যাকআপ ও পুনরুদ্ধার", style = MaterialTheme.typography.titleMedium)
              val backupText =
                if (prefs.lastBackupTimestamp == 0L) {
                  "এখনো কোনো ব্যাকআপ নেওয়া হয়নি"
                } else {
                  val formattedTime =
                    BusinessCalculator.formatDateTime(prefs.lastBackupTimestamp, bn)
                  "শেষ ব্যাকআপ: $formattedTime"
                }
              Text(
                text = backupText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              val vegCountText = BusinessCalculator.formatNumber(uiState.allVegetables.size, bn)
              val saleCountText = BusinessCalculator.formatNumber(uiState.sales.size, bn)
              StatusBadge(
                text = "স্থানীয় তথ্য: ${vegCountText}টি সবজি • ${saleCountText}টি বিক্রি",
                tone = BadgeTone.INFO,
              )
              Button(
                onClick = {
                  val dateTag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                  createBackupLauncher.launch("sobjikhata_backup_$dateTag.json")
                },
                enabled = !uiState.isSaving,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(BazarDimens.PrimaryButtonHeight)
                    .testTag("export_backup_button"),
              ) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ব্যাকআপ ফাইল সেভ করুন (JSON)")
              }
              OutlinedButton(
                onClick = { openRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                enabled = !uiState.isSaving,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(BazarDimens.PrimaryButtonHeight)
                    .testTag("restore_backup_button"),
              ) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ব্যাকআপ থেকে ফিরিয়ে আনুন")
              }
            }
          }
        }

        // 5. About & Offline Privacy
        item {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
              ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().testTag("settings_about_card"),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Filled.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text("সবজি খাতা • সংস্করণ ১.০", style = MaterialTheme.typography.titleSmall)
              }
              Text(
                text =
                  "সব হিসাব, বিক্রি ও বাকি খাতার তথ্য শুধুমাত্র আপনার ফোনের ভেতরেই " +
                    "সংরক্ষিত থাকে। ইন্টারনেট বা কোনো সার্ভার ছাড়াই সম্পূর্ণ নিরাপদে চলে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              StatusBadge(
                text = "১০০% অফলাইন • কোনো বিজ্ঞাপন বা ট্র্যাকিং নেই",
                tone = BadgeTone.SUCCESS,
                icon = Icons.Filled.OfflinePin,
              )
            }
          }
        }
      }
    }
  }

  if (showPinDialog) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pinInput by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = { showPinDialog = false }, sheetState = sheetState) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(
              horizontal = BazarDimens.SheetHorizontal,
              vertical = BazarDimens.SpaceSm,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(text = "৪-ডিজিট PIN সেট করুন", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
          value = pinInput,
          onValueChange = {
            if (it.length <= BusinessCalculator.PIN_LENGTH && it.all { c -> c.isDigit() }) {
              pinInput = it
            }
          },
          label = { Text("৪টি সংখ্যা লিখুন") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("setup_pin_dialog_input"),
        )
        Surface(
          color =
            com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors.current.warningContainer,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth().testTag("pin_recovery_warning"),
        ) {
          Text(
            text =
              "সতর্কতা: অফলাইন অ্যাপে PIN ভুলে গেলে পুনরুদ্ধারের সুযোগ নেই—" +
                "তাই আগে বা পরে অবশ্যই JSON ব্যাকআপ সেভ করে রাখুন।",
            style = MaterialTheme.typography.bodySmall,
            color = com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors.current.warning,
            modifier = Modifier.padding(10.dp),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
          horizontalArrangement = Arrangement.End,
        ) {
          val cancelTag = "setup_pin_cancel_button"
          TextButton(
            onClick = { showPinDialog = false },
            modifier = Modifier.minimumInteractiveComponentSize().testTag(cancelTag),
          ) {
            Text("বাতিল")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              onSetPinCode(pinInput)
              showPinDialog = false
            },
            enabled = pinInput.length == BusinessCalculator.PIN_LENGTH,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("setup_pin_confirm_button"),
          ) {
            Text("সেট করুন")
          }
        }
      }
    }
  }

  if (showDisablePinConfirm) {
    ConfirmationDialog(
      title = "PIN লক বন্ধ করবেন?",
      message = "PIN লক বন্ধ করলে যে কেউ ফোন থেকে সরাসরি সবজি খাতা খুলতে পারবে। আপনি কি নিশ্চিত?",
      confirmLabel = "হ্যাঁ, বন্ধ করুন",
      isDestructive = true,
      onConfirm = {
        showDisablePinConfirm = false
        onSetPinCode("")
      },
      onDismiss = { showDisablePinConfirm = false },
    )
  }

  if (pendingRestore != null) {
    val salesCount = BusinessCalculator.formatNumber(pendingRestore.sales.size, bn)
    val vegCount = BusinessCalculator.formatNumber(pendingRestore.vegetables.size, bn)
    ConfirmationDialog(
      title = "ব্যাকআপ থেকে ডেটা ফিরিয়ে আনবেন?",
      message =
        "ব্যাকআপ ফাইল যাচাই সম্পন্ন হয়েছে (${salesCount}টি বিক্রি ও ${vegCount}টি সবজি)। " +
          "এটি বর্তমান সব তথ্য প্রতিস্থাপিত করবে।",
      confirmLabel = "হ্যাঁ, ফিরিয়ে আনুন",
      isDestructive = true,
      onConfirm = onConfirmRestore,
      onDismiss = onDismissRestore,
    )
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clickable { onCheckedChange(!checked) }
        .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
