package com.jubayerhossain.sobjidokan.app.di

import android.app.Application
import com.jubayerhossain.sobjidokan.data.backup.BackupManager
import com.jubayerhossain.sobjidokan.data.local.AppDatabase
import com.jubayerhossain.sobjidokan.data.local.dao.ShopDao
import com.jubayerhossain.sobjidokan.data.prefs.AppThemeMode
import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferencesRepository
import com.jubayerhossain.sobjidokan.data.repository.ShopRepository

object AppModule {
  @Volatile private var application: Application? = null

  fun init(app: Application) {
    application = app
  }

  val app: Application
    get() =
      application
        ?: error(
          "AppModule not initialized. Call AppModule.init(application) in Application.onCreate()",
        )

  val database: AppDatabase
    get() = AppDatabase.getInstance(app)

  val shopDao: ShopDao
    get() = database.shopDao()

  val shopRepository: ShopRepository
    get() = ShopRepository(shopDao, database)

  val preferencesRepository: ShopPreferencesRepository
    get() = ShopPreferencesRepository(app)

  val backupManager: BackupManager
    get() = BackupManager(app, shopDao, preferencesRepository)

  val appThemeMode: AppThemeMode
    get() = AppThemeMode.SYSTEM
}
