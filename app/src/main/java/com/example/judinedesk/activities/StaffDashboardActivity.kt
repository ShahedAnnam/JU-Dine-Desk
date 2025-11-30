package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.judinedesk.R
import com.example.judinedesk.models.Staff
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StaffDashboardActivity : AppCompatActivity() {

    private lateinit var tvStaffName: TextView
    private lateinit var tvStaffInfo: TextView
    private lateinit var tvMealsServed: TextView
    private lateinit var tvTodayEarnings: TextView
    private lateinit var btnProfile: TextView
    private lateinit var btnLogout: TextView
    private lateinit var btnQRScanner: CardView
    private lateinit var btnShoppingList: CardView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentStaff: Staff

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_dashboard)

        if (!AuthHelper.isLoggedIn()) {
            redirectToLogin()
            return
        }

        currentStaff = getStaffData()
        setupViews()
        setupClickListeners()
        loadDashboardData()
    }

    private fun getStaffData(): Staff {
        return (AuthHelper.currentUserData as? Staff) ?: Staff(
            uid = AuthHelper.getCurrentUid() ?: "",
            email = AuthHelper.getCurrentUid() ?: "Unknown",
            name = "Staff Member",
            hall = "Unknown Hall",
            role = "staff"
        )
    }

    private fun setupViews() {
        tvStaffName = findViewById(R.id.tvStaffName)
        tvStaffInfo = findViewById(R.id.tvStaffInfo)
        tvMealsServed = findViewById(R.id.tvMealsServed)
        tvTodayEarnings = findViewById(R.id.tvTodayEarnings)
        btnProfile = findViewById(R.id.btnProfile)
        btnLogout = findViewById(R.id.btnLogout)
        btnQRScanner = findViewById(R.id.btnQRScanner)
        btnShoppingList = findViewById(R.id.btnShoppingList)

        // Set staff-specific data
        tvStaffName.text = currentStaff.name
        tvStaffInfo.text = "${currentStaff.position} - ${currentStaff.hall}"
    }

    private fun setupClickListeners() {
        btnProfile.setOnClickListener {
            showProfileDialog()
        }

        btnLogout.setOnClickListener {
            AuthHelper.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }

        btnQRScanner.setOnClickListener {
            Toast.makeText(this, "📱 Opening QR Scanner", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, QRScannerActivity::class.java))
        }

        btnShoppingList.setOnClickListener {
            startActivity(Intent(this, ShoppingListActivity::class.java))
        }

    }

    private fun showProfileDialog() {
        val profileMessage = """
        👤 Name: ${currentStaff.name}
        📧 Email: ${currentStaff.email}
        🏢 Hall: ${currentStaff.hall}
        💼 Position: ${currentStaff.position}
        📞 Contact: ${currentStaff.contactNumber}
        🏠 District: ${currentStaff.homeDistrict}
        🎂 Age: ${currentStaff.age} years
        💰 Salary: ৳${currentStaff.salary}
        📅 Joined: ${currentStaff.joinDate}
        🆔 Employee ID: ${currentStaff.employeeId}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Staff Profile")
            .setMessage(profileMessage)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadDashboardData() {
        loadTodayStats()
    }

    private fun loadTodayStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Count meals served today by this staff
        db.collection("meal_bookings")
            .whereEqualTo("date", today)
            .whereEqualTo("hall", currentStaff.hall)
            .whereEqualTo("servedBy", currentStaff.uid)
            .get()
            .addOnSuccessListener { documents ->
                val mealsServed = documents.size()
                tvMealsServed.text = mealsServed.toString()

                // Calculate earnings (assuming ৳30 per meal)
                val earnings = mealsServed * 30
                tvTodayEarnings.text = "৳ $earnings"
            }
            .addOnFailureListener { e ->
                Log.e("StaffDashboard", "Error loading stats: ${e.message}")
                tvMealsServed.text = "0"
                tvTodayEarnings.text = "৳ 0"
            }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to dashboard
        if (AuthHelper.isLoggedIn()) {
            loadDashboardData()
        }
    }
}