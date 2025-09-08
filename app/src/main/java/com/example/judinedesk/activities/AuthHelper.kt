package com.example.judinedesk.utils

import com.example.judinedesk.models.Student
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AuthHelper {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Check if user is logged in
    fun isLoggedIn(): Boolean = auth.currentUser != null

    // Get current UID
    fun getCurrentUid(): String? = auth.currentUser?.uid

    // Register Student by passing Student object
    fun registerStudent(student: Student, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(student.email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""
                    val studentWithUid = student.copy(uid = uid)

                    db.collection("students")
                        .document(uid)
                        .set(studentWithUid)
                        .addOnSuccessListener { callback(true, "Registration successful") }
                        .addOnFailureListener { e -> callback(false, e.message) }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    // Login
    fun loginStudent(email: String, password: String, callback: (Boolean, String?, Student?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    // Fetch student data from Firestore
                    db.collection("students")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val student = document.toObject(Student::class.java)
                                callback(true, "Login successful", student)
                            } else {
                                callback(false, "Student data not found", null)
                            }
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message, null)
                        }
                } else {
                    callback(false, task.exception?.message, null)
                }
            }
    }


    // Logout
    fun logout() {
        auth.signOut()
    }
}
