package com.jj.hotpotato

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val RATE = 48000
private const val FRAMES = 256   // bloco do misturador, cerca de 5 ms

private enum class Wave { SQUARE, TRIANGLE, SAWTOOTH, SINE }

private fun sfx(seconds: Float, render: FloatArray.() -> Unit) = FloatArray((seconds * RATE).toInt()).apply(render)

// Oscilador com envelope exponencial, somado ao que já está no buffer.
private fun FloatArray.tone(freq: Float, dur: Float, wave: Wave, vol: Float, delay: Float = 0f) {
    val start = (delay * RATE).toInt()
    val len = (dur * RATE).toInt()
    val decay = (0.0001f / vol).pow(1f / len)
    var gain = vol
    for (i in 0 until len) {
        val phase = (freq * i / RATE) % 1f
        val v = when (wave) {
            Wave.SQUARE -> if (phase < 0.5f) 1f else -1f
            Wave.TRIANGLE -> 4f * abs(phase - 0.5f) - 1f
            Wave.SAWTOOTH -> 2f * phase - 1f
            Wave.SINE -> sin(2f * PI.toFloat() * phase)
        }
        this[start + i] += v * gain
        gain *= decay
    }
}

// Ruído a decair, passado por um filtro passa-baixo (biquad), mais um tom grave.
private fun FloatArray.noiseBurst() {
    val w = 2f * PI.toFloat() * 900f / RATE
    val alpha = sin(w) / (2f * 1.12f)
    val cos = kotlin.math.cos(w)
    val a0 = 1f + alpha
    val b0 = (1f - cos) / 2f / a0
    val b1 = (1f - cos) / a0
    val a1 = -2f * cos / a0
    val a2 = (1f - alpha) / a0
    var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f
    for (i in indices) {
        val x = (Random.nextFloat() * 2f - 1f) * (1f - i.toFloat() / size).pow(2.5f)
        val y = b0 * x + b1 * x1 + b0 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x; y2 = y1; y1 = y
        this[i] += y * 0.9f
    }
}

// Sons gerados, sem ficheiros: calculados uma vez e tocados por um único misturador.
class Sound {
    private val blip = sfx(0.06f) { tone(520f, 0.06f, Wave.SQUARE, 0.06f) }
    private val ticks = Array(21) { step ->
        val heat = step / 20f
        sfx(0.035f) { tone(lerp(700f, 1300f, heat), 0.035f, Wave.SQUARE, lerp(0.05f, 0.12f, heat)) }
    }
    private val correct = sfx(0.3f) {
        tone(660f, 0.12f, Wave.TRIANGLE, 0.2f)
        tone(990f, 0.2f, Wave.TRIANGLE, 0.2f, 0.1f)
    }
    private val wrong = sfx(0.45f) {
        tone(220f, 0.18f, Wave.SAWTOOTH, 0.12f)
        tone(150f, 0.3f, Wave.SAWTOOTH, 0.12f, 0.15f)
    }
    private val win = sfx(0.7f) {
        listOf(523f, 659f, 784f, 1047f).forEachIndexed { i, f -> tone(f, 0.25f, Wave.TRIANGLE, 0.2f, i * 0.15f) }
    }
    private val boom = sfx(1.3f) {
        noiseBurst()
        tone(60f, 0.7f, Wave.SINE, 0.6f)
    }

    private val queue = ConcurrentLinkedQueue<FloatArray>()
    private var mixer: Thread? = null
    @Volatile private var running = false

    fun blip() = queue.add(blip)
    fun tick(heat: Float) = queue.add(ticks[(heat * 20f).roundToInt()])
    fun correct() = queue.add(correct)
    fun wrong() = queue.add(wrong)
    fun win() = queue.add(win)
    fun boom() = queue.add(boom)

    fun start() {
        if (mixer != null) return
        running = true
        mixer = thread(name = "som") { mix() }
    }

    fun stop() {
        running = false
        mixer?.join()
        mixer = null
        queue.clear()
    }

    private class Voice(val samples: FloatArray, var pos: Int = 0)

    private fun mix() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val format = AudioFormat.Builder()
            .setSampleRate(RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val track = AudioTrack.Builder()
            .setAudioFormat(format)
            .setAudioAttributes(attributes)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        track.play()

        val voices = ArrayList<Voice>()
        val block = FloatArray(FRAMES)
        while (running) {
            while (true) voices.add(Voice(queue.poll() ?: break))
            block.fill(0f)
            for (voice in voices) {
                val n = minOf(FRAMES, voice.samples.size - voice.pos)
                for (i in 0 until n) block[i] += voice.samples[voice.pos + i]
                voice.pos += n
            }
            voices.removeAll { it.pos >= it.samples.size }
            for (i in block.indices) block[i] = block[i].coerceIn(-1f, 1f)
            track.write(block, 0, FRAMES, AudioTrack.WRITE_BLOCKING)
        }
        track.stop()
        track.release()
    }
}
