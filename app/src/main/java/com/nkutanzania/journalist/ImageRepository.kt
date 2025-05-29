package com.nkutanzania.journalist

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class ImageRepository(private val context: Context) {

    fun getUploadDirectory(userId: Int? = null): File {
        val baseDir = File(context.getExternalFilesDir(null), "siu/uploads")

        // If userId is provided, create a user-specific subdirectory
        val directory = if (userId != null) {
            File(baseDir, userId.toString())
        } else {
            // Get current user ID from shared preferences
            val sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
            val currentUserId = sharedPreferences.getInt("userId", -1)

            if (currentUserId != -1) {
                File(baseDir, currentUserId.toString())
            } else {
                baseDir // Fallback to base directory if no user ID is available
            }
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory
    }

    fun getAllUserImages(): List<ImageItem> {
        val directory = getUploadDirectory()
        val imageItems = mutableListOf<ImageItem>()

        directory.listFiles()?.filter { it.extension.lowercase() == "jpg" }?.forEach { file ->
            // Extract status from filename or metadata
            // For now, we'll use the filename pattern SIU_{timestamp}_{status}.jpg
            val fileName = file.name
            val status = when {
                fileName.contains("_VERIFIED") -> "VERIFIED"
                fileName.contains("_FAILED") -> "FAILED"
                else -> "PENDING"
            }

            // Get timestamp from the file's last modified time
            val timestamp = file.lastModified()

            imageItems.add(ImageItem(Uri.fromFile(file), status, timestamp))
        }

        return imageItems.sortedByDescending { it.timestamp }
    }

    fun saveImageWithStatus(bitmap: Bitmap, status: String): Uri? {
        val directory = getUploadDirectory()
        val fileName = "SIU_${System.currentTimeMillis()}_$status.jpg"
        val file = File(directory, fileName)

        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("com.nkutanzania.siu.ImageRepository", "Error saving image: ${e.message}")
            null
        }
    }

    fun updateImageStatus(uri: Uri, newStatus: String): Uri? {
        val path = uri.path ?: return null
        val file = File(path)
        if (!file.exists()) return null

        val oldName = file.name
        val timestamp = oldName.split("_")[1]
        val newFileName = "SIU_${timestamp}_$newStatus.jpg"
        val newFile = File(file.parentFile, newFileName)

        return if (file.renameTo(newFile)) {
            Uri.fromFile(newFile)
        } else {
            null
        }
    }
}


data class ImageItem(
    val uri: Uri,
    val status: String,
    val timestamp: Long = System.currentTimeMillis() // Default to current time
)