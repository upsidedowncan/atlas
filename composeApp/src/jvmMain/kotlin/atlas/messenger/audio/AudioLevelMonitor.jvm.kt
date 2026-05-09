package atlas.messenger.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class JvmAudioLevelMonitor : AudioLevelMonitor {
    private var line: TargetDataLine? = null
    private var buffer = ByteArray(2048)
    private var running = false
    @Volatile
    private var lastLevel = 0f

    override fun start() {
        if (running) return
        try {
            val format = AudioFormat(44100f, 16, 1, true, true)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) return
            val tl = AudioSystem.getLine(info) as TargetDataLine
            tl.open(format)
            tl.start()
            line = tl
            running = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        running = false
        try {
            line?.stop()
            line?.close()
        } catch (_: Exception) {
        }
        line = null
    }

    override fun currentLevel(): Float {
        val l = line ?: return lastLevel
        if (!running) return lastLevel
        val available = l.available()
        if (available <= 0) return lastLevel
        val toRead = minOf(available, buffer.size)
        val read = l.read(buffer, 0, toRead)
        if (read <= 0) return lastLevel

        var sum = 0.0
        var count = 0
        for (i in 0 until read - 1 step 2) {
            val high = buffer[i].toInt()
            val low = buffer[i + 1].toInt()
            val sample = ((high shl 8) or (low and 0xFF)).toShort().toInt()
            sum += sample * sample
            count++
        }
        if (count == 0) return lastLevel

        val rms = kotlin.math.sqrt(sum / count)
        val db = 20.0 * kotlin.math.log10(rms / 32768.0 + 1e-10)
        val level = ((db + 60.0) / 50.0).coerceIn(0.0, 1.0).toFloat()
        lastLevel = level
        return level
    }
}

actual fun createAudioLevelMonitor(): AudioLevelMonitor = JvmAudioLevelMonitor()
