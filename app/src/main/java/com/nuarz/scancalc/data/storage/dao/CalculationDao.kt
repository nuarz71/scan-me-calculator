package com.nuarz.scancalc.data.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nuarz.scancalc.data.storage.entity.CalculationEntity

@Dao
internal interface CalculationDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(calculation: CalculationEntity)
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(calculations: List<CalculationEntity>)
	
	@Query("SELECT * FROM calculations WHERE id=:id")
	suspend fun getCalculation(id: Int): CalculationEntity?
	
	@Query("SELECT * FROM calculations ORDER BY id DESC")
	suspend fun getAllCalculation(): List<CalculationEntity>
}