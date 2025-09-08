package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Student
import com.example.judinedesk.utils.AuthHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etMobile: EditText
    private lateinit var etDepartment: EditText
    private lateinit var etBatch: EditText
    private lateinit var etClassRoll: EditText
    private lateinit var spHall: Spinner
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setupViews()
        setupSpinner()
        setupClickListeners()
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
    }

    private fun setupSpinner() {
        val halls = listOf("Hall A", "Hall B", "Hall C", "Hall D") // Add your hall names
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, halls)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spHall.adapter = adapter
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (!validateInputs(email, password, confirmPassword)) return@setOnClickListener

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
                hall = spHall.selectedItem.toString()
            )

            registerStudent(student, password)
        }

        tvLogin.setOnClickListener {
            goToLogin()
        }
    }

    private fun validateInputs(email: String, password: String, confirmPassword: String): Boolean {
        if (etName.text.isNullOrBlank() || email.isEmpty() || password.isEmpty() ||
            confirmPassword.isEmpty() || etMobile.text.isNullOrBlank() ||
            etDepartment.text.isNullOrBlank() || etBatch.text.isNullOrBlank() ||
            etClassRoll.text.isNullOrBlank()
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
        // Generate a simple unique userId: name + 4 random digits
        val randomDigits = (1000..9999).random()
        return "${name.replace(" ", "").lowercase()}$randomDigits"
    }

    private fun registerStudent(student: Student, password: String) {
        btnRegister.isEnabled = false

        AuthHelper.registerStudent(student, password) { success, message ->
            btnRegister.isEnabled = true
            if (success) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                goToLogin()
            } else {
                Toast.makeText(this, "Registration failed: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
