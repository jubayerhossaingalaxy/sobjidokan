package com.jubayerhossain.sobjidokan.feature.inventory

enum class StockFilter(
  val labelBn: String,
) {
  ALL("সব"),
  LOW("স্টক কম"),
  DEFICIT("ঘাটতি"),
  ARCHIVED("আর্কাইভ"),
}
