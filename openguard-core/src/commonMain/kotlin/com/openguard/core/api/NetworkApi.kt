package com.openguard.core.api

/**
 * API for network security — certificate pinning and TLS configuration.
 */
interface NetworkApi {

    /**
     * Returns the current pinning configuration.
     */
    val pinningConfig: PinningConfig

    /**
     * Verifies that a certificate (DER-encoded) matches the configured pins for [host].
     *
     * @param host            The hostname being connected to.
     * @param certificateDer  The DER-encoded certificate bytes from the TLS handshake.
     * @return true if the certificate matches a configured pin, false otherwise.
     */
    fun verifyCertificate(host: String, certificateDer: ByteArray): Boolean

    /**
     * Returns whether a proxy or MITM tool appears to be intercepting network traffic.
     * Detects installed proxy certificates in the trust store.
     */
    fun isMitmDetected(): Boolean
}

/**
 * Certificate pinning configuration.
 */
data class PinningConfig(
    val pins: Map<String, HostPinConfig>,
    val onPinFailure: PinFailureAction,
) {
    companion object {
        fun empty() = PinningConfig(pins = emptyMap(), onPinFailure = PinFailureAction.BLOCK_AND_REPORT)
    }

    class Builder {
        private val pins = mutableMapOf<String, HostPinConfig>()
        var onPinFailure: PinFailureAction = PinFailureAction.BLOCK_AND_REPORT

        fun pin(host: String, block: HostPinConfig.Builder.() -> Unit) {
            pins[host] = HostPinConfig.Builder(host).apply(block).build()
        }

        internal fun build() = PinningConfig(pins = pins.toMap(), onPinFailure = onPinFailure)
    }
}

/**
 * Pin configuration for a specific host.
 */
data class HostPinConfig(
    val host: String,
    val sha256Pins: List<String>,
    val includeSubdomains: Boolean,
) {
    class Builder(private val host: String) {
        private val sha256Pins = mutableListOf<String>()
        var includeSubdomains: Boolean = false

        /**
         * Adds a SHA-256 SPKI pin (Base64-encoded).
         *
         * Generate a pin for your certificate with:
         * ```
         * openssl s_client -connect api.example.com:443 | \
         *   openssl x509 -pubkey -noout | \
         *   openssl pkey -pubin -outform der | \
         *   openssl dgst -sha256 -binary | \
         *   base64
         * ```
         *
         * Always add at least one backup pin!
         */
        fun addSha256Pin(pin: String) {
            require(pin.isNotBlank()) { "Pin must not be blank." }
            sha256Pins.add(pin)
        }

        internal fun build(): HostPinConfig {
            require(sha256Pins.size >= 1) { "At least one pin must be configured for host: $host" }
            return HostPinConfig(
                host = host,
                sha256Pins = sha256Pins.toList(),
                includeSubdomains = includeSubdomains,
            )
        }
    }
}

enum class PinFailureAction {
    /** Block the connection and report the event. */
    BLOCK_AND_REPORT,
    /** Allow the connection but report the event. */
    REPORT_ONLY,
}
