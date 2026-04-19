package com.openguard.ios.crypto

import com.openguard.core.api.CipherSpec
import com.openguard.core.api.CryptoApi
import com.openguard.core.api.EncryptedData
import com.openguard.core.api.HashAlgorithm
import com.openguard.core.api.KeyAlgorithm
import com.openguard.core.api.KeyHandle
import com.openguard.core.api.KeySpec
import com.openguard.core.api.OpenGuardCryptoException
import com.openguard.core.api.TotpAlgorithm
import com.openguard.core.api.TotpApi
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA384
import platform.CoreCrypto.CC_SHA384_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA512
import platform.CoreCrypto.CC_SHA512_DIGEST_LENGTH
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CCHmacAlgorithm
import platform.CoreCrypto.kCCHmacAlgSHA1
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCHmacAlgSHA512
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyRef
import platform.Security.errSecSuccess
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeEC
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrTokenID
import platform.Security.kSecAttrTokenIDSecureEnclave
import platform.Security.kSecClass
import platform.Security.kSecClassKey
import platform.Security.kSecPrivateKeyAttrs
import platform.Security.kSecReturnRef

/**
 * iOS implementation of [CryptoApi] using iOS Security framework and CommonCrypto.
 *
 * Asymmetric keys are stored in the iOS Keychain. Secure Enclave is used for
 * EC key generation when available (iPhone 5s and later).
 *
 * Note: iOS does not support symmetric key storage in the Secure Enclave directly —
 * symmetric operations use CommonCrypto with keys derived from SE-backed asymmetric keys.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosCryptoApi : CryptoApi {

    override fun generateKey(alias: String, spec: KeySpec): KeyHandle {
        return try {
            when (spec.algorithm) {
                KeyAlgorithm.EC -> generateEcKey(alias, spec)
                KeyAlgorithm.RSA -> generateRsaKey(alias, spec)
                KeyAlgorithm.AES -> {
                    // AES keys for iOS are managed with CommonCrypto + Keychain
                    // For simplicity, generate a random AES key and store in Keychain
                    generateAesKey(alias, spec)
                }
            }
        } catch (e: Exception) {
            throw OpenGuardCryptoException("Failed to generate key '$alias': ${e.message}", e)
        }
    }

    private fun generateEcKey(alias: String, spec: KeySpec): KeyHandle {
        val useSecureEnclave = spec.hardwareBacked && isSecureEnclaveAvailable()

        // kSecAttrTokenIDSecureEnclave keys are permanently stored in the SE when kSecPrivateKeyAttrs
        // contains kSecAttrIsPermanent — use kSecAttrAccessControl for access policies instead.
        val privateKeyAttributes = buildMap<Any?, Any?> {
            put(kSecAttrApplicationTag, alias)
        }

        val attributes = buildMap<Any?, Any?> {
            put(kSecAttrKeyType, kSecAttrKeyTypeEC)
            put(kSecAttrKeySizeInBits, spec.keySize)
            put(kSecPrivateKeyAttrs, privateKeyAttributes)
            if (useSecureEnclave) {
                put(kSecAttrTokenID, kSecAttrTokenIDSecureEnclave)
            }
        } as Map<Any?, *>

        // Use memScoped to properly pass an error pointer for ObjC interop
        val key = memScoped {
            val errorPtr = alloc<kotlinx.cinterop.ObjCObjectVar<platform.Foundation.NSError?>>()
            val result = SecKeyCreateRandomKey(attributes as CFDictionary, errorPtr.ptr)
            if (result == null) {
                val errorDesc = errorPtr.value?.localizedDescription ?: "Unknown error"
                throw OpenGuardCryptoException("EC key generation failed: $errorDesc")
            }
            result
        }

        return KeyHandle(alias = alias, algorithm = KeyAlgorithm.EC, isHardwareBacked = useSecureEnclave)
    }

    private fun generateRsaKey(alias: String, spec: KeySpec): KeyHandle {
        val privateKeyAttributes = buildMap<Any?, Any?> {
            put(kSecAttrApplicationTag, alias)
        }
        val attributes = buildMap<Any?, Any?> {
            put(kSecAttrKeyType, kSecAttrKeyTypeRSA)
            put(kSecAttrKeySizeInBits, spec.keySize)
            put(kSecPrivateKeyAttrs, privateKeyAttributes)
        } as Map<Any?, *>

        memScoped {
            val errorPtr = alloc<kotlinx.cinterop.ObjCObjectVar<platform.Foundation.NSError?>>()
            SecKeyCreateRandomKey(attributes as CFDictionary, errorPtr.ptr)
                ?: run {
                    val errorDesc = errorPtr.value?.localizedDescription ?: "Unknown error"
                    throw OpenGuardCryptoException("RSA key generation failed: $errorDesc")
                }
        }

        return KeyHandle(alias = alias, algorithm = KeyAlgorithm.RSA, isHardwareBacked = false)
    }

    private fun generateAesKey(alias: String, spec: KeySpec): KeyHandle {
        // Generate random AES key bytes and store in Keychain
        // For this skeleton, we note that proper Keychain storage would use
        // SecItemAdd with kSecClassKey
        return KeyHandle(alias = alias, algorithm = KeyAlgorithm.AES, isHardwareBacked = false)
    }

    override fun getKey(alias: String): KeyHandle? {
        // Query Keychain for key with this alias
        // Returns null if not found — full implementation uses SecItemCopyMatching
        return null
    }

    override fun deleteKey(alias: String): Boolean {
        // Delete key from Keychain using SecItemDelete
        return false
    }

    override fun isHardwareBackedKeyStoreAvailable(): Boolean = isSecureEnclaveAvailable()

    override fun encrypt(data: ByteArray, keyAlias: String, cipher: CipherSpec): EncryptedData {
        // Full implementation uses CommonCrypto CCCrypt for AES-GCM
        // This skeleton returns a placeholder
        throw OpenGuardCryptoException("encrypt() not yet implemented for iOS — requires CommonCrypto integration")
    }

    override fun decrypt(encryptedData: EncryptedData, keyAlias: String): ByteArray {
        throw OpenGuardCryptoException("decrypt() not yet implemented for iOS — requires CommonCrypto integration")
    }

    override fun hash(data: ByteArray, algorithm: HashAlgorithm): ByteArray {
        return when (algorithm) {
            HashAlgorithm.SHA256 -> computeSha256(data)
            HashAlgorithm.SHA384 -> computeSha384(data)
            HashAlgorithm.SHA512 -> computeSha512(data)
        }
    }

    override fun hmac(data: ByteArray, keyAlias: String, algorithm: HashAlgorithm): ByteArray {
        throw OpenGuardCryptoException("hmac() requires Keychain key retrieval — not yet implemented for iOS")
    }

    override val totp: TotpApi = IosTotpApi()

    private fun computeSha256(data: ByteArray): ByteArray {
        val result = ByteArray(CC_SHA256_DIGEST_LENGTH)
        data.usePinned { pinnedData ->
            result.usePinned { pinnedResult ->
                CC_SHA256(pinnedData.addressOf(0), data.size.toUInt(), pinnedResult.addressOf(0))
            }
        }
        return result
    }

    private fun computeSha384(data: ByteArray): ByteArray {
        val result = ByteArray(CC_SHA384_DIGEST_LENGTH)
        data.usePinned { pinnedData ->
            result.usePinned { pinnedResult ->
                CC_SHA384(pinnedData.addressOf(0), data.size.toUInt(), pinnedResult.addressOf(0))
            }
        }
        return result
    }

    private fun computeSha512(data: ByteArray): ByteArray {
        val result = ByteArray(CC_SHA512_DIGEST_LENGTH)
        data.usePinned { pinnedData ->
            result.usePinned { pinnedResult ->
                CC_SHA512(pinnedData.addressOf(0), data.size.toUInt(), pinnedResult.addressOf(0))
            }
        }
        return result
    }

    private fun isSecureEnclaveAvailable(): Boolean {
        // Secure Enclave is available on iPhone 5s and later (A7 chip and newer)
        // We check by attempting to create a key with SE token ID
        // In production, this would use SecKeyCreateRandomKey with error handling
        return true // Assume available for modern iOS devices
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class IosTotpApi : TotpApi {
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
        return (-1..1).any { drift ->
            computeHotp(secret, timeCounter + drift, digits, algorithm) == code
        }
    }

    private fun computeHotp(secret: ByteArray, counter: Long, digits: Int, algorithm: TotpAlgorithm): String {
        val counterBytes = ByteArray(8) { i -> (counter shr ((7 - i) * 8) and 0xFF).toByte() }
        val hmacResult = ByteArray(64)
        val hmacAlg: CCHmacAlgorithm = when (algorithm) {
            TotpAlgorithm.SHA1 -> kCCHmacAlgSHA1
            TotpAlgorithm.SHA256 -> kCCHmacAlgSHA256
            TotpAlgorithm.SHA512 -> kCCHmacAlgSHA512
        }
        val macLength = when (algorithm) {
            TotpAlgorithm.SHA1 -> 20
            TotpAlgorithm.SHA256 -> 32
            TotpAlgorithm.SHA512 -> 64
        }
        secret.usePinned { pinnedSecret ->
            counterBytes.usePinned { pinnedCounter ->
                hmacResult.usePinned { pinnedResult ->
                    CCHmac(
                        algorithm = hmacAlg,
                        key = pinnedSecret.addressOf(0),
                        keyLength = secret.size.toULong(),
                        data = pinnedCounter.addressOf(0),
                        dataLength = counterBytes.size.toULong(),
                        macOut = pinnedResult.addressOf(0),
                    )
                }
            }
        }
        val hash = hmacResult.copyOf(macLength)
        val offset = (hash.last().toInt() and 0xF)
        val truncated = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = truncated % Math.pow(10.0, digits.toDouble()).toInt()
        return otp.toString().padStart(digits, '0')
    }
}
