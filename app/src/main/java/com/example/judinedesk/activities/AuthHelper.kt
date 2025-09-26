package com.example.judinedesk.utils

import com.example.judinedesk.models.Manager
import com.example.judinedesk.models.Staff
import com.example.judinedesk.models.Student
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

object AuthHelper {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var currentUserData: Any? = null   // cache user object (Student/Manager/Staff)

    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUid(): String? = auth.currentUser?.uid


    fun registerStudent(student: Student, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(student.email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // user is signed in now
                    val uid = auth.currentUser?.uid ?: ""
                    val studentWithUid = student.copy(uid = uid)

                    db.collection("students").document(uid)
                        .set(studentWithUid)
                        .addOnSuccessListener {
                            // store in-memory cache
                            currentUserData = studentWithUid
                            callback(true, "Registration successful")
                        }
                        .addOnFailureListener { e ->
                            // Rollback: delete created auth user to avoid orphan auth entry
                            auth.currentUser?.delete()?.addOnCompleteListener {
                                callback(false, "Failed to save student data: ${e.message}")
                            } ?: callback(false, "Failed to save student data: ${e.message}")
                        }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }
    fun loginUser(email: String, password: String, callback: (Boolean, String?, Any?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""

                    Log.d("Inside loginUser", "UID: $uid  ")
                    // Check Student
                    db.collection("students").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val student = doc.toObject(Student::class.java)
                                currentUserData = student   // ✅ cache it
                                callback(true, "Login successful", student)
                            } else {
                                // Check Manager
                                db.collection("managers").document(uid).get()
                                    .addOnSuccessListener { mDoc ->
                                        if (mDoc.exists()) {
                                            val manager = mDoc.toObject(Manager::class.java)
                                            currentUserData = manager
                                            Log.d("Inside manager", "UID: $manager  ")
                                            callback(true, "Login successful bruh", manager)
                                        } else {
                                            // Check Staff
                                            db.collection("staffs").document(uid).get()
                                                .addOnSuccessListener { sDoc ->
                                                    if (sDoc.exists()) {
                                                        val staff = sDoc.toObject(Staff::class.java)
                                                        currentUserData = staff
                                                        callback(true, "Login successful", staff)
                                                    } else {
                                                        callback(false, "User data not found", null)
                                                    }
                                                }
                                        }
                                    }
                            }
                        }
                } else {
                    callback(false, task.exception?.message, null)
                }
            }
    }

    fun logout() {
        currentUserData = null  // clear cache
        auth.signOut()
    }
}
