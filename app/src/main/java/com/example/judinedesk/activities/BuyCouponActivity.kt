package com.example.judinedesk.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Meal
import com.example.judinedesk.models.Student
import com.example.judinedesk.models.Manager
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BuyCouponActivity : AppCompatActivity() {

    private lateinit var tvHallName: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var tvLunchItems: TextView
    private lateinit var tvDinnerItems: TextView
    private lateinit var tvLunchPrice: TextView
    private lateinit var tvDinnerPrice: TextView
    private lateinit var btnBuyLunch: Button
    private lateinit var btnBuyDinner: Button

    private val db = FirebaseFirestore.getInstance()
    private var selectedDate: String = ""
    private var currentUser: Any? = null
    private var isManager: Boolean = false

    private var lunchMeal: Meal? = null
    private var dinnerMeal: Meal? = null

    private var lunchPurchased = false
    private var dinnerPurchased = false

    companion object {
        private const val TAG = "BuyCouponActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_coupon)

        try {
            currentUser = AuthHelper.currentUserData
            isManager = currentUser is Manager

            setupViews()
            setupRole()
            setupClicks()

            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            tvSelectedDate.text = "Today"

            loadMeals()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error initializing activity", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupViews() {
        try {
            tvHallName = findViewById(R.id.tvHallName)
            tvSelectedDate = findViewById(R.id.tvSelectedDate)
            btnSelectDate = findViewById(R.id.btnSelectDate)
            tvLunchItems = findViewById(R.id.tvLunchItems)
            tvDinnerItems = findViewById(R.id.tvDinnerItems)
            tvLunchPrice = findViewById(R.id.tvLunchPrice)
            tvDinnerPrice = findViewById(R.id.tvDinnerPrice)
            btnBuyLunch = findViewById(R.id.btnBuyLunch)
            btnBuyDinner = findViewById(R.id.btnBuyDinner)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views: ${e.message}")
            throw e
        }
    }

    private fun setupRole() {
        try {
            tvHallName.text = when (currentUser) {
                is Manager -> "${(currentUser as Manager).hall} (Manager)"
                is Student -> (currentUser as Student).hall
                else -> "Unknown Hall"
            }

            if (isManager) {
                btnBuyLunch.visibility = View.GONE
                btnBuyDinner.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up role: ${e.message}")
        }
    }

    private fun setupClicks() {
        try {
            btnSelectDate.setOnClickListener { showDatePicker() }

            btnBuyLunch.setOnClickListener {
                if (lunchMeal != null) {
                    showConfirmDialog(lunchMeal!!)
                } else {
                    Toast.makeText(this, "Lunch meal not available", Toast.LENGTH_SHORT).show()
                }
            }

            btnBuyDinner.setOnClickListener {
                if (dinnerMeal != null) {
                    showConfirmDialog(dinnerMeal!!)
                } else {
                    Toast.makeText(this, "Dinner meal not available", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up clicks: ${e.message}")
        }
    }

    private fun showDatePicker() {
        try {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, y, m, d ->
                    try {
                        val selectedCal = Calendar.getInstance()
                        selectedCal.set(y, m, d)

                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        selectedDate = sdf.format(selectedCal.time)

                        val displaySdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val displayDate = displaySdf.format(selectedCal.time)

                        val today = sdf.format(Date())
                        tvSelectedDate.text = if (selectedDate == today) "Today" else displayDate

                        loadMeals()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in date picker callback: ${e.message}")
                        Toast.makeText(this, "Error selecting date", Toast.LENGTH_SHORT).show()
                    }
                },
                year,
                month,
                day
            )

            datePicker.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing date picker: ${e.message}")
            Toast.makeText(this, "Cannot open date picker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMeals() {
        try {
            Log.d(TAG, "Loading meals for date: $selectedDate")

            // Reset all states
            resetButtonStates()

            val hall = getHallName()
            if (hall.isEmpty()) {
                Toast.makeText(this, "Hall information not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Show loading state
            btnBuyLunch.text = "Loading..."
            btnBuyDinner.text = "Loading..."
            btnBuyLunch.isEnabled = false
            btnBuyDinner.isEnabled = false

            db.collection("meals")
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("hall", hall)
                .get()
                .addOnSuccessListener { docs ->
                    try {
                        Log.d(TAG, "Meals query successful, found ${docs.size()} documents")

                        // Reset meal data
                        lunchMeal = null
                        dinnerMeal = null
                        lunchPurchased = false
                        dinnerPurchased = false

                        // Default text if no meals found
                        tvLunchItems.text = "Not posted yet"
                        tvDinnerItems.text = "Not posted yet"
                        tvLunchPrice.text = ""
                        tvDinnerPrice.text = ""

                        if (docs.isEmpty) {
                            Log.d(TAG, "No meals found for $selectedDate in $hall")
                            updateButtonStates()
                            Toast.makeText(this@BuyCouponActivity, "No meals posted for this date", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        for (doc in docs) {
                            try {
                                val meal = doc.toObject(Meal::class.java).copy(id = doc.id)
                                Log.d(TAG, "Found meal: ${meal.type}, ID: ${meal.id}")

                                if (meal.type.equals("Lunch", ignoreCase = true)) {
                                    lunchMeal = meal
                                    tvLunchItems.text = meal.items.joinToString("\n• ", "• ")
                                    tvLunchPrice.text = "Price: ${meal.price} BDT"
                                    Log.d(TAG, "Lunch meal set: ${meal.items.size} items")
                                }

                                if (meal.type.equals("Dinner", ignoreCase = true)) {
                                    dinnerMeal = meal
                                    tvDinnerItems.text = meal.items.joinToString("\n• ", "• ")
                                    tvDinnerPrice.text = "Price: ${meal.price} BDT"
                                    Log.d(TAG, "Dinner meal set: ${meal.items.size} items")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing meal document: ${e.message}")
                            }
                        }

                        updateButtonStates()
                        checkAlreadyBought()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing meals data: ${e.message}")
                        Toast.makeText(this@BuyCouponActivity, "Error processing meals data", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading meals: ${e.message}")
                    Toast.makeText(this@BuyCouponActivity, "Failed to load meals: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateButtonStates()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadMeals function: ${e.message}")
            Toast.makeText(this, "Error loading meals", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getHallName(): String {
        return try {
            when (currentUser) {
                is Manager -> (currentUser as Manager).hall
                is Student -> (currentUser as Student).hall
                else -> ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting hall name: ${e.message}")
            ""
        }
    }

    private fun resetButtonStates() {
        runOnUiThread {
            try {
                btnBuyLunch.text = "Buy Coupon"
                btnBuyDinner.text = "Buy Coupon"
                btnBuyLunch.isEnabled = false
                btnBuyDinner.isEnabled = false
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting button states: ${e.message}")
            }
        }
    }

    private fun updateButtonStates() {
        runOnUiThread {
            try {
                // Update lunch button
                if (lunchMeal != null && !isManager) {
                    btnBuyLunch.isEnabled = !lunchPurchased
                    btnBuyLunch.text = if (lunchPurchased) "Already Bought" else "Buy Coupon"
                } else {
                    btnBuyLunch.isEnabled = false
                    btnBuyLunch.text = if (isManager) "" else "Not Available"
                }

                // Update dinner button
                if (dinnerMeal != null && !isManager) {
                    btnBuyDinner.isEnabled = !dinnerPurchased
                    btnBuyDinner.text = if (dinnerPurchased) "Already Bought" else "Buy Coupon"
                } else {
                    btnBuyDinner.isEnabled = false
                    btnBuyDinner.text = if (isManager) "" else "Not Available"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating button states: ${e.message}")
            }
        }
    }

    private fun checkAlreadyBought() {
        try {
            val uid = AuthHelper.getCurrentUid()
            if (uid == null) {
                Log.e(TAG, "User ID is null")
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
                return
            }

            val hall = getHallName()
            if (hall.isEmpty()) {
                return
            }

            Log.d(TAG, "Checking purchases for user: $uid, date: $selectedDate, hall: $hall")

            db.collection("purchases")
                .whereEqualTo("studentId", uid)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("hall", hall)
                .get()
                .addOnSuccessListener { docs ->
                    try {
                        Log.d(TAG, "Purchases query successful, found ${docs.size()} documents")

                        // Reset purchase states
                        lunchPurchased = false
                        dinnerPurchased = false

                        if (docs.isEmpty) {
                            Log.d(TAG, "No purchases found for this date")
                            updateButtonStates()
                            return@addOnSuccessListener
                        }

                        for (doc in docs) {
                            try {
                                val type = doc.getString("type") ?: continue
                                val status = doc.getString("paymentStatus") ?: ""

                                Log.d(TAG, "Found purchase: type=$type, status=$status")

                                // Only consider paid purchases
                                if (status.equals("paid", ignoreCase = true)) {
                                    when (type.lowercase(Locale.getDefault())) {
                                        "lunch" -> {
                                            lunchPurchased = true
                                            Log.d(TAG, "Lunch already purchased")
                                        }
                                        "dinner" -> {
                                            dinnerPurchased = true
                                            Log.d(TAG, "Dinner already purchased")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing purchase document: ${e.message}")
                            }
                        }

                        updateButtonStates()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing purchases data: ${e.message}")
                        updateButtonStates()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking purchases: ${e.message}")
                    Toast.makeText(this@BuyCouponActivity, "Error checking purchase status", Toast.LENGTH_SHORT).show()
                    updateButtonStates()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAlreadyBought function: ${e.message}")
            updateButtonStates()
        }
    }

    private fun showConfirmDialog(meal: Meal) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Confirm Purchase")
                .setMessage("Do you want to buy ${meal.type} coupon for ${meal.price} BDT?")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Confirm") { dialog, _ ->
                    dialog.dismiss()
                    startPaymentProcess(meal)
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing confirm dialog: ${e.message}")
            Toast.makeText(this, "Cannot show confirmation dialog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPaymentProcess(meal: Meal) {
        try {
            val studentId = AuthHelper.getCurrentUid()
            if (studentId == null) {
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
                return
            }

            // Update button state immediately
            if (meal.type.equals("Lunch", ignoreCase = true)) {
                lunchPurchased = true
                btnBuyLunch.text = "Processing..."
                btnBuyLunch.isEnabled = false
            } else {
                dinnerPurchased = true
                btnBuyDinner.text = "Processing..."
                btnBuyDinner.isEnabled = false
            }

            val purchaseId = "${studentId}_${meal.id}_${System.currentTimeMillis()}"
            Log.d(TAG, "Starting payment process with purchaseId: $purchaseId")

            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra("purchaseId", purchaseId)
                putExtra("mealType", meal.type)
                putExtra("mealPrice", meal.price)
                putExtra("selectedDate", selectedDate)
                putExtra("hall", meal.hall)
                putExtra("mealId", meal.id)
                putExtra("studentId", studentId)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting payment process: ${e.message}")
            Toast.makeText(this, "Error starting payment", Toast.LENGTH_SHORT).show()
            updateButtonStates()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from payment activity
        Log.d(TAG, "Activity resumed, refreshing data")
        loadMeals()
    }
}