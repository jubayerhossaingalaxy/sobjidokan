package com.jubayerhossain.sobjidokan.domain.usecase

import com.jubayerhossain.sobjidokan.domain.model.VegetableSummary
import com.jubayerhossain.sobjidokan.domain.repository.VegetableRepository
import kotlinx.coroutines.flow.Flow

class VegetableUseCases(
  private val repository: VegetableRepository,
) {
  operator fun invoke(activeOnly: Boolean = true): Flow<List<VegetableSummary>> =
    if (activeOnly) repository.getActiveVegetables() else repository.getAllVegetables()

  fun getLowStockVegetables(): Flow<List<VegetableSummary>> = repository.getLowStockVegetables()

  fun getVegetableById(id: Long): VegetableSummary? = repository.getVegetableById(id)
}
