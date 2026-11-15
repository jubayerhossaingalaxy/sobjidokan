package com.jubayerhossain.sobjidokan.core.common

import java.math.BigDecimal
import java.math.RoundingMode

const val TAG = "SobjiDokan"

fun <T> requireNotNull(
  value: T?,
  message: () -> String,
): T {
  if (value == null) throw IllegalStateException(message())
  return value
}

fun safeDouble(value: Any?): Double =
  when (value) {
    null -> 0.0
    is Double -> if (value.isFinite()) value else 0.0
    is Float -> if (value.isFinite()) value.toDouble() else 0.0
    is Int -> value.toDouble()
    is Long -> value.toDouble()
    else -> 0.0
  }

fun roundToTwo(value: Double): Double =
  if (value.isFinite()) {
    BigDecimal
      .valueOf(
        value,
      ).setScale(2, RoundingMode.HALF_UP)
      .toDouble()
  } else {
    0.0
  }

fun <T> T.alsoIf(
  condition: Boolean,
  block: (T) -> Unit,
): T {
  if (condition) block(this)
  return this
}

@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun <R> runCatchingDefault(
  default: R,
  block: () -> R,
): R =
  try {
    block()
  } catch (e: Exception) {
    default
  }
