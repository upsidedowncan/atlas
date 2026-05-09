package atlas.messenger.crypto

import atlas.messenger.data.EncryptedPayload
import kotlin.js.Date

private class JsEncryptionService : EncryptionService {

    private val keyPairDeferred = kotlinx.coroutines.CompletableDeferred<dynamic>()
    private var _publicKeyBase64: String = ""

    init {
        generateKeyPairAsync()
    }

    private fun generateKeyPairAsync() {
        val subtle = js("crypto.subtle")
        val promise = subtle.generateKey(
            js("({ name: 'RSA-OAEP', modulusLength: 2048, publicExponent: new Uint8Array([1,0,1]), hash: 'SHA-256' })"),
            true,
            js("(['encrypt', 'decrypt'])")
        )
        promise.then { keyPair: dynamic ->
            subtle.exportKey("spki", keyPair.publicKey as Any).then { exported: dynamic ->
                val buffer = exported as dynamic
                val bytes = js("new Uint8Array(buffer)") as dynamic
                var binary = ""
                for (i in 0 until (bytes.length as Int)) {
                    binary += js("String.fromCharCode(bytes[i])") as String
                }
                _publicKeyBase64 = js("btoa(binary)") as String
                keyPairDeferred.complete(keyPair)
            }
        }
    }

    override val publicKeyBase64: String get() = _publicKeyBase64

    override fun encrypt(plaintext: String, recipientPublicKeyBase64: String): EncryptedPayload {
        throw UnsupportedOperationException("JS async crypto not supported synchronously — use the async variant.")
    }

    override fun encryptForSelf(plaintext: String): EncryptedPayload {
        throw UnsupportedOperationException("JS async crypto not supported synchronously — use the async variant.")
    }

    override fun decrypt(payload: EncryptedPayload): String {
        throw UnsupportedOperationException("JS async crypto not supported synchronously — use the async variant.")
    }
}

actual fun createEncryptionService(): EncryptionService = JsEncryptionService()
