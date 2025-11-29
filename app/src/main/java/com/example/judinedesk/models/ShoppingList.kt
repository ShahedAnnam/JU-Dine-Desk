package com.example.judinedesk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class ShoppingListItem(
    val id: String = "",
    val itemName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val estimatedCost: Double = 0.0,
    val priority: String = "Medium", // Low, Medium, High
    val category: String = "General", // Vegetables, Meat, Grocery, etc.
    val status: String = "Pending", // Pending, Purchased, Cancelled
    val addedBy: String = "", // staff UID
    val addedByName: String = "", // staff name
    val hall: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val purchasedAt: Long = 0L,
    val notes: String = ""
) : Parcelable