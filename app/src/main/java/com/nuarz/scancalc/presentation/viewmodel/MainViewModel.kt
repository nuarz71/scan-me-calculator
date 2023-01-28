package com.nuarz.scancalc.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.nuarz.scancalc.data.LocalDataSource
import com.nuarz.scancalc.data.params.CalculationParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	private val dataSource: LocalDataSource,
	private val textRecognizer: TextRecognizer
) : ViewModel() {
	
	private val recent = MutableLiveData<List<RecentUiModel>>(emptyList())
	
	private val storageMode = MutableLiveData(LocalDataSource.MODE_FILE)
	
	private val processImage = MutableLiveData(false)
	
	private val firsOnlyCalculation = MutableLiveData(true)
	private val errorProcessImageMsg = MutableLiveData<String>(null)
	
	val isProcessingImage: LiveData<Boolean>
		get() = processImage
	
	val recentCalculations: LiveData<List<RecentUiModel>>
		get() = recent
	
	val currentStorageMode: LiveData<Int>
		get() = storageMode
	
	val errorProcessingImage: LiveData<String>
		get() = errorProcessImageMsg
	
	init {
		getRecent()
	}
	
	fun switchStorage(mode: Int) {
		dataSource.updateStorage(mode) {
			storageMode.value = mode
			getRecent()
		}
	}
	
	fun processImage(image: InputImage) {
		processImage.value = true
		textRecognizer.process(image)
			.addOnSuccessListener {
				processCalculation(it.text)
			}
			.addOnFailureListener {
				errorProcessImageMsg.value = "Failed: ${it.message ?: "Unknown Error"} "
				processImage.value = false
			}
	}
	
	private fun processCalculation(text: String) {
		viewModelScope.launch {
			val result = async(Dispatchers.IO) {
				val regex = if (firsOnlyCalculation.value == true) {
					"(-?\\d*[.,]?\\d([/+\\-*]|[xX]))(-?\\d*[.,]?\\d+)".toRegex()
				} else {
					"(-?\\d*[.,]?\\d+([/+\\-*]|[xX]))+(-?\\d*[.,]?\\d+)".toRegex()
				}
				val matcher = regex.find(text.replace("\\h".toRegex(), ""))
				val operatorOperand = matcher?.let {
					Log.d(TAG, "Text Recognizer: ${it.value}")
					filterOperatorsAndOperands(
						it.value
							.replace("[xX]".toRegex(), "*")
							.replace(",".toRegex(), ".")
					)
				}
				if (operatorOperand == null) {
					errorProcessImageMsg.postValue(
						"Failed recognising text calculation from selected image"
					)
					return@async null
				}
				
				val calculation = try {
					calculator(operatorOperand.first, operatorOperand.second)
				} catch (e: Throwable) {
					Log.d(TAG, "Calculation failed: ${e.message}")
					errorProcessImageMsg.postValue(e.message)
					return@async null
				}
				dataSource.saveCalculation(
					CalculationParams(
						input = matcher.value,
						result = calculation
					)
				)
				matcher.value to calculation
			}
			val mutableRecent = recent.value?.toMutableList() ?: mutableListOf()
			val lastId = mutableRecent.firstOrNull()?.id ?: 0
			val pair = result.await()
			if (pair == null) {
				processImage.value = false
				return@launch
			}
			mutableRecent.add(
				0,
				RecentUiModel(
					id = lastId + 1,
					input = pair.first,
					result = pair.second
				)
			)
			recent.value = mutableRecent
			processImage.value = false
			
		}
	}
	
	private fun filterOperatorsAndOperands(
		input: String
	): Pair<List<Double>, List<String>> {
		val regex = "[*+/]|(?<![+*/-]|^)-".toRegex()
		val operators = input.split(regex).map { it.toDouble() }
		val operands = regex.findAll(input).map { it.value }.toList()
		if (firsOnlyCalculation.value == true && operators.size > 2) {
			return listOf(operators[0], operators[1]) to listOf(operands.first())
		}
		return operators to operands
	}
	
	private tailrec fun calculator(operators: List<Double>, operands: List<String>): Double {
		if (operators.size == 1 || operands.isEmpty()) {
			return operators.firstOrNull() ?: 0.0
		}
		
		if (operators.size > 2 && operands.size > 1) {
			val mutableOperators = operators.toMutableList()
			val mutableOperand = operands.toMutableList()
			val nextPriority = mutableOperand.getOrNull(1)?.let(::getOperandPriority) ?: 0
			val currentPriority = getOperandPriority(mutableOperand[0])
			return if (nextPriority > currentPriority) {
				val result =
					calculation(mutableOperators[1], mutableOperators[2], mutableOperand[1])
				mutableOperand.removeAt(1)
				mutableOperators[1] = result
				mutableOperators.removeAt(2)
				calculator(mutableOperators, mutableOperand)
			} else {
				val result = calculation(
					mutableOperators.first(),
					mutableOperators[1],
					mutableOperand.first()
				)
				mutableOperators[0] = result
				mutableOperators.removeAt(1)
				mutableOperand.removeAt(0)
				calculator(mutableOperators, mutableOperand)
			}
		}
		
		return calculation(operators.first(), operators[1], operands.first())
	}
	
	private fun getOperandPriority(operand: String): Int {
		return when (operand) {
			"*", "/" -> 1
			else -> 0
		}
	}
	
	private fun calculation(operator1: Double, operator2: Double, operand: String): Double {
		return when (operand) {
			"*" -> operator1 * operator2
			"/" -> operator1 / operator2
			"+" -> operator1 + operator2
			"-" -> operator1 - operator2
			else -> {
				operator1
			}
		}
	}
	
	private fun getRecent() {
		viewModelScope.launch {
			val items = async(Dispatchers.IO) {
				dataSource.getRecentCalculations().map {
					RecentUiModel(id = it.id, input = it.input, result = it.result)
				}
			}
			recent.value = items.await() ?: emptyList()
		}
	}
	
	companion object {
		const val TAG = "MainViewModel"
	}
}