package com.example.judinedesk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.models.QRCodeItem

class QRCodeAdapter(private val onItemClick: (QRCodeItem) -> Unit) :
    ListAdapter<QRCodeItem, QRCodeAdapter.QRCodeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QRCodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qr_code, parent, false)
        return QRCodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: QRCodeViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    class QRCodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvMealType: TextView = itemView.findViewById(R.id.tvMealType)
        private val tvHall: TextView = itemView.findViewById(R.id.tvHall)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        fun bind(item: QRCodeItem) {
            tvDate.text = item.getFormattedDate()
            tvMealType.text = "${item.mealType} Coupon"
            tvHall.text = item.hall
            tvPrice.text = "Price: ${item.price} BDT"

            // Set different background colors for lunch and dinner
            val backgroundRes = when (item.mealType.lowercase()) {
                "lunch" -> R.drawable.bg_lunch_card
                "dinner" -> R.drawable.bg_dinner_card
                else -> R.drawable.bg_meal_card
            }
            itemView.setBackgroundResource(backgroundRes)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<QRCodeItem>() {
        override fun areItemsTheSame(oldItem: QRCodeItem, newItem: QRCodeItem): Boolean {
            return oldItem.purchaseId == newItem.purchaseId
        }

        override fun areContentsTheSame(oldItem: QRCodeItem, newItem: QRCodeItem): Boolean {
            return oldItem == newItem
        }
    }
}