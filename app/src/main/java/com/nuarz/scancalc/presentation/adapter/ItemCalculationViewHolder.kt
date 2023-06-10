package com.nuarz.scancalc.presentation.adapter

import androidx.recyclerview.widget.RecyclerView
import com.nuarz.scancalc.databinding.ItemCalculationBinding
import com.nuarz.scancalc.presentation.viewmodel.RecentUiModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ItemCalculationViewHolder(private val binding: ItemCalculationBinding) :
    RecyclerView.ViewHolder(binding.root) {
    
    private val decimalFormat by lazy {
        DecimalFormat("#,###.###", DecimalFormatSymbols(Locale.getDefault())).apply {
            maximumIntegerDigits = 15
            maximumFractionDigits = 18
        }
    }
    
    fun bind(model: RecentUiModel) {
        binding.mainItemValueInput.text = model.input
        binding.mainItemValueResult.text = decimalFormat.format(model.result)
    }
}