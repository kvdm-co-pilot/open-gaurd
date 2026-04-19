package com.openguard.core.fixtures

import com.openguard.core.api.AccessControl
import com.openguard.core.api.OpenGuardStorageException
import com.openguard.core.api.StorageApi

/**
 * A simple in-memory fake implementation of [StorageApi] for use in unit tests.
 *
 * Values are stored in a [MutableMap] without any actual encryption.
 * Access control policies are recorded but not enforced.
 */
class FakeStorageApi : StorageApi {

    private val storage = mutableMapOf<String, ByteArray>()

    /** Tracks the [AccessControl] policy used when each key was stored. */
    val accessPolicies = mutableMapOf<String, AccessControl>()

    /** If set to a non-null key, [get] will throw [OpenGuardStorageException] for that key. */
    var throwOnGet: String? = null

    override fun put(key: String, value: ByteArray, accessControl: AccessControl) {
        storage[key] = value.copyOf()
        accessPolicies[key] = accessControl
    }

    override fun get(key: String): ByteArray? {
        if (key == throwOnGet) {
            throw OpenGuardStorageException("Simulated storage failure for key: $key")
        }
        return storage[key]?.copyOf()
    }

    override fun contains(key: String): Boolean = storage.containsKey(key)

    override fun delete(key: String): Boolean {
        accessPolicies.remove(key)
        return storage.remove(key) != null
    }

    override fun clearAll() {
        storage.clear()
        accessPolicies.clear()
    }

    override fun secureZero(buffer: ByteArray) {
        buffer.fill(0)
    }

    override fun secureZero(buffer: CharArray) {
        buffer.fill('\u0000')
    }

    /** Returns the number of entries currently stored. */
    val size: Int get() = storage.size

    /** Returns true if the backing store is empty. */
    val isEmpty: Boolean get() = storage.isEmpty()
}
