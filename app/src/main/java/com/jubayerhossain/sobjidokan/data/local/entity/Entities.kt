package com.jubayerhossain.sobjidokan.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class VegetableCategory(
  val labelBn: String,
) {
  LEAFY("শাকপাতা"),
  ROOT("মূল ও কন্দ"),
  FRUIT_VEG("ফল জাতীয়"),
  SPICE_OTHER("মসলা ও অন্যান্য"),
}

@Serializable
enum class UnitType(
  val labelBn: String,
  val baseFactor: Long,
) {
  KG("কেজি", 1000L), // stored in grams
  PIECE("পিস", 1L), // stored in count
  BUNCH("আঁটি", 1L), // stored in count
}

@Serializable
enum class PartyType(
  val labelBn: String,
) {
  CUSTOMER("ক্রেতা"),
  SUPPLIER("পাইকার"),
}

@Serializable
enum class PaymentMethod(
  val labelBn: String,
) {
  CASH("নগদ"),
  MOBILE("বিকাশ/নগদ"),
  CREDIT("বাকি"),
}

@Serializable
enum class ExpenseCategory(
  val labelBn: String,
) {
  RENT("দোকান ভাড়া"),
  TRANSPORT("পরিবহন ও ভ্যান"),
  PACKAGING("পলিথিন ও ব্যাগ"),
  UTILITY("বিদ্যুৎ ও পানি"),
  WAGES("শ্রমিক মজুরি"),
  OTHER("অন্যান্য"),
}

@Serializable
enum class WastageReason(
  val labelBn: String,
) {
  ROTTEN("পচে যাওয়া"),
  SHRUNK("শুকিয়ে যাওয়া"),
  DAMAGED("আঘাত বা নষ্ট"),
}

@Serializable
enum class MovementType(
  val labelBn: String,
) {
  PURCHASE("ক্রয়"),
  SALE("বিক্রি"),
  WASTAGE("অপচয়"),
  ADJUSTMENT("স্টক সমন্বয়"),
  RETURN_CANCEL("বাতিল/ফেরত"),
}

@Serializable
@Entity(tableName = "vegetables", indices = [Index(value = ["isArchived", "name"])])
data class VegetableEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val category: VegetableCategory,
  val unitType: UnitType,
  val stockUnits: Long = 0L, // grams for KG, count for PIECE/BUNCH
  val avgCostPerUnit: Double = 0.0, // price per 1 KG or per 1 PIECE/BUNCH
  val sellingPricePerUnit: Double = 0.0,
  val lowStockThresholdUnits: Long = 2000L, // 2 kg or 2 pieces default
  val isArchived: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "parties", indices = [Index(value = ["partyType", "name"])])
data class PartyEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val phone: String = "",
  val partyType: PartyType,
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
  tableName = "purchases",
  indices = [Index(value = ["timestamp"]), Index(value = ["supplierId", "isCancelled"])],
)
data class PurchaseEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val supplierId: Long? = null,
  val supplierName: String = "নগদ বাজার",
  val itemsTotal: Double,
  val extraCost: Double = 0.0,
  val paidAmount: Double,
  val dueAmount: Double = 0.0,
  val notes: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val isCancelled: Boolean = false,
)

@Serializable
@Entity(
  tableName = "purchase_items",
  indices = [Index(value = ["purchaseId"]), Index(value = ["vegetableId"])],
)
data class PurchaseItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val purchaseId: Long,
  val vegetableId: Long,
  val vegetableName: String,
  val unitType: UnitType,
  val quantityUnits: Long,
  val unitCost: Double,
  val lineTotal: Double,
)

@Serializable
@Entity(
  tableName = "sales",
  indices = [Index(value = ["timestamp"]), Index(value = ["customerId", "isCancelled"])],
)
data class SaleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val customerId: Long? = null,
  val customerName: String = "সাধারণ ক্রেতা",
  val subtotal: Double,
  val discount: Double = 0.0,
  val totalAmount: Double,
  val paidAmount: Double,
  val dueAmount: Double = 0.0,
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val notes: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val isCancelled: Boolean = false,
)

@Serializable
@Entity(
  tableName = "sale_items",
  indices = [Index(value = ["saleId"]), Index(value = ["vegetableId"])],
)
data class SaleItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val saleId: Long,
  val vegetableId: Long,
  val vegetableName: String,
  val unitType: UnitType,
  val quantityUnits: Long,
  val sellingPricePerUnit: Double,
  val costSnapshotPerUnit: Double,
  val lineTotal: Double,
)

@Serializable
@Entity(
  tableName = "wastages",
  indices = [Index(value = ["timestamp"]), Index(value = ["vegetableId", "isCancelled"])],
)
data class WastageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val vegetableId: Long,
  val vegetableName: String,
  val unitType: UnitType,
  val quantityUnits: Long,
  val unitCostSnapshot: Double,
  val lossAmount: Double,
  val reason: WastageReason,
  val notes: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val isCancelled: Boolean = false,
)

@Serializable
@Entity(tableName = "expenses", indices = [Index(value = ["timestamp", "isCancelled"])])
data class ExpenseEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val category: ExpenseCategory,
  val amount: Double,
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val isCancelled: Boolean = false,
)

@Serializable
@Entity(
  tableName = "due_payments",
  indices =
    [Index(value = ["partyId", "partyType", "isCancelled"]), Index(value = ["timestamp"])],
)
data class DuePaymentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val partyId: Long,
  val partyName: String,
  val partyType: PartyType,
  val amount: Double,
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val isCancelled: Boolean = false,
)

@Serializable
@Entity(
  tableName = "inventory_movements",
  indices = [Index(value = ["timestamp"]), Index(value = ["vegetableId"])],
)
data class InventoryMovementEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val vegetableId: Long,
  val vegetableName: String,
  val movementType: MovementType,
  val deltaUnits: Long,
  val previousStock: Long,
  val newStock: Long,
  val reason: String = "",
  val referenceId: Long? = null,
  val timestamp: Long = System.currentTimeMillis(),
)
