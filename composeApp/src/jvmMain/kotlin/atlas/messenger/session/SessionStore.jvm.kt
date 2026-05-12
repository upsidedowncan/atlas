package atlas.messenger.session

import java.io.File
import java.util.Properties

private class JvmSessionStore : SessionStore {
    private val userId = System.getProperty("atlas.user", "0")
    private val dir = File(System.getProperty("user.home"), ".atlas/user${userId}").also { it.mkdirs() }
    private val file = File(dir, "session.properties")

    override fun save(username: String, password: String) {
        val props = Properties()
        props["username"] = username
        props["password"] = password
        file.writer().use { props.store(it, null) }
    }

    override fun clear() {
        file.delete()
    }

    override fun load(): Pair<String, String>? {
        if (!file.exists()) return null
        return runCatching {
            val props = Properties()
            file.reader().use { props.load(it) }
            val username = props.getProperty("username") ?: return@runCatching null
            val password = props.getProperty("password") ?: return@runCatching null
            username to password
        }.getOrNull()
    }

    override fun savePreferences(preferences: UiPreferences) {
        val props = Properties()
        if (file.exists()) {
            runCatching { file.reader().use { props.load(it) } }
        }
        props["textScale"] = preferences.textScale.toString()
        props["accentColor"] = preferences.accentColor.toString()
        props["colorPreset"] = preferences.colorPreset
        props["contrast"] = preferences.contrast.toString()
        props["serverUrl"] = preferences.serverUrl
        file.writer().use { props.store(it, null) }
    }

    override fun loadPreferences(): UiPreferences? {
        if (!file.exists()) return null
        return runCatching {
            val props = Properties()
            file.reader().use { props.load(it) }
            UiPreferences(
                textScale = props.getProperty("textScale")?.toFloatOrNull() ?: 1.0f,
                accentColor = props.getProperty("accentColor")?.toIntOrNull() ?: 0xFF2196F3.toInt(),
                colorPreset = props.getProperty("colorPreset") ?: "DEFAULT",
                contrast = props.getProperty("contrast")?.toFloatOrNull() ?: 1.0f,
                serverUrl = props.getProperty("serverUrl") ?: "ws://127.0.0.1:8080",
            )
        }.getOrNull()
    }
}

actual fun createSessionStore(): SessionStore = JvmSessionStore()
