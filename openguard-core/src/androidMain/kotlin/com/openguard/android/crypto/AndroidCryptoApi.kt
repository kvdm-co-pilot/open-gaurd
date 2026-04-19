package com.openguard.android.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.openguard.core.api.CipherSpec
import com.openguard.core.api.CryptoApi
import com.openguard.core.api.EncryptedData
import com.openguard.core.api.HashAlgorithm
import com.openguard.core.api.KeyAlgorithm
import com.openguard.core.api.KeyHandle
import com.openguard.core.api.KeyPurpose
import com.openguard.core.api.KeySpec
import com.openguard.core.api.OpenGuardCryptoException
import com.openguard.core.api.TotpAlgorithm
import com.openguard.core.api.TotpApi
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android implementation of [CryptoApi] using Android Keystore.
 *
 * Keys are stored in the Android Keystore system and are not extractable.
 * On supported devices, keys are backed by a dedicated security chip (StrongBox).
 */
internal class AndroidCryptoApi : CryptoApi {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    override fun generateKey(alias: String, spec: KeySpec): KeyHandle {
        return try {
            when (spec.algorithm) {
                KeyAlgorithm.AES -> generateAesKey(alias, spec)
                KeyAlgorithm.RSA -> generateRsaKey(alias, spec)
                KeyAlgorithm.EC -> generateEcKey(alias, spec)
            }
        } catch (e: Exception) {
            throw OpenGuardCryptoException("Failed to generate key '$alias': ${e.message}", e)
        }
    }

    private fun generateAesKey(alias: String, spec: KeySpec): KeyHandle {
        val purposes = spec.purposes.fold(0) { acc, purpose ->
            acc or when (purpose) {
                KeyPurpose.ENCRYPT -> KeyProperties.PURPOSE_ENCRYPT
                KeyPurpose.DECRYPT -> KeyProperties.PURPOSE_DECRYPT
                KeyPurpose.SIGN -> KeyProperties.PURPOSE_SIGN
                KeyPurpose.VERIFY -> KeyProperties.PURPOSE_VERIFY
            }
        }

        val paramSpec = KeyGenParameterSpec.Builder(alias, purposes)
            .setKeySize(spec.keySize)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(spec.requireUserAuthentication)
            .apply {
                if (spec.userAuthenticationValiditySeconds > 0) {
                    setUserAuthenticationValidityDurationSeconds(spec.userAuthenticationValiditySeconds)
                }
                if (spec.hardwareBacked) {
                    trySetStrongBox(this)
                }
            }
            .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(paramSpec)
        keyGenerator.generateKey()

        val isHardwareBacked = isKeyHardwareBacked(alias)
        return KeyHandle(alias = alias, algorithm = KeyAlgorithm.AES, isHardwareBacked = isHardwareBacked)
    }

    private fun generateRsaKey(alias: String, spec: KeySpec): KeyHandle {
        val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        val paramSpec = KeyGenParameterSpec.Builder(alias, purposes)
            .setKeySize(spec.keySize)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
            .build()

        val keyPairGenerator = java.security.KeyPairGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        keyPairGenerator.initialize(paramSpec)
        keyPairGenerator.generateKeyPair()

        return KeyHandle(alias = alias, algorithm = KeyAlgorithm.RSA, isHardwareBacked = isKeyHardwareBacked(alias))
    }

    private fun generateEcKey(alias: String, spec: KeySpec): KeyHandle {
        val paramSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .build()

        val keyPairGenerator = java.security.KeyPairGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        keyPairGenerator.initialize(paramSpec)
        keyPairGenerator.generateKeyPair()

        return KeyHandle(alias = alias, algorithm = KeyAlgorithm.EC, isHardwareBacked = isKeyHardwareBacked(alias))
    }

    override fun getKey(alias: String): KeyHandle? {
        if (!keyStore.containsAlias(alias)) return null
        val entry = keyStore.getEntry(alias, null) ?: return null
        val algorithm = when (entry) {
            is KeyStore.SecretKeyEntry -> KeyAlgorithm.AES
            is KeyStore.PrivateKeyEntry -> {
                when {
                    entry.privateKey.algorithm.contains("RSA") -> KeyAlgorithm.RSA
                    else -> KeyAlgorithm.EC
                }
            }
            else -> return null
        }
        return KeyHandle(alias = alias, algorithm = algorithm, isHardwareBacked = isKeyHardwareBacked(alias))
    }

    override fun deleteKey(alias: String): Boolean {
        if (!keyStore.containsAlias(alias)) return false
        keyStore.deleteEntry(alias)
        return true
    }

    override fun isHardwareBackedKeyStoreAvailable(): Boolean {
        return try {
            // If AndroidKeyStore is available, hardware backing is at minimum TEE-based
            KeyStore.getInstance("AndroidKeyStore") != null
        } catch (_: Exception) {
            false
        }
    }

