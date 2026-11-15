@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.local.entity.DuePaymentEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.ui.PartyBalanceSummary
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyPickerSheet(
  title: String,
  defaultWalkInLabel: String,
  parties: List<PartyEntity>,
  selectedPartyId: Long?,
  partyType: PartyType,
  bn: Boolean,
  balanceByPartyId: Map<Long, Double> = emptyMap(),
  onSelectWalkIn: () -> Unit,
  onSelectParty: (PartyEntity) -> Unit,
  onAddNewClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(parties, query) {
      val q = query.trim()
      if (q.isEmpty()) {
        parties
      } else {
        parties.filter {
          it.name.contains(q, ignoreCase = true) || it.phone.contains(q, ignoreCase = true)
        }
      }
    }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .imePadding()
          .navigationBarsPadding()
          .padding(horizontal = BazarDimens.SheetHorizontal, vertical = BazarDimens.SpaceSm),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        TextButton(
          onClick = onAddNewClick,
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("picker_add_party_button"),
        ) {
          Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("নতুন ${partyType.labelBn}")
        }
      }
      Spacer(modifier = Modifier.height(BazarDimens.SpaceSm))
      SearchFieldBar(
        query = query,
        onQueryChange = { query = it },
        placeholder = "নাম বা মোবাইল নম্বর দিয়ে খুঁজুন...",
        testTag = "party_picker_search",
      )
      Spacer(modifier = Modifier.height(BazarDimens.SpaceMd))

      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(BazarDimens.SpaceSm),
        contentPadding = PaddingValues(bottom = BazarDimens.SpaceLg),
      ) {
        item(key = "walk_in_default") {
          val isWalkInSelected = selectedPartyId == null
          Card(
            onClick = {
              onSelectWalkIn()
              onDismiss()
            },
            colors =
              CardDefaults.cardColors(
                containerColor =
                  if (isWalkInSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  },
              ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().testTag("picker_walkin_option"),
          ) {
            Row(
              modifier = Modifier.padding(14.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = defaultWalkInLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (isWalkInSelected) {
                Icon(
                  Icons.Filled.Check,
                  contentDescription = "নির্বাচিত",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        if (filtered.isEmpty() && parties.isNotEmpty()) {
          item {
            Text(
              text = "খোঁজা অনুযায়ী কোনো ${partyType.labelBn} পাওয়া যায়নি।",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 12.dp),
            )
          }
        }

        items(filtered, key = { it.id }) { party ->
          val isSelected = selectedPartyId == party.id
          val due = balanceByPartyId[party.id] ?: 0.0
          Card(
            onClick = {
              onSelectParty(party)
              onDismiss()
            },
            colors =
              CardDefaults.cardColors(
                containerColor =
                  if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                  } else {
                    MaterialTheme.colorScheme.surface
                  },
              ),
            border =
              BorderStroke(
                width = if (isSelected) 1.5.dp else BazarDimens.CardHairlineBorder,
                color =
                  if (isSelected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.outlineVariant
                  },
              ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().testTag("picker_party_${party.id}"),
          ) {
            Row(
              modifier = Modifier.padding(14.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(party.name, style = MaterialTheme.typography.titleMedium)
                if (party.phone.isNotBlank()) {
                  Text(
                    party.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                if (due > BusinessCalculator.AMOUNT_EPSILON) {
                  val duePrefix = if (partyType == PartyType.CUSTOMER) "পাওনা" else "দেনা"
                  StatusBadge(
                    text = "$duePrefix ${BusinessCalculator.formatTaka(due, bn)}",
                    tone = BadgeTone.WARNING,
                  )
                }
                if (isSelected) {
                  Icon(
                    Icons.Filled.Check,
                    contentDescription = "নির্বাচিত",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailSheet(
  summary: PartyBalanceSummary,
  sales: List<SaleEntity>,
  purchases: List<PurchaseEntity>,
  payments: List<DuePaymentEntity>,
  bn: Boolean,
  shopName: String = "",
  onDismiss: () -> Unit,
  onPayClick: () -> Unit,
  onStartSaleForCustomer: (() -> Unit)? = null,
  onOpenSaleDetail: ((SaleEntity) -> Unit)? = null,
  onOpenPurchaseDetail: ((PurchaseEntity) -> Unit)? = null,
  onCancelDuePayment: ((Long) -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val bazarColors = LocalBazarColors.current
  val context = LocalContext.current
  val isCustomer = summary.party.partyType == PartyType.CUSTOMER
  var selectedSubTab by remember(summary.party.id) { mutableIntStateOf(0) }
  var cancellingPayment by remember(summary.party.id) { mutableStateOf<DuePaymentEntity?>(null) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = BazarDimens.SheetHorizontal, vertical = BazarDimens.SpaceSm),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = summary.party.name, style = MaterialTheme.typography.headlineSmall)
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            StatusBadge(
              text = summary.party.partyType.labelBn,
              tone = if (isCustomer) BadgeTone.INFO else BadgeTone.NEUTRAL,
            )
            if (summary.party.phone.isNotBlank()) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  Icons.Filled.Phone,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = summary.party.phone,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          if (summary.party.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = summary.party.notes,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(BazarDimens.SpaceMd))
      // 3-metric balance breakdown
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BazarDimens.SpaceSm),
      ) {
        PartyStatTile(
          label = if (isCustomer) "মোট কেনাকাটা" else "মোট সরবরাহ",
          value = BusinessCalculator.formatTaka(summary.totalBilled, bn),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        PartyStatTile(
          label = if (isCustomer) "মোট জমা" else "মোট পরিশোধ",
          value = BusinessCalculator.formatTaka(summary.totalPaid, bn),
          color = bazarColors.success,
          modifier = Modifier.weight(1f),
        )
        PartyStatTile(
          label = if (isCustomer) "বর্তমান পাওনা" else "বর্তমান দেনা",
          value = BusinessCalculator.formatTaka(summary.balanceDue, bn),
          color =
            if (summary.balanceDue > BusinessCalculator.AMOUNT_EPSILON) {
              MaterialTheme.colorScheme.error
            } else {
              bazarColors.success
            },
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(BazarDimens.SpaceMd))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BazarDimens.SpaceSm),
      ) {
        if (summary.balanceDue > BusinessCalculator.AMOUNT_EPSILON) {
          Button(
            onClick = onPayClick,
            modifier = Modifier.weight(1f).height(48.dp).testTag("detail_party_pay_button"),
          ) {
            Icon(
              Icons.Filled.AccountBalanceWallet,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isCustomer) "টাকা আদায়" else "টাকা পরিশোধ")
          }
        }
        if (isCustomer && onStartSaleForCustomer != null) {
          FilledTonalButton(
            onClick = onStartSaleForCustomer,
            modifier = Modifier.weight(1f).height(48.dp).testTag("detail_party_new_sale_button"),
          ) {
            Icon(
              Icons.Filled.PointOfSale,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("নতুন বিক্রি")
          }
        }
      }
      if (isCustomer && summary.balanceDue > BusinessCalculator.AMOUNT_EPSILON) {
        Spacer(modifier = Modifier.height(BazarDimens.SpaceSm))
        OutlinedButton(
          onClick = {
            val store = shopName.ifBlank { "সবজি খাতা" }
            val dueFormatted = BusinessCalculator.formatTaka(summary.balanceDue, bn)
            val msg =
              "আসসালামু আলাইকুম ${summary.party.name}, \"$store\"-এ আপনার বর্তমান " +
                "বকেয়া $dueFormatted। সুবিধামতো পরিশোধ করলে কৃতজ্ঞ থাকব।"
            val sendIntent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
              }
            try {
              context.startActivity(Intent.createChooser(sendIntent, "বাকি তাগাদা পাঠান"))
            } catch (_: Exception) {
              // No share target available on test environment or device
            }
          },
          modifier =
            Modifier.fillMaxWidth().height(48.dp).testTag("detail_party_share_due_button"),
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("বাকি তাগাদা পাঠান (WhatsApp/SMS)")
        }
      }

      Spacer(modifier = Modifier.height(BazarDimens.SpaceMd))
      val billsCount = if (isCustomer) sales.size else purchases.size
      TabRow(selectedTabIndex = selectedSubTab) {
        Tab(
          selected = selectedSubTab == 0,
          onClick = { selectedSubTab = 0 },
          text = {
            val billsLabel = if (isCustomer) "বিক্রি বিল" else "ক্রয় বিল"
            Text("$billsLabel (${BusinessCalculator.formatNumber(billsCount, bn)})")
          },
          modifier = Modifier.testTag("party_detail_tab_bills"),
        )
        Tab(
          selected = selectedSubTab == 1,
          onClick = { selectedSubTab = 1 },
          text = { Text("বকেয়া লেনদেন (${BusinessCalculator.formatNumber(payments.size, bn)})") },
          modifier = Modifier.testTag("party_detail_tab_payments"),
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 340.dp),
        contentPadding = PaddingValues(vertical = BazarDimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(BazarDimens.SpaceSm),
      ) {
        if (selectedSubTab == 0) {
          if (isCustomer) {
            if (sales.isEmpty()) {
              item {
                Text(
                  text = "এই ক্রেতার নামে এখনো কোনো বিক্রি রেকর্ড নেই।",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(vertical = 16.dp),
                )
              }
            } else {
              items(sales, key = { it.id }) { sale ->
                Card(
                  onClick = { onOpenSaleDetail?.invoke(sale) },
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                  shape = MaterialTheme.shapes.medium,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "বিল #${BusinessCalculator.formatNumber(sale.id, bn)}",
                        style = MaterialTheme.typography.titleSmall,
                      )
                      Text(
                        text = BusinessCalculator.formatDateTime(sale.timestamp, bn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = BusinessCalculator.formatTaka(sale.totalAmount, bn),
                        style =
                          MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration =
                              if (sale.isCancelled) {
                                TextDecoration.LineThrough
                              } else {
                                TextDecoration.None
                              },
                          ),
                        color =
                          if (sale.isCancelled) {
                            MaterialTheme.colorScheme.error
                          } else {
                            MaterialTheme.colorScheme.primary
                          },
                      )
                      when {
                        sale.isCancelled -> {
                          StatusBadge("বাতিল", BadgeTone.ERROR)
                        }

                        sale.dueAmount > BusinessCalculator.AMOUNT_EPSILON -> {
                          StatusBadge(
                            "বাকি ${BusinessCalculator.formatTaka(sale.dueAmount, bn)}",
                            BadgeTone.WARNING,
                          )
                        }

                        else -> {
                          StatusBadge("পরিশোধিত", BadgeTone.SUCCESS)
                        }
                      }
                    }
                  }
                }
              }
            }
          } else {
            if (purchases.isEmpty()) {
              item {
                Text(
                  text = "এই পাইকারের কাছ থেকে এখনো কোনো ক্রয় রেকর্ড নেই।",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(vertical = 16.dp),
                )
              }
            } else {
              items(purchases, key = { it.id }) { purchase ->
                Card(
                  onClick = { onOpenPurchaseDetail?.invoke(purchase) },
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                  shape = MaterialTheme.shapes.medium,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "ক্রয় #${BusinessCalculator.formatNumber(purchase.id, bn)}",
                        style = MaterialTheme.typography.titleSmall,
                      )
                      Text(
                        text = BusinessCalculator.formatDateTime(purchase.timestamp, bn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = BusinessCalculator.formatTaka(purchase.itemsTotal, bn),
                        style =
                          MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration =
                              if (purchase.isCancelled) {
                                TextDecoration.LineThrough
                              } else {
                                TextDecoration.None
                              },
                          ),
                        color = MaterialTheme.colorScheme.secondary,
                      )
                      when {
                        purchase.isCancelled -> {
                          StatusBadge("বাতিল", BadgeTone.ERROR)
                        }

                        purchase.dueAmount > BusinessCalculator.AMOUNT_EPSILON -> {
                          StatusBadge(
                            "দেনা ${BusinessCalculator.formatTaka(purchase.dueAmount, bn)}",
                            BadgeTone.WARNING,
                          )
                        }

                        else -> {
                          StatusBadge("পরিশোধিত", BadgeTone.SUCCESS)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        } else {
          if (payments.isEmpty()) {
            item {
              Text(
                text = "এখনো আলাদা কোনো বকেয়া আদায় বা পরিশোধ হয়নি।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
              )
            }
          } else {
            items(payments, key = { it.id }) { payment ->
              Card(
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                  ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("due_payment_card_${payment.id}"),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp).fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      val noteSuffix = if (payment.note.isNotBlank()) " • ${payment.note}" else ""
                      Text(
                        text = "${payment.paymentMethod.labelBn}$noteSuffix",
                        style = MaterialTheme.typography.titleSmall,
                      )
                      if (payment.isCancelled) {
                        StatusBadge(text = "বাতিল", tone = BadgeTone.ERROR)
                      }
                    }
                    Text(
                      text = BusinessCalculator.formatDateTime(payment.timestamp, bn),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = BusinessCalculator.formatTaka(payment.amount, bn),
                      style =
                        MaterialTheme.typography.titleSmall.copy(
                          fontWeight = FontWeight.Bold,
                          textDecoration =
                            if (payment.isCancelled) {
                              TextDecoration.LineThrough
                            } else {
                              TextDecoration.None
                            },
                        ),
                      color =
                        if (payment.isCancelled) {
                          MaterialTheme.colorScheme.error
                        } else {
                          bazarColors.success
                        },
                    )
                    if (!payment.isCancelled && onCancelDuePayment != null) {
                      IconButton(
                        onClick = { cancellingPayment = payment },
                        modifier =
                          Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cancel_due_payment_${payment.id}"),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Cancel,
                          contentDescription = "লেনদেন বাতিল করুন",
                          tint = MaterialTheme.colorScheme.error,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      HorizontalDivider()
      Spacer(modifier = Modifier.height(8.dp))
    }
  }

  cancellingPayment?.let { payment ->
    val actionLabel = if (isCustomer) "আদায়" else "পরিশোধ"
    val formattedAmount = BusinessCalculator.formatTaka(payment.amount, bn)
    ConfirmationDialog(
      title = "বকেয়া লেনদেন বাতিল করবেন?",
      message =
        "$formattedAmount পরিমাণ এই $actionLabel এন্ট্রিটি বাতিল করলে " +
          "বকেয়া হিসাব আগের অবস্থায় ফিরে যাবে।",
      confirmLabel = "হ্যাঁ, বাতিল করুন",
      isDestructive = true,
      onConfirm = {
        onCancelDuePayment?.invoke(payment.id)
        cancellingPayment = null
      },
      onDismiss = { cancellingPayment = null },
    )
  }
}

@Composable
private fun PartyStatTile(
  label: String,
  value: String,
  color: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = MaterialTheme.shapes.medium,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddPartyDialog(
  partyType: PartyType,
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var showError by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .imePadding()
          .navigationBarsPadding()
          .padding(horizontal = BazarDimens.SheetHorizontal, vertical = BazarDimens.SpaceSm),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(text = "নতুন ${partyType.labelBn}", style = MaterialTheme.typography.headlineSmall)
      OutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          showError = false
        },
        label = { Text("নাম *") },
        isError = showError,
        supportingText = { if (showError) Text("অনুগ্রহ করে নাম লিখুন") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("quick_add_party_name"),
      )
      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("মোবাইল নম্বর (ঐচ্ছিক)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("quick_add_party_phone"),
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_add_party_cancel"),
        ) {
          Text("বাতিল")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = { if (name.isBlank()) showError = true else onSave(name, phone) },
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("quick_add_party_confirm"),
        ) {
          Text("যোগ করুন")
        }
      }
    }
  }
}
