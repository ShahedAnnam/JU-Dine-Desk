package com.example.judinedesk.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Manager
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog

class ManagerDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvHallName: TextView
    private lateinit var tvStudentsCount: TextView
    private lateinit var tvMealsToday: TextView
    private lateinit var tvRecentActivity: TextView
    private lateinit var tvLunchItems: TextView
    private lateinit var tvDinnerItems: TextView
    private lateinit var btnProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var rgMealType: RadioGroup
    private lateinit var btnAddItem: Button
    private lateinit var llMealItemsContainer: LinearLayout
    private lateinit var btnPostMeal: Button
    private lateinit var btnViewReviews: Button
    private lateinit var btnViewStudents: Button
    private lateinit var tvTodayMealsTitle: TextView
    private lateinit var btnSelectMealDate: Button // NEW: Date selector for meal display

    private val db = FirebaseFirestore.getInstance()
    private var selectedDate: String = ""
    private var displayDate: String = "" // Date being displayed (can be today or selected date)
    private lateinit var currentManager: Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_dashboard)

        // Get manager object from intent or AuthHelper
        currentManager = getManagerData()

        setupViews()
        setupClickListeners()
        setupBackPressedHandler()
        loadDashboardData()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showLogoutConfirmation()
            }
        })
    }

    private fun getManagerData(): Manager {
        return when {
            intent.hasExtra("USER_OBJECT") -> {
                intent.getParcelableExtra<Manager>("USER_OBJECT") ?: getManagerFromAuthHelper()
            }
            else -> getManagerFromAuthHelper()
        }
    }

    private fun getManagerFromAuthHelper(): Manager {
        return (AuthHelper.currentUserData as? Manager) ?: Manager(
            uid = AuthHelper.getCurrentUid() ?: "",
            email = AuthHelper.getCurrentUid() ?: "Unknown",
            hall = "Unknown Hall",
            role = "manager"
        )
    }

    private fun setupViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvHallName = findViewById(R.id.tvHallName)
        tvStudentsCount = findViewById(R.id.tvStudentsCount)
        tvMealsToday = findViewById(R.id.tvMealsToday)
        tvRecentActivity = findViewById(R.id.tvRecentActivity)
        tvLunchItems = findViewById(R.id.tvLunchItems)
        tvDinnerItems = findViewById(R.id.tvDinnerItems)
        btnProfile = findViewById(R.id.btnProfile)
        btnLogout = findViewById(R.id.btnLogout)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        rgMealType = findViewById(R.id.rgMealType)
        btnAddItem = findViewById(R.id.btnAddItem)
        llMealItemsContainer = findViewById(R.id.llMealItemsContainer)
        btnPostMeal = findViewById(R.id.btnPostMeal)
        btnViewReviews = findViewById(R.id.btnViewReviews)
        btnViewStudents = findViewById(R.id.btnViewStudents)
        tvTodayMealsTitle = findViewById(R.id.tvTodayMealsTitle)
        btnSelectMealDate = findViewById(R.id.btnSelectMealDate) // NEW

        // Set manager-specific data - Focus on Hall Name
        tvHallName.text = currentManager.hall
        tvWelcome.text = "Welcome to Your Dining Management"

        // Set default display date to today
        displayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        updateMealDisplayTitle()

    }

    private fun setupClickListeners() {
        btnProfile.setOnClickListener {
            showProfileDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        // NEW: Date selector for meal display
        btnSelectMealDate.setOnClickListener {
            showMealDisplayDatePicker()
        }

        btnAddItem.setOnClickListener {
            addMealItemField()
        }

        btnPostMeal.setOnClickListener {
            postMeal()
        }

        btnViewReviews.setOnClickListener {
            viewReviews()
        }

        btnViewStudents.setOnClickListener {
            viewStudents()
        }
    }
    private fun showProfileDialog() {
        val profileMessage = """
            🏢 Hall: ${currentManager.hall}
            📧 Email: ${currentManager.email}
            🆔 Employee ID: ${currentManager.employeeId}
            👨‍💼 Role: ${currentManager.role}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("👤 Manager Profile")
            .setMessage(profileMessage)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("🚪 Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        AuthHelper.logout()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun loadDashboardData() {
        loadStudentsCount()
        loadMealsByDate()
        loadRecentActivity()
    }
    private fun loadStudentsCount() {
        db.collection("students")
            .whereEqualTo("hall", currentManager.hall)
            .get()
            .addOnSuccessListener { documents ->
                tvStudentsCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("ManagerDashboard", "Error loading students count: ${e.message}")
                tvStudentsCount.text = "0"
            }
    }

    private fun loadMealsCountForDate(date: String) {
        db.collection("meal_bookings")
            .whereEqualTo("date", date)
            .whereEqualTo("hall", currentManager.hall)
            .get()
            .addOnSuccessListener { documents ->
                tvMealsToday.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("ManagerDashboard", "Error loading meal count: ${e.message}")
                tvMealsToday.text = "0"
            }
    }

    private fun loadMealsByDate(date: String? = null) {
        val targetDate = date ?: displayDate

        db.collection("meals")
            .whereEqualTo("date", targetDate)
            .whereEqualTo("hall", currentManager.hall)
            .get()
            .addOnSuccessListener { documents ->
                var lunchItems = "Not posted yet"
                var dinnerItems = "Not posted yet"

                for (document in documents) {
                    val type = document.getString("type") ?: ""
                    val items = document.get("items") as? List<String> ?: emptyList()
                    val itemsText = if (items.isNotEmpty())
                        items.joinToString("\n• ", "• ")
                    else
                        "No items specified"

                    when (type.lowercase()) {
                        "lunch" -> lunchItems = itemsText
                        "dinner" -> dinnerItems = itemsText
                    }
                }
                // Update Today's Meals section
                tvLunchItems.text = lunchItems
                tvDinnerItems.text = dinnerItems

                // Update MEAL COUNT for that date
                loadMealsCountForDate(targetDate)
            }
            .addOnFailureListener { e ->
                Log.e("ManagerDashboard", "Error loading meals: ${e.message}")
                tvLunchItems.text = "Error loading lunch"
                tvDinnerItems.text = "Error loading dinner"
            }
    }

    private fun loadRecentActivity() {
        db.collection("meals")
            .whereEqualTo("hall", currentManager.hall)
            .orderBy("postedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { documents ->
                val activityText = StringBuilder()

                if (documents.isEmpty) {
                    activityText.append("• No recent activity")
                } else {
                    for (document in documents) {
                        val date = document.getString("date") ?: ""
                        val type = document.getString("type") ?: ""
                        activityText.append("• Meal posted for $date ($type)\n")
                    }
                }

                tvRecentActivity.text = activityText.toString()
            }
            .addOnFailureListener { e ->
                Log.e("ManagerDashboard", "Error loading recent activity: ${e.message}")
                tvRecentActivity.text = "• Error loading activity"
            }
    }

    private fun showDatePicker() {
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

    // NEW: Date picker for meal display
    private fun showMealDisplayDatePicker() {
            showDatePicker()
            loadMealsByDate(selectedDate)
            updateMealDisplayTitle()
    }

    // NEW: Update the meal display title
    private fun updateMealDisplayTitle() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val displaySdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        if (displayDate == today) {
            tvTodayMealsTitle.text = "🍛 Today's Meals"
        } else {
            val displayDateFormatted = displaySdf.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(displayDate)!!)
            tvTodayMealsTitle.text = "🍛 Meals for $displayDateFormatted"
        }
    }

    private fun addMealItemField() {
        val etMealItem = EditText(this)
        etMealItem.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 8
            bottomMargin = 8
        }
        etMealItem.hint = "Enter meal item (e.g., Rice, Chicken Curry)"
        etMealItem.setBackgroundResource(R.drawable.edittext_border)
        etMealItem.setPadding(16, 12, 16, 12)
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

        // Check if meal already exists for this date, type AND hall
        db.collection("meals")
            .whereEqualTo("date", selectedDate)
            .whereEqualTo("type", mealType)
            .whereEqualTo("hall", currentManager.hall) // IMPORTANT: Include hall in uniqueness check
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No existing meal, create new one
                    createNewMeal(mealType, meals)
                } else {
                    // Meal already exists, show options
                    showMealExistsDialog(documents.documents[0].id, mealType, meals)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error checking existing meals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewMeal(mealType: String, meals: List<String>) {
        val mealData = hashMapOf(
            "date" to selectedDate,
            "type" to mealType,
            "items" to meals,
            "hall" to currentManager.hall,
            "postedAt" to System.currentTimeMillis(),
            "postedBy" to currentManager.employeeId.ifEmpty { currentManager.uid },
            "managerUid" to currentManager.uid
        )

        db.collection("meals")
            .add(mealData)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Meal posted successfully!", Toast.LENGTH_SHORT).show()
                clearMealForm()
                loadRecentActivity()

                // Refresh meals display if we're viewing the same date
                    loadMealsByDate(displayDate)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Failed to post meal: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ManagerDashboard", "Error posting meal: ${e.message}")
            }
    }

    private fun showMealExistsDialog(existingMealId: String, mealType: String, newMeals: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Meal Already Exists")
            .setMessage("A $mealType meal already exists for $selectedDate in ${currentManager.hall}. What would you like to do?")
            .setPositiveButton("🔄 Replace") { _, _ ->
                replaceExistingMeal(existingMealId, mealType, newMeals)
            }
            .setNegativeButton("❌ Cancel", null)
            .show()
    }

    private fun replaceExistingMeal(mealId: String, mealType: String, newMeals: List<String>) {
        val mealData = hashMapOf(
            "date" to selectedDate,
            "type" to mealType,
            "items" to newMeals,
            "hall" to currentManager.hall,
            "postedAt" to System.currentTimeMillis(),
            "postedBy" to currentManager.employeeId.ifEmpty { currentManager.uid },
            "managerUid" to currentManager.uid
        )

        db.collection("meals")
            .document(mealId)
            .set(mealData)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Meal updated successfully!", Toast.LENGTH_SHORT).show()
                clearMealForm()
                loadRecentActivity()

                // Refresh meals display if we're viewing the same date
                if (selectedDate == displayDate) {
                    loadMealsByDate(displayDate)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Failed to update meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearMealForm() {
        llMealItemsContainer.removeAllViews()
        rgMealType.clearCheck()
        selectedDate = ""
        tvSelectedDate.text = "No date selected"
    }

    private fun viewReviews() {
        Toast.makeText(this, "⭐ Opening reviews for ${currentManager.hall}", Toast.LENGTH_SHORT).show()
    }

    private fun viewStudents() {
        Toast.makeText(this, "👥 Showing students of ${currentManager.hall}", Toast.LENGTH_SHORT).show()
    }
}