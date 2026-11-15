@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.R
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
  isSaving: Boolean,
  onCompleteSetup: (
    shopName: String,
    ownerName: String,
    useBengaliDigits: Boolean,
    seedDefaults: Boolean,
  ) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var shopName by remember { mutableStateOf("") }
  var ownerName by remember { mutableStateOf("") }
  var useBengaliDigits by remember { mutableStateOf(true) }
  var seedDefaults by remember { mutableStateOf(true) }
  var showNameError by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = BazarDimens.SheetHorizontal, vertical = BazarDimens.SpaceLg),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_bazar_hero),
          contentDescription = "সবজির দোকানের চিত্র",
          modifier = Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(20.dp)),
          contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(BazarDimens.SpaceLg))
        Text(
          text = "সবজি খাতা",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(BazarDimens.SpaceXs))
        Text(
          text = "সহজ বাংলায় দোকানের কেনাবেচা, স্টক, পচা ক্ষতি ও প্রকৃত নিট লাভের অফলাইন খাতা",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(BazarDimens.SpaceXl))

        OutlinedTextField(
          value = shopName,
          onValueChange = {
            shopName = it
            if (it.isNotBlank()) showNameError = false
          },
          label = { Text("দোকানের নাম *") },
          placeholder = { Text("যেমন: বিসমিল্লাহ সবজি ভান্ডার") },
          isError = showNameError,
          supportingText = { if (showNameError) Text("অনুগ্রহ করে দোকানের নাম লিখুন।") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          keyboardActions =
            KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
          modifier = Modifier.fillMaxWidth().testTag("setup_shop_name_input"),
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = ownerName,
          onValueChange = { ownerName = it },
          label = { Text("মালিকের নাম (ঐচ্ছিক)") },
          placeholder = { Text("আপনার নাম লিখুন") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
          modifier = Modifier.fillMaxWidth().testTag("setup_owner_name_input"),
        )

        Spacer(modifier = Modifier.height(BazarDimens.SpaceLg))
        Text(
          text = "সংখ্যা দেখার ধরন",
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(BazarDimens.SpaceSm))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilterChip(
            selected = useBengaliDigits,
            onClick = { useBengaliDigits = true },
            label = { Text("বাংলা সংখ্যা (১২৩)") },
            modifier =
              Modifier.weight(1f).minimumInteractiveComponentSize().testTag("chip_bn_digits"),
          )
          FilterChip(
            selected = !useBengaliDigits,
            onClick = { useBengaliDigits = false },
            label = { Text("ইংরেজি সংখ্যা (123)") },
            modifier =
              Modifier.weight(1f).minimumInteractiveComponentSize().testTag("chip_en_digits"),
          )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Card(
          onClick = { seedDefaults = !seedDefaults },
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = MaterialTheme.shapes.medium,
          modifier =
            Modifier
              .fillMaxWidth()
              .semantics(mergeDescendants = true) {}
              .testTag("checkbox_seed_vegetables"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(checked = seedDefaults, onCheckedChange = { seedDefaults = it })
            Column(modifier = Modifier.padding(start = 8.dp)) {
              Text(
                text = "প্রচলিত ১৫টি বাংলা সবজি দিয়ে শুরু করুন",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = "আলু, পেঁয়াজ, টমেটো, মরিচ, বেগুন, শাকপাতা আগে থেকেই সাজানো থাকবে",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Button(
          onClick = {
            if (shopName.trim().isEmpty()) {
              showNameError = true
            } else {
              onCompleteSetup(shopName, ownerName, useBengaliDigits, seedDefaults)
            }
          },
          enabled = !isSaving,
          modifier =
            Modifier
              .fillMaxWidth()
              .height(BazarDimens.PrimaryButtonHeight)
              .testTag("start_shop_button"),
        ) {
          Text(
            text = if (isSaving) "প্রস্তুত হচ্ছে..." else "দোকান শুরু করুন",
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
    }
  }
}

@Composable
fun PinLockScreen(
  shopName: String,
  lockoutUntilMs: Long = 0L,
  onTryUnlock: (String) -> Boolean,
) {
  val focusManager = LocalFocusManager.current
  var pin by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }
  var remainingSeconds by remember(lockoutUntilMs) { mutableLongStateOf(0L) }

  LaunchedEffect(lockoutUntilMs) {
    while (true) {
      val diff = lockoutUntilMs - System.currentTimeMillis()
      if (diff <= 0L) {
        remainingSeconds = 0L
        break
      }
      remainingSeconds =
        (diff + BusinessCalculator.ROUNDING_CEIL_OFFSET_MS) / BusinessCalculator.MS_PER_SECOND
      delay(BusinessCalculator.PIN_POLL_INTERVAL_MS)
    }
  }

  val isLockedOut = remainingSeconds > 0L

  fun submitPin() {
    if (isLockedOut || pin.length != BusinessCalculator.PIN_LENGTH) return
    if (!onTryUnlock(pin)) {
      errorText = "PIN সঠিক নয়, আবার চেষ্টা করুন"
      pin = ""
    } else {
      focusManager.clearFocus()
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(BazarDimens.Space2xl),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(64.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = "PIN লক",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(16.dp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = shopName.ifBlank { "সবজি খাতা" },
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "অ্যাপ খুলতে আপনার ৪-ডিজিট PIN লিখুন",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (isLockedOut) {
        Spacer(modifier = Modifier.height(12.dp))
        val remainingText = BusinessCalculator.formatNumber(remainingSeconds, true)
        StatusBadge(
          text = "অনেকবার ভুল PIN দেওয়া হয়েছে • $remainingText সেকেন্ড অপেক্ষা করুন",
          tone = BadgeTone.ERROR,
          modifier = Modifier.testTag("pin_lockout_banner"),
        )
      }
      Spacer(modifier = Modifier.height(BazarDimens.SpaceXl))
      OutlinedTextField(
        value = pin,
        onValueChange = { input ->
          val isValidInput =
            !isLockedOut &&
              input.length <= BusinessCalculator.PIN_LENGTH &&
              input.all { it.isDigit() }
          if (isValidInput) {
            pin = input
            errorText = null
          }
        },
        enabled = !isLockedOut,
        label = { Text("৪-ডিজিট PIN") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
          ),
        keyboardActions = KeyboardActions(onDone = { submitPin() }),
        isError = errorText != null && !isLockedOut,
        supportingText = { if (errorText != null && !isLockedOut) Text(errorText!!) },
        singleLine = true,
        modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().testTag("pin_unlock_input"),
      )
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = ::submitPin,
        enabled = !isLockedOut && pin.length == 4,
        modifier =
          Modifier
            .widthIn(max = 280.dp)
            .fillMaxWidth()
            .height(BazarDimens.PrimaryButtonHeight)
            .testTag("pin_unlock_button"),
      ) {
        Text("খুলুন", style = MaterialTheme.typography.titleMedium)
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "অফলাইন লোকাল PIN • খোলার পর সেটিংস থেকে পরিবর্তন বা বন্ধ করতে পারবেন",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 300.dp),
      )
    }
  }
}
