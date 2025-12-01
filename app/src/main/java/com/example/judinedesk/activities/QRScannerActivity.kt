package com.example.judinedesk.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.judinedesk.R
import com.example.judinedesk.models.Staff
import com.example.judinedesk.utils.AuthHelper
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage
class QRScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var scannerLine: View
    private lateinit var btnFlash: ImageView
    private lateinit var btnClose: ImageView
    private lateinit var btnManualInput: Button

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isScanning = false
    private lateinit var currentStaff: Staff
    private val db = FirebaseFirestore.getInstance()
    private lateinit var barcodeScanner: BarcodeScanner

    companion object {
        private const val TAG = "QRScannerActivity"
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner_camera_x)

        // Get current staff
        currentStaff = (AuthHelper.currentUserData as? Staff) ?: run {
            Toast.makeText(this, "Staff authentication required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        setupBarcodeScanner()
        checkCameraPermission()
    }

    private fun setupViews() {
        previewView = findViewById(R.id.previewView)
        scannerLine = findViewById(R.id.scannerLine)
        btnFlash = findViewById(R.id.btnFlash)
        btnClose = findViewById(R.id.btnClose)
        btnManualInput = findViewById(R.id.btnManualInput)

        btnFlash.setOnClickListener { toggleFlash() }
        btnClose.setOnClickListener { finish() }

        btnManualInput.setOnClickListener {
            showManualInputDialog()
        }

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupBarcodeScanner() {
        // Using ML Kit for better QR scanning
        barcodeScanner = BarcodeScanning.getClient()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture?.addListener({
            try {
                val cameraProvider = cameraProviderFuture!!.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image Analysis for QR scanning
                @OptIn(ExperimentalGetImage::class)
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }

                // Camera selector
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Bind use cases
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                // Start scanner animation
                startScannerAnimation()

            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera: ${e.message}")
                Toast.makeText(this, "Camera initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        if (isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null && rawValue.isNotEmpty()) {
                            // Found a QR code
                            isScanning = true
                            runOnUiThread {
                                handleScannedQRCode(rawValue)
                            }
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    // Allow scanning again after delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        isScanning = false
                    }, 2000)
                }
        } else {
            imageProxy.close()
        }
    }

    private fun startScannerAnimation() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.scanner_line_animation)
        scannerLine.startAnimation(animation)
    }

    private fun toggleFlash() {
        Toast.makeText(this, "Flash toggle coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun handleScannedQRCode(qrData: String) {
        Log.d(TAG, "QR Code Scanned: $qrData")
        Log.d(TAG, "QR Code Length: ${qrData.length}")

        // Show what we scanned for debugging
        val displayText = "Scanned: ${qrData.take(50)}..."
        Toast.makeText(this, displayText, Toast.LENGTH_LONG).show()

        validateQRCode(qrData)
    }

    private fun validateQRCode(qrData: String) {
        Log.d(TAG, "Validating QR Code: $qrData")

        // Check if it starts with JUDINE:
        if (!qrData.startsWith("JUDINE:")) {
            showErrorDialog("Invalid QR code. Not a JU Dine coupon.")
            return
        }

        // Remove "JUDINE:" prefix
        val dataWithoutPrefix = qrData.substring(7) // Remove "JUDINE:"

        // Split by :
        val parts = dataWithoutPrefix.split(":")

        if (parts.size < 3) {
            showErrorDialog("Invalid QR format. Expected: JUDINE:purchaseId:timestamp1:timestamp2")
            Log.d(TAG, "Parts after split: $parts")
            return
        }

        // Extract data - format is: purchaseId_timestamp:timestamp1:timestamp2
        val purchaseIdWithTimestamp = parts[0] // "MWfg9RGjtEcKYqn9O7GmC5FRxu53_1KbcHVz3Efxu3qdYBrMk_1764586977450"
        val timestamp1 = parts[1].toLongOrNull() // "1764586981375"
        val timestamp2 = parts.getOrNull(2)?.toLongOrNull()

        // Extract just the purchase ID (remove the _timestamp suffix)
        val purchaseId = if (purchaseIdWithTimestamp.contains("_")) {
            purchaseIdWithTimestamp.substringBeforeLast("_")
        } else {
            purchaseIdWithTimestamp
        }

        Log.d(TAG, "Extracted Purchase ID: $purchaseId")
        Log.d(TAG, "Original String: $purchaseIdWithTimestamp")
        Log.d(TAG, "Timestamp1: $timestamp1, Timestamp2: $timestamp2")

        // Use the first valid timestamp for expiration check
        val timestamp = timestamp1 ?: timestamp2 ?: System.currentTimeMillis()

        // Check expiration (24 hours)
        val currentTime = System.currentTimeMillis()
        if (currentTime - timestamp > 24 * 60 * 60 * 1000) {
            showErrorDialog("QR Code has expired (24 hours)")
            return
        }

        // Fetch purchase from Firestore
        fetchPurchase(purchaseId)
    }

    private fun fetchPurchase(purchaseId: String) {
        Log.d(TAG, "Fetching purchase from Firestore: $purchaseId")

        // First try to get by document ID
        db.collection("purchases").document(purchaseId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        // Get studentId from document data
                        val studentId = data["studentId"] as? String ?: ""
                        processPurchase(data, purchaseId, studentId)
                    } else {
                        showErrorDialog("Invalid purchase data in document")
                    }
                } else {
                    // Try searching by purchaseId field if document ID doesn't match
                    searchPurchaseByPurchaseIdField(purchaseId)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching purchase: ${e.message}")
                showErrorDialog("Error validating QR Code: ${e.message}")
            }
    }

    private fun searchPurchaseByPurchaseIdField(purchaseId: String) {
        Log.d(TAG, "Searching by purchaseId field: $purchaseId")

        db.collection("purchases")
            .whereEqualTo("purchaseId", purchaseId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showErrorDialog("Purchase not found: $purchaseId")
                    return@addOnSuccessListener
                }

                val document = documents.documents[0]
                val data = document.data
                if (data != null) {
                    val actualDocId = document.id
                    val studentId = data["studentId"] as? String ?: ""
                    Log.d(TAG, "Found purchase with document ID: $actualDocId")
                    processPurchase(data, actualDocId, studentId)
                } else {
                    showErrorDialog("Invalid purchase data")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error searching purchase: ${e.message}")
                showErrorDialog("Error finding purchase")
            }
    }

    private fun processPurchase(data: Map<String, Any>, purchaseId: String, studentId: String) {
        val paymentStatus = data["paymentStatus"] as? String
        val mealType = data["type"] as? String
        val hall = data["hall"] as? String
        val date = data["date"] as? String
        val price = data["price"] as? Double ?: 0.0
        val isUsed = data["isUsed"] as? Boolean ?: false

        Log.d(TAG, "Processing purchase: $purchaseId")
        Log.d(TAG, "Payment status: $paymentStatus, Used: $isUsed, Hall: $hall, Date: $date")

        if (isUsed) {
            showErrorDialog("This coupon has already been used")
            return
        }

        if (paymentStatus != "paid") {
            showErrorDialog("Payment not completed for this coupon")
            return
        }

        if (hall != currentStaff.hall) {
            showErrorDialog("This coupon is for $hall hall\nYou are from ${currentStaff.hall}")
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (date != today) {
            showErrorDialog("This coupon is for $date, not today ($today)")
            return
        }

        showConfirmationDialog(purchaseId, studentId, mealType, price, hall)
    }

    private fun showConfirmationDialog(
        purchaseId: String,
        studentId: String,
        mealType: String?,
        price: Double,
        hall: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("✅ Confirm Meal Service")
            .setMessage(
                """
                Meal Type: ${mealType ?: "Unknown"}
                Price: ৳$price
                Hall: $hall
                Student ID: ${studentId.take(8)}...
                Staff: ${currentStaff.name}
                
                Do you want to mark this coupon as served?
                """.trimIndent()
            )
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                isScanning = true
            }
            .setPositiveButton("Confirm Serve") { dialog, _ ->
                dialog.dismiss()
                markCouponAsServed(purchaseId, studentId, mealType ?: "Unknown", price)
            }
            .setCancelable(false)
            .show()
    }

    private fun markCouponAsServed(purchaseId: String, studentId: String, mealType: String, price: Double) {
        val currentTime = System.currentTimeMillis()

        val updateData = hashMapOf<String, Any>(
            "isUsed" to true,
            "servedBy" to currentStaff.uid,
            "servedByName" to currentStaff.name,
            "servedAt" to currentTime,
            "servedHall" to currentStaff.hall
        )

        db.collection("purchases").document(purchaseId)
            .update(updateData)
            .addOnSuccessListener {
                val bookingData = hashMapOf<String, Any>(
                    "purchaseId" to purchaseId,
                    "studentId" to studentId,
                    "mealType" to mealType,
                    "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    "hall" to currentStaff.hall,
                    "price" to price,
                    "servedBy" to currentStaff.uid,
                    "servedByName" to currentStaff.name,
                    "servedAt" to currentTime,
                    "servedTimestamp" to currentTime
                )

                db.collection("meal_bookings")
                    .add(bookingData)
                    .addOnSuccessListener {
                        showSuccessDialog(mealType, price, studentId)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error creating booking: ${e.message}")
                        showSuccessDialog(mealType, price, studentId)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating purchase: ${e.message}")
                showErrorDialog("Failed to mark as served: ${e.message}")
            }
    }

    private fun showSuccessDialog(mealType: String, price: Double, studentId: String) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("🎉 Meal Served Successfully!")
                .setMessage(
                    """
                    ✅ Meal Type: $mealType
                    ✅ Amount: ৳$price
                    ✅ Student ID: ${studentId.take(8)}...
                    ✅ Served By: ${currentStaff.name}
                    ✅ Time: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}
                    
                    Meal has been marked as served in the system.
                    """.trimIndent()
                )
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    isScanning = true
                    Toast.makeText(this, "✓ Meal served successfully!", Toast.LENGTH_LONG).show()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun showErrorDialog(message: String) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("❌ Error")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    isScanning = true
                }
                .show()
        }
    }

    private fun showManualInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_qr_input, null)
        val etQRCode = dialogView.findViewById<EditText>(R.id.etQRCode)

        AlertDialog.Builder(this)
            .setTitle("Enter QR Code Manually")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Validate") { _, _ ->
                val qrCode = etQRCode.text.toString().trim()
                if (qrCode.isNotEmpty()) {
                    handleScannedQRCode(qrCode)
                } else {
                    Toast.makeText(this, "Please enter QR code", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        isScanning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}