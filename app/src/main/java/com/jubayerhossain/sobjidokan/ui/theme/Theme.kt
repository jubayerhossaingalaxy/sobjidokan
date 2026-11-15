package com.jubayerhossain.sobjidokan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.prefs.AppThemeMode

private val LightColorScheme =
  lightColorScheme(
    primary = BazarPrimaryLight,
    onPrimary = BazarOnPrimaryLight,
    primaryContainer = BazarPrimaryContainerLight,
    onPrimaryContainer = BazarOnPrimaryContainerLight,
    secondary = BazarSecondaryLight,
    onSecondary = BazarOnSecondaryLight,
    secondaryContainer = BazarSecondaryContainerLight,
    onSecondaryContainer = BazarOnSecondaryContainerLight,
    tertiary = BazarTertiaryLight,
    onTertiary = BazarOnTertiaryLight,
    tertiaryContainer = BazarTertiaryContainerLight,
    onTertiaryContainer = BazarOnTertiaryContainerLight,
    background = BazarBackgroundLight,
    onBackground = BazarOnBackgroundLight,
    surface = BazarSurfaceLight,
    onSurface = BazarOnSurfaceLight,
    surfaceVariant = BazarSurfaceVariantLight,
    onSurfaceVariant = BazarOnSurfaceVariantLight,
    surfaceContainerLow = BazarSurfaceContainerLowLight,
    outline = BazarOutlineLight,
    outlineVariant = BazarOutlineVariantLight,
    error = BazarErrorLight,
    onError = BazarOnErrorLight,
    errorContainer = BazarErrorContainerLight,
    onErrorContainer = BazarOnErrorContainerLight,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = BazarPrimaryDark,
    onPrimary = BazarOnPrimaryDark,
    primaryContainer = BazarPrimaryContainerDark,
    onPrimaryContainer = BazarOnPrimaryContainerDark,
    secondary = BazarSecondaryDark,
    onSecondary = BazarOnSecondaryDark,
    secondaryContainer = BazarSecondaryContainerDark,
    onSecondaryContainer = BazarOnSecondaryContainerDark,
    tertiary = BazarTertiaryDark,
    onTertiary = BazarOnTertiaryDark,
    tertiaryContainer = BazarTertiaryContainerDark,
    onTertiaryContainer = BazarOnTertiaryContainerDark,
    background = BazarBackgroundDark,
    onBackground = BazarOnBackgroundDark,
    surface = BazarSurfaceDark,
    onSurface = BazarOnSurfaceDark,
    surfaceVariant = BazarSurfaceVariantDark,
    onSurfaceVariant = BazarOnSurfaceVariantDark,
    surfaceContainerLow = BazarSurfaceContainerLowDark,
    outline = BazarOutlineDark,
    outlineVariant = BazarOutlineVariantDark,
    error = BazarErrorDark,
    onError = BazarOnErrorDark,
    errorContainer = BazarErrorContainerDark,
    onErrorContainer = BazarOnErrorContainerDark,
  )

@Immutable
data class BazarSemanticColors(
  val success: Color,
  val successContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val info: Color,
  val infoContainer: Color,
)

val LocalBazarColors =
  staticCompositionLocalOf {
    BazarSemanticColors(
      success = BazarSuccessLight,
      successContainer = BazarSuccessContainerLight,
      warning = BazarWarningLight,
      warningContainer = BazarWarningContainerLight,
      info = BazarInfoLight,
      infoContainer = BazarInfoContainerLight,
    )
  }

val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun sobjiKhataTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  darkTheme: Boolean =
    when (themeMode) {
      AppThemeMode.SYSTEM -> isSystemInDarkTheme()
      AppThemeMode.LIGHT -> false
      AppThemeMode.DARK -> true
    },
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val semanticColors =
    if (darkTheme) {
      BazarSemanticColors(
        success = BazarSuccessDark,
        successContainer = BazarSuccessContainerDark,
        warning = BazarWarningDark,
        warningContainer = BazarWarningContainerDark,
        info = BazarInfoDark,
        infoContainer = BazarInfoContainerDark,
      )
    } else {
      BazarSemanticColors(
        success = BazarSuccessLight,
        successContainer = BazarSuccessContainerLight,
        warning = BazarWarningLight,
        warningContainer = BazarWarningContainerLight,
        info = BazarInfoLight,
        infoContainer = BazarInfoContainerLight,
      )
    }

  CompositionLocalProvider(LocalBazarColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes,
      content = content,
    )
  }
}
