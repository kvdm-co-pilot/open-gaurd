package com.openguard.core

import com.openguard.core.api.DetectionApi
import com.openguard.core.api.CryptoApi
import com.openguard.core.api.NetworkApi
import com.openguard.core.api.StorageApi
import com.openguard.android.detection.AndroidDetectionApi
import com.openguard.android.crypto.AndroidCryptoApi
import com.openguard.android.network.AndroidNetworkApi
import com.openguard.android.storage.AndroidStorageApi

internal actual fun createDetectionApi(config: OpenGuardConfig): DetectionApi =
    AndroidDetectionApi(config)

internal actual fun createCryptoApi(config: OpenGuardConfig): CryptoApi =
    AndroidCryptoApi()

internal actual fun createNetworkApi(config: OpenGuardConfig): NetworkApi =
    AndroidNetworkApi(config.network)

internal actual fun createStorageApi(config: OpenGuardConfig): StorageApi =
    AndroidStorageApi()
