// Create a new file: models/Purchase.kt
package com.example.judinedesk.models

import java.util.*

data class Purchase(
    val purchaseId: String = "",
    val studentId: String = "",
    val mealId: String = "",
    val type: String = "", // meal type like "Breakfast", "Lunch", "Dinner"
    val date: String = "", // selected date
    val displayDate: String = "", // formatted date for display
    val hall: String = "", // dining hall
    val price: Double = 0.0,
    val qrData: String = "",
    val paymentStatus: String = "pending", // pending, paid, cancelled
    val paymentTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Optional: Add helper methods
    fun isPaid(): Boolean = paymentStatus == "paid"
    fun getFormattedPrice(): String = "%.2f BDT".format(price)
}