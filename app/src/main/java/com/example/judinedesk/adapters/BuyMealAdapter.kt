package com.example.judinedesk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R

class BuyMealAdapter(
    private val mealList: List<Map<String, Any>>,
    private val onBuyClicked: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<BuyMealAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMealType: TextView = view.findViewById(R.id.tvMealType)
        val tvItems: TextView = view.findViewById(R.id.tvItems)
        val btnBuy: Button = view.findViewById(R.id.btnBuyCoupon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_buy_meal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = mealList[position]

        holder.tvMealType.text = meal["type"]?.toString() ?: "Unknown Meal"

        val items = meal["items"] as? List<String> ?: emptyList()
        holder.tvItems.text = items.joinToString("\n• ", "• ")

        holder.btnBuy.setOnClickListener {
            onBuyClicked(meal)
        }
    }

    override fun getItemCount() = mealList.size
}