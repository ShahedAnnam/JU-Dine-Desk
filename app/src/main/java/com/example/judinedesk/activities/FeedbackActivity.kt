package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        val listViewMeals = findViewById<ListView>(R.id.listViewMeals)

        val mealItems = mutableListOf<String>()
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val today = LocalDate.now()

        repeat(7) { i ->
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(formatter)
            mealItems.add("$dateStr - Breakfast")
            mealItems.add("$dateStr - Lunch")
            mealItems.add("$dateStr - Dinner")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mealItems)
        listViewMeals.adapter = adapter

        listViewMeals.setOnItemClickListener { _, _, position, _ ->
            val selected = mealItems[position]
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("meal_name", selected)
            startActivity(intent)
        }
    }
}
