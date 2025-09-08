package com.example.judinedesk.models

data class Meal(
    val id: String = "",
    val date: String = "",
    val type: String = "", // Lunch or Dinner
    val items: List<String> = emptyList(),
    val hall: String = ""
)