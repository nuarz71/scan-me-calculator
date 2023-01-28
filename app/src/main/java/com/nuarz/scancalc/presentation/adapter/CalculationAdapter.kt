package com.nuarz.scancalc.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.nuarz.scancalc.databinding.ItemCalculationBinding
import com.nuarz.scancalc.presentation.viewmodel.RecentUiModel

class CalculationAdapter : ListAdapter<RecentUiModel, ItemCalculationViewHolder>(
	object : DiffUtil.ItemCallback<RecentUiModel>() {
		override fun areItemsTheSame(
			oldItem: RecentUiModel,
			newItem: RecentUiModel
		): Boolean {
			return oldItem.id == newItem.id
		}
		
		override fun areContentsTheSame(
			oldItem: RecentUiModel,
			newItem: RecentUiModel
		): Boolean {
			return oldItem == newItem
		}
	}
) {
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemCalculationViewHolder {
		return ItemCalculationViewHolder(
			ItemCalculationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)
	}
	
	override fun onBindViewHolder(holder: ItemCalculationViewHolder, position: Int) {
		holder.bind(getItem(position))
	}
}