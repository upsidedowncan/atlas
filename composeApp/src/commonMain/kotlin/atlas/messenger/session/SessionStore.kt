package atlas.messenger.session

interface SessionStore {
    fun save(username: String, password: String)
    fun clear()
    fun load(): Pair<String, String>?
    fun savePreferences(preferences: UiPreferences)
    fun loadPreferences(): UiPreferences?
}

data class UiPreferences(
    val textScale: Float = 1.0f,
    val accentColor: Int = 0xFF2196F3.toInt(),
    val colorPreset: String = "DEFAULT",
    val contrast: Float = 1.0f,
    val serverUrl: String = "ws://127.0.0.1:8080",
)

expect fun createSessionStore(): SessionStore
