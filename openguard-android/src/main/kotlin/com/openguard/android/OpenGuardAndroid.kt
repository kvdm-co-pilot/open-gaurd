package com.openguard.android

import android.content.Context
import android.view.Window
import android.view.WindowManager
import com.openguard.core.OpenGuard
import com.openguard.core.OpenGuardConfig

/**
 * Android-specific OpenGuard extensions.
 *
 * Provides Android Context-aware initialization and utility functions.
 */
object OpenGuardAndroid {

    private var _applicationContext: Context? = null

    /**
     * The application context stored at initialization time.
     * Accessible after [initialize] is called.
     */
    val applicationContext: Context
        get() = _applicationContext
            ?: error("OpenGuardAndroid not initialized. Call OpenGuardAndroid.initialize() first.")

    /**
     * Initializes OpenGuard with the Android application context.
     *
     * Call this from `Application.onCreate()` before using any OpenGuard APIs.
     *
     * @param context The application context (must be application context, not activity context).
     * @param config  The SDK configuration.
     */
    fun initialize(context: Context, config: OpenGuardConfig = OpenGuardConfig.secureDefaults()) {
        _applicationContext = context.applicationContext
        OpenGuard.initialize(config = config)
    }

    /**
     * Applies screen security to the given [Window] by setting [WindowManager.LayoutParams.FLAG_SECURE].
     *
     * This prevents screenshots and screen recording of the window, and blanks the app
     * in the recent apps switcher.
     *
     * Call this in `Activity.onCreate()` before `setContentView()`:
     * ```kotlin
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     super.onCreate(savedInstanceState)
     *     OpenGuardAndroid.secureWindow(window)
     *     setContentView(R.layout.activity_main)
     * }
     * ```
     *
     * @param window The activity window to secure.
     */
    fun secureWindow(window: Window) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }
}
