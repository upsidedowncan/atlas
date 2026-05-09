package atlas.messenger.crypto

import atlas.messenger.data.EncryptedPayload
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Security.*
import platform.Foundation.*

private class IosEncryptionService : EncryptionService {

    private val privateKey: SecKeyRef
    private val publicKey: SecKeyRef

    init {
        val attributes = CFDictionaryCreateMutable(null, 3, null, null)!!
        CFDictionaryAddValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeRSA)
        CFDictionaryAddValue(attributes, kSecAttrKeySizeInBits, CFNumberCreate(null, kCFNumberIntType, cValuesOf(2048)))
        CFDictionaryAddValue(attributes, kSecAttrCanSign, kCFBooleanFalse)

        var error: CFErrorRef? = null
        val privKey = SecKeyCreateRandomKey(attributes, error.ptr as *) 
            ?: throw IllegalStateException("Ошибка генерации RSA ключа")
        CFRelease(attributes)

        privateKey = privKey
        publicKey = SecKeyCopyPublicKey(privKey)
            ?: throw IllegalStateException("Ошибка извлечения публичного ключа")
    }

    override val publicKeyBase64: String
        get() {
            val keyData = SecKeyCopyExternalRepresentation(publicKey, null)
                ?: throw IllegalStateException("Ошибка экспорта публичного ключа")
            val data = keyData as NSData
            return data.base64EncodedStringWithOptions(0u)
        }

    override fun encrypt(plaintext: String, recipientPublicKeyBase64: String): EncryptedPayload {
        val recipientKey = importRsaPublicKey(recipientPublicKeyBase64)

        val aesKeyBytes = ByteArray(32).also { platform.posix.arc4random_buf(it.refTo(0), 32u) }
        val iv = ByteArray(12).also { platform.posix.arc4random_buf(it.refTo(0), 12u) }

        val rsaAlgorithm = kSecKeyAlgorithmRSAEncryptionOAEPSHA256
        val aesKeyData = aesKeyBytes.toNSData()
        val encryptedKeyData = SecKeyCreateEncryptedData(recipientKey, rsaAlgorithm, aesKeyData as CFDataRef, null)
            ?: throw IllegalStateException("Ошибка RSA-шифрования ключа")

        val plaintextBytes = plaintext.encodeToByteArray()
        val ciphertextBytes = aesGcmEncrypt(plaintextBytes, aesKeyBytes, iv)

        val ciphertext = ciphertextBytes.copyOf(ciphertextBytes.size - 16)
        val tag = ciphertextBytes.copyOfRange(ciphertextBytes.size - 16, ciphertextBytes.size)

        return EncryptedPayload(
            encryptedKey = (encryptedKeyData as NSData).base64EncodedStringWithOptions(0u),
            iv = iv.toNSData().base64EncodedStringWithOptions(0u),
            ciphertext = ciphertext.toNSData().base64EncodedStringWithOptions(0u),
            tag = tag.toNSData().base64EncodedStringWithOptions(0u),
        )
    }

    override fun encryptForSelf(plaintext: String): EncryptedPayload =
        encrypt(plaintext, publicKeyBase64)

    override fun decrypt(payload: EncryptedPayload): String {
        val encryptedKeyData = NSData.create(base64EncodedString = payload.encryptedKey, options = 0u)
            ?: throw IllegalArgumentException("Некорректный encryptedKey")
        val iv = NSData.create(base64EncodedString = payload.iv, options = 0u)?.toByteArray()
            ?: throw IllegalArgumentException("Некорректный IV")
        val ciphertext = NSData.create(base64EncodedString = payload.ciphertext, options = 0u)?.toByteArray()
            ?: throw IllegalArgumentException("Некорректный ciphertext")
        val tag = NSData.create(base64EncodedString = payload.tag, options = 0u)?.toByteArray()
            ?: throw IllegalArgumentException("Некорректный tag")

        val aesKeyData = SecKeyCreateDecryptedData(
            privateKey,
            kSecKeyAlgorithmRSAEncryptionOAEPSHA256,
            encryptedKeyData as CFDataRef,
            null,
        ) ?: throw IllegalStateException("Ошибка RSA-дешифрования ключа")

        val aesKeyBytes = (aesKeyData as NSData).toByteArray()
        val plainBytes = aesGcmDecrypt(ciphertext + tag, aesKeyBytes, iv)
        return plainBytes.decodeToString()
    }

    private fun importRsaPublicKey(base64: String): SecKeyRef {
        val keyData = NSData.create(base64EncodedString = base64, options = 0u)
            ?: throw IllegalArgumentException("Некорректный публичный ключ")
        val attributes = CFDictionaryCreateMutable(null, 3, null, null)!!
        CFDictionaryAddValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeRSA)
        CFDictionaryAddValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)
        CFDictionaryAddValue(attributes, kSecAttrKeySizeInBits, CFNumberCreate(null, kCFNumberIntType, cValuesOf(2048)))
        val key = SecKeyCreateWithData(keyData as CFDataRef, attributes, null)
        CFRelease(attributes)
        return key ?: throw IllegalStateException("Ошибка импорта публичного ключа")
    }

    private fun aesGcmEncrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        commonCryptoAesGcm(plaintext, key, iv, encrypt = true)

    private fun aesGcmDecrypt(ciphertextWithTag: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        commonCryptoAesGcm(ciphertextWithTag, key, iv, encrypt = false)

    private fun commonCryptoAesGcm(data: ByteArray, key: ByteArray, iv: ByteArray, encrypt: Boolean): ByteArray {
        val outputSize = if (encrypt) data.size + 16 else data.size
        val output = ByteArray(outputSize)
        data.usePinned { dataPinned ->
            key.usePinned { keyPinned ->
                iv.usePinned { ivPinned ->
                    output.usePinned { outPinned ->
                        if (encrypt) {
                            platform.Security.CCCryptorGCM(
                                kCCEncrypt.toInt().toUInt(),
                                kCCAlgorithmAES.toUInt(),
                                keyPinned.addressOf(0),
                                key.size.toULong(),
                                ivPinned.addressOf(0),
                                iv.size.toULong(),
                                null, 0u,
                                dataPinned.addressOf(0),
                                data.size.toULong(),
                                outPinned.addressOf(0),
                                outPinned.addressOf(data.size), 16u,
                            )
                        } else {
                            val ciphertext = data.copyOf(data.size - 16)
                            val tag = data.copyOfRange(data.size - 16, data.size)
                            ciphertext.usePinned { cipherPinned ->
                                tag.usePinned { tagPinned ->
                                    platform.Security.CCCryptorGCM(
                                        kCCDecrypt.toInt().toUInt(),
                                        kCCAlgorithmAES.toUInt(),
                                        keyPinned.addressOf(0),
                                        key.size.toULong(),
                                        ivPinned.addressOf(0),
                                        iv.size.toULong(),
                                        null, 0u,
                                        cipherPinned.addressOf(0),
                                        ciphertext.size.toULong(),
                                        outPinned.addressOf(0),
                                        tagPinned.addressOf(0), 16u,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return if (encrypt) output else output.copyOf(data.size - 16)
    }
}

private fun ByteArray.toNSData(): NSData = this.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(this.length.toInt())
    result.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return result
}

actual fun createEncryptionService(): EncryptionService = IosEncryptionService()

actual fun initEncryptionService() {}
