package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {

    private lateinit var tvMealType: TextView
    private lateinit var tvAmount: TextView
    private lateinit var btnConfirmPayment: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutPayment: LinearLayout
    private lateinit var layoutProcessing: LinearLayout
    private lateinit var layoutSuccess: LinearLayout
    private lateinit var layoutError: LinearLayout
    private lateinit var btnRetry: Button

    private val db = FirebaseFirestore.getInstance()
    private lateinit var purchaseId: String
    private lateinit var mealType: String
    private var mealPrice: Double = 30.0
    private lateinit var selectedDate: String
    private lateinit var hall: String
    private lateinit var mealId: String
    private lateinit var studentId: String

    companion object {
        private const val TAG = "PaymentActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Get data from intent
        purchaseId = intent.getStringExtra("purchaseId") ?: run {
            showError("Missing purchase data")
            return
        }

        mealType = intent.getStringExtra("mealType") ?: "Meal"
        mealPrice = intent.getDoubleExtra("mealPrice", 50.0)
        selectedDate = intent.getStringExtra("selectedDate") ?: ""
        hall = intent.getStringExtra("hall") ?: ""
        mealId = intent.getStringExtra("mealId") ?: ""
        studentId = intent.getStringExtra("studentId") ?: ""

        Log.d(TAG, "Starting payment for: $purchaseId")

        setupViews()
        setupPaymentInfo()
        setupClickListeners()
    }

    private fun setupViews() {
        tvMealType = findViewById(R.id.tvMealType)
        tvAmount = findViewById(R.id.tvAmount)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        progressBar = findViewById(R.id.progressBar)
        layoutPayment = findViewById(R.id.layoutPayment)
        layoutProcessing = findViewById(R.id.layoutProcessing)
        layoutSuccess = findViewById(R.id.layoutSuccess)
        layoutError = findViewById(R.id.layoutError)
        btnRetry = findViewById(R.id.btnRetry)

        // Hide all layouts except payment initially
        layoutProcessing.visibility = android.view.View.GONE
        layoutSuccess.visibility = android.view.View.GONE
        layoutError.visibility = android.view.View.GONE
        layoutPayment.visibility = android.view.View.VISIBLE
    }

    private fun setupPaymentInfo() {
        tvMealType.text = "$mealType Coupon"
        tvAmount.text = "Amount: $mealPrice BDT"
    }

    private fun setupClickListeners() {
        btnConfirmPayment.setOnClickListener {
            processPayment()
        }

        btnRetry.setOnClickListener {
            // Reset to payment screen
            layoutError.visibility = android.view.View.GONE
            layoutPayment.visibility = android.view.View.VISIBLE
            btnConfirmPayment.isEnabled = true
        }
    }

    private fun processPayment() {
        // Show processing state
        layoutPayment.visibility = android.view.View.GONE
        layoutProcessing.visibility = android.view.View.VISIBLE
        btnConfirmPayment.isEnabled = false

        Log.d(TAG, "Processing payment for: $purchaseId")

        // Simulate payment processing (2 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            completePayment()
        }, 2000)
    }

    private fun completePayment() {
        // Generate QR data based on purchase ID
        val qrData = "JUDINE:$purchaseId:${System.currentTimeMillis()}"
        val timestamp = System.currentTimeMillis()
        val displayDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

        // Create a unique identifier for this meal type and date
        val mealDateId = "${selectedDate}_${mealType.lowercase()}"

        Log.d(TAG, "Generated QR Data: $qrData")
        Log.d(TAG, "Meal Date ID: $mealDateId")

        val purchaseData = hashMapOf(
            "purchaseId" to purchaseId,
            "studentId" to studentId,
            "mealId" to mealId,
            "type" to mealType,
            "date" to selectedDate,
            "displayDate" to displayDate,
            "hall" to hall,
            "price" to mealPrice,
            "qrData" to qrData,
            "paymentStatus" to "paid",
            "paymentTimestamp" to timestamp,
            "createdAt" to timestamp,
            "mealDateId" to mealDateId, // Add this for easy querying
            "isActive" to true // Add this to mark active QR codes
        )

        Log.d(TAG, "Purchase data: $purchaseData")

        // Try to save to Firestore
        db.collection("purchases").document(purchaseId)
            .set(purchaseData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ SUCCESS: Payment successfully saved to Firestore")

                // Show success state
                layoutProcessing.visibility = android.view.View.GONE
                layoutSuccess.visibility = android.view.View.VISIBLE

                // Navigate to QR Code Activity after 1.5 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToQRCode(qrData)
                }, 1500)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Firestore save failed: ${e.message}", e)
                showError("Payment failed: ${e.message ?: "Unknown error"}")
            }
    }

    private fun navigateToQRCode(qrData: String) {
        try {
            val intent = Intent(this, QRCodeActivity::class.java).apply {
                putExtra("qrData", qrData)
                putExtra("purchaseId", purchaseId)
                putExtra("mealType", mealType)
                putExtra("mealPrice", mealPrice)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to QRCode: ${e.message}", e)
            showError("Failed to generate QR code")
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        runOnUiThread {
            layoutPayment.visibility = android.view.View.GONE
            layoutProcessing.visibility = android.view.View.GONE
            layoutSuccess.visibility = android.view.View.GONE
            layoutError.visibility = android.view.View.VISIBLE

            val tvError = findViewById<TextView>(R.id.tvError)
            tvError.text = message

            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
