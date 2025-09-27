package com.example.judinedesk.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R

class ReviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val textMeal = findViewById<TextView>(R.id.textMeal)
        val editComment = findViewById<EditText>(R.id.editComment)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        val meal = intent.getStringExtra("meal_name") ?: "Meal"
        textMeal.text = "Feedback for: $meal"

        btnSubmit.setOnClickListener {
            val comment = editComment.text.toString().trim()
            if (comment.isEmpty()) {
                Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Feedback submitted for $meal", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
