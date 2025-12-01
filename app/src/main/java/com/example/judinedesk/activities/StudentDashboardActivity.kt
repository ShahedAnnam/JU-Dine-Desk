package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
    private lateinit var tvNoticeCount: TextView
    private lateinit var tvLunchStatus: TextView
    private lateinit var tvDinnerStatus: TextView
    private lateinit var btnProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var btnMealMenu: CardView
    private lateinit var btnBuyCoupon: CardView
    private lateinit var btnQRCode: CardView
    private lateinit var btnAIChat: CardView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentStudent: Student
    private val handler = Handler(Looper.getMainLooper())

    // Notice variables
    private var currentNoticeIndex = 0
    private var noticesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        if (!AuthHelper.isLoggedIn()) {
            redirectToLogin()
            return
        }

        currentStudent = getStudentData()
        setupViews()
        setupClickListeners()
        loadDashboardData()
    }

    private fun getStudentData(): Student {
        return (AuthHelper.currentUserData as? Student) ?: Student(
            uid = AuthHelper.getCurrentUid() ?: "",
            email = AuthHelper.getCurrentUid() ?: "Unknown",
            hall = "21 No Hall",
            role = "student",
            name = "Student"
        )
    }

    private fun setupViews() {
        tvStudentName = findViewById(R.id.tvStudentName)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvNoticeMarquee = findViewById(R.id.tvNoticeMarquee)
        tvNoticeCount = findViewById(R.id.tvNoticeCount)
        tvLunchStatus = findViewById(R.id.tvLunchStatus)
        tvDinnerStatus = findViewById(R.id.tvDinnerStatus)
        btnProfile = findViewById(R.id.btnProfile)
        btnLogout = findViewById(R.id.btnLogout)
        btnMealMenu = findViewById(R.id.btnMealMenu)
        btnBuyCoupon = findViewById(R.id.btnBuyCoupon)
        btnQRCode = findViewById(R.id.btnQRCode)
        btnAIChat = findViewById(R.id.btnAIChat)

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
            startActivity(Intent(this, BuyCouponActivity::class.java))
        }

        btnQRCode.setOnClickListener {
            startActivity(Intent(this, MyQRCodesActivity::class.java))
        }

        btnAIChat.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        loadNotices()
        loadTodayMealStatus()
    }

    private fun loadNotices() {
        Log.d("NoticeLoad", "Loading notices for hall: ${currentStudent.hall}")

        db.collection("notices")
            .whereEqualTo("hall", currentStudent.hall)
            //.orderBy("postedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("NoticeLoad", "✅ Loaded ${documents.size()} notices")
                noticesList.clear()

                if (documents.isEmpty()) {
                    showNoNotices()
                } else {
                    // Format notices for marquee
                    for (document in documents) {
                        val title = document.getString("title") ?: "New Notice"
                        val message = document.getString("message") ?: ""
                        val timestamp = document.getLong("postedAt") ?: 0L

                        val timeText = formatNoticeTime(timestamp)
                        val formattedNotice = "📢 $title: $message • $timeText"
                        noticesList.add(formattedNotice)
                    }

                    tvNoticeCount.text = "${noticesList.size} new"
                    startNoticeMarquee()
                }
            }
            .addOnFailureListener { e ->
                Log.e("NoticeLoad", "❌ Failed to load notices: ${e.message}")
                showErrorState()
            }
    }

    private fun showNoNotices() {
        noticesList.clear()
        noticesList.add("🌟 Welcome to ${currentStudent.hall}! No notices available yet.")
        tvNoticeCount.text = "0 new"
        startNoticeMarquee()
    }

    private fun showErrorState() {
        noticesList.clear()
        noticesList.add("❌ Connection issue. Please check your internet and try again.")
        tvNoticeCount.text = "0 new"
        startNoticeMarquee()
    }

    private fun startNoticeMarquee() {
        handler.removeCallbacks(marqueeRunnable)

        if (noticesList.isNotEmpty()) {
            currentNoticeIndex = 0
            // Start marquee immediately
            handler.post(marqueeRunnable)
        }
    }

    private val marqueeRunnable = object : Runnable {
        override fun run() {
            if (noticesList.isNotEmpty() && currentNoticeIndex < noticesList.size) {
                val notice = noticesList[currentNoticeIndex]
                tvNoticeMarquee.text = notice
                tvNoticeMarquee.isSelected = true // This enables the marquee effect

                Log.d("Marquee", "Showing notice: ${notice.take(50)}...")

                // Move to next notice
                currentNoticeIndex = (currentNoticeIndex + 1) % noticesList.size

                // Change notice every 8 seconds (marquee scrolls continuously within each notice)
                handler.postDelayed(this, 8000)
            }
        }
    }

    private fun formatNoticeTime(timestamp: Long): String {
        if (timestamp == 0L) return "Recently"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
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


    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(marqueeRunnable)
    }
}