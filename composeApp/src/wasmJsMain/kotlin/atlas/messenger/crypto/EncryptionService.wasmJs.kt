package atlas.messenger.crypto

import atlas.messenger.data.EncryptedPayload

private class WasmJsEncryptionService : EncryptionService {
    override val publicKeyBase64: String = ""
    override fun encrypt(plaintext: String, recipientPublicKeyBase64: String): EncryptedPayload =
        throw UnsupportedOperationException("WasmJS async crypto requires coroutine-based WebCrypto API wrapper.")
    override fun encryptForSelf(plaintext: String): EncryptedPayload =
        throw UnsupportedOperationException("WasmJS async crypto requires coroutine-based WebCrypto API wrapper.")
    override fun decrypt(payload: EncryptedPayload): String =
        throw UnsupportedOperationException("WasmJS async crypto requires coroutine-based WebCrypto API wrapper.")
}

actual fun createEncryptionService(): EncryptionService = WasmJsEncryptionService()

actual fun initEncryptionService() {}
