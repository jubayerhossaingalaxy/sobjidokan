package com.jubayerhossain.sobjidokan.ui

import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableCategory
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageReason
import com.jubayerhossain.sobjidokan.data.repository.ShopRepository
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import kotlinx.coroutines.flow.MutableSharedFlow

/** Owns vegetable catalog, pricing, stock adjustment, and wastage actions. */
internal class ShopInventoryActions(
  private val repo: ShopRepository,
  private val snackbarEvents: MutableSharedFlow<String>,
  private val uiStateProvider: () -> ShopUiState,
  private val launchSerializedSave: (suspend () -> Unit) -> Unit,
) {
  fun saveVegetable(
    existingId: Long?,
    name: String,
    category: VegetableCategory,
    unitType: UnitType,
    avgCost: Double,
    sellingPrice: Double,
    lowStockThreshold: Long,
    initialStock: Long,
    onResult: (Result<Unit>) -> Unit,
  ) {
    launchSerializedSave {
      @Suppress("TooGenericExceptionCaught")
      try {
        val result =
          repo.saveVegetable(
            existingId = existingId,
            name = name,
            category = category,
            unitType = unitType,
            avgCost = avgCost,
            sellingPrice = sellingPrice,
            lowStockThreshold = lowStockThreshold,
            initialStock = initialStock,
          )
        if (result.isSuccess) {
          snackbarEvents.tryEmit(
            if (existingId == null) "নতুন সবজি যোগ হয়েছে" else "সবজির তথ্য আপডেট হয়েছে",
          )
        }
        onResult(result)
      } catch (e: Exception) {
        snackbarEvents.tryEmit("সবজির তথ্য সংরক্ষণ করা যায়নি।")
        onResult(Result.failure(e))
      }
    }
  }

  fun toggleArchiveVegetable(veg: VegetableEntity) {
    launchSerializedSave {
      try {
        repo.setVegetableArchived(veg, !veg.isArchived)
        snackbarEvents.tryEmit(
          if (!veg.isArchived) {
            "${veg.name} আর্কাইভ করা হয়েছে"
          } else {
            "${veg.name} পুনরায় চালু হয়েছে"
          },
        )
      } catch (_: Exception) {
        snackbarEvents.tryEmit("সবজির অবস্থা পরিবর্তন করা যায়নি।")
      }
    }
  }

  fun bulkUpdateVegetableSellingPrices(
    updates: Map<Long, Double>,
    onSuccess: () -> Unit = {},
  ) {
    if (updates.isEmpty()) {
      onSuccess()
      return
    }
    launchSerializedSave {
      try {
        val count = repo.bulkUpdateSellingPrices(updates)
        if (count > 0) {
          val bn = uiStateProvider().preferences.useBengaliDigits
          snackbarEvents.tryEmit(
            "সকালের দর আপডেট হয়েছে (${BusinessCalculator.formatNumber(count, bn)}টি)",
          )
        } else {
          snackbarEvents.tryEmit("কোনো দরের পরিবর্তন পাওয়া যায়নি")
        }
        onSuccess()
      } catch (_: Exception) {
        snackbarEvents.tryEmit("দর আপডেট সংরক্ষণ করা যায়নি।")
      }
    }
  }

  fun adjustStock(
    vegId: Long,
    actualStockUnits: Long,
    reason: String,
    onSuccess: () -> Unit = {},
  ) {
    if (actualStockUnits < 0L) {
      snackbarEvents.tryEmit("বাস্তব স্টক ঋণাত্মক হতে পারে না।")
      return
    }
    launchSerializedSave {
      try {
        repo.adjustStock(vegId, actualStockUnits, reason)
        snackbarEvents.tryEmit("বাস্তব স্টক সমন্বয় সম্পন্ন হয়েছে")
        onSuccess()
      } catch (_: Exception) {
        snackbarEvents.tryEmit("স্টক সমন্বয় করা যায়নি।")
      }
    }
  }

  fun recordWastage(
    vegId: Long,
    quantityUnits: Long,
    reason: WastageReason,
    notes: String,
    onSuccess: () -> Unit = {},
  ) {
    if (quantityUnits <= 0L) {
      snackbarEvents.tryEmit("অপচয়ের পরিমাণ শূন্যের বেশি হতে হবে।")
      return
    }
    launchSerializedSave {
      try {
        repo.recordWastage(vegId, quantityUnits, reason, notes)
        snackbarEvents.tryEmit("পচা/নষ্ট অপচয় সংরক্ষিত হয়েছে")
        onSuccess()
      } catch (_: Exception) {
        snackbarEvents.tryEmit("অপচয় সংরক্ষণ করা যায়নি।")
      }
    }
  }

  fun cancelWastage(wastageId: Long) {
    launchSerializedSave {
      try {
        repo.cancelWastage(wastageId)
        snackbarEvents.tryEmit("অপচয় বাতিল হয়েছে ও স্টক ফেরত এসেছে")
      } catch (_: Exception) {
        snackbarEvents.tryEmit("অপচয় বাতিল করা যায়নি।")
      }
    }
  }
}
