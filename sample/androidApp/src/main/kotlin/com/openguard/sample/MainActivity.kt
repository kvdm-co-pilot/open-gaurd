package com.openguard.sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.openguard.android.OpenGuardAndroid
import com.openguard.core.OpenGuard
import com.openguard.core.detection.ThreatSeverity
import kotlinx.coroutines.launch

/**
 * Sample main activity demonstrating OpenGuard integration.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply FLAG_SECURE to prevent screenshots on this screen
        OpenGuardAndroid.secureWindow(window)

        setContentView(R.layout.activity_main)
        val statusText = findViewById<TextView>(R.id.status_text)

        lifecycleScope.launch {
            val result = OpenGuard.detection.checkAll()

            val statusMessage = when (result.highestSeverity) {
                ThreatSeverity.CRITICAL -> "❌ CRITICAL threat detected — session blocked"
                ThreatSeverity.HIGH -> "⚠️ HIGH threat detected — features restricted"
                ThreatSeverity.MEDIUM -> "⚠️ MEDIUM threat — monitoring active"
                ThreatSeverity.LOW, ThreatSeverity.INFO -> "ℹ️ Low-level anomaly detected"
                ThreatSeverity.NONE -> "✅ Device environment is secure"
            }

            val details = if (result.threats.isNotEmpty()) {
                result.threats.joinToString("\n") { threat ->
                    "• ${threat.type} (${threat.severity})"
                }
            } else {
                "No threats detected"
            }

            statusText.text = "$statusMessage\n\n$details"
        }
    }
}
