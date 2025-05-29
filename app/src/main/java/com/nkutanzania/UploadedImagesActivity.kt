package com.nkutanzania.journalist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UploadedImagesActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cameraButton: FloatingActionButton
    private lateinit var imageRepository: ImageRepository
    private lateinit var adapter: ImageAdapter
    private lateinit var logoutButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var noImagesTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uploaded_images)

        imageRepository = ImageRepository(this)

        recyclerView = findViewById(R.id.imagesRecyclerView)
        cameraButton = findViewById(R.id.cameraButton)
        logoutButton = findViewById(R.id.logout)
        backButton = findViewById(R.id.backButton)
        noImagesTextView = findViewById(R.id.noImagesTextView)

        setupRecyclerView()

        cameraButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("LAUNCH_CAMERA", true)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            logoutUser()
        }

        backButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadImages()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ImageAdapter(mutableListOf()) { imageItem ->
            // Handle image click - show full screen
            val intent = Intent(this, UploadResultActivity::class.java)
            intent.putExtra("IMAGE_URI", imageItem.uri.toString())
            intent.putExtra("UPLOAD_STATUS", imageItem.status)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun loadImages() {
        val images = imageRepository.getAllUserImages()
        adapter.updateImages(images)

        // Show or hide the "No uploaded images" message based on whether images exist
        if (images.isEmpty()) {
            noImagesTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noImagesTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}

class ImageAdapter(
    private val images: MutableList<ImageItem>,
    private val onImageClick: (ImageItem) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    fun updateImages(newImages: List<ImageItem>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount() = images.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.itemImageView)
        private val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(imageItem: ImageItem) {
            imageView.setImageURI(imageItem.uri)

            // Get the formatted relative time
            timestampText.text = getRelativeTimeString(imageItem.timestamp)

            // Status icon handling remains the same
            when (imageItem.status) {
                "VERIFIED" -> {
                    statusIcon.setImageResource(R.drawable.ic_verified)
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.white))
                }
                "FAILED" -> {
                    statusIcon.setImageResource(R.drawable.ic_failed)
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
                }
                else -> {
                    statusIcon.setImageResource(R.drawable.ic_pending)
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                }
            }

            itemView.setOnClickListener { onImageClick(imageItem) }
        }

        private fun getRelativeTimeString(timestamp: Long): String {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

            val now = Calendar.getInstance()
            val timestampCal = Calendar.getInstance().apply { timeInMillis = timestamp }

            return when {
                // Today
                isSameDay(timestampCal, now) -> {
                    "Today at ${timeFormat.format(Date(timestamp))}"
                }
                // Yesterday
                isYesterday(timestampCal, now) -> {
                    "Yesterday at ${timeFormat.format(Date(timestamp))}"
                }
                // Same year
                timestampCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
                    "${dateFormat.format(Date(timestamp))} at ${timeFormat.format(Date(timestamp))}"
                }
                // Different year
                else -> {
                    "${fullDateFormat.format(Date(timestamp))} at ${timeFormat.format(Date(timestamp))}"
                }
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
            val yesterday = Calendar.getInstance().apply {
                timeInMillis = cal2.timeInMillis
                add(Calendar.DAY_OF_YEAR, -1)
            }
            return isSameDay(cal1, yesterday)
        }
    }
}