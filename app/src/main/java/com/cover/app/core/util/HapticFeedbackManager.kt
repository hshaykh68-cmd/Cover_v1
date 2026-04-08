package com.cover.app.core.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Light click feedback
     */
    fun lightClick() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(10)
            }
        }
    }

    /**
     * Heavy click feedback
     */
    fun heavyClick() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(20)
            }
        }
    }

    /**
     * Double click feedback
     */
    fun doubleClick() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 10, 50, 10), -1)
            }
        }
    }

    /**
     * Success feedback
     */
    fun success() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), intArrayOf(0, 100, 0, 100), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 30, 50, 30), -1)
            }
        }
    }

    /**
     * Error feedback
     */
    fun error() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 30, 50), intArrayOf(0, 150, 0, 100), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 50, 30, 50), -1)
            }
        }
    }

    /**
     * Pattern unlock feedback
     */
    fun patternTick() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(5, 50))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(5)
            }
        }
    }

    /**
     * Lockout warning feedback
     */
    fun warning() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), intArrayOf(0, 200, 0, 200), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 100, 100, 100), -1)
            }
        }
    }
}
