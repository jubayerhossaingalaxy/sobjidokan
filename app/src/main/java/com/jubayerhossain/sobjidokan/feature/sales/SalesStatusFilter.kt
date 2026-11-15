package com.jubayerhossain.sobjidokan.feature.sales

enum class SalesStatusFilter(
  val labelBn: String,
) {
  ALL("সব"),
  PAID("পরিশোধিত"),
  DUE("বাকি আছে"),
  CANCELLED("বাতিল"),
}
