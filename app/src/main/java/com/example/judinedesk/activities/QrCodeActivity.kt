package com.example.judinedesk.activities

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.*

class QRCodeActivity : AppCompatActivity() {

    private lateinit var ivQRCode: ImageView
    private lateinit var tvMealType: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvPurchaseId: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnBack: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutQRContent: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private lateinit var purchaseId: String
    private lateinit var qrData: String

    companion object {
        private const val TAG = "QRCodeActivity"
        private const val QR_CODE_SIZE = 800
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)

        // Get data from intent
        qrData = intent.getStringExtra("qrData") ?: run {
            showError("Missing QR data")
            return
        }
        purchaseId = intent.getStringExtra("purchaseId") ?: run {
            showError("Missing purchase data")
            return
        }

        Log.d(TAG, "Displaying QR for purchase: $purchaseId")
        Log.d(TAG, "QR Data: $qrData")

        setupViews()
        generateQRCode()
        loadPurchaseDetails()
    }

    private fun setupViews() {
        ivQRCode = findViewById(R.id.ivQRCode)
        tvMealType = findViewById(R.id.tvMealType)
        tvPrice = findViewById(R.id.tvPrice)
        tvPurchaseId = findViewById(R.id.tvPurchaseId)
        tvStatus = findViewById(R.id.tvStatus)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        layoutQRContent = findViewById(R.id.layoutQRContent)

        // Hide content initially, show progress
        layoutQRContent.visibility = android.view.View.GONE
        progressBar.visibility = android.view.View.VISIBLE

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun generateQRCode() {
        try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.MARGIN] = 1
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            runOnUiThread {
                ivQRCode.setImageBitmap(bitmap)
                progressBar.visibility = android.view.View.GONE
                layoutQRContent.visibility = android.view.View.VISIBLE
            }

            Log.d(TAG, "QR code generated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "QR code generation failed: ${e.message}", e)
            showError("Failed to generate QR code")
        }
    }

    private fun loadPurchaseDetails() {
        // Get meal details from intent first (fallback)
        val mealType = intent.getStringExtra("mealType") ?: "Meal"
        val mealPrice = intent.getDoubleExtra("mealPrice", 0.0)

        // Try to load from Firestore for updated data
        db.collection("purchases").document(purchaseId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val purchase = document.data
                    val type = purchase?.get("type") as? String ?: mealType
                    val price = purchase?.get("price") as? Double ?: mealPrice
                    val status = purchase?.get("paymentStatus") as? String ?: "paid"
                    val date = purchase?.get("displayDate") as? String ?: ""
                    val hall = purchase?.get("hall") as? String ?: ""

                    updateUI(type, price, status, date, hall)
                    Log.d(TAG, "Purchase details loaded from Firestore")
                } else {
                    // Use intent data if Firestore document doesn't exist
                    updateUI(mealType, mealPrice, "paid", "", "")
                    Log.w(TAG, "Purchase document not found, using intent data")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load purchase details: ${e.message}")
                // Use intent data as fallback
                updateUI(mealType, mealPrice, "paid", "", "")
            }
    }

    private fun updateUI(mealType: String, price: Double, status: String, date: String, hall: String) {
        runOnUiThread {
            tvMealType.text = "$mealType Coupon"
            tvPrice.text = "Price: ${price} BDT"
            tvPurchaseId.text = "ID: $purchaseId"
            tvStatus.text = "Status: ${status.uppercase(Locale.getDefault())}"

            // You can add more UI updates here for date and hall if needed
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        runOnUiThread {
            progressBar.visibility = android.view.View.GONE
            layoutQRContent.visibility = android.view.View.GONE

            Toast.makeText(this, message, Toast.LENGTH_LONG).show()

            // Optionally show a retry button or go back

        }
    }
}