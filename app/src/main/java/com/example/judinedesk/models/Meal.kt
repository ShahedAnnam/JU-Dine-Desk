// Update your Meal.kt model
package com.example.judinedesk.models

data class MealFeedback(
    val studentId: String = "",
    val studentName: String = "",
    val feedback: String = "",
    val timestamp: Long = 0L,
    val rating: Float = 0f
)

data class Meal(
    val id: String = "",
    val date: String = "",
    val type: String = "", // Lunch or Dinner
    val items: List<String> = emptyList(),
    val hall: String = "",
    val feedback: List<MealFeedback> = emptyList() // Updated to use MealFeedback objects
)