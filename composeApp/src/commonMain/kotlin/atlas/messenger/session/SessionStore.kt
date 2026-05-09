package atlas.messenger.session

interface SessionStore {
    fun save(username: String, password: String)
    fun clear()
    fun load(): Pair<String, String>?
}

expect fun createSessionStore(): SessionStore
