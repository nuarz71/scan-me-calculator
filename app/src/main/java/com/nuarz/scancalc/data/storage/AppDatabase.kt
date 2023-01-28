package com.nuarz.scancalc.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nuarz.scancalc.data.storage.dao.CalculationDao
import com.nuarz.scancalc.data.storage.entity.CalculationEntity

@Database(
	entities = [CalculationEntity::class],
	version = 1
)
internal abstract class AppDatabase : RoomDatabase() {
	abstract val calculationDao: CalculationDao
	
	companion object {
		fun create(context: Context): AppDatabase {
			return Room.databaseBuilder(context, AppDatabase::class.java, "app_database").build()
		}
	}
}