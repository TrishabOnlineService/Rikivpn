package com.example.config

import android.content.Context
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json

object AssetCrypto {
    // In a production app, do NOT hardcode this. Derive it securely (e.g. from a backend or keystore).
    // For this example, we match the key used in the build.gradle.kts encryption task.
    private const val SECRET_STRING = "my-super-secret-key-32bytes-long"

    fun decryptServerList(context: Context): List<ServerEntry> {
        val encrypted = context.assets.open("config.bin").readBytes()
        if (encrypted.isEmpty()) return emptyList()

        val key = deriveKeyFromDeviceOrRemote()
        val plaintext = aesGcmDecrypt(encrypted, key)
        
        return Json { ignoreUnknownKeys = true }.decodeFromString<List<ServerEntry>>(plaintext)
    }

    private fun deriveKeyFromDeviceOrRemote(): ByteArray {
        return SECRET_STRING.toByteArray().copyOf(32)
    }

    private fun aesGcmDecrypt(encryptedData: ByteArray, key: ByteArray): String {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        // Extract IV (first 12 bytes)
        val iv = encryptedData.copyOfRange(0, 12)
        val ciphertext = encryptedData.copyOfRange(12, encryptedData.size)
        
        val spec: AlgorithmParameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes)
    }
}
