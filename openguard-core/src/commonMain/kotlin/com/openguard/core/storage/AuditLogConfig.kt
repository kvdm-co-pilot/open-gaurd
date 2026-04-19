package com.openguard.core.storage

/**
 * Audit log configuration.
 */
class AuditLogConfig private constructor(
    val enabled: Boolean,
    val remoteEndpoint: String?,
    val localRetentionDays: Int,
    val signEvents: Boolean,
) {
    companion object {
        fun default(): AuditLogConfig = Builder().build()
    }

    class Builder {
        var enabled: Boolean = true
        var remoteEndpoint: String? = null
        var localRetentionDays: Int = 7
        var signEvents: Boolean = true

        internal fun build(): AuditLogConfig = AuditLogConfig(
            enabled = enabled,
            remoteEndpoint = remoteEndpoint,
            localRetentionDays = localRetentionDays,
            signEvents = signEvents,
        )
    }
}
