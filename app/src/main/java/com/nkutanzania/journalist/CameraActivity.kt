package com.nkutanzania.journalist

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.Manifest
import android.content.ContentValues
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import androidx.biometric.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nkutanzania.journalist.R
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

private const val TAG = "CameraActivity"
private const val LOCATION_PERMISSION_CODE = 102

class CameraActivity : BaseActivity(), LocationListener {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var captionTextView: TextView

    private lateinit var logoutButton: Button
    private lateinit var uploadButton: ImageButton
    //    private lateinit var retakeButton: FloatingActionButton
    private lateinit var imagePreview: ImageView
    private lateinit var cameraContainer: View
    private lateinit var previewContainer: View
    private lateinit var captureButton: Button
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewUploadsButton: Button
    private lateinit var verificationText: TextView


    private var capturedImage: Bitmap? = null
    private val CAMERA_REQUEST_CODE = 100
    private val STORAGE_PERMISSION_CODE = 101
    private val IMAGE_CAPTURE_CODE = 1001
    private var imageUri: Uri? = null

    // creating http client
    private val client = ApiUtils.client
    private lateinit var cameraExecutor: ExecutorService

    // Location related variables
    private var locationManager: LocationManager? = null
    private var lastKnownLocation: Location? = null
    private var isLocationPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageRepository = ImageRepository(this)

        if (!isUserLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }



