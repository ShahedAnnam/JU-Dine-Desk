package com.example.judinedesk.models

data class QRCodeItem(
    val purchaseId: String = "",
    val studentId: String = "",
    val mealType: String = "", // "Lunch" or "Dinner"
    val date: String = "", // YYYY-MM-DD format
    val displayDate: String = "", // Formatted date
    val hall: String = "",
    val price: Double = 0.0,
    val qrData: String = "",
    val paymentTimestamp: Long = 0L
) {
    fun getFormattedDate(): String {
        return displayDate.ifEmpty { date }
    }

    fun isLunch(): Boolean = mealType.equals("lunch", ignoreCase = true)
    fun isDinner(): Boolean = mealType.equals("dinner", ignoreCase = true)
}