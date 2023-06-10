package com.nuarz.scancalc.data.storage

import com.nuarz.scancalc.data.params.CalculationParams
import com.nuarz.scancalc.data.storage.entity.CalculationEntity

internal interface AppStorage {
	suspend fun saveItem(item: CalculationParams) : Long
	suspend fun saveItems(items: List<CalculationEntity>)
	suspend fun getItems(): List<CalculationEntity>
}