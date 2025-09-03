package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnGetStarted = findViewById<Button>(R.id.btn_get_started)

        btnGetStarted.setOnClickListener {
            val intent = Intent(this, StudentDashboardActivity::class.java)
            startActivity(intent)
        }
    }
}