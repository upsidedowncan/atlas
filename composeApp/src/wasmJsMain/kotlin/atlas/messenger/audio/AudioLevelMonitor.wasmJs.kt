package atlas.messenger.audio

private object NoOpAudioLevelMonitor : AudioLevelMonitor {
    override fun start() {}
    override fun stop() {}
    override fun currentLevel(): Float = 0f
}

actual fun createAudioLevelMonitor(): AudioLevelMonitor = NoOpAudioLevelMonitor
