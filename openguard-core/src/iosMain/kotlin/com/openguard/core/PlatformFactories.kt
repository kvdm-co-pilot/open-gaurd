package com.openguard.core

import com.openguard.core.api.DetectionApi
import com.openguard.core.api.CryptoApi
import com.openguard.core.api.NetworkApi
import com.openguard.core.api.StorageApi
import com.openguard.ios.detection.IosDetectionApi
import com.openguard.ios.crypto.IosCryptoApi
import com.openguard.ios.network.IosNetworkApi
import com.openguard.ios.storage.IosStorageApi

internal actual fun createDetectionApi(config: OpenGuardConfig): DetectionApi =
    IosDetectionApi(config)

internal actual fun createCryptoApi(config: OpenGuardConfig): CryptoApi =
    IosCryptoApi()

internal actual fun createNetworkApi(config: OpenGuardConfig): NetworkApi =
    IosNetworkApi(config.network)

internal actual fun createStorageApi(config: OpenGuardConfig): StorageApi =
    IosStorageApi()
