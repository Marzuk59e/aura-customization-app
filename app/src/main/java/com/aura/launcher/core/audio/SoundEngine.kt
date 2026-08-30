package com.aura.launcher.core.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Lightweight sound + haptic feedback engine.
 *
 * Mirrors `AudioEngine.playHapticSound(type)` from the web build
 * (js/core/audio-engine.js) — every style-preset tap, mood-chip tap,
 * or category tap on the web plays a short chime plus a haptic tick.
 * The Android app previously had no equivalent at all, so interactions
 * felt flat/unfinished compared to the web reference.
 *
 * No audio asset files are required: short tones are synthesized with
 * ToneGenerator, so this works immediately without adding .mp3/.wav
 * resources to the project.
 */
object SoundEngine {

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    @Volatile
    var isMuted: Boolean = false
        private set

    /** Call once (e.g. from Application.onCreate or the first screen) before use. */
    fun init(context: Context) {
        if (toneGenerator == null) {
            toneGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_SYSTEM, /* volume 0-100 */ 55)
            }.getOrNull()
        }
        if (vibrator == null) {
            vibrator = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.applicationContext
                        .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            }.getOrNull()
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun toggleMuted(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    /**
     * Plays a short chime + haptic tick.
     * [type] mirrors the web's sound "flavors": "crystal" (light, for
     * simple taps/selections) and "cyber" (slightly deeper, for
     * preset/mood/theme changes).
     */
    fun playHapticSound(type: String = "crystal") {
        if (isMuted) return

        val tone = when (type) {
            "cyber" -> ToneGenerator.TONE_CDMA_PIP
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        runCatching { toneGenerator?.startTone(tone, 90) }

        val vibrateMs = if (type == "cyber") 25L else 12L
        runCatching {
            val v = vibrator ?: return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(vibrateMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(vibrateMs)
            }
        }
    }
}
