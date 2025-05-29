package com.nkutanzania.journalist

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import java.util.concurrent.TimeUnit

private const val TAG = "ApiUtils"

object ApiUtils {
    // Server endpoints
    const val BASE_URL = "http://172.20.10.2:8000" // we will change to HTTPS
    const val REGISTER_ENDPOINT = "$BASE_URL/auth/register/"
    const val LOGIN_ENDPOINT = "$BASE_URL/auth/login/"
    const val UPLOAD_ENDPOINT = "$BASE_URL/auth/upload/"
    const val LOGOUT_ENDPOINT = "$BASE_URL/auth/logout/"
    const val SETUP_PUBLIC_KEY = "$BASE_URL/auth/users/setup-public-key/"

    // Add a preconfigured OkHttpClient
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(50, TimeUnit.SECONDS)
            .readTimeout(50, TimeUnit.SECONDS)
            .writeTimeout(50, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }


    /**
     * Encrypts data using AES encryption with the server's public key
     */
    fun encryptData(data: ByteArray, serverPublicKeyBase64: String?): Pair<ByteArray, ByteArray>? {
        if (serverPublicKeyBase64 == null) {
            Log.e(TAG, "Server public key not found")
            return null
        }

        val serverPublicKey = getServerPublicKey(serverPublicKeyBase64) ?: return null

        try {
            // Generate AES Key
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val aesKey = keyGenerator.generateKey()

            // Encrypt image with AES
            val cipherAES = Cipher.getInstance("AES/GCM/NoPadding")
            cipherAES.init(Cipher.ENCRYPT_MODE, aesKey)
            val iv = cipherAES.iv // Initialization vector for decryption
            val encryptedData = cipherAES.doFinal(data)

            // Encrypt AES key with RSA
            val cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipherRSA.init(Cipher.ENCRYPT_MODE, serverPublicKey)
            val encryptedAesKey = cipherRSA.doFinal(aesKey.encoded)

            return Pair(encryptedData + iv, encryptedAesKey) // Send encrypted image + IV + AES Key
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting data: ${e.message}")
            return null
        }
    }

    /**
     * Gets the server's public key from a Base64 encoded string
     */
    private fun getServerPublicKey(publicKeyBase64: String): PublicKey? {
        return try {
            val keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            keyFactory.generatePublic(keySpec)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid Base64 encoding for public key")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve public key: ${e.message}")
            null
        }
    }

    /**
     * Compresses a bitmap to a byte array
     */
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    /**
     * Calculates SHA-256 hash of data
     */
    fun calculateHash(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }
}