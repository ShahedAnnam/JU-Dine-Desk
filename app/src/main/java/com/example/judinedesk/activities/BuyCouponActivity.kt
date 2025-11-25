package com.example.judinedesk.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.adapters.BuyMealAdapter
import com.example.judinedesk.models.Student
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BuyCouponActivity : AppCompatActivity() {

    private lateinit var rvMealList: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentStudent: Student
    private var today: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_coupon)

        rvMealList = findViewById(R.id.rvMealList)

        currentStudent = AuthHelper.currentUserData as Student

        today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        loadMealsForToday()
    }

    private fun loadMealsForToday() {

        db.collection("meals")
            .whereEqualTo("date", today)
            .whereEqualTo("hall", currentStudent.hall)
            .get()
            .addOnSuccessListener { documents ->

                val mealList = documents.map { doc ->
                    mapOf(
                        "mealId" to doc.id,
                        "title" to (doc.getString("title") ?: ""),
                        "price" to (doc.get("price") ?: ""),
                        "type" to (doc.getString("type") ?: ""),
                        "items" to (doc.get("items") as? List<String> ?: emptyList())
                    )
                }

                rvMealList.layoutManager = LinearLayoutManager(this)
                rvMealList.adapter = BuyMealAdapter(mealList) { meal ->
                    buyCoupon(meal["type"] as String)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load meals", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buyCoupon(mealType: String) {

        val couponRef = db.collection("coupons")

        couponRef.whereEqualTo("studentId", currentStudent.uid)
            .whereEqualTo("date", today)
            .whereEqualTo("mealType", mealType)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    Toast.makeText(this, "Already purchased $mealType coupon", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val couponData = mapOf(
                    "studentId" to currentStudent.uid,
                    "studentName" to currentStudent.name,
                    "hall" to currentStudent.hall,
                    "date" to today,
                    "mealType" to mealType,
                    "timestamp" to System.currentTimeMillis()
                )

                couponRef.add(couponData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Coupon purchased", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Purchase failed", Toast.LENGTH_SHORT).show()
                    }
            }
    }
}
