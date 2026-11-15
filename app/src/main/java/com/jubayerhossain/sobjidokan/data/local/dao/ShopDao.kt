package com.jubayerhossain.sobjidokan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jubayerhossain.sobjidokan.data.local.entity.DuePaymentEntity
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.InventoryMovementEntity
import com.jubayerhossain.sobjidokan.data.local.entity.MovementType
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageReason
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import kotlinx.coroutines.flow.Flow

data class ShopDbSnapshot(
  val vegetables: List<VegetableEntity>,
  val parties: List<PartyEntity>,
  val purchases: List<PurchaseEntity>,
  val purchaseItems: List<PurchaseItemEntity>,
  val sales: List<SaleEntity>,
  val saleItems: List<SaleItemEntity>,
  val wastages: List<WastageEntity>,
  val expenses: List<ExpenseEntity>,
  val duePayments: List<DuePaymentEntity>,
  val movements: List<InventoryMovementEntity>,
)

@Dao
interface ShopDao {
  @Query("SELECT * FROM vegetables ORDER BY isArchived ASC, name ASC")
  fun observeAllVegetables(): Flow<List<VegetableEntity>>

  @Query("SELECT * FROM vegetables WHERE isArchived = 0 ORDER BY name ASC")
  fun observeActiveVegetables(): Flow<List<VegetableEntity>>

  @Query("SELECT * FROM vegetables WHERE id = :id")
  suspend fun getVegetableById(id: Long): VegetableEntity?

