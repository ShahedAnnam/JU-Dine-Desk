package com.example.judinedesk.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import com.example.judinedesk.models.Manager
import java.util.*
import android.util.Log

class ManagerDashboardActivity : AppCompatActivity() {

    private lateinit var tvHallName: TextView
    private lateinit var btnProfile: Button
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var rgMealType: RadioGroup
    private lateinit var btnAddItem: Button
    private lateinit var llMealItemsContainer: LinearLayout
    private lateinit var btnPostMeal: Button

    private val db = FirebaseFirestore.getInstance()
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_dashboard)

        Log.d("Inside managerDashboard activity", "  ")

        setupViews()

        val manager = intent.getParcelableExtra<Manager>("manager")
        tvHallName.text = manager?.hall ?: "No Hall"

        setupClickListeners()
    }

    private fun setupViews() {
        tvHallName = findViewById(R.id.tvHallName)
        btnProfile = findViewById(R.id.btnProfile)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        rgMealType = findViewById(R.id.rgMealType)
        btnAddItem = findViewById(R.id.btnAddItem)
        llMealItemsContainer = findViewById(R.id.llMealItemsContainer)
        btnPostMeal = findViewById(R.id.btnPostMeal)
    }



    private fun setupClickListeners() {

        btnProfile.setOnClickListener {
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
        }

        btnSelectDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, y, m, d ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                c.set(y, m, d)
                selectedDate = sdf.format(c.time)
                tvSelectedDate.text = selectedDate
            }, year, month, day)
            datePicker.show()
        }

        btnAddItem.setOnClickListener {
            addMealItemField()
        }

        btnPostMeal.setOnClickListener {
            postMeal()
        }
    }

    private fun addMealItemField() {
        val etMealItem = EditText(this)
        etMealItem.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8 }
        etMealItem.hint = "Meal Item"
        llMealItemsContainer.addView(etMealItem)
    }

    private fun postMeal() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedTypeId = rgMealType.checkedRadioButtonId
        if (selectedTypeId == -1) {
            Toast.makeText(this, "Please select Lunch or Dinner", Toast.LENGTH_SHORT).show()
            return
        }

        val mealType = findViewById<RadioButton>(selectedTypeId).text.toString()
        val meals = mutableListOf<String>()

        for (i in 0 until llMealItemsContainer.childCount) {
            val child = llMealItemsContainer.getChildAt(i)
            if (child is EditText) {
                val text = child.text.toString().trim()
                if (text.isNotEmpty()) meals.add(text)
            }
        }

        if (meals.isEmpty()) {
            Toast.makeText(this, "Please add at least one meal item", Toast.LENGTH_SHORT).show()
            return
        }

        val mealData = hashMapOf(
            "date" to selectedDate,
            "type" to mealType,
            "items" to meals,
            "hall" to tvHallName.text.toString()
        )

        db.collection("meals")
            .add(mealData)
            .addOnSuccessListener {
                Toast.makeText(this, "Meal posted successfully!", Toast.LENGTH_SHORT).show()
                llMealItemsContainer.removeAllViews()
                rgMealType.clearCheck()
                selectedDate = ""
                tvSelectedDate.text = "Select Date"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to post meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
