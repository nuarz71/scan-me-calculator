package com.nuarz.scancalc.presentation.viewmodel

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
	
	@Provides
	fun provideTextRecognizer(): TextRecognizer {
		return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
	}
}