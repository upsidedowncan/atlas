package atlas.messenger.session

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

private class IosSessionStore : SessionStore {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json { ignoreUnknownKeys = true }

    override fun save(username: String, password: String) {
        defaults.setObject(username as Any, forKey = "atlas.username")
        defaults.setObject(password as Any, forKey = "atlas.password")
        defaults.synchronize()
    }

    override fun clear() {
        defaults.removeObjectForKey("atlas.username")
        defaults.removeObjectForKey("atlas.password")
        defaults.removeObjectForKey("atlas.prefs")
        defaults.removeObjectForKey("atlas.miteChats")
        defaults.synchronize()
    }

    override fun load(): Pair<String, String>? {
        val username = defaults.stringForKey("atlas.username") ?: return null
        val password = defaults.stringForKey("atlas.password") ?: return null
        return username to password
    }

    override fun savePreferences(preferences: UiPreferences) {
        defaults.setObject(json.encodeToString(preferences) as Any, forKey = "atlas.prefs")
        defaults.synchronize()
    }

    override fun loadPreferences(): UiPreferences? {
        val raw = defaults.stringForKey("atlas.prefs") ?: return null
        return runCatching { json.decodeFromString<UiPreferences>(raw) }.getOrNull()
    }

    override fun saveMiteChats(chats: List<PersistedMiteChat>) {
        defaults.setObject(json.encodeToString(chats) as Any, forKey = "atlas.miteChats")
        defaults.synchronize()
    }

    override fun loadMiteChats(): List<PersistedMiteChat> {
        val raw = defaults.stringForKey("atlas.miteChats") ?: return emptyList()
        return runCatching { json.decodeFromString<List<PersistedMiteChat>>(raw) }.getOrDefault(emptyList())
    }
}

actual fun createSessionStore(): SessionStore = IosSessionStore()
