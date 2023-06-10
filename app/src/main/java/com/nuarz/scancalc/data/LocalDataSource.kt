package com.nuarz.scancalc.data

import com.nuarz.scancalc.data.dto.CalculationDto
import com.nuarz.scancalc.data.params.CalculationParams
import com.nuarz.scancalc.data.storage.AppStorage

class LocalDataSource internal constructor(
    private val fileStorage: AppStorage,
    private val dbStorage: AppStorage
) {
    
    private var storage: AppStorage = fileStorage
    private var currentMode = MODE_FILE
    
    suspend fun saveCalculation(params: CalculationParams) : Long {
        return storage.saveItem(params)
    }
    
    suspend fun getRecentCalculations(): List<CalculationDto> {
        return try {
            storage.getItems().map {
                CalculationDto(
                    id = it.id,
                    input = it.input,
                    result = it.result
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun updateStorage(mode: Int, onUpdate: () -> Unit) {
        if (mode == currentMode) return
        when (mode) {
            MODE_FILE -> {
                storage = fileStorage
                currentMode = mode
            }
            
            MODE_DB -> {
                storage = dbStorage
                currentMode = mode
            }
        }
        onUpdate()
    }
    
    companion object {
        const val MODE_FILE = 1
        const val MODE_DB = 2
    }
}