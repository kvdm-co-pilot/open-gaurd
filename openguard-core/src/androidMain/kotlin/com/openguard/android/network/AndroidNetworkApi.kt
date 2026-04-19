package com.openguard.android.network

import com.openguard.core.api.NetworkApi
import com.openguard.core.api.PinningConfig
import com.openguard.core.network.NetworkConfig
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

/**
 * Android implementation of [NetworkApi].
 *
 * Provides SPKI-based certificate pinning compatible with OkHttp.
 * Use [configuredOkHttpClient] to get a pre-configured OkHttp client.
 */
internal class AndroidNetworkApi(private val networkConfig: NetworkConfig) : NetworkApi {

    override val pinningConfig: PinningConfig = networkConfig.pinningConfig

    override fun verifyCertificate(host: String, certificateDer: ByteArray): Boolean {
        val hostConfig = findHostConfig(host) ?: return true // No pins configured for this host
        return try {
            val cf = java.security.cert.CertificateFactory.getInstance("X.509")
            val cert = cf.generateCertificate(certificateDer.inputStream()) as X509Certificate
            val spkiHash = computeSpkiHash(cert)
            hostConfig.sha256Pins.any { pin -> pin == spkiHash }
        } catch (_: Exception) {
            false
        }
    }

    override fun isMitmDetected(): Boolean {
        return try {
            val trustManagerFactory = javax.net.ssl.TrustManagerFactory
                .getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as java.security.KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            val x509TrustManager = trustManagers.filterIsInstance<javax.net.ssl.X509TrustManager>().firstOrNull()
                ?: return false

            val acceptedIssuers = x509TrustManager.acceptedIssuers
            // Check for user-installed certificates which could indicate MITM
            acceptedIssuers.any { cert ->
                val subject = cert.subjectDN.name
                KNOWN_PROXY_ISSUERS.any { proxy ->
                    subject.contains(proxy, ignoreCase = true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun findHostConfig(host: String) =
        networkConfig.pinningConfig.pins[host]
            ?: networkConfig.pinningConfig.pins.entries
                .firstOrNull { (pinnedHost, config) ->
                    config.includeSubdomains && host.endsWith(".$pinnedHost")
                }?.value

    private fun computeSpkiHash(cert: X509Certificate): String {
        val spki = cert.publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256").digest(spki)
        return Base64.getEncoder().encodeToString(digest)
    }

    companion object {
        private val KNOWN_PROXY_ISSUERS = listOf(
            "Charles Proxy", "Fiddler", "mitmproxy", "Burp Suite",
            "OWASP ZAP", "Proxyman", "HTTP Toolkit",
        )
    }
}