        // Set up biometric authentication
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Show camera UI after successful authentication
                    showCameraUI()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        // User clicked negative button or canceled, still show camera
                        showCameraUI()
                    } else {
                        Toast.makeText(this@CameraActivity,
                            "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate to access camera")
            .setNegativeButtonText("Skip")
            .build()


        setContentView(R.layout.activity_camera)

        captionTextView = findViewById(R.id.caption)
        // Initialize UI components
        imagePreview = findViewById(R.id.appLogo)
        uploadButton = findViewById(R.id.uploadButton)
//        retakeButton = findViewById(R.id.retakeButton)
        logoutButton = findViewById(R.id.logout)
        cameraContainer = findViewById(R.id.cameraContainer)
        previewContainer = findViewById(R.id.previewContainer)
        captureButton = findViewById(R.id.captureButton)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        verificationText = findViewById(R.id.verificationText);
        viewUploadsButton = findViewById(R.id.viewUploadsButton)


        // Setup button listeners
        captureButton.setOnClickListener { launchCamera() }
        uploadButton.setOnClickListener { uploadImage() }
//        retakeButton.setOnClickListener { showCameraUI() }
        logoutButton.setOnClickListener { logoutUser() }
        viewUploadsButton.setOnClickListener { navigateToUploadedImages() }

        // Initialize location manager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Request location permissions if needed
        checkLocationPermission()

        // Check if we should launch camera immediately
        if (intent.getBooleanExtra("LAUNCH_CAMERA", false)) {
            launchCamera()
        }

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        biometricPrompt.authenticate(promptInfo)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Camera and storage permissions are required", Toast.LENGTH_SHORT).show()
                }
            }
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isLocationPermissionGranted = true
                    startLocationUpdates()
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            isLocationPermissionGranted = true
            startLocationUpdates()
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        if (isLocationPermissionGranted) {
            try {
                // Request more frequent updates to ensure location is available at capture time
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L, // Update every 0.5 seconds (more frequent)
                    0f,   // Update regardless of distance moved
                    this
                )

                // Use network provider as fallback
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    500L,
                    0f,
                    this
                )

                // Get last known location - try multiple providers
                lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            ?: locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                Log.d(TAG, "Initial location: ${lastKnownLocation?.latitude}, ${lastKnownLocation?.longitude}")
            } catch (e: SecurityException) {
                Log.e(TAG, "Location security exception: ${e.message}")
            }
        }
    }

    // Add a method to ensure location data is available before upload
    private fun ensureLocationAvailable(callback: () -> Unit) {
        if (lastKnownLocation != null) {
            // We already have a location, proceed immediately
            callback()
            return
        }

        // No location yet, let's try to get one quickly
        try {
            Toast.makeText(this, "Acquiring location...", Toast.LENGTH_SHORT).show()

            // Create a single update request
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
                locationManager?.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            lastKnownLocation = location
                            Log.d(TAG, "Got location update: ${location.latitude}, ${location.longitude}")
                            callback()
                        }

                        // Implement other required methods
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    null
                )

                // Also try network provider
                locationManager?.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (lastKnownLocation == null) {
                                lastKnownLocation = location
                                Log.d(TAG, "Got network location: ${location.latitude}, ${location.longitude}")
                                callback()
                            }
                        }

                        // Implement other required methods
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    null
                )

                // Set a timeout to proceed anyway after 15 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    if (lastKnownLocation == null) {
                        Log.d(TAG, "Location timeout - proceeding without location")
                        callback()
                    }
                }, 30000)
            } else {
                // No permission, proceed without location
                callback()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location: ${e.message}")
            callback()
        }
    }

    override fun onLocationChanged(location: Location) {
        lastKnownLocation = location
        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        if (isLocationPermissionGranted) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager?.removeUpdates(this)
    }

    private fun navigateToUploadedImages() {
        val intent = Intent(this, UploadedImagesActivity::class.java)
        startActivity(intent)
        // Don't call finish() here so the user can come back to the camera
    }

    private fun showCameraUI() {
        cameraContainer.visibility = View.VISIBLE
        previewContainer.visibility = View.GONE
        viewUploadsButton.visibility = View.VISIBLE // Show "My Uploads" button when in camera mode
    }

    private fun showPreviewUI() {
        cameraContainer.visibility = View.GONE
        previewContainer.visibility = View.VISIBLE
        viewUploadsButton.visibility = View.GONE // Hide "My Uploads" button when in preview mode
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        return sharedPreferences.contains("accessToken")
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAMERA_REQUEST_CODE)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        // Create a file for the image
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")

        // Store the image in MediaStore
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        // Create camera intent with full-size image capture
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            // Use the full-quality image
            imageUri?.let { uri ->
                // Load the image into a Bitmap for preview
                capturedImage = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                imagePreview.setImageBitmap(capturedImage)
                showPreviewUI()
            } ?: run {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun collectDeviceMetadata(): JSONObject {
        val metadata = JSONObject()

        try {
            // Device OS information
            metadata.put("os_name", "Android")
            metadata.put("os_version", Build.VERSION.RELEASE)
            metadata.put("api_level", Build.VERSION.SDK_INT)

            // Device model information
            metadata.put("device_manufacturer", Build.MANUFACTURER)
            metadata.put("device_model", Build.MODEL)
            metadata.put("device_brand", Build.BRAND)
            metadata.put("device_product", Build.PRODUCT)

            // Time information
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
            metadata.put("capture_time", calendar.timeInMillis)
            metadata.put("timezone", TimeZone.getDefault().id)
            metadata.put("locale", Locale.getDefault().toString())

            // Location information (if available)
            if (lastKnownLocation != null) {
                val locationObj = JSONObject()
                locationObj.put("latitude", lastKnownLocation!!.latitude)
                locationObj.put("longitude", lastKnownLocation!!.longitude)
                locationObj.put("accuracy", lastKnownLocation!!.accuracy)
                locationObj.put("altitude", lastKnownLocation!!.altitude)
                locationObj.put("provider", lastKnownLocation!!.provider)
                locationObj.put("time", lastKnownLocation!!.time)
                metadata.put("location", locationObj)
                Log.d(TAG, "Added location to metadata: lat=${lastKnownLocation!!.latitude}, lng=${lastKnownLocation!!.longitude}")
            } else {
                Log.w(TAG, "No location available to add to metadata!")
                metadata.put("location_available", false)
                metadata.put("location_error", "No location data available")
            }

            // Add device ID information (remember to handle this information responsibly)
            metadata.put("device_id", Build.SERIAL)

        } catch (e: Exception) {
            Log.e(TAG, "Error collecting metadata: ${e.message}")
            metadata.put("error", "Failed to collect some device metadata: ${e.message}")
        }

        Log.d(TAG, "Final metadata: $metadata")
        return metadata
    }

    private fun uploadImage() {
        if (capturedImage == null) {
            Toast.makeText(this, "No image to upload", Toast.LENGTH_SHORT).show()
            return
        }


        uploadProgressBar.visibility = View.VISIBLE
        verificationText.visibility=View.VISIBLE

        // Add this to force layout update
//        uploadProgressBar.invalidate()

        // Ensure we have location data before proceeding
        ensureLocationAvailable {
            // make sure the progress bar is visible
            runOnUiThread {
                uploadProgressBar.visibility = View.VISIBLE
                verificationText.visibility=View.VISIBLE
            }

            // Save image locally first
            val savedImageUri = saveImageLocally(capturedImage!!)

            // Convert bitmap to byte array
            val imageBytes = ApiUtils.bitmapToByteArray(capturedImage!!)

            // Calculate hash
            val hash = ApiUtils.calculateHash(imageBytes)

            // Get server public key
            val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
            val serverPublicKey = sharedPreferences.getString("serverPublicKey", null)

            // Encrypt data
            val encryptedResult = ApiUtils.encryptData(imageBytes, serverPublicKey)

            if (encryptedResult == null) {
                Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show()
                uploadProgressBar.visibility = View.GONE
                verificationText.visibility=View.GONE
                return@ensureLocationAvailable
            }

            val (encryptedImage, encryptedAesKey) = encryptedResult

            // Collect device metadata
            val deviceMetadata = collectDeviceMetadata()

            // Log the metadata to see if location is included
            Log.d(TAG, "Metadata being sent: $deviceMetadata")

            sendToServer(encryptedImage, encryptedAesKey, hash, savedImageUri?.toString(), deviceMetadata)
        }
    }

    private fun saveImageLocally(bitmap: Bitmap): Uri? {
        return imageRepository.saveImageWithStatus(bitmap, "PENDING")
    }

    private fun sendToServer(encryptedData: ByteArray, encryptedKey: ByteArray, hash: ByteArray, savedImageUriString: String?, metadata: JSONObject) {
        val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("accessToken", null)

        if (accessToken == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
            uploadProgressBar.visibility = View.GONE
            verificationText.visibility=View.GONE
            return
        }

        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        val encryptedKeyBase64 = Base64.encodeToString(encryptedKey, Base64.NO_WRAP)
        val metadataString = metadata.toString()
        val caption = captionTextView.text.toString()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "encrypted.jpg",
                encryptedData.toRequestBody("application/octet-stream".toMediaType()))
            .addFormDataPart("hash_value", hashBase64)
            .addFormDataPart("aes_key", encryptedKeyBase64) // Send AES key separately
            .addFormDataPart("metadata", metadataString) // Add the device metadata
            .addFormDataPart("caption", caption) // Add the photo caption
            .build()

        val request = Request.Builder()
            .url(ApiUtils.UPLOAD_ENDPOINT)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {

                    uploadProgressBar.visibility = View.GONE
                    verificationText.visibility=View.GONE
                    // Navigate to result page with failure status
                    val intent = Intent(this@CameraActivity, UploadResultActivity::class.java)
                    intent.putExtra("IMAGE_URI", savedImageUriString)
                    intent.putExtra("UPLOAD_STATUS", "FAILED")
                    startActivity(intent)
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {

                    uploadProgressBar.visibility = View.GONE
                    verificationText.visibility=View.GONE

                    if (response.isSuccessful) {
                        try {
                            // Parse the response to get verification status
                            val responseBody = response.body?.string()
                            val jsonResponse = JSONObject(responseBody ?: "{}")
                            val verification = jsonResponse.optJSONObject("verification")

                            // Get actual verification status from server
                            val uploadStatus = if (verification?.optBoolean("status") == true)
                                "VERIFIED" else "FAILED"

                            // Update the image status
                            val updatedUri = imageRepository.updateImageStatus(Uri.parse(savedImageUriString), uploadStatus)

                            // Navigate with the actual status from server
                            val intent = Intent(this@CameraActivity, UploadResultActivity::class.java)
                            intent.putExtra("IMAGE_URI", updatedUri.toString())
                            intent.putExtra("UPLOAD_STATUS", uploadStatus)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Log.e("CameraActivity", "Error parsing response: ${e.message}")
                            // Fallback to FAILED if we can't parse the verification status
                            val intent = Intent(this@CameraActivity, UploadResultActivity::class.java)
                            intent.putExtra("IMAGE_URI", savedImageUriString)
                            intent.putExtra("UPLOAD_STATUS", "FAILED")
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // Navigate to result page with failure status
                        val intent = Intent(this@CameraActivity, UploadResultActivity::class.java)
                        intent.putExtra("IMAGE_URI", savedImageUriString)
                        intent.putExtra("UPLOAD_STATUS", "FAILED")
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}