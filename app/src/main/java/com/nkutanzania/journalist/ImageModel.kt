package com.nkutanzania.journalist

import android.graphics.Bitmap

data class ImageModel(
    val id: String,
    val imageUrl: String?,
    val encryptedData: String?,
    val encryptedAESKey: String?,
    val isEncrypted: Boolean = false,
    val createdAt: String?,
    val hash: String? = null,
    val decryptedBitmap: Bitmap? = null,
    val isDecrypted: Boolean = false
)