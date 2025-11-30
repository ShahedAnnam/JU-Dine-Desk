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
    val cost: Double = 0.0, // Real cost, not estimated
    val category: String = "General",
    val addedBy: String = "",
    val addedByName: String = "",
    val hall: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) : Parcelable