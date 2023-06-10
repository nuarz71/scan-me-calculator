package com.nuarz.scancalc.data.storage.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "calculations")
internal data class CalculationEntity(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "id")
	val id: Long = 0,
	
	@ColumnInfo(name = "input")
	val input: String = "",
	
	@ColumnInfo(name = "result")
	val result: Double = 0.0
)
