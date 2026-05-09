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
}

actual fun createSessionStore(): SessionStore = JvmSessionStore()
