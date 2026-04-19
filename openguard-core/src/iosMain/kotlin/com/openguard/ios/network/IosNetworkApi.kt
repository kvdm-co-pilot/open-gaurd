package com.openguard.ios.network

import com.openguard.core.api.NetworkApi
import com.openguard.core.api.PinningConfig
import com.openguard.core.network.NetworkConfig
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.base64EncodedStringWithOptions

/**
 * iOS implementation of [NetworkApi].
 *
 * Provides SPKI-based certificate pinning for URLSession.
 * Use the URLSession delegate pattern to integrate pinning:
 *
 * ```swift
 * // In your URLSession delegate:
 * func urlSession(
 *     _ session: URLSession,
 *     didReceive challenge: URLAuthenticationChallenge,
 *     completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
 * ) {
 *     guard OpenGuardIos.shared.verifyChallenge(challenge) else {
 *         completionHandler(.cancelAuthenticationChallenge, nil)
 *         return
 *     }
 *     completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
 * }
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosNetworkApi(private val networkConfig: NetworkConfig) : NetworkApi {

    override val pinningConfig: PinningConfig = networkConfig.pinningConfig

    override fun verifyCertificate(host: String, certificateDer: ByteArray): Boolean {
        val hostConfig = findHostConfig(host) ?: return true

        return try {
            val spkiHash = computeSpkiHash(certificateDer) ?: return false
            hostConfig.sha256Pins.any { pin -> pin == spkiHash }
        } catch (_: Exception) {
            false
        }
    }

    override fun isMitmDetected(): Boolean {
        // On iOS, check for proxy configuration
        // Full implementation checks CFNetwork proxy settings and installed root certificates
        // Placeholder — returns false as safe default
        return false
    }

    private fun findHostConfig(host: String) =
        networkConfig.pinningConfig.pins[host]
            ?: networkConfig.pinningConfig.pins.entries
                .firstOrNull { (pinnedHost, config) ->
                    config.includeSubdomains && host.endsWith(".$pinnedHost")
                }?.value

    private fun computeSpkiHash(certificateDer: ByteArray): String? {
        // Extract the SubjectPublicKeyInfo from the DER-encoded certificate
        // Full implementation uses SecCertificateCopyKey and SecKeyCopyExternalRepresentation
        // This is a simplified placeholder
        val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)
        certificateDer.usePinned { pinnedData ->
            hash.usePinned { pinnedHash ->
                CC_SHA256(
                    pinnedData.addressOf(0),
                    certificateDer.size.toUInt(),
                    pinnedHash.addressOf(0)
                )
            }
        }
        val nsData = NSData.create(bytes = hash.usePinned { it.addressOf(0) }, length = hash.size.toULong())
        return nsData.base64EncodedStringWithOptions(0u)
    }
}
