package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Manager
import com.example.judinedesk.models.Staff
import com.example.judinedesk.models.Student
import com.example.judinedesk.utils.AuthHelper
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If user is logged in → send to specific dashboard
        if (AuthHelper.isLoggedIn()) {
            redirectToDashboard()
            return
        }

        setContentView(R.layout.activity_main)

        // Add smooth entrance animations
        animateEntrance()
        setupNavigation()
    }

    private fun animateEntrance() {
        val imageAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_fade_in)
        val titleAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        val buttonsAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_up_delay)

        // Animate dining concept image
        findViewById<ImageView>(R.id.ivDiningConcept).startAnimation(imageAnimation)

        // Animate buttons with delay
        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<MaterialButton>(R.id.btnLogin).startAnimation(buttonsAnimation)
            findViewById<MaterialButton>(R.id.btnRegister).startAnimation(buttonsAnimation)
        }, 300)
    }

    private fun setupNavigation() {
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            animateButtonClick(btnLogin) {
                startActivity(Intent(this, LoginActivity::class.java))
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
            }
        }

        btnRegister.setOnClickListener {
            animateButtonClick(btnRegister) {
                startActivity(Intent(this, RegisterActivity::class.java))
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
            }
        }
    }

    private fun animateButtonClick(button: MaterialButton, action: () -> Unit) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction {
                        action()
                    }
            }
    }

    private fun redirectToDashboard() {
        val user = AuthHelper.currentUserData

        val intent = when (user) {
            is Student -> Intent(this, StudentDashboardActivity::class.java)
            is Manager -> Intent(this, ManagerDashboardActivity::class.java)
            is Staff -> Intent(this, StaffDashboardActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
        finish()
    }

    override fun onStart() {
        super.onStart()
        if (AuthHelper.isLoggedIn() && !isTaskRoot) {
            Handler(Looper.getMainLooper()).postDelayed({
                redirectToDashboard()
            }, 1000)
        }
    }
}