package com.nkutanzania.journalist

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nkutanzania.journalist.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

private const val TAG = "RegisterActivity"

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var retypePasswordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var idEditText: EditText
    private lateinit var idErrorText: TextView
    private lateinit var usernameErrorText: TextView
    private lateinit var emailErrorText: TextView
    private lateinit var phoneErrorText: TextView
    private lateinit var passwordErrorText: TextView
    private lateinit var retypePasswordErrorText: TextView
    private lateinit var registerButton: Button
    private lateinit var goLogInButton: Button
    private var photoUri: Uri? = null
    private lateinit var progressBar: ProgressBar
    private val PICK_IMAGE_REQUEST = 1
    private val keyAlias = "MySecureKey"
    private var userId: String = ""
    private lateinit var publicKeyString: String
    private lateinit var togglePasswordVisibility: ImageButton
    private lateinit var toggleRetypePasswordVisibility: ImageButton

    private val client = ApiUtils.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize UI components
        usernameEditText = findViewById(R.id.et_username)
        emailEditText = findViewById(R.id.et_email)
        passwordEditText = findViewById(R.id.et_password)
        retypePasswordEditText = findViewById(R.id.et_retype_password)
        phoneEditText = findViewById(R.id.et_phone)
        idEditText = findViewById(R.id.et_id)
        registerButton = findViewById(R.id.btn_register)
        goLogInButton = findViewById(R.id.btn_go_login)
        togglePasswordVisibility = findViewById(R.id.toggle_password_visibility)
        toggleRetypePasswordVisibility = findViewById(R.id.toggle_retype_password_visibility)

        // Initialize error text views
        idErrorText = findViewById(R.id.tv_id_error)
        usernameErrorText = findViewById(R.id.tv_username_error)
        emailErrorText = findViewById(R.id.tv_email_error)
        phoneErrorText = findViewById(R.id.tv_phone_error)
        passwordErrorText = findViewById(R.id.tv_password_error)
        retypePasswordErrorText = findViewById(R.id.tv_retype_password_error)

        // Hide all error messages initially
        hideAllErrorMessages()

        // Add progress bar
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        // Initially disable the register button
        registerButton.isEnabled = false
        registerButton.alpha = 0.5f

        // Set up text watchers to validate input in real-time
        setupTextWatchers()

        // Set up click listeners
        registerButton.setOnClickListener {
            if (validateInputs()) {
                sendRegistrationData()
            }
        }

        // password toggle functionality
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

        // retype password toggle functionality
        toggleRetypePasswordVisibility.setOnClickListener {
            if (retypePasswordEditText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Show password
                retypePasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleRetypePasswordVisibility.setImageResource(R.drawable.ic_visibility)
            } else {
                // Hide password
                retypePasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleRetypePasswordVisibility.setImageResource(R.drawable.ic_visibility_off)
            }
            // Keep cursor at the end of text
            retypePasswordEditText.setSelection(retypePasswordEditText.text.length)
        }

        goLogInButton.setOnClickListener {
            switchToLoginPage()
        }
    }

    private fun hideAllErrorMessages() {
        idErrorText.visibility = View.GONE
        usernameErrorText.visibility = View.GONE
        emailErrorText.visibility = View.GONE
        phoneErrorText.visibility = View.GONE
        passwordErrorText.visibility = View.GONE
        retypePasswordErrorText.visibility = View.GONE
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateFieldsAndUpdateButton()

                // Reset field styles as user types
                when (currentFocus) {
                    idEditText -> {
                        idEditText.background = ContextCompat.getDrawable(this@RegisterActivity,
                            R.drawable.rounded_edit_text
                        )
                        idErrorText.visibility = View.GONE
                    }
                    usernameEditText -> {
                        usernameEditText.background = ContextCompat.getDrawable(this@RegisterActivity,
                            R.drawable.rounded_edit_text
                        )
                        usernameErrorText.visibility = View.GONE
                    }
                    emailEditText -> {
                        emailEditText.background = ContextCompat.getDrawable(this@RegisterActivity,
                            R.drawable.rounded_edit_text
                        )
                        emailErrorText.visibility = View.GONE
                    }
                    phoneEditText -> {
                        phoneEditText.background = ContextCompat.getDrawable(this@RegisterActivity,
                            R.drawable.rounded_edit_text
                        )
                        phoneErrorText.visibility = View.GONE
                    }
                    passwordEditText -> {
                        passwordEditText.background = ContextCompat.getDrawable(this@RegisterActivity,
                            R.drawable.rounded_edit_text
                        )
                        passwordErrorText.visibility = View.GONE
                    }
                    retypePasswordEditText -> {
                        retypePasswordEditText.background = ContextCompat.getDrawable(this@RegisterActivity,
                            R.drawable.rounded_edit_text
                        )
                        retypePasswordErrorText.visibility = View.GONE
                    }
                }
            }
        }

        // Add text watchers to all fields
        usernameEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        retypePasswordEditText.addTextChangedListener(textWatcher)
        phoneEditText.addTextChangedListener(textWatcher)
        idEditText.addTextChangedListener(textWatcher)
    }

    private fun validateFieldsAndUpdateButton() {
        val allFieldsFilled =
            usernameEditText.text.isNotEmpty() &&
                    emailEditText.text.isNotEmpty() &&
                    passwordEditText.text.isNotEmpty() &&
                    retypePasswordEditText.text.isNotEmpty() &&
                    phoneEditText.text.isNotEmpty() &&
                    idEditText.text.isNotEmpty()

        registerButton.isEnabled = allFieldsFilled
        registerButton.alpha = if (allFieldsFilled) 1.0f else 0.5f
    }

    private fun validateInputs(): Boolean {
        // Reset all input fields to normal state
        resetInputStyles()

        var isValid = true

        // Validate ID
        if (idEditText.text.toString().isEmpty()) {
            showError(idEditText, idErrorText, "ID number is required")
            isValid = false
        }

        // Validate Username
        if (usernameEditText.text.toString().isEmpty()) {
            showError(usernameEditText, usernameErrorText, "Username is required")
            isValid = false
        }

        // Validate Email
        val email = emailEditText.text.toString()
        if (email.isEmpty()) {
            showError(emailEditText, emailErrorText, "Email is required")
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(emailEditText, emailErrorText, "Enter a valid email address")
            isValid = false
        }

        // Validate Phone
        val phone = phoneEditText.text.toString()
        if (phone.isEmpty()) {
            showError(phoneEditText, phoneErrorText, "Phone number is required")
            isValid = false
        } else if (phone.length < 10) {
            showError(phoneEditText, phoneErrorText, "Phone number must be at least 10 digits")
            isValid = false
        }

        // Validate Password
        val password = passwordEditText.text.toString()
        if (password.isEmpty()) {
            showError(passwordEditText, passwordErrorText, "Password is required")
            isValid = false
        } else if (password.length < 6) {
            showError(passwordEditText, passwordErrorText, "Password must be at least 6 characters")
            isValid = false
        } else if (!password.containsLettersAndDigits()) {
            showError(passwordEditText, passwordErrorText, "Password must contain both letters and numbers")
            isValid = false
        }

        // Validate Retype Password
        val retypePassword = retypePasswordEditText.text.toString()
        if (retypePassword.isEmpty()) {
            showError(retypePasswordEditText, retypePasswordErrorText, "Please confirm your password")
            isValid = false
        } else if (password != retypePassword) {
            showError(retypePasswordEditText, retypePasswordErrorText, "Passwords do not match")
            isValid = false
        }

        return isValid
    }

    private fun String.containsLettersAndDigits(): Boolean {
        var hasLetter = false
        var hasDigit = false

        for (char in this) {
            if (char.isLetter()) hasLetter = true
            if (char.isDigit()) hasDigit = true

            // Early return if both conditions are met
            if (hasLetter && hasDigit) return true
        }

        return false
    }

    private fun resetInputStyles() {
        // Reset all fields to normal state
        idEditText.background = ContextCompat.getDrawable(this, R.drawable.rounded_edit_text)
        usernameEditText.background = ContextCompat.getDrawable(this, R.drawable.rounded_edit_text)
        emailEditText.background = ContextCompat.getDrawable(this, R.drawable.rounded_edit_text)
        phoneEditText.background = ContextCompat.getDrawable(this, R.drawable.rounded_edit_text)
        passwordEditText.background = ContextCompat.getDrawable(this, R.drawable.rounded_edit_text)
        retypePasswordEditText.background = ContextCompat.getDrawable(this,
            R.drawable.rounded_edit_text
        )

        // Hide all error messages
        idErrorText.visibility = View.GONE
        usernameErrorText.visibility = View.GONE
        emailErrorText.visibility = View.GONE
        phoneErrorText.visibility = View.GONE
        passwordErrorText.visibility = View.GONE
        retypePasswordErrorText.visibility = View.GONE
    }

    private fun showError(editText: EditText, errorTextView: TextView, message: String) {
        // Change the border to red
        editText.background = ContextCompat.getDrawable(this, R.drawable.rounded_edit_text_error)

        // Show error message
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
        errorTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
    }

    private fun switchToLoginPage() {
        Log.d(TAG, "Switching to LoginActivity...")
        try {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish() // Close this activity
            Log.d(TAG, "LoginActivity started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error switching to login page: ${e.message}")
            Toast.makeText(this, "Cannot open login page: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun switchToCameraPage(){
        val cameraPage = Intent(this, CameraActivity::class.java)
        startActivity(cameraPage)
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            photoUri = data?.data
            Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateRSAKeyPair(): KeyPair? {
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .build()

            keyPairGenerator.initialize(parameterSpec)
            keyPairGenerator.generateKeyPair()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate RSA key pair: ${e.message}")
            null
        }
    }

    private fun encodePublicKeyToBase64(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    private fun savePrivateKey(privateKey: PrivateKey) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val keyEntry = KeyStore.PrivateKeyEntry(privateKey, null)
        val keyStoreProtection = KeyStore.PasswordProtection(null)
        keyStore.setEntry(keyAlias, keyEntry, keyStoreProtection)
    }

    private fun sendPublicKeyToServer(publicKey: String, userId: String) {
        // Show progress indicator
        progressBar.visibility = View.VISIBLE
        registerButton.isEnabled = false
        registerButton.text = "Setting up security..."


        val url = ApiUtils.SETUP_PUBLIC_KEY
        val jsonObject = JSONObject().apply {
            put("public_key", publicKey)
            put("user_id", userId)
        }
        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    registerButton.text = "Register"
                    Toast.makeText(this@RegisterActivity, "Failed to exchange secure key: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    registerButton.text = "Register"

                    if (response.isSuccessful) {
                        try {
                            val responseBody = response.body?.string()
                            val jsonResponse = JSONObject(responseBody ?: "{}")

                            // Extract server's public key from response
                            val serverPublicKey = jsonResponse.optString("server_public_key", "")

                            if (serverPublicKey.isNotEmpty()) {
                                // Store server's public key for future use
                                saveServerPublicKey(serverPublicKey)
                                Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                                Log.i(TAG, "Secure key exchange successful ${serverPublicKey}")
                                // Navigate to login screen
                                switchToLoginPage()
                            } else {
                                Log.e(TAG, "Server response did not contain public key")
                                Toast.makeText(this@RegisterActivity, "Invalid server response", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: JSONException) {
                            Log.e(TAG, "Error parsing server response: ${e.message}")
                            Toast.makeText(this@RegisterActivity, "Error parsing server response", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity, "Server error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveServerPublicKey(serverPublicKeyString: String) {
        val sharedPreferences = getSharedPreferences("SecureKeys", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("SERVER_PUBLIC_KEY", serverPublicKeyString)
            apply()
        }
        Log.d(TAG, "Server public key saved successfully")
    }

    private fun sendRegistrationData() {
        // Show progress indicator
        progressBar.visibility = View.VISIBLE
        registerButton.isEnabled = false
        registerButton.text = "Registering..."

        val jsonObject = JSONObject().apply {
            put("username", usernameEditText.text.toString().trim())
            put("email", emailEditText.text.toString().trim())
            put("password", passwordEditText.text.toString().trim())
            put("phone_number", phoneEditText.text.toString().trim())
            put("id_card", idEditText.text.toString().trim())
        }
        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(ApiUtils.REGISTER_ENDPOINT).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    registerButton.text = "Register"
                    Toast.makeText(this@RegisterActivity, "Failed to register: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        try {
                            val responseBody = response.body?.string()
                            val jsonResponse = JSONObject(responseBody ?: "{}")
                            userId = jsonResponse.optString("user_id", "")

                            val keyPair = generateRSAKeyPair()
                            if (keyPair == null || keyPair.private == null) {
                                progressBar.visibility = View.GONE
                                registerButton.isEnabled = true
                                registerButton.text = "Register"
                                Toast.makeText(this@RegisterActivity, "Failed to generate security keys", Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "Key pair generation failed")
                                return@runOnUiThread
                            }

                            publicKeyString = encodePublicKeyToBase64(keyPair.public)
                            sendPublicKeyToServer(publicKeyString, userId)
                        } catch (e: JSONException) {
                            progressBar.visibility = View.GONE
                            registerButton.isEnabled = true
                            registerButton.text = "Register"
                            Toast.makeText(this@RegisterActivity, "Registration error: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Error Registering user: ${e.message}")
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        registerButton.isEnabled = true
                        registerButton.text = "Register"

                        try {
                            val errorBody = response.body?.string()
                            val errorJson = JSONObject(errorBody ?: "{}")
                            val errorMessage = errorJson.optString("detail", "Registration failed")
                            Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@RegisterActivity, "Registration failed: ${response.code}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }
}