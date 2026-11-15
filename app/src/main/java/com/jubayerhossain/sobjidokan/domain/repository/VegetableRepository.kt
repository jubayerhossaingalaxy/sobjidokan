package com.jubayerhossain.sobjidokan.domain.repository

import com.jubayerhossain.sobjidokan.domain.model.VegetableSummary
import kotlinx.coroutines.flow.Flow

interface VegetableRepository {
  fun getAllVegetables(): Flow<List<VegetableSummary>>

  fun getActiveVegetables(): Flow<List<VegetableSummary>>

  fun getVegetableById(id: Long): VegetableSummary?

  fun getLowStockVegetables(): Flow<List<VegetableSummary>>
}
