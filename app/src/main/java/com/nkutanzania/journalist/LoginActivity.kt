package com.nkutanzania.journalist

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.nkutanzania.journalist.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

private const val TAG = "LoginActivity"

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var togglePasswordVisibility: ImageButton
    private lateinit var progressBar: ProgressBar


    // Create OkHttp client with logging interceptor
    private val client = ApiUtils.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Load the XML layout

        // Initialize UI elements
        usernameEditText = findViewById(R.id.et_username)
        passwordEditText = findViewById(R.id.et_password)
        loginButton = findViewById(R.id.btn_login)
        registerButton = findViewById(R.id.btn_go_register)
        togglePasswordVisibility = findViewById(R.id.toggle_password_visibility)
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        // Add password toggle functionality
        togglePasswordVisibility.setOnClickListener {
            if (passwordEditText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Show password
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility)
            } else {
                // Hide password
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off)
            }
            // Keep cursor at the end of text
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        // Handle registration button click
        loginButton.setOnClickListener {
            sendLoginData()
        }
        // Handle switch to login
        registerButton.setOnClickListener {
            switchToRegisterPage()
        }
    }

    fun isValidPassword(password: String): Boolean {
        val passwordRegex = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#\$%^&+=!])(?=\\S+\$).{8,}$")
        return password.matches(passwordRegex)
    }

    // Function to send registration data to the backend server
    private fun sendLoginData() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Enter credentials", Toast.LENGTH_SHORT).show()
            return
        }else{
            progressBar.visibility = View.VISIBLE
            loginButton.isEnabled = false
            loginButton.text = "Logging in..."
            loginUser(username, password)
        }
    }


    private fun switchToRegisterPage() {
        Log.d(TAG, "Switching to RegisterActivity...") // Debug log
        try {
            val registerIntent = Intent(this, RegisterActivity::class.java)
            startActivity(registerIntent)
            Log.d(TAG, "RegisterActivity started successfully") // Debug log
        } catch (e: Exception) {
            // Log the error
            Log.d("CameraActivity", "Error switching to register page: ${e.message}")
            // Show a message to the user
            Toast.makeText(this, "Cannot open register page: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loginUser(username: String, password: String) {
//        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()
        val request = Request.Builder()
            .url(ApiUtils.LOGIN_ENDPOINT)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    loginButton.text = "Login"
                    Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val accessToken = jsonResponse.getString("access")
                    val refreshToken = jsonResponse.getString("refresh")
                    val userId = jsonResponse.getInt("user_id")

                    // Retrieve public key if available
                    val serverPublicKey = jsonResponse.optString("server_public_key", "")

                    // Save tokens, userId and public key
                    saveAuthData(accessToken, refreshToken, serverPublicKey, userId)

                    Log.i("PUBLIC_SERVER_KEY", serverPublicKey.toString())
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        loginButton.isEnabled = true
                        loginButton.text = "Login"
                        Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, CameraActivity::class.java))
                        finish()
                    }
                } else {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        loginButton.isEnabled = true
                        loginButton.text = "Login"
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveAuthData(accessToken: String, refreshToken: String, serverPublicKey: String, userId: Int ) {
        val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("accessToken", accessToken)
        editor.putString("refreshToken", refreshToken)
        editor.putInt("userId", userId)
        if (serverPublicKey.isNotEmpty()) {
            editor.putString("serverPublicKey", serverPublicKey) // Save public key if available
        }
        editor.apply()
    }


}
