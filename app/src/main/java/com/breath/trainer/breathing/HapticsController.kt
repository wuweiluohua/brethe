package com.breath.trainer.breathing

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 简洁的触感反馈：在吸气/呼气阶段开始时震动一下。
 * 非常短的脉冲避免干扰。
 */
class HapticsController(context: Context) {

    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @Volatile var enabled: Boolean = true

    fun pulseStart(amplitude: Int = 80) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(60L, amplitude.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(60L)
        }
    }

    fun pulseSoft() {
        pulseStart(amplitude = 50)
    }
}
