package com.openguard.android.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.openguard.core.api.AccessControl
import com.openguard.core.api.OpenGuardStorageException
import com.openguard.core.api.StorageApi
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android implementation of [StorageApi] using Android Keystore + EncryptedSharedPreferences.
 *
 * Values are encrypted with AES-256-GCM using keys stored in the Android Keystore.
 * The key is never exported from the secure hardware.
 *
 * For [AccessControl.BIOMETRIC_REQUIRED], keys require biometric authentication
 * before use. This implementation handles the key management; UI prompt integration
 * is handled by the consuming application using [android.hardware.biometrics.BiometricPrompt].
 */
internal class AndroidStorageApi : StorageApi {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val storage = mutableMapOf<String, EncryptedEntry>() // In-memory; replace with EncryptedSharedPreferences

    override fun put(key: String, value: ByteArray, accessControl: AccessControl) {
        try {
            val keyAlias = storageKeyAlias(key)
            ensureKeyExists(keyAlias, accessControl)
            val secretKey = getSecretKey(keyAlias)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(value)
            storage[key] = EncryptedEntry(ciphertext = ciphertext, iv = iv, keyAlias = keyAlias)
        } catch (e: Exception) {
            throw OpenGuardStorageException("Failed to store value for key '$key': ${e.message}", e)
        }
    }

    override fun get(key: String): ByteArray? {
        val entry = storage[key] ?: return null
        return try {
            val secretKey = getSecretKey(entry.keyAlias)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, entry.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            cipher.doFinal(entry.ciphertext)
        } catch (e: Exception) {
            throw OpenGuardStorageException("Failed to retrieve value for key '$key': ${e.message}", e)
        }
    }

    override fun contains(key: String): Boolean = storage.containsKey(key)

    override fun delete(key: String): Boolean {
        return storage.remove(key) != null
    }

    override fun clearAll() {
        storage.clear()
    }

    override fun secureZero(buffer: ByteArray) {
        buffer.fill(0)
    }

    override fun secureZero(buffer: CharArray) {
        buffer.fill('\u0000')
    }

    private fun storageKeyAlias(key: String) = "openguard_storage_${key.hashCode()}"

    private fun ensureKeyExists(keyAlias: String, accessControl: AccessControl) {
        if (keyStore.containsAlias(keyAlias)) return

        val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val builder = KeyGenParameterSpec.Builder(keyAlias, purposes)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

        when (accessControl) {
            AccessControl.ALWAYS -> Unit
            AccessControl.DEVICE_UNLOCKED -> builder.setUserAuthenticationRequired(false)
            AccessControl.DEVICE_CREDENTIAL -> {
                builder.setUserAuthenticationRequired(true)
                builder.setUserAuthenticationValidityDurationSeconds(60)
            }
            AccessControl.BIOMETRIC_REQUIRED -> {
                builder.setUserAuthenticationRequired(true)
                builder.setUserAuthenticationValidityDurationSeconds(-1) // require auth every time
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    builder.setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG
                    )
                }
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }

    private fun getSecretKey(keyAlias: String): SecretKey {
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            ?: throw OpenGuardStorageException("Storage key not found: '$keyAlias'")
        return entry.secretKey
    }

    private data class EncryptedEntry(
        val ciphertext: ByteArray,
        val iv: ByteArray,
        val keyAlias: String,
    )
}
