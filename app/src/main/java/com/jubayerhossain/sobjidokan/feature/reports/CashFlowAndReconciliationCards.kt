@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.domain.calculator.FinancialSummary
import com.jubayerhossain.sobjidokan.ui.components.PnlKeyValueRow
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors

@Composable
internal fun CashFlowAndReconciliationCards(
  periodSummary: FinancialSummary,
  useBengaliDigits: Boolean,
) {
  val colors = LocalBazarColors.current

  Column(verticalArrangement = Arrangement.spacedBy(BazarDimens.SpaceMd)) {
    Card(
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text("নগদ টাকার হিসাব", style = MaterialTheme.typography.titleMedium)
        PnlKeyValueRow(
          label = "ক্যাশে এল (নগদ বিক্রি + বাকি আদায়)",
          value = BusinessCalculator.formatTaka(periodSummary.cashInflow, useBengaliDigits),
        )
        PnlKeyValueRow(
          label = "ক্যাশ থেকে গেল (ক্রয় + খরচ + দেনা শোধ)",
          value = BusinessCalculator.formatTaka(periodSummary.cashOutflow, useBengaliDigits),
        )
        HorizontalDivider()
        PnlKeyValueRow(
          label = "নিট নগদ প্রবাহ",
          value =
            BusinessCalculator.formatTaka(
              periodSummary.netCashFlow,
              useBengaliDigits,
              showSign = true,
            ),
          bold = true,
        )
      }
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border =
        BorderStroke(
          BazarDimens.CardHairlineBorder,
          MaterialTheme.colorScheme.outlineVariant,
        ),
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier.fillMaxWidth().testTag("eod_cash_reconcile_card"),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("দিন শেষ ক্যাশ বাক্স মিলানো", style = MaterialTheme.typography.titleMedium)
        PnlKeyValueRow(
          label = "নগদ বিক্রি ও বাকি আদায় (+)",
          value =
            BusinessCalculator.formatTaka(
              periodSummary.cashSalesAndCollections,
              useBengaliDigits,
            ),
          valueColor = colors.success,
        )
        PnlKeyValueRow(
          label = "নগদ ক্রয়, খরচ ও দেনা শোধ (-)",
          value = BusinessCalculator.formatTaka(periodSummary.cashOutflowOnly, useBengaliDigits),
          valueColor = MaterialTheme.colorScheme.secondary,
        )
        HorizontalDivider()
        PnlKeyValueRow(
          label = "ক্যাশ বাক্সে থাকার কথা",
          value =
            BusinessCalculator.formatTaka(
              periodSummary.expectedCashInDrawer,
              useBengaliDigits,
              showSign = true,
            ),
          bold = true,
          labelColor = MaterialTheme.colorScheme.primary,
          valueColor = MaterialTheme.colorScheme.primary,
        )
        if (periodSummary.mobileSalesAndCollections > BusinessCalculator.AMOUNT_EPSILON) {
          PnlKeyValueRow(
            label = "বিকাশ / মোবাইল জমা (বাক্সের বাইরে)",
            value =
              BusinessCalculator.formatTaka(
                periodSummary.mobileSalesAndCollections,
                useBengaliDigits,
              ),
            labelColor = colors.info,
            valueColor = colors.info,
          )
        }
      }
    }
  }
}
