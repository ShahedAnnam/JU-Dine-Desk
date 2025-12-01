package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.adapters.QRCodeAdapter
import com.example.judinedesk.models.QRCodeItem
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class MyQRCodesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QRCodeAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var qrCodesListener: ListenerRegistration? = null
    private lateinit var studentId: String

    companion object {
        private const val TAG = "MyQRCodesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_qr_codes)

        setupViews()
        setupRecyclerView()
        getStudentIdAndLoadQRCodes()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = QRCodeAdapter { qrCodeItem ->
            // Handle QR code item click - show full QR code
            val intent = Intent(this, QRCodeActivity::class.java).apply {
                putExtra("qrData", qrCodeItem.qrData)
                putExtra("purchaseId", qrCodeItem.purchaseId)
                putExtra("mealType", qrCodeItem.mealType)
                putExtra("mealPrice", qrCodeItem.price)
                putExtra("selectedDate", qrCodeItem.date)
                putExtra("hall", qrCodeItem.hall)
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun getStudentIdAndLoadQRCodes() {
        progressBar.visibility = android.view.View.VISIBLE
        tvEmpty.visibility = android.view.View.GONE

        // Method 1: Get student ID from Firebase Auth (Recommended)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            studentId = currentUser.uid
            Log.d(TAG, "Using Firebase Auth UID: $studentId")
            loadQRCodes()
            return
        }



        // If all methods fail, show error
        showError("Unable to identify student. Please log in again.")
        Log.e(TAG, "No student ID found from any source")
    }

    private fun loadQRCodes() {
        Log.d(TAG, "Loading QR codes for student: $studentId")

        if (studentId.isEmpty()) {
            showError("Student ID is empty")
            return
        }

        // Get today's date in the same format as stored in Firestore
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d(TAG, "Today's date for filtering: $todayDate")

        // Listen for real-time updates
        qrCodesListener = db.collection("purchases")
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("paymentStatus", "paid")
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = android.view.View.GONE

                if (error != null) {
                    Log.e(TAG, "Firestore error: ${error.message}")
                    tvEmpty.visibility = android.view.View.VISIBLE
                    tvEmpty.text = "Error: ${error.message}"
                    return@addSnapshotListener
                }

                val qrCodeItems = mutableListOf<QRCodeItem>()

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "No documents found for student: $studentId")
                    tvEmpty.visibility = android.view.View.VISIBLE
                    tvEmpty.text = "No QR codes found for today. Purchase meals first."
                    return@addSnapshotListener
                }

                Log.d(TAG, "Found ${snapshot.documents.size} purchase documents")

                snapshot.documents.forEach { document ->
                    val data = document.data
                    Log.d(TAG, "Document ID: ${document.id}, Data: $data")

                    try {
                        // Convert Firestore data to QRCodeItem
                        val item = QRCodeItem(
                            purchaseId = data?.get("purchaseId")?.toString() ?: document.id,
                            studentId = data?.get("studentId")?.toString() ?: studentId,
                            mealType = data?.get("type")?.toString() ?: "Unknown Meal",
                            date = data?.get("date")?.toString() ?: "",
                            displayDate = data?.get("displayDate")?.toString() ?: "",
                            hall = data?.get("hall")?.toString() ?: "Unknown Hall",
                            price = convertToDouble(data?.get("price")),
                            qrData = data?.get("qrData")?.toString() ?: "",
                            paymentTimestamp = convertToLong(data?.get("paymentTimestamp"))
                        )

                        // Only add if we have QR data AND the date matches today
                        if (item.qrData.isNotEmpty() && item.date == todayDate) {
                            qrCodeItems.add(item)
                            Log.d(TAG, "✓ Added TODAY'S QR: ${item.mealType} for ${item.date}")
                        } else if (item.qrData.isNotEmpty() && item.date != todayDate) {
                            Log.d(TAG, "⚠ Skipping - Not today's QR code: ${item.date} (Today is: $todayDate)")
                        } else {
                            Log.w(TAG, "⚠ Skipping - No QR data: ${document.id}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing document ${document.id}: ${e.message}")
                    }
                }

                // Sort by date (newest first) and then by meal type
                qrCodeItems.sortWith(compareByDescending<QRCodeItem> { it.date }
                    .thenBy { if (it.mealType.lowercase() == "lunch") 0 else 1 })

                adapter.submitList(qrCodeItems)

                if (qrCodeItems.isEmpty()) {
                    tvEmpty.visibility = android.view.View.VISIBLE
                    tvEmpty.text = "No QR codes for today. Purchase today's meals first."
                    Log.w(TAG, "No today's QR code items found. Today's date: $todayDate")
                } else {
                    tvEmpty.visibility = android.view.View.GONE
                    Log.i(TAG, "✅ Successfully loaded ${qrCodeItems.size} QR codes for today")

                    // Debug: Log all loaded QR codes
                    qrCodeItems.forEach { item ->
                        Log.d(TAG, "📱 Today's QR: ${item.date} - ${item.mealType} - ${item.qrData.take(20)}...")
                    }
                }
            }
    }

    // Helper functions to safely convert Firestore types
    private fun convertToDouble(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun convertToLong(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Error: $message")
        runOnUiThread {
            progressBar.visibility = android.view.View.GONE
            tvEmpty.visibility = android.view.View.VISIBLE
            tvEmpty.text = message
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        qrCodesListener?.remove()
        Log.d(TAG, "MyQRCodesActivity destroyed")
    }
}