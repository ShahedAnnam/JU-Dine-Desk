package com.example.judinedesk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.models.ShoppingListItem
import com.google.android.material.chip.Chip

class ShoppingListAdapter(
    private var items: List<ShoppingListItem>,
    private val onItemAction: (ShoppingListItem, String) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvCost: TextView = itemView.findViewById(R.id.tvCost)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAddedBy: TextView = itemView.findViewById(R.id.tvAddedBy)
        val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
        val chipPriority: Chip = itemView.findViewById(R.id.chipPriority)
        val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        val btnMarkPurchased: Button = itemView.findViewById(R.id.btnMarkPurchased)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val layoutActions: View = itemView.findViewById(R.id.layoutActions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvItemName.text = item.itemName
        holder.tvQuantity.text = "${item.quantity} ${item.unit}"
        holder.tvCost.text = "৳${item.estimatedCost.toInt()}"
        holder.tvCategory.text = item.category
        holder.tvAddedBy.text = "By ${item.addedByName}"

        // Priority chip
        holder.chipPriority.text = item.priority
        when (item.priority) {
            "High" -> holder.chipPriority.setChipBackgroundColorResource(android.R.color.holo_red_light)
            "Medium" -> holder.chipPriority.setChipBackgroundColorResource(android.R.color.holo_orange_light)
            "Low" -> holder.chipPriority.setChipBackgroundColorResource(android.R.color.holo_green_light)
        }

// Status chip
        holder.chipStatus.text = item.status
        when (item.status) {
            "Purchased" -> {
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
                holder.btnMarkPurchased.visibility = View.GONE
            }
            else -> {
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_blue_light)
                holder.btnMarkPurchased.visibility = View.VISIBLE
            }
        }

        // Notes
        if (item.notes.isNotEmpty()) {
            holder.tvNotes.text = item.notes
            holder.tvNotes.visibility = View.VISIBLE
        } else {
            holder.tvNotes.visibility = View.GONE
        }

        // Action buttons
        holder.btnMarkPurchased.setOnClickListener {
            onItemAction(item, "mark_purchased")
        }
        holder.btnEdit.setOnClickListener {
            onItemAction(item, "edit")
        }
        holder.btnDelete.setOnClickListener {
            onItemAction(item, "delete")
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ShoppingListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}