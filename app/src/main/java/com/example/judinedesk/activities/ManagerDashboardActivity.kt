package com.example.judinedesk.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.cardview.widget.CardView
import com.example.judinedesk.R
import android.graphics.Color
import com.example.judinedesk.models.Manager
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ManagerDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvHallName: TextView
    private lateinit var tvLunchCount: TextView
    private lateinit var tvDinnerCount: TextView
    private lateinit var tvLunchTaken: TextView
    private lateinit var tvDinnerTaken: TextView
    private lateinit var btnProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var btnMealMenu: CardView
    private lateinit var btnPostNotice: CardView
    private lateinit var btnPostMeal: CardView
    private lateinit var btnShoppingList: CardView
    private lateinit var btnRegisterStaff: CardView


    private val db = FirebaseFirestore.getInstance()
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("USER_OBJECT", Manager::class.java) ?: getManagerFromAuthHelper()
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Manager>("USER_OBJECT") ?: getManagerFromAuthHelper()
                }
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
        tvLunchCount = findViewById(R.id.tvLunchCount)
        tvDinnerCount = findViewById(R.id.tvDinnerCount)
        tvLunchTaken = findViewById(R.id.tvLunchTaken)
        tvDinnerTaken = findViewById(R.id.tvDinnerTaken)
        btnProfile = findViewById(R.id.btnProfile)
        btnLogout = findViewById(R.id.btnLogout)
        btnMealMenu = findViewById(R.id.btnMealMenu)
        btnPostNotice = findViewById(R.id.btnPostNotice)
        btnPostMeal = findViewById(R.id.btnPostMeal)
        btnShoppingList = findViewById(R.id.btnShoppingList)
        btnRegisterStaff = findViewById(R.id.btnRegisterStaff)


        // Set manager-specific data
        tvHallName.text = currentManager.hall
        tvWelcome.text = "Welcome to Your Dining Management"
    }

    private fun setupClickListeners() {
        btnProfile.setOnClickListener {
            showProfileDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        btnMealMenu.setOnClickListener {
            // Open Meal View Activity (same as student)
            startActivity(Intent(this, MealViewActivity::class.java))
        }

        btnPostNotice.setOnClickListener {
            showPostNoticeDialog()
        }

        btnPostMeal.setOnClickListener {
            showPostMealDialog()
        }

        btnShoppingList.setOnClickListener {
            // Open Shopping Table Activity
            val intent = Intent(this, ShoppingTableActivity::class.java)
            intent.putExtra("USER_OBJECT", currentManager)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
        }
        btnRegisterStaff.setOnClickListener {
            startActivity(Intent(this, RegisterStaffActivity::class.java))
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
        loadMealCounts()
        loadMealsTaken()
    }

    private fun loadMealCounts() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Count lunch meals
        db.collection("meals")
            .whereEqualTo("date", today)
            .whereEqualTo("hall", currentManager.hall)
            .whereEqualTo("type", "Lunch")
            .get()
            .addOnSuccessListener { documents ->
                tvLunchCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("ManagerDashboard", "Error loading lunch count: ${e.message}")
                tvLunchCount.text = "0"
            }

        // Count dinner meals
        db.collection("meals")
            .whereEqualTo("date", today)
            .whereEqualTo("hall", currentManager.hall)
            .whereEqualTo("type", "Dinner")
            .get()
            .addOnSuccessListener { documents ->
                tvDinnerCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("ManagerDashboard", "Error loading dinner count: ${e.message}")
                tvDinnerCount.text = "0"
            }
    }

    private fun loadMealsTaken() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Count lunch bookings
        db.collection("meal_bookings")
            .whereEqualTo("date", today)
            .whereEqualTo("hall", currentManager.hall)
            .whereEqualTo("mealType", "Lunch")
            .get()
            .addOnSuccessListener { documents ->
                tvLunchTaken.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("ManagerDashboard", "Error loading lunch taken: ${e.message}")
                tvLunchTaken.text = "0"
            }

        // Count dinner bookings
        db.collection("meal_bookings")
            .whereEqualTo("date", today)
            .whereEqualTo("hall", currentManager.hall)
            .whereEqualTo("mealType", "Dinner")
            .get()
            .addOnSuccessListener { documents ->
                tvDinnerTaken.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("ManagerDashboard", "Error loading dinner taken: ${e.message}")
                tvDinnerTaken.text = "0"
            }
    }


    // Floating Dialog for Posting Meal
    private fun showPostMealDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_post_meal, null)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDateDialog)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tvSelectedDateDialog)
        val rgMealType = dialogView.findViewById<RadioGroup>(R.id.rgMealTypeDialog)
        val btnAddItem = dialogView.findViewById<Button>(R.id.btnAddItemDialog)
        val llMealItemsContainer = dialogView.findViewById<LinearLayout>(R.id.llMealItemsContainerDialog)
        val btnSubmitMeal = dialogView.findViewById<Button>(R.id.btnSubmitMeal)

        var selectedDate = ""
        val mealItems = mutableListOf<String>()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSelectDate.setOnClickListener {
            showDatePickerDialog { date ->
                selectedDate = date
                tvSelectedDate.text = date
            }
        }

        btnAddItem.setOnClickListener {
            addMealItemFieldDialog(llMealItemsContainer, mealItems)
        }

        btnSubmitMeal.setOnClickListener {
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedTypeId = rgMealType.checkedRadioButtonId
            if (selectedTypeId == -1) {
                Toast.makeText(this, "Please select Lunch or Dinner", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mealType = when (selectedTypeId) {
                R.id.rbLunchDialog -> "Lunch"
                R.id.rbDinnerDialog -> "Dinner"
                else -> ""
            }

            // Check if meal items list is empty
            if (mealItems.isEmpty()) {
                Toast.makeText(this, "Please add at least one meal item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            postMealToFirestore(selectedDate, mealType, mealItems)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, y, m, d ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            c.set(y, m, d)
            val selectedDate = sdf.format(c.time)
            onDateSelected(selectedDate)
        }, year, month, day)
        datePicker.show()
    }

    private fun addMealItemFieldDialog(container: LinearLayout, mealItems: MutableList<String>) {
        // Get the dialog view elements properly
        val dialogView = container.rootView
        val etMealItem = dialogView.findViewById<EditText>(R.id.etMealItem)
        val tvAddedItemsLabel = dialogView.findViewById<TextView>(R.id.tvAddedItemsLabel)

        val mealItemText = etMealItem.text.toString().trim()

        if (mealItemText.isEmpty()) {
            Toast.makeText(this, "Please enter a meal item first", Toast.LENGTH_SHORT).show()
            return
        }

        // Add to meal items list
        mealItems.add(mealItemText)

        // Create a TextView to show the added item
        val tvMealItem = TextView(this)
        tvMealItem.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 4
            bottomMargin = 4
        }
        tvMealItem.text = "• $mealItemText"
        tvMealItem.setTextColor(Color.parseColor("#333333"))
        tvMealItem.textSize = 14f
        tvMealItem.setPadding(8, 8, 8, 8)

        // Show the "Added Items" label
        tvAddedItemsLabel.visibility = View.VISIBLE

        container.addView(tvMealItem)

        // Clear the input field
        etMealItem.text.clear()

        Toast.makeText(this, "✅ $mealItemText added", Toast.LENGTH_SHORT).show()
    }

    private fun postMealToFirestore(date: String, mealType: String, mealItems: List<String>) {
        // Check if meal already exists
        db.collection("meals")
            .whereEqualTo("date", date)
            .whereEqualTo("type", mealType)
            .whereEqualTo("hall", currentManager.hall)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    createNewMeal(date, mealType, mealItems)
                } else {
                    showMealExistsDialog(documents.documents[0].id, date, mealType, mealItems)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error checking existing meals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewMeal(date: String, mealType: String, mealItems: List<String>) {
        val mealData = hashMapOf(
            "date" to date,
            "type" to mealType,
            "items" to mealItems,
            "hall" to currentManager.hall,
            "postedAt" to System.currentTimeMillis(),
            "postedBy" to currentManager.employeeId.ifEmpty { currentManager.uid },
            "managerUid" to currentManager.uid
        )

        db.collection("meals")
            .add(mealData)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Meal posted successfully!", Toast.LENGTH_SHORT).show()
                loadDashboardData() // Refresh dashboard
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Failed to post meal: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ManagerDashboard", "Error posting meal: ${e.message}")
            }
    }

    private fun showMealExistsDialog(mealId: String, date: String, mealType: String, newMeals: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Meal Already Exists")
            .setMessage("A $mealType meal already exists for $date. What would you like to do?")
            .setPositiveButton("🔄 Replace") { _, _ ->
                replaceExistingMeal(mealId, date, mealType, newMeals)
            }
            .setNegativeButton("❌ Cancel", null)
            .show()
    }

    private fun replaceExistingMeal(mealId: String, date: String, mealType: String, newMeals: List<String>) {
        val mealData = hashMapOf(
            "date" to date,
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
                loadDashboardData() // Refresh dashboard
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Failed to update meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Floating Dialog for Posting Notice
    private fun showPostNoticeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_post_notice, null)
        val etNoticeTitle = dialogView.findViewById<EditText>(R.id.etNoticeTitle)
        val etNoticeMessage = dialogView.findViewById<EditText>(R.id.etNoticeMessage)
        val btnSubmitNotice = dialogView.findViewById<Button>(R.id.btnSubmitNotice)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSubmitNotice.setOnClickListener {
            val title = etNoticeTitle.text.toString().trim()
            val message = etNoticeMessage.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter notice title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter notice message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            postNoticeToFirestore(title, message)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun postNoticeToFirestore(title: String, message: String) {
        val noticeData = hashMapOf(
            "title" to title,
            "message" to message,
            "hall" to currentManager.hall,
            "postedBy" to currentManager.employeeId.ifEmpty { currentManager.uid },
            "postedAt" to System.currentTimeMillis(),
            "managerUid" to currentManager.uid
        )

        db.collection("notices")
            .add(noticeData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "✅ Notice published successfully!", Toast.LENGTH_SHORT).show()
                // Also refresh the dashboard to show the new notice
                loadDashboardData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Failed to publish notice: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ManagerDashboard", "Error posting notice: ${e.message}")
            }
    }
}