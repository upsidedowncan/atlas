package atlas.messenger.session

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

private class AndroidSessionStore(private val context: Context) : SessionStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("atlas_session", Context.MODE_PRIVATE)

    override fun save(username: String, password: String) {
        prefs.edit()
            .putString("username", username)
            .putString("password", password)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun load(): Pair<String, String>? {
        val username = prefs.getString("username", null) ?: return null
        val password = prefs.getString("password", null) ?: return null
        return username to password
    }

    override fun savePreferences(preferences: UiPreferences) {
        prefs.edit()
            .putFloat("textScale", preferences.textScale)
            .putInt("accentColor", preferences.accentColor)
            .putString("colorPreset", preferences.colorPreset)
            .putFloat("contrast", preferences.contrast)
            .putString("serverUrl", preferences.serverUrl)
            .apply()
    }

    override fun loadPreferences(): UiPreferences? {
        if (!prefs.contains("textScale") && !prefs.contains("accentColor") && !prefs.contains("serverUrl")) return null
        return UiPreferences(
            textScale = prefs.getFloat("textScale", 1.0f),
            accentColor = prefs.getInt("accentColor", 0xFF2196F3.toInt()),
            colorPreset = prefs.getString("colorPreset", "DEFAULT") ?: "DEFAULT",
            contrast = prefs.getFloat("contrast", 1.0f),
            serverUrl = prefs.getString("serverUrl", "ws://127.0.0.1:8080") ?: "ws://127.0.0.1:8080",
        )
    }
}

private var androidContext: Context? = null

fun initSessionStore(context: Context) {
    androidContext = context.applicationContext
}

actual fun createSessionStore(): SessionStore {
    val ctx = androidContext ?: (android.app.Application::class.java.let {
        try {
            it.getDeclaredField("INSTANCE").get(null) as? Context
        } catch (e: Exception) {
            null
        }
    }) ?: throw IllegalStateException("Call initSessionStore(context) before using SessionStore on Android")
    return AndroidSessionStore(ctx)
}
