package atlas.messenger.crypto

import atlas.messenger.data.EncryptedPayload

interface EncryptionService {
    val publicKeyBase64: String
    fun encrypt(plaintext: String, recipientPublicKeyBase64: String): EncryptedPayload
    fun encryptForSelf(plaintext: String): EncryptedPayload
    fun decrypt(payload: EncryptedPayload): String
}

expect fun createEncryptionService(): EncryptionService

expect fun initEncryptionService()
