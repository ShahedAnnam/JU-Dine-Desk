package com.example.judinedesk.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.models.Meal

class BuyCouponAdapter(
    private val meals: List<Meal>,
    private val onBuyClick: (Meal) -> Unit
) : RecyclerView.Adapter<BuyCouponAdapter.BuyCouponViewHolder>() {

    inner class BuyCouponViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val tvMealType: TextView = itemView.findViewById(R.id.tvMealType)
        val tvMealItems: TextView = itemView.findViewById(R.id.tvMealItems)
        val btnBuyCoupon: Button = itemView.findViewById(R.id.btnBuyCoupon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyCouponViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_buy_coupon,parent,false)
        return BuyCouponViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuyCouponViewHolder, position: Int) {
        val meal = meals[position]
        holder.tvMealType.text = meal.type.uppercase()
        holder.tvMealItems.text = if(meal.items.isNotEmpty()) meal.items.joinToString("\n• ", "• ") else "No items"

        holder.btnBuyCoupon.setOnClickListener{
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Confirm Purchase")
                .setMessage("Do you want to buy ${meal.type} coupon?")
                .setPositiveButton("Confirm"){ _, _ -> onBuyClick(meal) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount(): Int = meals.size
}
