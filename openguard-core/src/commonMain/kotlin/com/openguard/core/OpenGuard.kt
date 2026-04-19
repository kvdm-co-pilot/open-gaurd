package com.openguard.core

import com.openguard.core.api.DetectionApi
import com.openguard.core.api.CryptoApi
import com.openguard.core.api.NetworkApi
import com.openguard.core.api.StorageApi
import com.openguard.core.detection.DetectionEngine
import com.openguard.core.detection.ThreatEvent

/**
 * Main entry point for the OpenGuard RASP SDK.
 *
 * Initialize once at application startup before any sensitive operations:
 * ```kotlin
 * // Android (Application.onCreate)
 * OpenGuard.initialize(context = this, config = OpenGuardConfig { ... })
 *
 * // iOS (via iosMain actual)
 * OpenGuard.initialize(config = OpenGuardConfig { ... })
 * ```
 *
 * All APIs are then accessible via the [OpenGuard] singleton:
 * ```kotlin
 * val result = OpenGuard.detection.checkAll()
 * OpenGuard.secureStorage.put("token", tokenBytes)
 * ```
 */
object OpenGuard {

    private var _isInitialized = false
    private lateinit var _config: OpenGuardConfig
    private lateinit var _detection: DetectionApi
    private lateinit var _crypto: CryptoApi
    private lateinit var _network: NetworkApi
    private lateinit var _secureStorage: StorageApi
    private lateinit var _engine: DetectionEngine

    /**
     * Whether the SDK has been initialized.
     */
    val isInitialized: Boolean get() = _isInitialized

    /**
     * Returns the current SDK configuration. Throws if not initialized.
     */
    val config: OpenGuardConfig
        get() {
            checkInitialized()
            return _config
        }

    /**
     * Detection API for running RASP checks.
     */
    val detection: DetectionApi
        get() {
            checkInitialized()
            return _detection
        }

    /**
     * Cryptography API for encryption, hashing, and key management.
     */
    val crypto: CryptoApi
        get() {
            checkInitialized()
            return _crypto
        }

    /**
     * Network security API for certificate pinning and TLS configuration.
     */
    val network: NetworkApi
        get() {
            checkInitialized()
            return _network
        }

    /**
     * Secure storage API backed by Android Keystore / iOS Keychain.
     */
    val secureStorage: StorageApi
        get() {
            checkInitialized()
            return _secureStorage
        }

    /**
     * Current SDK version.
     */
    const val VERSION = "0.1.0-alpha"

    /**
     * Initializes OpenGuard with the given platform context and configuration.
     * Must be called before any other SDK method.
     *
     * This function is `expect`ed — the actual implementation is platform-specific.
     *
     * @param config The SDK configuration.
     */
    fun initialize(config: OpenGuardConfig = OpenGuardConfig.secureDefaults()) {
        if (_isInitialized) return
        _config = config
        _detection = createDetectionApi(config)
        _crypto = createCryptoApi(config)
        _network = createNetworkApi(config)
        _secureStorage = createStorageApi(config)
        _engine = DetectionEngine(
            detectionApi = _detection,
            config = config.detection,
            onThreat = { event -> handleThreat(event) }
        )
        _isInitialized = true
        _engine.start()
    }

    /**
     * Shuts down the OpenGuard SDK and releases resources.
     * Call this when the application is terminating.
     */
    fun shutdown() {
        if (!_isInitialized) return
        _engine.stop()
        _isInitialized = false
    }

    private fun handleThreat(event: ThreatEvent) {
        _config.threatResponseCallbacks.dispatch(event)
    }

    private fun checkInitialized() {
        check(_isInitialized) {
            "OpenGuard is not initialized. Call OpenGuard.initialize() first."
        }
    }
}

/**
 * Platform-specific factory functions. Implemented via `expect`/`actual`.
 */
internal expect fun createDetectionApi(config: OpenGuardConfig): DetectionApi
internal expect fun createCryptoApi(config: OpenGuardConfig): CryptoApi
internal expect fun createNetworkApi(config: OpenGuardConfig): NetworkApi
internal expect fun createStorageApi(config: OpenGuardConfig): StorageApi
