package com.openguard.core.api

/**
 * API for cryptographic operations using platform-native secure key storage.
 *
 * Keys are stored in Android Keystore (Android) or iOS Keychain / Secure Enclave (iOS).
 * Keys marked as hardware-backed are never extractable from the secure hardware.
 */
interface CryptoApi {

    /**
     * Generates a new symmetric or asymmetric key and stores it securely.
     *
     * @param alias A unique identifier for the key within the secure key store.
     * @param spec  The key specification (algorithm, size, purposes, access control).
     * @return A [KeyHandle] referencing the generated key.
     * @throws OpenGuardCryptoException if key generation fails.
     */
    fun generateKey(alias: String, spec: KeySpec): KeyHandle

    /**
     * Returns a handle to an existing key, or null if no key with that alias exists.
     */
    fun getKey(alias: String): KeyHandle?

    /**
     * Deletes a key from the secure key store.
     *
     * @return true if the key was deleted, false if it did not exist.
     */
    fun deleteKey(alias: String): Boolean

    /**
     * Returns true if a hardware-backed key store is available on this device.
     * On Android this means Android Keystore with StrongBox or TEE backing.
     * On iOS this means Secure Enclave is available.
     */
    fun isHardwareBackedKeyStoreAvailable(): Boolean

    /**
     * Encrypts [data] using the key identified by [keyAlias].
     *
     * @param data     The plaintext data to encrypt.
     * @param keyAlias The alias of the key to use (must exist and have ENCRYPT purpose).
     * @param cipher   The cipher specification (defaults to AES-256-GCM).
     * @return [EncryptedData] containing the ciphertext and all parameters needed for decryption.
     */
    fun encrypt(
        data: ByteArray,
        keyAlias: String,
        cipher: CipherSpec = CipherSpec.AES_256_GCM,
    ): EncryptedData

    /**
     * Decrypts [encryptedData] using the key identified by [keyAlias].
     *
     * @param encryptedData The encrypted payload returned from [encrypt].
     * @param keyAlias      The alias of the key to use (must have DECRYPT purpose).
     * @return The decrypted plaintext bytes.
     * @throws OpenGuardCryptoException if decryption fails (wrong key, tampered data, etc).
     */
    fun decrypt(encryptedData: EncryptedData, keyAlias: String): ByteArray

    /**
     * Computes a cryptographic hash of [data].
     *
     * @param data      The data to hash.
     * @param algorithm The hash algorithm to use.
     * @return The raw hash bytes.
     */
    fun hash(data: ByteArray, algorithm: HashAlgorithm = HashAlgorithm.SHA256): ByteArray

    /**
     * Computes an HMAC over [data] using [keyAlias].
     */
    fun hmac(data: ByteArray, keyAlias: String, algorithm: HashAlgorithm = HashAlgorithm.SHA256): ByteArray

    /**
     * TOTP (Time-Based One-Time Password, RFC 6238) operations.
     */
    val totp: TotpApi
}

/**
 * Supported cipher specifications.
 */
enum class CipherSpec(
    val algorithm: String,
    val mode: String,
    val padding: String,
    val ivLength: Int,
    val tagLength: Int,
) {
    /** AES-256 in GCM mode with no padding. Recommended for authenticated encryption. */
    AES_256_GCM("AES", "GCM", "NoPadding", ivLength = 12, tagLength = 128),
    /** AES-256 in CBC mode with PKCS7 padding. Use when GCM is not available. */
    AES_256_CBC("AES", "CBC", "PKCS7Padding", ivLength = 16, tagLength = 0),
    /** ChaCha20-Poly1305. Alternative to AES-GCM. */
    CHACHA20_POLY1305("ChaCha20", "Poly1305", "NoPadding", ivLength = 12, tagLength = 128),
}

/**
 * Key specification for key generation.
 */
data class KeySpec(
    val algorithm: KeyAlgorithm,
    val keySize: Int,
    val hardwareBacked: Boolean = true,
    val requireUserAuthentication: Boolean = false,
    val userAuthenticationValiditySeconds: Int = -1,
    val purposes: Set<KeyPurpose>,
)

/**
 * Handle to a key stored in the secure key store.
 */
data class KeyHandle(
    val alias: String,
    val algorithm: KeyAlgorithm,
    val isHardwareBacked: Boolean,
)

/**
 * The result of an [CryptoApi.encrypt] operation.
 */
data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val tag: ByteArray,
    val cipherSpec: CipherSpec,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedData) return false
        return ciphertext.contentEquals(other.ciphertext) &&
            iv.contentEquals(other.iv) &&
            tag.contentEquals(other.tag) &&
            cipherSpec == other.cipherSpec
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + tag.contentHashCode()
        result = 31 * result + cipherSpec.hashCode()
        return result
    }
}

enum class KeyAlgorithm { AES, RSA, EC }

enum class KeyPurpose { ENCRYPT, DECRYPT, SIGN, VERIFY }

enum class HashAlgorithm(val jcaName: String) {
    SHA256("SHA-256"),
    SHA384("SHA-384"),
    SHA512("SHA-512"),
}

/**
 * API for generating and validating TOTP tokens (RFC 6238).
 */
interface TotpApi {
    /**
     * Generates a TOTP code using the provided [secret].
     *
     * @param secret    The shared secret (HMAC key), stored encrypted.
     * @param digits    Number of OTP digits (6 or 8).
     * @param timeStep  Time step in seconds (default: 30).
     * @param algorithm HMAC algorithm (SHA1 for compatibility, SHA256 preferred).
     */
    fun generate(
        secret: ByteArray,
        digits: Int = 6,
        timeStep: Long = 30,
        algorithm: TotpAlgorithm = TotpAlgorithm.SHA256,
    ): String

    /**
     * Validates a TOTP [code] against the provided [secret].
     * Allows one time-step of drift in both directions.
     */
    fun validate(
        code: String,
        secret: ByteArray,
        digits: Int = 6,
        timeStep: Long = 30,
        algorithm: TotpAlgorithm = TotpAlgorithm.SHA256,
    ): Boolean
}

enum class TotpAlgorithm { SHA1, SHA256, SHA512 }

/** Thrown when a cryptographic operation fails. */
class OpenGuardCryptoException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
