package com.example.judinedesk.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.adapters.MealFeedbackAdapter
import com.example.judinedesk.models.MealFeedback
import com.example.judinedesk.models.Student
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MealViewActivity : AppCompatActivity() {

    private lateinit var tvHallName: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var tvLunchItems: TextView
    private lateinit var tvDinnerItems: TextView
    private lateinit var btnAddLunchReview: Button
    private lateinit var btnAddDinnerReview: Button
    private lateinit var rvLunchFeedback: RecyclerView
    private lateinit var rvDinnerFeedback: RecyclerView
    private lateinit var tvNoLunchFeedback: TextView
    private lateinit var tvNoDinnerFeedback: TextView

    private val db = FirebaseFirestore.getInstance()
    private var selectedDate: String = ""
    private lateinit var currentStudent: Student
    private lateinit var lunchFeedbackAdapter: MealFeedbackAdapter
    private lateinit var dinnerFeedbackAdapter: MealFeedbackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_view)

        // Get student data
        currentStudent = getStudentData()

        setupViews()
        setupClickListeners()
        setupRecyclerViews()

        // Set default date to today and load meals
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        tvSelectedDate.text = "Today"
        loadMealsByDate()
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
        tvHallName = findViewById(R.id.tvHallName)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvLunchItems = findViewById(R.id.tvLunchItems)
        tvDinnerItems = findViewById(R.id.tvDinnerItems)
        btnAddLunchReview = findViewById(R.id.btnAddLunchReview)
        btnAddDinnerReview = findViewById(R.id.btnAddDinnerReview)
        rvLunchFeedback = findViewById(R.id.rvLunchFeedback)
        rvDinnerFeedback = findViewById(R.id.rvDinnerFeedback)
        tvNoLunchFeedback = findViewById(R.id.tvNoLunchFeedback)
        tvNoDinnerFeedback = findViewById(R.id.tvNoDinnerFeedback)

        tvHallName.text = currentStudent.hall
    }

    private fun setupClickListeners() {
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnAddLunchReview.setOnClickListener {
            showFeedbackDialog("Lunch")
        }

        btnAddDinnerReview.setOnClickListener {
            showFeedbackDialog("Dinner")
        }
    }

    private fun setupRecyclerViews() {
        lunchFeedbackAdapter = MealFeedbackAdapter(emptyList())
        dinnerFeedbackAdapter = MealFeedbackAdapter(emptyList())

        rvLunchFeedback.apply {
            layoutManager = LinearLayoutManager(this@MealViewActivity)
            adapter = lunchFeedbackAdapter
        }

        rvDinnerFeedback.apply {
            layoutManager = LinearLayoutManager(this@MealViewActivity)
            adapter = dinnerFeedbackAdapter
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

            val displaySdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val displayDate = displaySdf.format(c.time)
            tvSelectedDate.text = displayDate

            loadMealsByDate()
        }, year, month, day)
        datePicker.show()
    }

    private fun loadMealsByDate() {
        Log.d("MealViewActivity", "Loading meals for: $selectedDate, Hall: ${currentStudent.hall}")

        db.collection("meals")
            .whereEqualTo("date", selectedDate)
            .whereEqualTo("hall", currentStudent.hall)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MealViewActivity", "Found ${documents.size()} total meals")

                var lunchItems = "Not posted yet"
                var dinnerItems = "Not posted yet"
                var lunchFeedback = emptyList<Map<String, Any>>()
                var dinnerFeedback = emptyList<Map<String, Any>>()

                for (document in documents) {
                    val type = document.getString("type") ?: ""
                    val items = document.get("items") as? List<String> ?: emptyList()
                    val itemsText = if (items.isNotEmpty())
                        items.joinToString("\n• ", "• ")
                    else "No items specified"

                    // Handle feedback - read as List of Maps
                    val feedbackData = document.get("feedback")
                    val feedbackList = if (feedbackData is List<*>) {
                        feedbackData.filterIsInstance<Map<String, Any>>()
                    } else {
                        emptyList()
                    }

                    Log.d("MealViewActivity", "Meal: type=$type, items=$items, feedbackCount=${feedbackList.size}")

                    when (type.lowercase()) {
                        "lunch" -> {
                            lunchItems = itemsText
                            lunchFeedback = feedbackList
                        }
                        "dinner" -> {
                            dinnerItems = itemsText
                            dinnerFeedback = feedbackList
                        }
                    }
                }

                // Update UI
                tvLunchItems.text = lunchItems
                tvDinnerItems.text = dinnerItems

                // Update feedback lists
                updateFeedbackViews(lunchFeedback, dinnerFeedback)

                // Enable/disable review buttons based on meal availability
                btnAddLunchReview.isEnabled = lunchItems != "Not posted yet"
                btnAddDinnerReview.isEnabled = dinnerItems != "Not posted yet"

            }
            .addOnFailureListener { e ->
                Log.e("MealViewActivity", "Error loading meals: ${e.message}")
                Toast.makeText(this, "Error loading meals", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFeedbackViews(lunchFeedback: List<Map<String, Any>>, dinnerFeedback: List<Map<String, Any>>) {
        // Convert Map to MealFeedback objects for adapter
        val lunchFeedbackObjects = lunchFeedback.map { data ->
            MealFeedback(
                studentId = data["studentId"] as? String ?: "",
                studentName = data["studentName"] as? String ?: "",
                feedback = data["feedback"] as? String ?: "",
                rating = (data["rating"] as? Double ?: 0.0).toFloat(),
                timestamp = data["timestamp"] as? Long ?: 0L
            )
        }

        val dinnerFeedbackObjects = dinnerFeedback.map { data ->
            MealFeedback(
                studentId = data["studentId"] as? String ?: "",
                studentName = data["studentName"] as? String ?: "",
                feedback = data["feedback"] as? String ?: "",
                rating = (data["rating"] as? Double ?: 0.0).toFloat(),
                timestamp = data["timestamp"] as? Long ?: 0L
            )
        }

        // Update lunch feedback
        if (lunchFeedbackObjects.isNotEmpty()) {
            lunchFeedbackAdapter.updateData(lunchFeedbackObjects)
            rvLunchFeedback.visibility = android.view.View.VISIBLE
            tvNoLunchFeedback.visibility = android.view.View.GONE
        } else {
            rvLunchFeedback.visibility = android.view.View.GONE
            tvNoLunchFeedback.visibility = android.view.View.VISIBLE
        }

        // Update dinner feedback
        if (dinnerFeedbackObjects.isNotEmpty()) {
            dinnerFeedbackAdapter.updateData(dinnerFeedbackObjects)
            rvDinnerFeedback.visibility = android.view.View.VISIBLE
            tvNoDinnerFeedback.visibility = android.view.View.GONE
        } else {
            rvDinnerFeedback.visibility = android.view.View.GONE
            tvNoDinnerFeedback.visibility = android.view.View.VISIBLE
        }
    }

    private fun showFeedbackDialog(mealType: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)
        val etFeedback = dialogView.findViewById<EditText>(R.id.etFeedback)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        AlertDialog.Builder(this)
            .setTitle("Add Review for $mealType")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val feedbackText = etFeedback.text.toString().trim()
                val rating = ratingBar.rating

                if (feedbackText.isEmpty()) {
                    Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                submitFeedback(mealType, feedbackText, rating)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun submitFeedback(mealType: String, feedbackText: String, rating: Float) {
        Log.d("MealViewActivity", "Submitting feedback for: $mealType, Date: $selectedDate, Hall: ${currentStudent.hall}")

        // Find the meal document for the selected date and type
        db.collection("meals")
            .whereEqualTo("date", selectedDate)
            .whereEqualTo("type", mealType)
            .whereEqualTo("hall", currentStudent.hall)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MealViewActivity", "Found ${documents.size()} meals matching criteria")

                if (documents.isEmpty) {
                    Toast.makeText(this, "❌ No meal found for $mealType on $selectedDate", Toast.LENGTH_LONG).show()
                    Log.e("MealViewActivity", "No meal found for: date=$selectedDate, type=$mealType, hall=${currentStudent.hall}")
                    return@addOnSuccessListener
                }

                val document = documents.documents[0]
                val mealId = document.id

                Log.d("MealViewActivity", "Found meal: ID=$mealId, Data=${document.data}")

                // Create new feedback
                val newFeedback = mapOf(
                    "studentId" to currentStudent.uid,
                    "studentName" to currentStudent.name,
                    "feedback" to feedbackText,
                    "rating" to rating,
                    "timestamp" to System.currentTimeMillis()
                )

                // Get existing feedback or create empty list
                val existingFeedback = document.get("feedback") as? List<Map<String, Any>> ?: emptyList()
                val updatedFeedback = existingFeedback.toMutableList().apply {
                    add(newFeedback)
                }

                Log.d("MealViewActivity", "Updating feedback: $updatedFeedback")

                // Update in Firestore
                db.collection("meals").document(mealId)
                    .update("feedback", updatedFeedback)
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        Log.d("MealViewActivity", "Feedback updated successfully")
                        loadMealsByDate() // Refresh to show new feedback
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "❌ Failed to submit review", Toast.LENGTH_SHORT).show()
                        Log.e("MealViewActivity", "Error updating feedback: ${e.message}", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error finding meal", Toast.LENGTH_SHORT).show()
                Log.e("MealViewActivity", "Error querying meals: ${e.message}", e)
            }
    }
}