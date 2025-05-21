package com.nkutanzania.journalist

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Base activity class that provides common functionality across activities
 * such as logout and navigation
 */
abstract class BaseActivity : AppCompatActivity() {


    protected fun logoutUser() {
        val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        val refreshToken = sharedPreferences.getString("refreshToken", "")
        val accessToken = sharedPreferences.getString("accessToken", null)

        if (refreshToken.isNullOrEmpty()) {
            Toast.makeText(this, "No refresh token found", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        // Create a FormBody instead of JSONObject to ensure proper formatting
        val requestBody = FormBody.Builder()
            .add("refresh", refreshToken)
            .build()

        val request = Request.Builder()
            .url(ApiUtils.LOGOUT_ENDPOINT)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        // Use OkHttp client to make the request
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@BaseActivity, "Logout failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful) {
                        // Clear local storage
                        val editor = sharedPreferences.edit()
                        editor.clear()
                        editor.apply()

                        Toast.makeText(this@BaseActivity, "Logout successful!", Toast.LENGTH_LONG).show()
                        navigateToLogin()
                    } else {
                        Toast.makeText(this@BaseActivity, "Logout failed: ${response.code} - ${responseBody}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    protected fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        // Clear the entire activity stack and start fresh
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun goBack() {
        onBackPressed()
    }
}