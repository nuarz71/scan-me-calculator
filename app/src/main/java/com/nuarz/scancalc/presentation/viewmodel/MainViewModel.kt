package com.nuarz.scancalc.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val textRecognizer: TextRecognizer
) : ViewModel() {
    
    
    private val _processImage = MutableLiveData(false)
    
    private val _firsOnlyCalculation = MutableLiveData(true)
    private val _errorProcessImageMsg = MutableLiveData<String?>(null)
    private val _currentCalculation = MutableLiveData<Pair<String, Double>?>(null)
    val calculationResult: LiveData<Pair<String, Double>?>
        get() = _currentCalculation
    val isProcessingImage: LiveData<Boolean>
        get() = _processImage
    
    val errorProcessingImage: LiveData<String?>
        get() = _errorProcessImageMsg
    
    fun processImage(image: InputImage) {
        _processImage.value = true
        _errorProcessImageMsg.value = null
        _currentCalculation.value = null
        textRecognizer.process(image)
            .addOnSuccessListener {
                processCalculation(it)
            }
            .addOnFailureListener {
                _errorProcessImageMsg.value = "Failed: ${it.message ?: "Unknown Error"} "
                _processImage.value = false
            }
    }
    
    private fun processCalculation(rawText: Text) {
        viewModelScope.launch(Dispatchers.Default) {
            val builder = StringBuilder()
            rawText.textBlocks.forEach {
                builder.appendLine(it.text)
            }
            val text = builder.toString()
            Log.d(TAG, "Raw text: $text")
            if (text.isBlank()) {
                _errorProcessImageMsg.postValue(
                    "Failed recognising text from selected image"
                )
                _currentCalculation.postValue(null)
                _processImage.postValue(false)
                return@launch
            }
            val matcher = searchCalculationFormula(text)
            val operatorOperand = matcher?.let {
                Log.d(TAG, "Text Recognizer: ${it.value}")
                filterOperatorsAndOperands(
                    it.value
                        .replace("[xX]".toRegex(), "*")
                        .replace(",".toRegex(), ".")
                )
            }
            if (operatorOperand == null) {
                _errorProcessImageMsg.postValue(
                    "Failed recognising text from selected image"
                )
                _currentCalculation.postValue(null)
                _processImage.postValue(false)
                return@launch
            }
            
            val calculation = try {
                calculator(operatorOperand.first, operatorOperand.second)
            } catch (e: Throwable) {
                Log.d(TAG, "Calculation failed: ${e.message}")
                _errorProcessImageMsg.postValue(e.message)
                _currentCalculation.postValue(null)
                _processImage.postValue(false)
                return@launch
            }
            _currentCalculation.postValue(matcher.value to calculation)
            _processImage.postValue(false)
            
        }
    }
    
    private fun searchCalculationFormula(text: String): MatchResult? {
        val regex = if (_firsOnlyCalculation.value == true) {
            "(-?\\d*[.,]?\\d([/+\\-*]|[xX]))(-?\\d*[.,]?\\d+)".toRegex()
        } else {
            "(-?\\d*[.,]?\\d+([/+\\-*]|[xX]))+(-?\\d*[.,]?\\d+)".toRegex()
        }
        return regex.find(text.replace("\\h".toRegex(), ""))
    }
    
    private fun filterOperatorsAndOperands(
        input: String
    ): Pair<List<Double>, List<String>> {
        val regex = "[*+/]|(?<![+*/-]|^)-".toRegex()
        val operators = input.split(regex).map { it.toDouble() }
        val operands = regex.findAll(input).map { it.value }.toList()
        if (_firsOnlyCalculation.value == true && operators.size >= 2) {
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
    
    companion object {
        const val TAG = "MainViewModel"
    }
}