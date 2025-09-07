package com.example.judinedesk.utils

import com.google.firebase.auth.FirebaseAuth

object AuthHelper {
    private val auth = FirebaseAuth.getInstance()

    // Check if user is logged in
    fun isLoggedIn(): Boolean = auth.currentUser != null

    // Get current user email
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    // Register new user
    fun registerUser(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, "Registration successful")
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    // Login user
    fun loginUser(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, "Login successful")
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    // Logout user
    fun logout() {
        auth.signOut()
    }
}