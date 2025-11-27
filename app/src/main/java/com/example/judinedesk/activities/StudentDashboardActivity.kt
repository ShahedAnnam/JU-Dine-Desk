package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.judinedesk.R
import com.example.judinedesk.models.Student
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var tvStudentName: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var tvNoticeMarquee: TextView
    private lateinit var tvLunchStatus: TextView
    private lateinit var tvDinnerStatus: TextView
    private lateinit var tvRecentActivity: TextView
    private lateinit var btnProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var btnMealMenu: CardView
    private lateinit var btnBuyCoupon: CardView
    private lateinit var btnQRCode: CardView
    private lateinit var btnAIChat: CardView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentStudent: Student
    private val handler = Handler(Looper.getMainLooper())
    private var currentNoticeIndex = 0
    private var noticesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        // Check login
        if (!AuthHelper.isLoggedIn()) {
            redirectToLogin()
            return
        }

        // Get student data
        currentStudent = getStudentData()

        setupViews()
        setupClickListeners()
        loadDashboardData()
        startNoticeMarquee()
    }

    private fun getStudentData(): Student {
        return (AuthHelper.currentUserData as? Student) ?: Student(
            uid = AuthHelper.getCurrentUid() ?: "",
            email = AuthHelper.getCurrentUid() ?: "Unknown",
            hall = "Unknown Hall",
            role = "student",
            name = "Student"
        )
    }

    private fun setupViews() {
        tvStudentName = findViewById(R.id.tvStudentName)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvNoticeMarquee = findViewById(R.id.tvNoticeMarquee)
        tvLunchStatus = findViewById(R.id.tvLunchStatus)
        tvDinnerStatus = findViewById(R.id.tvDinnerStatus)
        tvRecentActivity = findViewById(R.id.tvRecentActivity)
        btnProfile = findViewById(R.id.btnProfile)
        btnLogout = findViewById(R.id.btnLogout)
        btnMealMenu = findViewById(R.id.btnMealMenu)
        btnBuyCoupon = findViewById(R.id.btnBuyCoupon)
        btnQRCode = findViewById(R.id.btnQRCode)
        btnAIChat = findViewById(R.id.btnAIChat)

        // Set student-specific data
        tvStudentName.text = currentStudent.name
        tvWelcome.text = "Welcome to ${currentStudent.hall}"
    }

    private fun setupClickListeners() {
        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            AuthHelper.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }

        btnMealMenu.setOnClickListener {
            startActivity(Intent(this, MealViewActivity::class.java))
        }

        btnBuyCoupon.setOnClickListener {
            Toast.makeText(this, "🎫 Opening Coupon Purchase", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, BuyCouponActivity::class.java))
        }

        btnQRCode.setOnClickListener {
            Toast.makeText(this, "📱 Generating QR Code", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, QRCodeActivity::class.java))
        }

        btnAIChat.setOnClickListener {
            Toast.makeText(this, "🤖 Opening AI Assistant", Toast.LENGTH_SHORT).show()
             startActivity(Intent(this, ChatbotActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        loadNotices()
        loadTodayMealStatus()
        loadRecentActivity()
    }

    private fun loadNotices() {
        db.collection("notices")
            .whereEqualTo("hall", currentStudent.hall)
            .orderBy("postedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                noticesList.clear()

                if (documents.isEmpty) {
                    noticesList.add("No new notices available")
                } else {
                    for (document in documents) {
                        val title = document.getString("title") ?: ""
                        val message = document.getString("message") ?: ""
                        noticesList.add("📢 $title: $message")
                    }
                }

                startNoticeMarquee()
            }
            .addOnFailureListener { e ->
                Log.e("StudentDashboard", "Error loading notices: ${e.message}")
                noticesList.add("Error loading notices")
                startNoticeMarquee()
            }
    }

    private fun startNoticeMarquee() {
        handler.removeCallbacks(marqueeRunnable)

        if (noticesList.isNotEmpty()) {
            currentNoticeIndex = 0
            handler.post(marqueeRunnable)
        } else {
            tvNoticeMarquee.text = "No notices available"
        }
    }

    private val marqueeRunnable = object : Runnable {
        override fun run() {
            if (noticesList.isNotEmpty()) {
                tvNoticeMarquee.text = noticesList[currentNoticeIndex]
                tvNoticeMarquee.isSelected = true // Enable marquee

                currentNoticeIndex = (currentNoticeIndex + 1) % noticesList.size
                handler.postDelayed(this, 5000) // Change notice every 5 seconds
            }
        }
    }

    private fun loadTodayMealStatus() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Check lunch
        db.collection("meals")
            .whereEqualTo("date", today)
            .whereEqualTo("hall", currentStudent.hall)
            .whereEqualTo("type", "Lunch")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvLunchStatus.text = "Not Posted"
                    tvLunchStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                } else {
                    tvLunchStatus.text = "Available"
                    tvLunchStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                }
            }

        // Check dinner
        db.collection("meals")
            .whereEqualTo("date", today)
            .whereEqualTo("hall", currentStudent.hall)
            .whereEqualTo("type", "Dinner")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvDinnerStatus.text = "Not Posted"
                    tvDinnerStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                } else {
                    tvDinnerStatus.text = "Available"
                    tvDinnerStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                }
            }
    }

    private fun loadRecentActivity() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("meal_bookings")
            .whereEqualTo("studentId", currentStudent.uid)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { documents ->
                val activityText = StringBuilder()

                if (documents.isEmpty) {
                    activityText.append("• No meals booked today\n")
                    activityText.append("• Check meal menu to book")
                } else {
                    for (document in documents) {
                        val mealType = document.getString("mealType") ?: ""
                        activityText.append("• $mealType meal booked\n")
                    }
                }

                tvRecentActivity.text = activityText.toString()
            }
            .addOnFailureListener { e ->
                tvRecentActivity.text = "• Error loading activity\n• Please try again later"
            }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(marqueeRunnable)
    }
}