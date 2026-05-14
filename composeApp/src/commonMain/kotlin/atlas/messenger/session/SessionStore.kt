package atlas.messenger.session

import kotlinx.serialization.Serializable

interface SessionStore {
    fun save(username: String, password: String)
    fun clear()
    fun load(): Pair<String, String>?
    fun savePreferences(preferences: UiPreferences)
    fun loadPreferences(): UiPreferences?
    fun saveMiteChats(chats: List<PersistedMiteChat>)
    fun loadMiteChats(): List<PersistedMiteChat>
}

data class UiPreferences(
    val textScale: Float = 1.0f,
    val accentColor: Int = 0xFF2196F3.toInt(),
    val colorPreset: String = "DEFAULT",
    val contrast: Float = 1.0f,
    val serverUrl: String = "ws://127.0.0.1:8080",
)

@Serializable
data class PersistedMiteChat(
    val id: String,
    val title: String,
    val updatedAtMs: Long,
    val messages: List<PersistedMiteMessage>,
)

@Serializable
data class PersistedMiteMessage(
    val id: String,
    val text: String,
    val reasoning: String = "",
    val isOwn: Boolean,
    val timestampMs: Long,
)

expect fun createSessionStore(): SessionStore
