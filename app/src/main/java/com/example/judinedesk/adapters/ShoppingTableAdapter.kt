package com.example.judinedesk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.models.ShoppingListItem

class ShoppingTableAdapter(
    private var items: List<ShoppingListItem>
) : RecyclerView.Adapter<ShoppingTableAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSerial: TextView = itemView.findViewById(R.id.tvSerial)
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvUnit: TextView = itemView.findViewById(R.id.tvUnit)
        val tvCost: TextView = itemView.findViewById(R.id.tvCost)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAddedBy: TextView = itemView.findViewById(R.id.tvAddedBy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_table_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Serial number (starting from 1)
        holder.tvSerial.text = (position + 1).toString()
        holder.tvItemName.text = item.itemName
        holder.tvQuantity.text = item.quantity.toString()
        holder.tvUnit.text = item.unit
        holder.tvCost.text = "৳${item.cost.toInt()}"
        holder.tvCategory.text = item.category
        holder.tvAddedBy.text = item.addedByName

        // Alternate row background for better readability
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.light_gray))
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ShoppingListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}