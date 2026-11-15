@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors

private const val MIN_PROGRESS_BAR_FRACTION = 0.06f

@Composable
fun StatusBadge(
  text: String,
  tone: BadgeTone,
  icon: ImageVector? = null,
  modifier: Modifier = Modifier,
) {
  val bazarColors = LocalBazarColors.current
  val (bg, fg) =
    when (tone) {
      BadgeTone.SUCCESS -> {
        bazarColors.successContainer to bazarColors.success
      }

      BadgeTone.WARNING -> {
        bazarColors.warningContainer to bazarColors.warning
      }

      BadgeTone.ERROR -> {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
      }

      BadgeTone.INFO -> {
        bazarColors.infoContainer to bazarColors.info
      }

      BadgeTone.NEUTRAL -> {
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
      }
    }

  Surface(
    modifier = modifier.semantics(mergeDescendants = true) {},
    color = bg,
    shape = MaterialTheme.shapes.small,
  ) {
    Row(
      modifier =
        Modifier.padding(horizontal = BazarDimens.SpaceSm, vertical = BazarDimens.SpaceXs),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(BazarDimens.SpaceXs),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = fg,
          modifier = Modifier.size(14.dp),
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  actionTestTag: String? = null,
  modifier: Modifier = Modifier,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
    )
    if (trailingContent != null) {
      trailingContent()
    } else if (actionLabel != null && onAction != null) {
      TextButton(
        onClick = onAction,
        modifier =
          Modifier.minimumInteractiveComponentSize().let {
            if (actionTestTag != null) it.testTag(actionTestTag) else it
          },
      ) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
fun SearchFieldBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  testTag: String = "search_field",
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = modifier.fillMaxWidth().testTag(testTag),
    placeholder = {
      Text(
        text = placeholder,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = "খুঁজুন",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("${testTag}_clear"),
        ) {
          Icon(imageVector = Icons.Filled.Close, contentDescription = "খোঁজা মুছুন")
        }
      }
    },
    singleLine = true,
    shape = MaterialTheme.shapes.medium,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
  )
}

@Composable
fun MetricSummaryCard(
  title: String,
  valueText: String,
  subtitle: String? = null,
  icon: ImageVector,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val content: @Composable () -> Unit = {
    Column(modifier = Modifier.padding(BazarDimens.CardPadding)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.height(BazarDimens.SpaceSm))
      Text(
        text = valueText,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = accentColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(BazarDimens.SpaceXs))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }

  if (onClick != null) {
    Card(
      onClick = onClick,
      modifier = modifier.semantics(mergeDescendants = true) {},
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = containerColor),
      border =
        BorderStroke(BazarDimens.CardHairlineBorder, MaterialTheme.colorScheme.outlineVariant),
    ) {
      content()
    }
  } else {
    Card(
      modifier = modifier.semantics(mergeDescendants = true) {},
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = containerColor),
      border =
        BorderStroke(BazarDimens.CardHairlineBorder, MaterialTheme.colorScheme.outlineVariant),
    ) {
      content()
    }
  }
}

@Composable
fun PnlKeyValueRow(
  label: String,
  value: String,
  bold: Boolean = false,
  labelColor: Color = MaterialTheme.colorScheme.onSurface,
  valueColor: Color = labelColor,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style =
        if (bold) {
          MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        } else {
          MaterialTheme.typography.bodyMedium
        },
      color = labelColor,
      modifier = Modifier.weight(1f).padding(end = BazarDimens.SpaceSm),
    )
    Text(
      text = value,
      style =
        if (bold) {
          MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        } else {
          MaterialTheme.typography.bodyMedium
        },
      color = valueColor,
    )
  }
}

@Composable
fun StockHealthBar(
  currentUnits: Long,
  thresholdUnits: Long,
  modifier: Modifier = Modifier,
) {
  val bazarColors = LocalBazarColors.current
  val safeThreshold = thresholdUnits.coerceAtLeast(1L)
  val targetFull = (safeThreshold * 3L).toFloat()
  val fraction = (currentUnits.coerceAtLeast(0L).toFloat() / targetFull).coerceIn(0f, 1f)
  val barColor =
    when {
      currentUnits <= 0L -> MaterialTheme.colorScheme.error
      currentUnits <= thresholdUnits -> bazarColors.warning
      else -> bazarColors.success
    }
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(5.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
  ) {
    if (fraction > 0f) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth(fraction.coerceAtLeast(MIN_PROGRESS_BAR_FRACTION))
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(barColor),
      )
    }
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  tip: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(BazarDimens.Space2xl),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.size(BazarDimens.EmptyStateBadgeSize),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(18.dp),
      )
    }
    Spacer(modifier = Modifier.height(BazarDimens.SpaceLg))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = tip,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(modifier = Modifier.height(BazarDimens.SpaceLg))
      Button(
        onClick = onAction,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("empty_state_action"),
      ) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
fun ConfirmationDialog(
  title: String,
  message: String,
  confirmLabel: String,
  dismissLabel: String = "না, ফিরে যান",
  isDestructive: Boolean = false,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = if (isDestructive) Icons.Filled.Warning else Icons.Filled.CheckCircle,
        contentDescription = null,
        tint =
          if (isDestructive) {
            MaterialTheme.colorScheme.error
          } else {
            LocalBazarColors.current.warning
          },
      )
    },
    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors =
          if (isDestructive) {
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            )
          } else {
            ButtonDefaults.buttonColors()
          },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("confirm_dialog_yes"),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("confirm_dialog_no"),
      ) {
        Text(dismissLabel)
      }
    },
  )
}
