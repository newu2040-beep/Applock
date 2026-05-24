package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.sin

object ProceduralSoundGenerator {

    private var activeTrack: AudioTrack? = null
    private var isPlaying = false

    /**
     * Play manually chosen wrong attempt sound profile
     */
    fun playSound(context: Context, soundType: String, tts: TextToSpeech?, customText: String = "") {
        stopSound()
        
        when (soundType) {
            "Siren Alarm Threat Loop" -> {
                generateToneLoop { sampleIndex, sampleRate ->
                    val hz = 800.0 + 300.0 * sin(2.0 * Math.PI * (sampleIndex % sampleRate) / sampleRate * 2.0)
                    sin(2.0 * Math.PI * sampleIndex * hz / sampleRate)
                }
            }
            "AI Vocal Lockdown Prompt" -> {
                val phrase = if (customText.isNotEmpty()) customText else "System lockdown initiated. Invalid security biometric identification."
                tts?.setPitch(0.75f)
                tts?.setSpeechRate(0.95f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "synth_fail_id")
                } else {
                    @Suppress("DEPRECATION")
                    tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null)
                }
            }
            "Short Sci-Fi Beep Error" -> {
                generateToneSingle(0.18f) { sampleIndex, sampleRate ->
                    val block = (sampleIndex / (sampleRate * 0.08)).toInt()
                    if (block == 0 || block == 2) {
                        sin(2.0 * Math.PI * sampleIndex * 1500.0 / sampleRate)
                    } else {
                        0.0
                    }
                }
            }
            "Retro Arcade Buzzer" -> {
                generateToneSingle(0.35f) { sampleIndex, sampleRate ->
                    // Square wave at low 110Hz for buzz effect
                    val phase = (sampleIndex * 110.0 / sampleRate) % 1.0
                    if (phase < 0.5) 0.35 else -0.35
                }
            }
            "Dramatic Nuclear Alert" -> {
                generateToneLoop { sampleIndex, sampleRate ->
                    val secondBlock = sampleIndex / (sampleRate * 0.4)
                    if (secondBlock.toInt() % 2 == 0) {
                        sin(2.0 * Math.PI * sampleIndex * 440.0 / sampleRate)
                    } else {
                        0.0
                    }
                }
            }
            "High-Frequency Sonic Sweep" -> {
                generateToneSingle(0.5f) { sampleIndex, sampleRate ->
                    val progress = sampleIndex.toDouble() / (sampleRate * 0.5)
                    val hz = 1000.0 + progress * 2000.0
                    sin(2.0 * Math.PI * sampleIndex * hz / sampleRate)
                }
            }
            else -> {
                // Fallback basic bip
                generateToneSingle(0.15f) { sampleIndex, sampleRate ->
                    sin(2.0 * Math.PI * sampleIndex * 700.0 / sampleRate)
                }
            }
        }
    }

    fun stopSound() {
        isPlaying = false
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        activeTrack = null
    }

    private fun generateToneSingle(durationSec: Float, formula: (sampleIndex: Int, sampleRate: Int) -> Double) {
        val sampleRate = 22050
        val numSamples = (durationSec * sampleRate).toInt()
        val generatedSnd = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val sample = formula(i, sampleRate)
            generatedSnd[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        try {
            val bufSize = numSamples * 2
            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                    AudioFormat.Builder()
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .build(),
                    bufSize,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_ALARM,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize,
                    AudioTrack.MODE_STATIC
                )
            }

            track.write(generatedSnd, 0, numSamples)
            track.play()
            activeTrack = track
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateToneLoop(formula: (sampleIndex: Int, sampleRate: Int) -> Double) {
        isPlaying = true
        Thread {
            val sampleRate = 22050
            // 1.5 seconds ring buffer loop
            val bufferSize = sampleRate * 3
            val soundBuffer = ShortArray(bufferSize)

            for (i in 0 until bufferSize) {
                val sample = formula(i, sampleRate)
                soundBuffer[i] = (sample * 0.7 * Short.MAX_VALUE).toInt().toShort()
            }

            try {
                val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioTrack(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                        AudioFormat.Builder()
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .build(),
                        bufferSize * 2,
                        AudioTrack.MODE_STATIC,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_ALARM,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize * 2,
                        AudioTrack.MODE_STATIC
                    )
                }

                track.write(soundBuffer, 0, bufferSize)
                track.setLoopPoints(0, bufferSize, -1)
                track.play()
                activeTrack = track

                // Let it loop under isPlaying
                while (isPlaying) {
                    Thread.sleep(100)
                }
                
                track.stop()
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
