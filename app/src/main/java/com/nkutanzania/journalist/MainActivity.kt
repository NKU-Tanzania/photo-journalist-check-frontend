package com.nkutanzania.journalist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nkutanzania.journalist.R

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if user is already logged in
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        // Get shared preferences to check if user is logged in
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        // Using a handler to delay the navigation slightly (like a splash screen)
        Handler(Looper.getMainLooper()).postDelayed({
            if (isLoggedIn) {
                // User is logged in, go to home activity
                navigateToCamera()
            } else {
                // User is not logged in, go to login activity
                navigateToLogin()
            }
        }, 1500) // 1.5 second delay
    }

    private fun navigateToCamera() {
        try {
            val cameraIntent = Intent(this, CameraActivity::class.java)
            startActivity(cameraIntent)
            finish() // Close this activity
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to home: ${e.message}")
            // Fallback to login if home activity doesn't exist yet
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        try {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish() // Close this activity
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to login: ${e.message}")

            // Fallback to register if login activity doesn't exist yet
            try {
                val registerIntent = Intent(this, RegisterActivity::class.java)
                startActivity(registerIntent)
                finish() // Close this activity
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to register: ${e.message}")
            }
        }
    }
}