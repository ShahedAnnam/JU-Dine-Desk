package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Student
import com.example.judinedesk.utils.AuthHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var etMobile: TextInputEditText
    private lateinit var etDepartment: TextInputEditText
    private lateinit var etBatch: TextInputEditText
    private lateinit var etClassRoll: TextInputEditText
    private lateinit var spHall: MaterialAutoCompleteTextView
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setupBackPressedHandler()
        setupViews()
        setupSpinner()
        setupClickListeners()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToLogin()
            }
        })
    }

    private fun setupViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etMobile = findViewById(R.id.etMobile)
        etDepartment = findViewById(R.id.etDepartment)
        etBatch = findViewById(R.id.etBatch)
        etClassRoll = findViewById(R.id.etClassRoll)
        spHall = findViewById(R.id.spHall)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        // Setup back button
        findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSpinner() {
        val halls = arrayOf(
            "Select Hall",
            "21 No Hall",
            "Shaheed Tajuddin Hall",
            "Shaheed Rafiq-Jabbar Hall",
            "Shaheed Salam-Barkat Hall",
            "Al Biruni Hall"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, halls)
        spHall.setAdapter(adapter)

        // Set default selection
        spHall.setText(halls[0], false)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val selectedHall = spHall.text.toString().trim()

            if (!validateInputs(email, password, confirmPassword, selectedHall)) return@setOnClickListener

            val name = etName.text.toString().trim()
            val userId = generateUserId(name)
            val student = Student(
                name = name,
                userId = userId,
                email = email,
                mobile = etMobile.text.toString().trim(),
                department = etDepartment.text.toString().trim(),
                batch = etBatch.text.toString().trim(),
                classRoll = etClassRoll.text.toString().trim(),
                hall = selectedHall
            )

            registerStudent(student, password)
        }

        tvLogin.setOnClickListener {
            goToLogin()
        }
    }

    private fun validateInputs(email: String, password: String, confirmPassword: String, hall: String): Boolean {
        if (etName.text.isNullOrBlank() || email.isEmpty() || password.isEmpty() ||
            confirmPassword.isEmpty() || etMobile.text.isNullOrBlank() ||
            etDepartment.text.isNullOrBlank() || etBatch.text.isNullOrBlank() ||
            etClassRoll.text.isNullOrBlank() || hall == "Select Hall" || hall.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun generateUserId(name: String): String {
        val randomDigits = (1000..9999).random()
        return "${name.replace(" ", "").lowercase()}$randomDigits"
    }

    private fun registerStudent(student: Student, password: String) {
        btnRegister.isEnabled = false
        btnRegister.text = "Creating Account..."

        AuthHelper.registerStudent(student, password) { success, message ->
            btnRegister.isEnabled = true
            btnRegister.text = "Create Account"

            if (success) {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                goToLogin()
            } else {
                Toast.makeText(this, "Registration failed: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
}