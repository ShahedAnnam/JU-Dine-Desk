package com.example.judinedesk.models

data class User(
    // Basic Information (Common for all users)
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val role: String = "", // "student", "manager", "staff", "admin"
    val profileImage: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // Student Specific Fields
    val studentId: String = "",
    val department: String = "",
    val batch: String = "",
    val rollNumber: String = "",
    val hall: String = "",

    // Manager & Staff Specific Fields
    val employeeId: String = "",
    val assignedHall: String = "",
    val position: String = "", // For staff: "cook", "cashier", "cleaner", etc.

    // Admin Specific Fields
    val adminLevel: String = "" // "super_admin", "hall_admin", etc.
) {
    // Helper function to check user role
    fun isStudent(): Boolean = role == "student"
    fun isManager(): Boolean = role == "manager"
    fun isStaff(): Boolean = role == "staff"
    fun isAdmin(): Boolean = role == "admin"
}