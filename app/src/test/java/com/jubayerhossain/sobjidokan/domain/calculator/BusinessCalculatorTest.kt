package com.jubayerhossain.sobjidokan.domain.calculator

import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessCalculatorTest {
  @Test
  fun calculatesLineTotalsUsingStoredBaseUnits() {
    assertEquals(60.0, BusinessCalculator.calculateLineTotal(1_500L, UnitType.KG, 40.0), 0.0)
    assertEquals(30.0, BusinessCalculator.calculateLineTotal(3L, UnitType.PIECE, 10.0), 0.0)
  }

  @Test
  fun roundsLineTotalsHalfUp() {
    assertEquals(1.01, BusinessCalculator.calculateLineTotal(1L, UnitType.PIECE, 1.005), 0.0)
  }

  @Test
  fun rejectsInvalidLineTotals() {
    assertEquals(0.0, BusinessCalculator.calculateLineTotal(0L, UnitType.KG, 50.0), 0.0)
    assertEquals(0.0, BusinessCalculator.calculateLineTotal(1L, UnitType.KG, -1.0), 0.0)
    assertEquals(0.0, BusinessCalculator.calculateLineTotal(1L, UnitType.KG, Double.NaN), 0.0)
    assertEquals(
      0.0,
      BusinessCalculator.calculateLineTotal(1L, UnitType.KG, Double.POSITIVE_INFINITY),
      0.0,
    )
  }

  @Test
  fun calculatesWeightedAverageCostAcrossStockBatches() {
    val averageCost =
      BusinessCalculator.calculateWeightedAverageCost(
        currentStockUnits = 1_000L,
        currentAvgCost = 80.0,
        addedUnits = 2_000L,
        addedLineTotal = 200.0,
        unitType = UnitType.KG,
      )

    assertEquals(93.33, averageCost, 0.0)
  }

  @Test
  fun calculatesWastageLossFromAverageCost() {
    assertEquals(40.0, BusinessCalculator.calculateWastageLoss(500L, 80.0, UnitType.KG), 0.0)
    assertEquals(0.0, BusinessCalculator.calculateWastageLoss(0L, 80.0, UnitType.KG), 0.0)
  }

  @Test
  fun roundsNonFiniteAmountsSafely() {
    assertEquals(1.01, BusinessCalculator.round2(1.005), 0.0)
    assertEquals(-1.01, BusinessCalculator.round2(-1.005), 0.0)
    assertEquals(0.0, BusinessCalculator.round2(Double.NaN), 0.0)
  }

  @Test
  fun formatsDigitsAndQuantitiesForBengaliUsers() {
    assertEquals("দাম: ১২.৫%", BusinessCalculator.toDisplayDigits("দাম: 12.5%", true))
    assertEquals("-১ কেজি ৫০০ গ্রাম", BusinessCalculator.formatQuantity(-1_500L, UnitType.KG, true))
    assertEquals("1,234", BusinessCalculator.formatNumber(1_234, false))
    assertEquals("১,২৩৪", BusinessCalculator.formatNumber(1_234, true))
  }
}
