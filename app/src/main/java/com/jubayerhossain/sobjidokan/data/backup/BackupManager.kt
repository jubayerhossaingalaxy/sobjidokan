package com.jubayerhossain.sobjidokan.data.backup

import android.content.Context
import android.net.Uri
import com.jubayerhossain.sobjidokan.data.local.dao.ShopDao
import com.jubayerhossain.sobjidokan.data.local.entity.DuePaymentEntity
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.InventoryMovementEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageEntity
import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferences
import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SobjiKhataBackupPayload(
  val schemaTag: String = SCHEMA_TAG,
  val version: Int = 1,
  val exportedAt: Long = System.currentTimeMillis(),
  val shopName: String = "",
  val ownerName: String = "",
  val useBengaliDigits: Boolean = true,
  val showCreditLedger: Boolean = true,
  val vegetables: List<VegetableEntity> = emptyList(),
  val parties: List<PartyEntity> = emptyList(),
  val purchases: List<PurchaseEntity> = emptyList(),
  val purchaseItems: List<PurchaseItemEntity> = emptyList(),
  val sales: List<SaleEntity> = emptyList(),
  val saleItems: List<SaleItemEntity> = emptyList(),
  val wastages: List<WastageEntity> = emptyList(),
  val expenses: List<ExpenseEntity> = emptyList(),
  val duePayments: List<DuePaymentEntity> = emptyList(),
  val movements: List<InventoryMovementEntity> = emptyList(),
) {
  companion object {
    const val SCHEMA_TAG = "sobjikhata_backup_v1"
  }
}

