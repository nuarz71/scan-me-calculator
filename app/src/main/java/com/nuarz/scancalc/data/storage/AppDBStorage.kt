package com.nuarz.scancalc.data.storage

import com.nuarz.scancalc.data.params.CalculationParams
import com.nuarz.scancalc.data.storage.dao.CalculationDao
import com.nuarz.scancalc.data.storage.entity.CalculationEntity

internal class AppDBStorage(
	private val dao: CalculationDao
) : AppStorage {
	
	override suspend fun saveItem(item: CalculationParams) : Long {
		return dao.insert(CalculationEntity(input = item.input, result = item.result))
	}
	
	override suspend fun saveItems(items: List<CalculationEntity>) {
		dao.insertAll(items)
	}
	
	override suspend fun getItems(): List<CalculationEntity> {
		return dao.getAllCalculation()
	}
}