  @Query("SELECT * FROM vegetables WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
  suspend fun findVegetableByName(name: String): VegetableEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertVegetable(veg: VegetableEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVegetables(vegs: List<VegetableEntity>)

  @Update suspend fun updateVegetable(veg: VegetableEntity)

  @Query("SELECT * FROM parties ORDER BY name ASC")
  fun observeAllParties(): Flow<List<PartyEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertParty(party: PartyEntity): Long

  @Query("SELECT * FROM purchases ORDER BY timestamp DESC, id DESC")
  fun observeAllPurchases(): Flow<List<PurchaseEntity>>

  @Query("SELECT * FROM purchase_items")
  fun observeAllPurchaseItems(): Flow<List<PurchaseItemEntity>>

  @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
  suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity>

  @Insert suspend fun insertPurchase(purchase: PurchaseEntity): Long

  @Insert suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

  @Query("SELECT * FROM purchases WHERE id = :purchaseId")
  suspend fun getPurchaseById(purchaseId: Long): PurchaseEntity?

  @Query("UPDATE purchases SET isCancelled = 1 WHERE id = :purchaseId")
  suspend fun markPurchaseCancelled(purchaseId: Long)

  @Query("SELECT * FROM sales ORDER BY timestamp DESC, id DESC")
  fun observeAllSales(): Flow<List<SaleEntity>>

  @Query("SELECT * FROM sales WHERE id = :saleId")
  suspend fun getSaleById(saleId: Long): SaleEntity?

  @Query("SELECT * FROM sale_items")
  fun observeAllSaleItems(): Flow<List<SaleItemEntity>>

  @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
  suspend fun getSaleItems(saleId: Long): List<SaleItemEntity>

  @Insert suspend fun insertSale(sale: SaleEntity): Long

  @Insert suspend fun insertSaleItems(items: List<SaleItemEntity>)

  @Query("UPDATE sales SET isCancelled = 1 WHERE id = :saleId")
  suspend fun markSaleCancelled(saleId: Long)

  @Query("SELECT * FROM wastages ORDER BY timestamp DESC, id DESC")
  fun observeAllWastages(): Flow<List<WastageEntity>>

  @Query("SELECT * FROM wastages WHERE id = :wastageId")
  suspend fun getWastageById(wastageId: Long): WastageEntity?

  @Query("UPDATE wastages SET isCancelled = 1 WHERE id = :wastageId")
  suspend fun markWastageCancelled(wastageId: Long)

  @Insert suspend fun insertWastage(wastage: WastageEntity): Long

  @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
  fun observeAllExpenses(): Flow<List<ExpenseEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateExpense(expense: ExpenseEntity): Long

  @Query("UPDATE expenses SET isCancelled = 1 WHERE id = :expenseId")
  suspend fun markExpenseCancelled(expenseId: Long)

  @Query("SELECT * FROM due_payments ORDER BY timestamp DESC, id DESC")
  fun observeAllDuePayments(): Flow<List<DuePaymentEntity>>

  @Insert suspend fun insertDuePayment(payment: DuePaymentEntity): Long

  @Query("UPDATE due_payments SET isCancelled = 1 WHERE id = :paymentId")
  suspend fun markDuePaymentCancelled(paymentId: Long)

  @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC, id DESC LIMIT 200")
  fun observeRecentMovements(): Flow<List<InventoryMovementEntity>>

  @Insert suspend fun insertMovement(movement: InventoryMovementEntity)

  // Snapshot getters for JSON backup and atomic UI state loading
  @Query("SELECT * FROM vegetables ORDER BY isArchived ASC, name ASC")
  suspend fun getAllVegetablesSnapshot(): List<VegetableEntity>

  @Query("SELECT * FROM parties ORDER BY name ASC")
  suspend fun getAllPartiesSnapshot(): List<PartyEntity>

  @Query("SELECT * FROM purchases ORDER BY timestamp DESC, id DESC")
  suspend fun getAllPurchasesSnapshot(): List<PurchaseEntity>

  @Query("SELECT * FROM purchase_items")
  suspend fun getAllPurchaseItemsSnapshot(): List<PurchaseItemEntity>

  @Query("SELECT * FROM sales ORDER BY timestamp DESC, id DESC")
  suspend fun getAllSalesSnapshot(): List<SaleEntity>

  @Query("SELECT * FROM sale_items")
  suspend fun getAllSaleItemsSnapshot(): List<SaleItemEntity>

  @Query("SELECT * FROM wastages ORDER BY timestamp DESC, id DESC")
  suspend fun getAllWastagesSnapshot(): List<WastageEntity>

  @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
  suspend fun getAllExpensesSnapshot(): List<ExpenseEntity>

  @Query("SELECT * FROM due_payments ORDER BY timestamp DESC, id DESC")
  suspend fun getAllDuePaymentsSnapshot(): List<DuePaymentEntity>

  @Query("SELECT * FROM inventory_movements")
  suspend fun getAllMovementsSnapshot(): List<InventoryMovementEntity>

  @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC, id DESC LIMIT 200")
  suspend fun getRecentMovementsSnapshot(): List<InventoryMovementEntity>

  @Transaction
  suspend fun loadAtomicSnapshot(): ShopDbSnapshot =
    ShopDbSnapshot(
      vegetables = getAllVegetablesSnapshot(),
      parties = getAllPartiesSnapshot(),
      purchases = getAllPurchasesSnapshot(),
      purchaseItems = getAllPurchaseItemsSnapshot(),
      sales = getAllSalesSnapshot(),
      saleItems = getAllSaleItemsSnapshot(),
      wastages = getAllWastagesSnapshot(),
      expenses = getAllExpensesSnapshot(),
      duePayments = getAllDuePaymentsSnapshot(),
      movements = getRecentMovementsSnapshot(),
    )

  // Atomic transactions
  @Transaction
  suspend fun completePurchaseAtomic(
    purchase: PurchaseEntity,
    items: List<PurchaseItemEntity>,
  ): Long {
    val purchaseId = insertPurchase(purchase)
    val attachedItems = items.map { it.copy(purchaseId = purchaseId) }
    insertPurchaseItems(attachedItems)
    for (item in attachedItems) {
      val veg = getVegetableById(item.vegetableId) ?: continue
      val newAvgCost =
        BusinessCalculator.calculateWeightedAverageCost(
          currentStockUnits = veg.stockUnits,
          currentAvgCost = veg.avgCostPerUnit,
          addedUnits = item.quantityUnits,
          addedLineTotal = item.lineTotal,
          unitType = veg.unitType,
        )
      val newStock = veg.stockUnits + item.quantityUnits
      updateVegetable(veg.copy(stockUnits = newStock, avgCostPerUnit = newAvgCost))
      insertMovement(
        InventoryMovementEntity(
          vegetableId = veg.id,
          vegetableName = veg.name,
          movementType = MovementType.PURCHASE,
          deltaUnits = item.quantityUnits,
          previousStock = veg.stockUnits,
          newStock = newStock,
          reason = "ক্রয় #$purchaseId",
          referenceId = purchaseId,
          timestamp = purchase.timestamp,
        ),
      )
    }
    return purchaseId
  }

  @Transaction
  suspend fun cancelPurchaseAtomic(purchaseId: Long) {
    val purchase = getPurchaseById(purchaseId) ?: return
    if (purchase.isCancelled) return
    val items = getPurchaseItems(purchaseId)
    markPurchaseCancelled(purchaseId)
    for (item in items) {
      val veg = getVegetableById(item.vegetableId) ?: continue
      val newStock = veg.stockUnits - item.quantityUnits
      updateVegetable(veg.copy(stockUnits = newStock))
      insertMovement(
        InventoryMovementEntity(
          vegetableId = veg.id,
          vegetableName = veg.name,
          movementType = MovementType.RETURN_CANCEL,
          deltaUnits = -item.quantityUnits,
          previousStock = veg.stockUnits,
          newStock = newStock,
          reason = "ক্রয় বাতিল #$purchaseId",
          referenceId = purchaseId,
        ),
      )
    }
  }

  @Transaction
  suspend fun completeSaleAtomic(
    sale: SaleEntity,
    items: List<SaleItemEntity>,
  ): Long {
    val saleId = insertSale(sale)
    val attachedItems =
      items.map { item ->
        val veg = getVegetableById(item.vegetableId)
        val costSnapshot = veg?.avgCostPerUnit ?: item.costSnapshotPerUnit
        item.copy(saleId = saleId, costSnapshotPerUnit = costSnapshot)
      }
    insertSaleItems(attachedItems)
    for (item in attachedItems) {
      val veg = getVegetableById(item.vegetableId) ?: continue
      val newStock = veg.stockUnits - item.quantityUnits
      updateVegetable(veg.copy(stockUnits = newStock))
      insertMovement(
        InventoryMovementEntity(
          vegetableId = veg.id,
          vegetableName = veg.name,
          movementType = MovementType.SALE,
          deltaUnits = -item.quantityUnits,
          previousStock = veg.stockUnits,
          newStock = newStock,
          reason = "বিক্রি #$saleId",
          referenceId = saleId,
          timestamp = sale.timestamp,
        ),
      )
    }
    return saleId
  }

  @Transaction
  suspend fun cancelSaleAtomic(saleId: Long) {
    val sale = getSaleById(saleId) ?: return
    if (sale.isCancelled) return
    val items = getSaleItems(saleId)
    markSaleCancelled(saleId)
    for (item in items) {
      val veg = getVegetableById(item.vegetableId) ?: continue
      val newStock = veg.stockUnits + item.quantityUnits
      updateVegetable(veg.copy(stockUnits = newStock))
      insertMovement(
        InventoryMovementEntity(
          vegetableId = veg.id,
          vegetableName = veg.name,
          movementType = MovementType.RETURN_CANCEL,
          deltaUnits = item.quantityUnits,
          previousStock = veg.stockUnits,
          newStock = newStock,
          reason = "বিক্রি বাতিল/ফেরত #$saleId",
          referenceId = saleId,
        ),
      )
    }
  }

  @Transaction
  suspend fun recordWastageAtomic(
    vegetableId: Long,
    quantityUnits: Long,
    reason: WastageReason,
    notes: String,
    timestamp: Long,
  ) {
    val veg = getVegetableById(vegetableId) ?: return
    val loss =
      BusinessCalculator.calculateWastageLoss(quantityUnits, veg.avgCostPerUnit, veg.unitType)
    val wastageId =
      insertWastage(
        WastageEntity(
          vegetableId = veg.id,
          vegetableName = veg.name,
          unitType = veg.unitType,
          quantityUnits = quantityUnits,
          unitCostSnapshot = veg.avgCostPerUnit,
          lossAmount = loss,
          reason = reason,
          notes = notes,
          timestamp = timestamp,
        ),
      )
    val newStock = veg.stockUnits - quantityUnits
    updateVegetable(veg.copy(stockUnits = newStock))
    insertMovement(
      InventoryMovementEntity(
        vegetableId = veg.id,
        vegetableName = veg.name,
        movementType = MovementType.WASTAGE,
        deltaUnits = -quantityUnits,
        previousStock = veg.stockUnits,
        newStock = newStock,
        reason = "${reason.labelBn} (#$wastageId)",
        referenceId = wastageId,
        timestamp = timestamp,
      ),
    )
  }

  @Transaction
  suspend fun cancelWastageAtomic(wastageId: Long) {
    val wastage = getWastageById(wastageId) ?: return
    if (wastage.isCancelled) return
    markWastageCancelled(wastageId)
    val veg = getVegetableById(wastage.vegetableId) ?: return
    val newStock = veg.stockUnits + wastage.quantityUnits
    updateVegetable(veg.copy(stockUnits = newStock))
    insertMovement(
      InventoryMovementEntity(
        vegetableId = veg.id,
        vegetableName = veg.name,
        movementType = MovementType.RETURN_CANCEL,
        deltaUnits = wastage.quantityUnits,
        previousStock = veg.stockUnits,
        newStock = newStock,
        reason = "অপচয় বাতিল #$wastageId",
        referenceId = wastageId,
      ),
    )
  }

  @Transaction
  suspend fun adjustStockAtomic(
    vegetableId: Long,
    actualNewStockUnits: Long,
    reason: String,
  ) {
    val veg = getVegetableById(vegetableId) ?: return
    val delta = actualNewStockUnits - veg.stockUnits
    updateVegetable(veg.copy(stockUnits = actualNewStockUnits))
    insertMovement(
      InventoryMovementEntity(
        vegetableId = veg.id,
        vegetableName = veg.name,
        movementType = MovementType.ADJUSTMENT,
        deltaUnits = delta,
        previousStock = veg.stockUnits,
        newStock = actualNewStockUnits,
        reason = reason.ifBlank { "বাস্তব স্টক সমন্বয়" },
      ),
    )
  }

  @Transaction
  suspend fun bulkUpdateSellingPricesAtomic(updates: Map<Long, Double>): Int {
    var updatedCount = 0
    updates.forEach { (vegId, rawNewPrice) ->
      if (!rawNewPrice.isFinite() || rawNewPrice <= 0.0) return@forEach
      val rounded = BusinessCalculator.round2(rawNewPrice)
      if (rounded <= 0.0) return@forEach
      val current = getVegetableById(vegId) ?: return@forEach
      if (
        kotlin.math.abs(current.sellingPricePerUnit - rounded) >=
        BusinessCalculator.AMOUNT_EPSILON
      ) {
        updateVegetable(current.copy(sellingPricePerUnit = rounded))
        updatedCount++
      }
    }
    return updatedCount
  }

  @Query("DELETE FROM vegetables")
  suspend fun clearVegetables()

  @Query("DELETE FROM parties")
  suspend fun clearParties()

  @Query("DELETE FROM purchases")
  suspend fun clearPurchases()

  @Query("DELETE FROM purchase_items")
  suspend fun clearPurchaseItems()

  @Query("DELETE FROM sales")
  suspend fun clearSales()

  @Query("DELETE FROM sale_items")
  suspend fun clearSaleItems()

  @Query("DELETE FROM wastages")
  suspend fun clearWastages()

  @Query("DELETE FROM expenses")
  suspend fun clearExpenses()

  @Query("DELETE FROM due_payments")
  suspend fun clearDuePayments()

  @Query("DELETE FROM inventory_movements")
  suspend fun clearMovements()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restoreParties(items: List<PartyEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restorePurchases(items: List<PurchaseEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restorePurchaseItems(items: List<PurchaseItemEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restoreSales(items: List<SaleEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restoreSaleItems(items: List<SaleItemEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restoreWastages(items: List<WastageEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restoreExpenses(items: List<ExpenseEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restoreDuePayments(items: List<DuePaymentEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun restoreMovements(items: List<InventoryMovementEntity>)

  @Transaction
  suspend fun restoreFullBackupAtomic(
    vegetables: List<VegetableEntity>,
    parties: List<PartyEntity>,
    purchases: List<PurchaseEntity>,
    purchaseItems: List<PurchaseItemEntity>,
    sales: List<SaleEntity>,
    saleItems: List<SaleItemEntity>,
    wastages: List<WastageEntity>,
    expenses: List<ExpenseEntity>,
    duePayments: List<DuePaymentEntity>,
    movements: List<InventoryMovementEntity>,
  ) {
    clearMovements()
    clearDuePayments()
    clearExpenses()
    clearWastages()
    clearSaleItems()
    clearSales()
    clearPurchaseItems()
    clearPurchases()
    clearParties()
    clearVegetables()

    insertVegetables(vegetables)
    restoreParties(parties)
    restorePurchases(purchases)
    restorePurchaseItems(purchaseItems)
    restoreSales(sales)
    restoreSaleItems(saleItems)
    restoreWastages(wastages)
    restoreExpenses(expenses)
    restoreDuePayments(duePayments)
    restoreMovements(movements)
  }
}