class BackupManager(
  private val context: Context,
  private val dao: ShopDao,
  private val prefsRepo: ShopPreferencesRepository,
) {
  private val json =
    Json {
      ignoreUnknownKeys = true
      prettyPrint = true
      encodeDefaults = true
    }

  suspend fun exportToUri(uri: Uri): Result<Long> =
    withContext(Dispatchers.IO) {
      runCatching {
        val prefs: ShopPreferences = prefsRepo.preferencesFlow.first()
        val now = System.currentTimeMillis()
        val payload =
          SobjiKhataBackupPayload(
            exportedAt = now,
            shopName = prefs.shopName,
            ownerName = prefs.ownerName,
            useBengaliDigits = prefs.useBengaliDigits,
            showCreditLedger = prefs.showCreditLedger,
            vegetables = dao.getAllVegetablesSnapshot(),
            parties = dao.getAllPartiesSnapshot(),
            purchases = dao.getAllPurchasesSnapshot(),
            purchaseItems = dao.getAllPurchaseItemsSnapshot(),
            sales = dao.getAllSalesSnapshot(),
            saleItems = dao.getAllSaleItemsSnapshot(),
            wastages = dao.getAllWastagesSnapshot(),
            expenses = dao.getAllExpensesSnapshot(),
            duePayments = dao.getAllDuePaymentsSnapshot(),
            movements = dao.getAllMovementsSnapshot(),
          )
        val encoded = json.encodeToString(SobjiKhataBackupPayload.serializer(), payload)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
          stream.write(encoded.toByteArray(Charsets.UTF_8))
          stream.flush()
        } ?: error("ফাইল লেখা সম্ভব হয়নি")
        prefsRepo.updateLastBackup(now)
        now
      }
    }

  companion object {
    private const val MAX_BACKUP_BYTES =
      10 * 1024 * 1024 // 10 MB guard against malformed oversized inputs
  }

  suspend fun validateBackupUri(uri: Uri): Result<SobjiKhataBackupPayload> =
    withContext(Dispatchers.IO) {
      runCatching {
        val rawBytes =
          context.contentResolver.openInputStream(uri)?.use { stream ->
            val chunk = ByteArray(16 * 1024)
            val out = java.io.ByteArrayOutputStream()
            var totalRead = 0
            while (true) {
              val n = stream.read(chunk)
              if (n <= 0) break
              totalRead += n
              require(totalRead <= MAX_BACKUP_BYTES) {
                "ব্যাকআপ ফাইলটি অত্যধিক বড় (১০ মেগাবাইটের বেশি)"
              }
              out.write(chunk, 0, n)
            }
            out.toByteArray()
          } ?: error("ফাইল পড়া যায়নি")

        val text = rawBytes.toString(Charsets.UTF_8)
        require(text.isNotBlank() && text.trimStart().startsWith("{")) { "অবৈধ ব্যাকআপ ফাইল" }
        val payload = json.decodeFromString(SobjiKhataBackupPayload.serializer(), text)
        require(payload.schemaTag == SobjiKhataBackupPayload.SCHEMA_TAG && payload.version == 1) {
          "অবৈধ বা অসমর্থিত ব্যাকআপ সংস্করণ"
        }
        validatePayloadIntegrity(payload)
        payload
      }
    }

  private fun validatePayloadIntegrity(payload: SobjiKhataBackupPayload) {
    require(payload.exportedAt > 0L) { "ব্যাকআপ তারিখ অবৈধ" }
    validateUniqueIds(payload)
    validateCatalogAndParties(payload)
    validatePurchases(payload)
    validateSales(payload)
    validateAdjustments(payload)
  }

  private fun validateUniqueIds(payload: SobjiKhataBackupPayload) {
    requireUnique(payload.vegetables, { it.id }, "ব্যাকআপে সবজির ডুপ্লিকেট আইডি পাওয়া গেছে")
    requireUnique(payload.parties, { it.id }, "ব্যাকআপে ক্রেতা/পাইকারের ডুপ্লিকেট আইডি পাওয়া গেছে")
    requireUnique(payload.sales, { it.id }, "ব্যাকআপে বিক্রির ডুপ্লিকেট আইডি পাওয়া গেছে")
    requireUnique(payload.purchases, { it.id }, "ব্যাকআপে ক্রয়ের ডুপ্লিকেট আইডি পাওয়া গেছে")
    requireUnique(payload.saleItems, { it.id }, "ব্যাকআপে বিক্রির আইটেমে ডুপ্লিকেট আইডি পাওয়া গেছে")
    requireUnique(
      payload.purchaseItems,
      { it.id },
      "ব্যাকআপে ক্রয়ের আইটেমে ডুপ্লিকেট আইডি পাওয়া গেছে",
    )
    requireUnique(payload.wastages, { it.id }, "ব্যাকআপে অপচয়ের ডুপ্লিকেট আইডি পাওয়া গেছে")
    requireUnique(payload.expenses, { it.id }, "ব্যাকআপে খরচের ডুপ্লিকেট আইডি পাওয়া গেছে")
    requireUnique(
      payload.duePayments,
      { it.id },
      "ব্যাকআপে বকেয়া লেনদেনের ডুপ্লিকেট আইডি পাওয়া গেছে",
    )
    requireUnique(payload.movements, { it.id }, "ব্যাকআপে স্টক লগে ডুপ্লিকেট আইডি পাওয়া গেছে")
  }

  private inline fun <T, K> requireUnique(
    items: List<T>,
    keySelector: (T) -> K,
    message: String,
  ) {
    require(items.distinctBy(keySelector).size == items.size) { message }
  }

  private fun validateCatalogAndParties(payload: SobjiKhataBackupPayload) {
    require(
      payload.vegetables.all {
        it.name.isNotBlank() &&
          isNonNegativeFinite(it.avgCostPerUnit) &&
          isNonNegativeFinite(it.sellingPricePerUnit) &&
          it.lowStockThresholdUnits >= 0L
      },
    ) {
      "ব্যাকআপে সবজির তথ্য ত্রুটিপূর্ণ"
    }
    require(payload.parties.all { it.name.isNotBlank() }) {
      "ব্যাকআপে ক্রেতা/পাইকারের তথ্য ত্রুটিপূর্ণ"
    }
  }

  private fun validateSales(payload: SobjiKhataBackupPayload) {
    val saleIds = payload.sales.map { it.id }.toHashSet()
    require(payload.saleItems.all { it.saleId in saleIds }) {
      "ব্যাকআপে বিচ্ছিন্ন বিক্রির আইটেম পাওয়া গেছে"
    }
    require(
      payload.sales.all {
        isNonNegativeFinite(it.subtotal) &&
          isNonNegativeFinite(it.discount) &&
          isNonNegativeFinite(it.totalAmount) &&
          isNonNegativeFinite(it.paidAmount) &&
          isNonNegativeFinite(it.dueAmount) &&
          it.timestamp >= 0L
      },
    ) {
      "ব্যাকআপে বিক্রির হিসাব ত্রুটিপূর্ণ"
    }
    require(
      payload.saleItems.all {
        it.quantityUnits > 0L &&
          isNonNegativeFinite(it.sellingPricePerUnit) &&
          isNonNegativeFinite(it.costSnapshotPerUnit) &&
          isNonNegativeFinite(it.lineTotal)
      },
    ) {
      "ব্যাকআপে বিক্রির আইটেম ত্রুটিপূর্ণ"
    }
  }

  private fun validatePurchases(payload: SobjiKhataBackupPayload) {
    val purchaseIds = payload.purchases.map { it.id }.toHashSet()
    require(payload.purchaseItems.all { it.purchaseId in purchaseIds }) {
      "ব্যাকআপে বিচ্ছিন্ন ক্রয়ের আইটেম পাওয়া গেছে"
    }
    require(
      payload.purchases.all {
        isNonNegativeFinite(it.itemsTotal) &&
          isNonNegativeFinite(it.extraCost) &&
          isNonNegativeFinite(it.paidAmount) &&
          isNonNegativeFinite(it.dueAmount) &&
          it.timestamp >= 0L
      },
    ) {
      "ব্যাকআপে ক্রয়ের হিসাব ত্রুটিপূর্ণ"
    }
    require(
      payload.purchaseItems.all {
        it.quantityUnits > 0L &&
          isNonNegativeFinite(it.unitCost) &&
          isNonNegativeFinite(it.lineTotal)
      },
    ) {
      "ব্যাকআপে ক্রয়ের আইটেম ত্রুটিপূর্ণ"
    }
  }

  private fun validateAdjustments(payload: SobjiKhataBackupPayload) {
    require(
      payload.wastages.all {
        it.quantityUnits > 0L &&
          isNonNegativeFinite(it.unitCostSnapshot) &&
          isNonNegativeFinite(it.lossAmount) &&
          it.timestamp >= 0L
      },
    ) {
      "ব্যাকআপে অপচয়ের তথ্য ত্রুটিপূর্ণ"
    }
    require(payload.expenses.all { isNonNegativeFinite(it.amount) && it.timestamp >= 0L }) {
      "ব্যাকআপে খরচের তথ্য ত্রুটিপূর্ণ"
    }
    require(
      payload.duePayments.all { it.amount.isFinite() && it.amount > 0.0 && it.timestamp >= 0L },
    ) {
      "ব্যাকআপে বকেয়া পরিশোধের তথ্য ত্রুটিপূর্ণ"
    }
    require(payload.movements.all { it.vegetableName.isNotBlank() && it.timestamp >= 0L }) {
      "ব্যাকআপে স্টক লগের তথ্য ত্রুটিপূর্ণ"
    }
  }

  private fun isNonNegativeFinite(value: Double): Boolean = value.isFinite() && value >= 0.0

  suspend fun restoreFromPayload(payload: SobjiKhataBackupPayload): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        dao.restoreFullBackupAtomic(
          vegetables = payload.vegetables,
          parties = payload.parties,
          purchases = payload.purchases,
          purchaseItems = payload.purchaseItems,
          sales = payload.sales,
          saleItems = payload.saleItems,
          wastages = payload.wastages,
          expenses = payload.expenses,
          duePayments = payload.duePayments,
          movements = payload.movements,
        )
        if (payload.shopName.isNotBlank()) {
          prefsRepo.saveInitialSetup(
            shopName = payload.shopName,
            ownerName = payload.ownerName,
            useBengaliDigits = payload.useBengaliDigits,
          )
          prefsRepo.setShowCreditLedger(payload.showCreditLedger)
        }
      }
    }
}
