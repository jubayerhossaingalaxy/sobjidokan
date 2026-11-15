package com.jubayerhossain.sobjidokan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jubayerhossain.sobjidokan.data.local.dao.ShopDao
import com.jubayerhossain.sobjidokan.data.local.entity.DuePaymentEntity
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseCategory
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.InventoryMovementEntity
import com.jubayerhossain.sobjidokan.data.local.entity.MovementType
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableCategory
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageReason

class ShopTypeConverters {
  @TypeConverter fun fromVegetableCategory(v: VegetableCategory): String = v.name

  @TypeConverter
  fun toVegetableCategory(v: String): VegetableCategory = VegetableCategory.valueOf(v)

  @TypeConverter fun fromUnitType(v: UnitType): String = v.name

  @TypeConverter fun toUnitType(v: String): UnitType = UnitType.valueOf(v)

  @TypeConverter fun fromPartyType(v: PartyType): String = v.name

  @TypeConverter fun toPartyType(v: String): PartyType = PartyType.valueOf(v)

  @TypeConverter fun fromPaymentMethod(v: PaymentMethod): String = v.name

  @TypeConverter fun toPaymentMethod(v: String): PaymentMethod = PaymentMethod.valueOf(v)

  @TypeConverter fun fromExpenseCategory(v: ExpenseCategory): String = v.name

  @TypeConverter fun toExpenseCategory(v: String): ExpenseCategory = ExpenseCategory.valueOf(v)

  @TypeConverter fun fromWastageReason(v: WastageReason): String = v.name

  @TypeConverter fun toWastageReason(v: String): WastageReason = WastageReason.valueOf(v)

  @TypeConverter fun fromMovementType(v: MovementType): String = v.name

  @TypeConverter fun toMovementType(v: String): MovementType = MovementType.valueOf(v)
}

@Database(
  entities =
    [
      VegetableEntity::class,
      PartyEntity::class,
      PurchaseEntity::class,
      PurchaseItemEntity::class,
      SaleEntity::class,
      SaleItemEntity::class,
      WastageEntity::class,
      ExpenseEntity::class,
      DuePaymentEntity::class,
      InventoryMovementEntity::class,
    ],
  version = 2,
  exportSchema = false,
)
@TypeConverters(ShopTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun shopDao(): ShopDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    private fun createIndex(
      db: SupportSQLiteDatabase,
      name: String,
      table: String,
      cols: String,
    ) {
      db.execSQL("CREATE INDEX IF NOT EXISTS `$name` ON `$table` ($cols)")
    }

    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          createIndex(
            db,
            "index_vegetables_isArchived_name",
            "vegetables",
            "`isArchived`, `name`",
          )
          createIndex(db, "index_parties_partyType_name", "parties", "`partyType`, `name`")
          createIndex(db, "index_purchases_timestamp", "purchases", "`timestamp`")
          createIndex(
            db,
            "index_purchases_supplierId_isCancelled",
            "purchases",
            "`supplierId`, `isCancelled`",
          )
          createIndex(db, "index_purchase_items_purchaseId", "purchase_items", "`purchaseId`")
          createIndex(db, "index_purchase_items_vegetableId", "purchase_items", "`vegetableId`")
          createIndex(db, "index_sales_timestamp", "sales", "`timestamp`")
          createIndex(
            db,
            "index_sales_customerId_isCancelled",
            "sales",
            "`customerId`, `isCancelled`",
          )
          createIndex(db, "index_sale_items_saleId", "sale_items", "`saleId`")
          createIndex(db, "index_sale_items_vegetableId", "sale_items", "`vegetableId`")
          createIndex(db, "index_wastages_timestamp", "wastages", "`timestamp`")
          createIndex(
            db,
            "index_wastages_vegetableId_isCancelled",
            "wastages",
            "`vegetableId`, `isCancelled`",
          )
          createIndex(
            db,
            "index_expenses_timestamp_isCancelled",
            "expenses",
            "`timestamp`, `isCancelled`",
          )
          createIndex(
            db,
            "index_due_payments_partyId_partyType_isCancelled",
            "due_payments",
            "`partyId`, `partyType`, `isCancelled`",
          )
          createIndex(db, "index_due_payments_timestamp", "due_payments", "`timestamp`")
          createIndex(
            db,
            "index_inventory_movements_timestamp",
            "inventory_movements",
            "`timestamp`",
          )
          createIndex(
            db,
            "index_inventory_movements_vegetableId",
            "inventory_movements",
            "`vegetableId`",
          )
        }
      }

    fun getInstance(context: Context): AppDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room
              .databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "sobji_khata.db",
              ).addMigrations(MIGRATION_1_2)
              .build()
              .also { instance = it }
        }

    @androidx.annotation.VisibleForTesting
    internal fun resetInstanceForTest() {
      synchronized(this) {
        instance?.close()
        instance = null
      }
    }
  }
}
