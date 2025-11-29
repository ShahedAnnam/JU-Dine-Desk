package com.example.judinedesk.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_coupon)

        currentUser = AuthHelper.currentUserData
        isManager = currentUser is Manager

        setupViews()
        setupRole()
        setupClicks()

        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        tvSelectedDate.text = "Today"

        loadMeals()
    }

    private fun setupViews() {
        tvHallName = findViewById(R.id.tvHallName)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvLunchItems = findViewById(R.id.tvLunchItems)
        tvDinnerItems = findViewById(R.id.tvDinnerItems)
        tvLunchPrice = findViewById(R.id.tvLunchPrice)
        tvDinnerPrice = findViewById(R.id.tvDinnerPrice)
        btnBuyLunch = findViewById(R.id.btnBuyLunch)
        btnBuyDinner = findViewById(R.id.btnBuyDinner)
    }

    private fun setupRole() {
        tvHallName.text = when (currentUser) {
            is Manager -> "${(currentUser as Manager).hall} (Manager)"
            is Student -> (currentUser as Student).hall
            else -> "Unknown Hall"
        }

        if (isManager) {
            btnBuyLunch.visibility = View.GONE
            btnBuyDinner.visibility = View.GONE
        }
    }

    private fun setupClicks() {
        btnSelectDate.setOnClickListener { showDatePicker() }

        btnBuyLunch.setOnClickListener { lunchMeal?.let { showConfirmDialog(it) } }
        btnBuyDinner.setOnClickListener { dinnerMeal?.let { showConfirmDialog(it) } }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, y, m, d ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(y, m, d)

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = sdf.format(selectedCal.time)

                val displaySdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val displayDate = displaySdf.format(selectedCal.time)

                val today = sdf.format(Date())
                tvSelectedDate.text = if (selectedDate == today) "Today" else displayDate

                loadMeals()
            },
            year,
            month,
            day
        )

        datePicker.show()
    }

    private fun loadMeals() {
        val hall = when (currentUser) {
            is Manager -> (currentUser as Manager).hall
            is Student -> (currentUser as Student).hall
            else -> ""
        }

        db.collection("meals")
            .whereEqualTo("date", selectedDate)
            .whereEqualTo("hall", hall)
            .get()
            .addOnSuccessListener { docs ->
                lunchMeal = null
                dinnerMeal = null

                tvLunchItems.text = "Not posted yet"
                tvDinnerItems.text = "Not posted yet"
                tvLunchPrice.text = ""
                tvDinnerPrice.text = ""
                btnBuyLunch.isEnabled = false
                btnBuyDinner.isEnabled = false

                for (doc in docs) {
                    val meal = doc.toObject(Meal::class.java).copy(id = doc.id)

                    if (meal.type.equals("Lunch", ignoreCase = true)) {
                        lunchMeal = meal
                        tvLunchItems.text = meal.items.joinToString("\n• ", "• ")
                        tvLunchPrice.text = "Price: ${meal.price} BDT"
                        btnBuyLunch.isEnabled = !isManager
                    }

                    if (meal.type.equals("Dinner", ignoreCase = true)) {
                        dinnerMeal = meal
                        tvDinnerItems.text = meal.items.joinToString("\n• ", "• ")
                        tvDinnerPrice.text = "Price: ${meal.price} BDT"
                        btnBuyDinner.isEnabled = !isManager
                    }
                }

                checkAlreadyBought()
            }
    }

    private fun checkAlreadyBought() {
        val uid = AuthHelper.getCurrentUid() ?: return

        val hall = when (currentUser) {
            is Manager -> (currentUser as Manager).hall
            is Student -> (currentUser as Student).hall
            else -> ""
        }

        db.collection("purchases")
            .whereEqualTo("studentId", uid)
            .whereEqualTo("date", selectedDate)
            .whereEqualTo("hall", hall)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    when (doc.getString("type")) {
                        "Lunch" -> {
                            btnBuyLunch.text = "Already Bought"
                            btnBuyLunch.isEnabled = false
                        }
                        "Dinner" -> {
                            btnBuyDinner.text = "Already Bought"
                            btnBuyDinner.isEnabled = false
                        }
                    }
                }
            }
    }

    // -------------------------------
    //   CONFIRMATION DIALOG
    // -------------------------------
    private fun showConfirmDialog(meal: Meal) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Purchase")
            .setMessage("Do you want to buy ${meal.type} coupon?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Confirm") { dialog, _ ->
                dialog.dismiss()
                savePurchase(meal)
            }
            .show()
    }

    // -------------------------------
    //   SAVE PURCHASE + OPEN QR PAGE
    // -------------------------------
    private fun savePurchase(meal: Meal) {
        val studentId = AuthHelper.getCurrentUid() ?: return
        val purchaseId = "${studentId}_${meal.id}"

        val purchaseData = mapOf(
            "studentId" to studentId,
            "mealId" to meal.id,
            "type" to meal.type,
            "date" to selectedDate,
            "hall" to meal.hall,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("purchases").document(purchaseId)
            .set(purchaseData)
            .addOnSuccessListener {

                val qrData = "purchaseId=$purchaseId"

                val intent = Intent(this, QrCodeActivity::class.java)
                intent.putExtra("qr_data", qrData)
                startActivity(intent)

                Toast.makeText(this, "${meal.type} coupon purchased!", Toast.LENGTH_SHORT).show()

                if (meal.type == "Lunch") {
                    btnBuyLunch.text = "Already Bought"
                    btnBuyLunch.isEnabled = false
                } else {
                    btnBuyDinner.text = "Already Bought"
                    btnBuyDinner.isEnabled = false
                }
            }
    }
}