    override fun encrypt(data: ByteArray, keyAlias: String, cipher: CipherSpec): EncryptedData {
        return try {
            val secretKey = getSecretKey(keyAlias)
            val jcaCipher = Cipher.getInstance("${cipher.algorithm}/${cipher.mode}/${cipher.padding}")
            jcaCipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = jcaCipher.iv
            val ciphertext = jcaCipher.doFinal(data)
            EncryptedData(
                ciphertext = ciphertext,
                iv = iv,
                tag = ByteArray(0),
                cipherSpec = cipher,
            )
        } catch (e: Exception) {
            throw OpenGuardCryptoException("Encryption failed for key '$keyAlias': ${e.message}", e)
        }
    }

    override fun decrypt(encryptedData: EncryptedData, keyAlias: String): ByteArray {
        return try {
            val secretKey = getSecretKey(keyAlias)
            val jcaCipher = Cipher.getInstance(
                "${encryptedData.cipherSpec.algorithm}/${encryptedData.cipherSpec.mode}/${encryptedData.cipherSpec.padding}"
            )
            val spec = GCMParameterSpec(encryptedData.cipherSpec.tagLength, encryptedData.iv)
            jcaCipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            jcaCipher.doFinal(encryptedData.ciphertext)
        } catch (e: Exception) {
            throw OpenGuardCryptoException("Decryption failed for key '$keyAlias': ${e.message}", e)
        }
    }

    override fun hash(data: ByteArray, algorithm: HashAlgorithm): ByteArray {
        return MessageDigest.getInstance(algorithm.jcaName).digest(data)
    }

    override fun hmac(data: ByteArray, keyAlias: String, algorithm: HashAlgorithm): ByteArray {
        return try {
            val secretKey = getSecretKey(keyAlias)
            val macAlgorithm = when (algorithm) {
                HashAlgorithm.SHA256 -> "HmacSHA256"
                HashAlgorithm.SHA384 -> "HmacSHA384"
                HashAlgorithm.SHA512 -> "HmacSHA512"
            }
            val mac = Mac.getInstance(macAlgorithm)
            mac.init(secretKey)
            mac.doFinal(data)
        } catch (e: Exception) {
            throw OpenGuardCryptoException("HMAC failed for key '$keyAlias': ${e.message}", e)
        }
    }

    override val totp: TotpApi = AndroidTotpApi()

    private fun getSecretKey(alias: String): SecretKey {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            ?: throw OpenGuardCryptoException("Key not found or not a secret key: '$alias'")
        return entry.secretKey
    }

    private fun isKeyHardwareBacked(alias: String): Boolean {
        return try {
            val entry = keyStore.getEntry(alias, null)
            when (entry) {
                is KeyStore.SecretKeyEntry -> {
                    val factory = javax.crypto.SecretKeyFactory.getInstance(
                        entry.secretKey.algorithm, "AndroidKeyStore"
                    )
                    val keyInfo = factory.getKeySpec(
                        entry.secretKey,
                        android.security.keystore.KeyInfo::class.java
                    ) as android.security.keystore.KeyInfo
                    keyInfo.isInsideSecureHardware
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun trySetStrongBox(builder: KeyGenParameterSpec.Builder) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                builder.setIsStrongBoxBacked(true)
            } catch (_: Exception) {
                // StrongBox not available on this device — fall back to TEE
            }
        }
    }
}

internal class AndroidTotpApi : TotpApi {
    override fun generate(
        secret: ByteArray,
        digits: Int,
        timeStep: Long,
        algorithm: TotpAlgorithm,
    ): String {
        val timeCounter = System.currentTimeMillis() / 1000 / timeStep
        return computeHotp(secret, timeCounter, digits, algorithm)
    }

    override fun validate(
        code: String,
        secret: ByteArray,
        digits: Int,
        timeStep: Long,
        algorithm: TotpAlgorithm,
    ): Boolean {
        val timeCounter = System.currentTimeMillis() / 1000 / timeStep
        // Allow 1 step drift in each direction
        return (-1..1).any { drift ->
            computeHotp(secret, timeCounter + drift, digits, algorithm) == code
        }
    }

    private fun computeHotp(secret: ByteArray, counter: Long, digits: Int, algorithm: TotpAlgorithm): String {
        val macAlgorithm = when (algorithm) {
            TotpAlgorithm.SHA1 -> "HmacSHA1"
            TotpAlgorithm.SHA256 -> "HmacSHA256"
            TotpAlgorithm.SHA512 -> "HmacSHA512"
        }
        val counterBytes = ByteArray(8) { i -> (counter shr ((7 - i) * 8) and 0xFF).toByte() }
        val mac = Mac.getInstance(macAlgorithm)
        mac.init(javax.crypto.spec.SecretKeySpec(secret, macAlgorithm))
        val hash = mac.doFinal(counterBytes)
        val offset = (hash.last().toInt() and 0xF)
        val truncated = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = truncated % Math.pow(10.0, digits.toDouble()).toInt()
        return otp.toString().padStart(digits, '0')
    }
}
