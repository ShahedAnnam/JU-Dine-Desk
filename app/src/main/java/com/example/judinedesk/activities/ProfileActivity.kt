package com.example.judinedesk.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Student
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvMobile: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvBatch: TextView
    private lateinit var tvClassRoll: TextView
    private lateinit var tvHall: TextView
    private lateinit var btnUpdateProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthHelper.isLoggedIn()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_profile)
        setupViews()
        loadStudentData()
    }

    private fun setupViews() {
        tvName = findViewById(R.id.tv_name)
        tvEmail = findViewById(R.id.tv_email)
        tvMobile = findViewById(R.id.tv_mobile)
        tvDepartment = findViewById(R.id.tv_department)
        tvBatch = findViewById(R.id.tv_batch)
        tvClassRoll = findViewById(R.id.tv_class_roll)
        tvHall = findViewById(R.id.tv_hall)
        btnUpdateProfile = findViewById(R.id.btn_update_profile)
    }

    private fun loadStudentData() {
        val uid = AuthHelper.getCurrentUid() ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("students").document(uid).get()
            .addOnSuccessListener { doc ->
                val student = doc.toObject(Student::class.java)
                if (student != null) displayStudentData(student)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayStudentData(student: Student) {
        tvName.text = student.name
        tvEmail.text = student.email
        tvMobile.text = student.mobile
        tvDepartment.text = student.department
        tvBatch.text = student.batch
        tvClassRoll.text = student.classRoll
        tvHall.text = student.hall

        btnUpdateProfile.setOnClickListener {
            Toast.makeText(this, "Update functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
