package com.openguard.core.api

/**
 * API for secure storage backed by Android Keystore / iOS Keychain.
 *
 * All values are encrypted before storage. Keys in [AccessControl.BIOMETRIC_REQUIRED]
 * mode will prompt the user for biometric authentication on retrieval.
 */
interface StorageApi {

    /**
     * Stores [value] securely under [key].
     *
     * @param key           A unique identifier for this stored value.
     * @param value         The data to store (will be encrypted at rest).
     * @param accessControl How to control access to this value.
     */
    fun put(key: String, value: ByteArray, accessControl: AccessControl = AccessControl.DEVICE_CREDENTIAL)

    /**
     * Retrieves a stored value by [key].
     *
     * If [accessControl] was [AccessControl.BIOMETRIC_REQUIRED], this may trigger
     * a biometric prompt. Returns null if the key does not exist.
     *
     * @param key The identifier used when [put] was called.
     * @return The decrypted bytes, or null if no value exists for [key].
     * @throws OpenGuardStorageException if decryption or authentication fails.
     */
    fun get(key: String): ByteArray?

    /**
     * Returns true if a value exists for the given [key].
     */
    fun contains(key: String): Boolean

    /**
     * Deletes the value stored under [key].
     *
     * @return true if the value was deleted, false if it did not exist.
     */
    fun delete(key: String): Boolean

    /**
     * Deletes all values stored by OpenGuard.
     * Use with caution — this is irreversible.
     */
    fun clearAll()

    /**
     * Overwrites all bytes in [buffer] with zeros to prevent sensitive data
     * from lingering in memory after use.
     */
    fun secureZero(buffer: ByteArray)

    /**
     * Overwrites all chars in [buffer] with null characters.
     */
    fun secureZero(buffer: CharArray)
}

/**
 * Access control policy for stored values.
 */
enum class AccessControl {
    /**
     * Value can be retrieved at any time without user authentication.
     * Use only for non-sensitive data.
     */
    ALWAYS,

    /**
     * Value can be retrieved when the device is unlocked.
     */
    DEVICE_UNLOCKED,

    /**
     * Value can be retrieved after the user authenticates with device credential (PIN, pattern, password).
     */
    DEVICE_CREDENTIAL,

    /**
     * Value can only be retrieved after the user authenticates with biometrics.
     * If biometrics are not enrolled or the biometric set changes, the key is invalidated.
     */
    BIOMETRIC_REQUIRED,
}

/** Thrown when a secure storage operation fails. */
class OpenGuardStorageException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
