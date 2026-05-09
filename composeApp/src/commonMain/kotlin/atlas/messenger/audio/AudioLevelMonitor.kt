package atlas.messenger.audio

interface AudioLevelMonitor {
    fun start()
    fun stop()
    fun currentLevel(): Float
}

expect fun createAudioLevelMonitor(): AudioLevelMonitor
