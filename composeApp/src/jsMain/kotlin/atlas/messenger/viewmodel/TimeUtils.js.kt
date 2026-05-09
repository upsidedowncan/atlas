package atlas.messenger.viewmodel

actual fun currentTimeMs(): Long = js("Date.now()").unsafeCast<Double>().toLong()
