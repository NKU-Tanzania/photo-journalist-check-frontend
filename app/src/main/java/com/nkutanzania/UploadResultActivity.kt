package com.nkutanzania.journalist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class UploadResultActivity : BaseActivity() {

    private lateinit var imageView: ImageView
    private lateinit var statusText: TextView
    private lateinit var statusProgressBar: ProgressBar
    private lateinit var viewAllButton: Button
    private lateinit var cameraButton: FloatingActionButton
    private lateinit var retryButton: Button
    private lateinit var logoutButton: Button

    private var imageUri: String? = null
    private var uploadStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_result)

        // Initialize UI components
        imageView = findViewById(R.id.resultImageView)
        statusText = findViewById(R.id.statusText)
        statusProgressBar = findViewById(R.id.statusProgressBar)
        viewAllButton = findViewById(R.id.viewAllButton)
        cameraButton = findViewById(R.id.cameraButton)
        retryButton = findViewById(R.id.retryButton)
        logoutButton = findViewById(R.id.logout)

        cameraButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("LAUNCH_CAMERA", true)
            startActivity(intent)
        }

        // Get data from intent
        imageUri = intent.getStringExtra("IMAGE_URI")
        uploadStatus = intent.getStringExtra("UPLOAD_STATUS") ?: "PENDING"

        // Set the image
        imageUri?.let {
            imageView.setImageURI(Uri.parse(it))
        }

        // Set initial status
        updateStatusUI(uploadStatus ?: "PENDING")

        // Button click listeners
        viewAllButton.setOnClickListener {
            navigateToUploadedImages()
        }


        retryButton.setOnClickListener {
            retryUpload()
        }

        logoutButton.setOnClickListener {
            logoutUser()
        }

    }

    private fun updateStatusUI(status: String) {
        when (status) {
            "PENDING" -> {
                statusText.text = "Waiting for verification..."
                statusProgressBar.visibility = View.VISIBLE
                retryButton.visibility = View.GONE
            }
            "VERIFIED" -> {
                statusText.text = "Uploaded and Verified ✔"
                statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                statusProgressBar.visibility = View.GONE
                retryButton.visibility = View.GONE
            }
            "FAILED" -> {
                statusText.text = "Failed to verify x "
                statusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                statusProgressBar.visibility = View.GONE
                retryButton.visibility = View.VISIBLE
            }
        }
    }

    private fun retryUpload() {
        // Logic to retry the upload
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra("RETRY_IMAGE_URI", imageUri)
        startActivity(intent)
        finish()
    }

    private fun navigateToUploadedImages() {
        val intent = Intent(this, UploadedImagesActivity::class.java)
        intent.putExtra("IMAGE_URI", imageUri)
        intent.putExtra("UPLOAD_STATUS", uploadStatus)
        startActivity(intent)
        finish()
    }

    private fun navigateToCamera() {
        startActivity(Intent(this, CameraActivity::class.java))
        finish()
    }


}