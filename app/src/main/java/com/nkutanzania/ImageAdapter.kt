//package com.nkutanzania.journalist
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.ProgressBar
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.nkutanzania.journalist.R
//import java.text.SimpleDateFormat
//import java.util.Locale
//
//class com.nkutanzania.journalist.ImageAdapter(
//    private val imageList: List<ImageModel>,
//    private val onImageClick: (ImageModel) -> Unit
//) : RecyclerView.Adapter<com.nkutanzania.journalist.ImageAdapter.ImageViewHolder>() {
//
//    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val imageView: ImageView = view.findViewById(R.id.imageView)
////        val dateText: TextView = view.findViewById(R.id.dateTextView)
////        val encryptedIcon: ImageView = view.findViewById(R.id.encryptedIcon)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_image, parent, false)
//        return ImageViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
//        val image = imageList[position]
//
//        // Set date
//        if (image.createdAt != null) {
//            holder.dateText.visibility = View.VISIBLE
//            holder.dateText.text = formatDate(image.createdAt)
//        } else {
//            holder.dateText.visibility = View.GONE
//        }
//
//        // Set encryption indicator
//        if (image.isEncrypted) {
//            holder.encryptedIcon.visibility = View.VISIBLE
//            if (image.isDecrypted) {
//                holder.encryptedIcon.setImageResource(R.drawable.placeholder) // Replace with unlock icon
//            } else {
//                holder.encryptedIcon.setImageResource(R.drawable.placeholder) // Replace with lock icon
//            }
//        } else {
//            holder.encryptedIcon.visibility = View.GONE
//        }
//
//        // Set image
//        if (image.isDecrypted && image.decryptedBitmap != null) {
//            // Show decrypted image
//            holder.imageView.setImageBitmap(image.decryptedBitmap)
//        } else if (image.imageUrl != null) {
//            // For non-decrypted images, use a placeholder
//            holder.imageView.setImageResource(R.drawable.placeholder)
//
//            // You can use a simpler image loading approach if you don't have Glide
//            // You could implement this with AsyncTask or Coroutines
//            loadImageFromUrl(image.imageUrl, holder.imageView)
//        } else {
//            // Fallback
//            holder.imageView.setImageResource(R.drawable.placeholder)
//        }
//
//        // Set click listener
//        holder.itemView.setOnClickListener {
//            onImageClick(image)
//        }
//    }
//
//    private fun loadImageFromUrl(url: String, imageView: ImageView) {
//        // Simple placeholder for image loading
//        // In a real app, you'd implement this with an AsyncTask or Coroutines
//        // For now, we'll just use the placeholder
//    }
//
//    private fun formatDate(dateString: String): String {
//        // Simple date formatting
//        return try {
//            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
//            val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
//            val date = inputFormat.parse(dateString)
//            outputFormat.format(date ?: return dateString)
//        } catch (e: Exception) {
//            dateString
//        }
//    }
//
//    override fun getItemCount(): Int = imageList.size
//}