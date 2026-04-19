package com.openguard.sample

import android.app.Application
import android.util.Log
import com.openguard.android.OpenGuardAndroid
import com.openguard.core.OpenGuardConfig
import com.openguard.core.detection.ThreatReaction
import com.openguard.core.detection.ThreatSeverity

/**
 * Sample application demonstrating OpenGuard SDK integration.
 *
 * In your production app, configure OpenGuard with your actual API endpoints
 * and certificate pins.
 */
class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initOpenGuard()
    }

    private fun initOpenGuard() {
        val config = OpenGuardConfig {
            detection {
                rootJailbreakDetection {
                    enabled = true
                    reaction = ThreatReaction.BLOCK_AND_REPORT
                }
                debuggerDetection {
                    enabled = true
                    // In debug builds you may want to disable this:
                    reaction = if (BuildConfig.DEBUG) ThreatReaction.LOG_ONLY
                               else ThreatReaction.BLOCK_AND_REPORT
                }
                hookDetection {
                    enabled = true
                    reaction = ThreatReaction.BLOCK_AND_REPORT
                }
                emulatorSimulatorDetection {
                    enabled = true
                    reaction = ThreatReaction.WARN_AND_REPORT
                }
                tamperDetection {
                    enabled = true
                    // Set your expected signing certificate hash here:
                    // expectedSignatureHash = "YOUR_CERT_SHA256_HASH_BASE64"
                    reaction = ThreatReaction.BLOCK_AND_REPORT
                }
                screenshotPrevention {
                    enabled = true
                }
            }

            network {
                // Replace with your actual API endpoint and certificate pins.
                // Generate pins with:
                //   openssl s_client -connect api.example.com:443 | \
                //   openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | \
                //   openssl dgst -sha256 -binary | base64
                certificatePinning {
                    // Example — replace with your real pins:
                    // pin("api.example.com") {
                    //     addSha256Pin("AAAA...primary_pin...AAAA=")
                    //     addSha256Pin("BBBB...backup_pin...BBBB=")
                    //     includeSubdomains = false
                    // }
                }
                minimumTlsVersion = com.openguard.core.network.TlsVersion.TLS_1_2
                detectMitm = true
            }

            auditLog {
                enabled = true
                // Replace with your audit log endpoint:
                // remoteEndpoint = "https://audit.example.com/events"
                localRetentionDays = 7
                signEvents = true
            }

            threatResponse {
                onCriticalThreat { event ->
                    Log.e("OpenGuard", "CRITICAL THREAT: $event")
                    // In production: terminate session, show security dialog
                }
                onHighThreat { event ->
                    Log.w("OpenGuard", "HIGH THREAT: $event")
                    // In production: restrict payment features, report to server
                }
                onMediumThreat { event ->
                    Log.i("OpenGuard", "MEDIUM THREAT: $event")
                    // In production: warn user, report to server
                }
            }
        }

        OpenGuardAndroid.initialize(
            context = this,
            config = config,
        )
    }
}
