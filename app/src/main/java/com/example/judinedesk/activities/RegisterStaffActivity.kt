package com.example.judinedesk.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Manager
import com.example.judinedesk.models.Staff
import com.example.judinedesk.utils.AuthHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RegisterStaffActivity : AppCompatActivity() {

    private lateinit var etStaffName: TextInputEditText
    private lateinit var etStaffEmail: TextInputEditText
    private lateinit var etStaffPassword: TextInputEditText
    private lateinit var etStaffAge: TextInputEditText
    private lateinit var etStaffContact: TextInputEditText
    private lateinit var etStaffDistrict: TextInputEditText
    private lateinit var etEmployeeId: TextInputEditText
    private lateinit var etStaffPosition: TextInputEditText
    private lateinit var etStaffSalary: TextInputEditText
    private lateinit var etJoinDate: TextInputEditText
    private lateinit var btnRegisterStaff: Button
    private lateinit var btnCancel: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentManager: Manager

    // Staff registration rules
    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val DEFAULT_PASSWORD = "staff123"
        private const val MIN_AGE = 18
        private const val MAX_AGE = 65
        private const val MIN_SALARY = 5000.0 // Minimum salary 5000 BDT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_staff)

        currentManager = getManagerData()
        setupViews()
        setupClickListeners()
    }

    private fun getManagerData(): Manager {
        return try {
            val manager = AuthHelper.currentUserData as? Manager
            if (manager != null) {
                manager
            } else {
                Log.w("RegisterStaff", "Manager data not found in AuthHelper, creating basic manager")
                Manager(
                    uid = AuthHelper.getCurrentUid() ?: "unknown",
                    email = auth.currentUser?.email ?: "unknown@example.com",
                    hall = "Unknown Hall",
                    role = "manager",
                    employeeId = "MGR-${System.currentTimeMillis()}"
                )
            }
        } catch (e: Exception) {
            Log.e("RegisterStaff", "Error getting manager data: ${e.message}")
            Manager(
                uid = AuthHelper.getCurrentUid() ?: "unknown",
                email = auth.currentUser?.email ?: "unknown@example.com",
                hall = "Unknown Hall",
                role = "manager",
                employeeId = "MGR-${System.currentTimeMillis()}"
            )
        }
    }

    private fun setupViews() {
        etStaffName = findViewById(R.id.etStaffName)
        etStaffEmail = findViewById(R.id.etStaffEmail)
        etStaffPassword = findViewById(R.id.etStaffPassword)
        etStaffAge = findViewById(R.id.etStaffAge)
        etStaffContact = findViewById(R.id.etStaffContact)
        etStaffDistrict = findViewById(R.id.etStaffDistrict)
        etEmployeeId = findViewById(R.id.etEmployeeId)
        etStaffPosition = findViewById(R.id.etStaffPosition)
        etStaffSalary = findViewById(R.id.etStaffSalary)
        etJoinDate = findViewById(R.id.etJoinDate)
        btnRegisterStaff = findViewById(R.id.btnRegisterStaff)
        btnCancel = findViewById(R.id.btnCancel)

        // Set default join date to today
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etJoinDate.setText(today)

        // Auto-generate Employee ID and set default password
        generateEmployeeId()
        etStaffPassword.setText(DEFAULT_PASSWORD)

        // Make Employee ID field non-editable since it's auto-generated
        etEmployeeId.isEnabled = false
        etEmployeeId.isFocusable = false
    }

    private fun generateEmployeeId() {
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (1000..9999).random()
        val employeeId = "STAFF-${timestamp.toString().takeLast(6)}-$randomSuffix"
        etEmployeeId.setText(employeeId)
    }

    private fun setupClickListeners() {
        etJoinDate.setOnClickListener {
            showDatePicker()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnRegisterStaff.setOnClickListener {
            registerStaff()
        }

        // Regenerate Employee ID on long press (for testing/if needed)
        etEmployeeId.setOnLongClickListener {
            generateEmployeeId()
            Toast.makeText(this, "Employee ID regenerated", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, y, m, d ->
            val selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
            etJoinDate.setText(selectedDate)
        }, year, month, day)
        datePicker.show()
    }

    private fun registerStaff() {
        val name = etStaffName.text.toString().trim()
        val email = etStaffEmail.text.toString().trim()
        val password = etStaffPassword.text.toString().trim()
        val age = etStaffAge.text.toString().trim().toIntOrNull() ?: 0
        val contact = etStaffContact.text.toString().trim()
        val district = etStaffDistrict.text.toString().trim()
        val employeeId = etEmployeeId.text.toString().trim()
        val position = etStaffPosition.text.toString().trim()
        val salary = etStaffSalary.text.toString().trim().toDoubleOrNull() ?: 0.0
        val joinDate = etJoinDate.text.toString().trim()

        // Validation Rules
        if (!validateInput(name, email, password, age, contact, district, position, salary, joinDate)) {
            return
        }

        // Show loading
        btnRegisterStaff.isEnabled = false
        btnRegisterStaff.text = "Registering..."

        // Create staff in Firebase Auth and Firestore
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val staffUid = authResult.user?.uid ?: ""

                // Use safe access for manager fields with fallbacks
                val managerHall = if (currentManager.hall.isNotEmpty()) currentManager.hall else "Unknown Hall"

                val staff = Staff(
                    uid = staffUid,
                    employeeId = employeeId,
                    hall = managerHall,
                    role = "staff",
                    email = email,
                    name = name,
                    age = age,
                    homeDistrict = district,
                    contactNumber = contact,
                    position = position,
                    salary = salary,
                    joinDate = joinDate,
                    isActive = true
                )

                // Save staff to Firestore
                db.collection("staffs").document(staffUid)
                    .set(staff)
                    .addOnSuccessListener {
                        btnRegisterStaff.isEnabled = true
                        btnRegisterStaff.text = "Register Staff"

                        // Show success message with credentials
                        showSuccessDialog(email, password, employeeId)
                    }
                    .addOnFailureListener { e ->
                        btnRegisterStaff.isEnabled = true
                        btnRegisterStaff.text = "Register Staff"
                        Toast.makeText(this, "❌ Failed to save staff data: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("RegisterStaff", "Firestore error: ${e.message}")
                        // Delete the created auth user if Firestore fails
                        auth.currentUser?.delete()?.addOnCompleteListener {
                            Log.d("RegisterStaff", "Rollback: Auth user deleted due to Firestore failure")
                        }
                    }
            }
            .addOnFailureListener { e ->
                btnRegisterStaff.isEnabled = true
                btnRegisterStaff.text = "Register Staff"
                val errorMessage = when {
                    e.message?.contains("email address is already") == true -> "Email already exists"
                    e.message?.contains("badly formatted") == true -> "Invalid email format"
                    e.message?.contains("password is invalid") == true -> "Password is too weak"
                    else -> "Failed to create staff account: ${e.message}"
                }
                Toast.makeText(this, "❌ $errorMessage", Toast.LENGTH_SHORT).show()
                Log.e("RegisterStaff", "Auth error: ${e.message}")
            }
    }

    private fun validateInput(
        name: String,
        email: String,
        password: String,
        age: Int,
        contact: String,
        district: String,
        position: String,
        salary: Double,
        joinDate: String
    ): Boolean {
        // Check empty fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || contact.isEmpty() ||
            district.isEmpty() || position.isEmpty() || joinDate.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return false
        }

        // Password validation
        if (password.length < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, "Password must be at least $MIN_PASSWORD_LENGTH characters", Toast.LENGTH_SHORT).show()
            return false
        }

        // Age validation
        if (age < MIN_AGE || age > MAX_AGE) {
            Toast.makeText(this, "Please enter a valid age ($MIN_AGE-$MAX_AGE)", Toast.LENGTH_SHORT).show()
            return false
        }

        // Contact validation
        if (contact.length < 10 || !contact.matches(Regex("^[0-9+]+\$"))) {
            Toast.makeText(this, "Please enter a valid contact number", Toast.LENGTH_SHORT).show()
            return false
        }

        // Salary validation
        if (salary < MIN_SALARY) {
            Toast.makeText(this, "Salary must be at least ৳$MIN_SALARY", Toast.LENGTH_SHORT).show()
            return false
        }

        // Email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showSuccessDialog(email: String, password: String, employeeId: String) {
        val message = """
        ✅ Staff Registered Successfully!
        
        📧 Email: $email
        🔑 Password: $password
        🆔 Employee ID: $employeeId
        
        Please share these credentials with the staff member.
        They should change their password after first login.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Registration Successful")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}