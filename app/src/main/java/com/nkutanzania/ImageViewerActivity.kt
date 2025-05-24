package com.nkutanzania.journalist

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

private const val TAG = "ImageViewerActivity"

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var fullImageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        // Initialize UI components
        fullImageView = findViewById(R.id.fullImageView)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)

        // Get image URI from intent
        val imageUriString = intent.getStringExtra("IMAGE_URI")

        if (imageUriString != null) {
            loadImage(Uri.parse(imageUriString))
        } else {
            showError("No image provided")
        }

        // Set click listener to close activity when image is tapped
        fullImageView.setOnClickListener {
            finish()
        }
    }

    private fun loadImage(imageUri: Uri) {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        try {
            val imagePath = imageUri.path
            if (imagePath == null) {
                showError("Invalid image path")
                return
            }

            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                showError("Image file not found")
                return
            }

            // Load the image in background
            Thread {
                try {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

                    runOnUiThread {
                        if (bitmap != null) {
                            fullImageView.setImageBitmap(bitmap)
                            progressBar.visibility = View.GONE
                        } else {
                            showError("Failed to decode image")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image: ${e.message}")
                    runOnUiThread {
                        showError("Error: ${e.message}")
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image URI: ${e.message}")
            showError("Error: ${e.message}")
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        errorText.text = message
        Log.e(TAG, "Image viewer error: $message")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Add transition animation
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}