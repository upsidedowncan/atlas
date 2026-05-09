package atlas.messenger.viewmodel

actual fun currentTimeMs(): Long = js("Date.now()").toLong()
