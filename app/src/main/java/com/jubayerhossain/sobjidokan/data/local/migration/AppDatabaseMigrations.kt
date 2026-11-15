package com.jubayerhossain.sobjidokan.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
  val MIGRATION_1_2 =
    object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_vegetables_isArchived_name " +
            "ON vegetables (`isArchived`, `name`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_parties_partyType_name " +
            "ON parties (`partyType`, `name`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_purchases_timestamp " +
            "ON purchases (`timestamp`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_purchases_supplierId_isCancelled " +
            "ON purchases (`supplierId`, `isCancelled`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_purchase_items_purchaseId " +
            "ON purchase_items (`purchaseId`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_purchase_items_vegetableId " +
            "ON purchase_items (`vegetableId`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_sales_timestamp " + "ON sales (`timestamp`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_sales_customerId_isCancelled " +
            "ON sales (`customerId`, `isCancelled`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_sale_items_saleId " + "ON sale_items (`saleId`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_sale_items_vegetableId " +
            "ON sale_items (`vegetableId`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_wastages_timestamp " + "ON wastages (`timestamp`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_wastages_vegetableId_isCancelled " +
            "ON wastages (`vegetableId`, `isCancelled`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_expenses_timestamp_isCancelled " +
            "ON expenses (`timestamp`, `isCancelled`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_due_payments_partyId_partyType_isCancelled " +
            "ON due_payments (`partyId`, `partyType`, `isCancelled`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_due_payments_timestamp " +
            "ON due_payments (`timestamp`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_inventory_movements_timestamp " +
            "ON inventory_movements (`timestamp`)",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_inventory_movements_vegetableId " +
            "ON inventory_movements (`vegetableId`)",
        )
      }
    }

  val ALL_MIGRATIONS = listOf(MIGRATION_1_2)
}
