package com.openguard.core.network

import com.openguard.core.api.PinningConfig

/**
 * Network security configuration.
 */
class NetworkConfig private constructor(
    val pinningConfig: PinningConfig,
    val minimumTlsVersion: TlsVersion,
    val enforceCertificateTransparency: Boolean,
    val detectMitm: Boolean,
) {
    companion object {
        fun default(): NetworkConfig = Builder().build()
    }

    class Builder {
        private var pinningConfig = PinningConfig.empty()
        var minimumTlsVersion: TlsVersion = TlsVersion.TLS_1_2
        var enforceCertificateTransparency: Boolean = false
        var detectMitm: Boolean = true

        fun certificatePinning(block: PinningConfig.Builder.() -> Unit) {
            pinningConfig = PinningConfig.Builder().apply(block).build()
        }

        internal fun build(): NetworkConfig = NetworkConfig(
            pinningConfig = pinningConfig,
            minimumTlsVersion = minimumTlsVersion,
            enforceCertificateTransparency = enforceCertificateTransparency,
            detectMitm = detectMitm,
        )
    }
}

/**
 * Minimum TLS version to accept.
 */
enum class TlsVersion(val value: String) {
    TLS_1_2("TLSv1.2"),
    TLS_1_3("TLSv1.3"),
}
