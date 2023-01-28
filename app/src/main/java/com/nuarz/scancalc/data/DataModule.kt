package com.nuarz.scancalc.data

import android.content.Context
import com.nuarz.scancalc.data.storage.AppDBStorage
import com.nuarz.scancalc.data.storage.AppDatabase
import com.nuarz.scancalc.data.storage.AppFileStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
	
	@Provides
	internal fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
		return AppDatabase.create(context)
	}
	
	@Provides
	internal fun provideLocalDataSource(
		@ApplicationContext context: Context,
		database: AppDatabase
	): LocalDataSource {
		return LocalDataSource(
			fileStorage = AppFileStorage(
				context.filesDir,
				context.getSharedPreferences("app_pref", Context.MODE_PRIVATE)
			),
			dbStorage = AppDBStorage(database.calculationDao)
		)
	}
}