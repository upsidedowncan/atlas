package atlas.messenger.viewmodel

import platform.CoreFoundation.CFAbsoluteTimeGetCurrent

actual fun currentTimeMs(): Long = ((CFAbsoluteTimeGetCurrent() + 978307200.0) * 1000).toLong()
