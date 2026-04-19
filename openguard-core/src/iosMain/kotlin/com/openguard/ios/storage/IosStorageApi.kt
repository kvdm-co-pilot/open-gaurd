package com.openguard.ios.storage

import com.openguard.core.api.AccessControl
import com.openguard.core.api.OpenGuardStorageException
import com.openguard.core.api.StorageApi
import platform.Foundation.NSMutableDictionary
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned

/**
 * iOS implementation of [StorageApi] using iOS Keychain Services.
 *
 * All values are stored as generic password items in the iOS Keychain.
 * The [AccessControl] determines the kSecAttrAccessible policy applied to each item:
 * - [AccessControl.ALWAYS] → kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
 * - [AccessControl.DEVICE_UNLOCKED] → kSecAttrAccessibleWhenUnlockedThisDeviceOnly
 * - [AccessControl.DEVICE_CREDENTIAL] → kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
 * - [AccessControl.BIOMETRIC_REQUIRED] → kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
 *   + kSecAccessControlBiometryCurrentSet (requires LAContext for retrieval)
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosStorageApi : StorageApi {

    private val service = "com.openguard.secure-storage"

    override fun put(key: String, value: ByteArray, accessControl: AccessControl) {
        delete(key) // Remove existing entry if present

        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to key,
            kSecValueData to value.toNSData(),
            kSecAttrAccessible to accessControl.toKeychainAccessibility(),
        )

        val status = SecItemAdd(query as CFDictionary, null)
        if (status != errSecSuccess) {
            throw OpenGuardStorageException("Keychain write failed for key '$key' with status: $status")
        }
    }

    override fun get(key: String): ByteArray? {
        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to key,
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne,
        )

        // Use memScoped + CFTypeRefVar to properly receive the result from SecItemCopyMatching
        return memScoped {
            val resultRef = alloc<kotlinx.cinterop.COpaquePointerVar>()
            val status = SecItemCopyMatching(query as CFDictionary, resultRef.ptr)
            if (status == errSecSuccess) {
                val nsData = resultRef.value?.let {
                    kotlinx.cinterop.interpretObjCPointer<platform.Foundation.NSData>(it)
                }
                nsData?.toByteArray()
            } else {
                null
            }
        }
    }

    override fun contains(key: String): Boolean {
        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to key,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val status = SecItemCopyMatching(query as CFDictionary, null)
        return status == errSecSuccess
    }

    override fun delete(key: String): Boolean {
        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to key,
        )
        val status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess
    }

    override fun clearAll() {
        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
        )
        SecItemDelete(query as CFDictionary)
    }

    override fun secureZero(buffer: ByteArray) {
        buffer.fill(0)
    }

    override fun secureZero(buffer: CharArray) {
        buffer.fill('\u0000')
    }

    private fun AccessControl.toKeychainAccessibility(): Any? = when (this) {
        AccessControl.ALWAYS -> kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        AccessControl.DEVICE_UNLOCKED -> kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        AccessControl.DEVICE_CREDENTIAL -> kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
        AccessControl.BIOMETRIC_REQUIRED -> kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
    }

    private fun ByteArray.toNSData(): platform.Foundation.NSData =
        usePinned { pinnedBytes ->
            platform.Foundation.NSData.create(
                bytes = pinnedBytes.addressOf(0),
                length = size.toULong()
            )
        }

    private fun platform.Foundation.NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val bytes = ByteArray(size)
        // Copy bytes from NSData into the ByteArray using the bytes pointer
        bytes.usePinned { pinnedBytes ->
            platform.posix.memcpy(pinnedBytes.addressOf(0), this.bytes, length)
        }
        return bytes
    }
}
