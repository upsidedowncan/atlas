@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, dev.whyoleg.cryptography.DelicateCryptographyApi::class, dev.whyoleg.cryptography.CryptographyProviderApi::class)

package atlas.messenger.crypto

import atlas.messenger.data.EncryptedPayload
import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import kotlinx.coroutines.runBlocking
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

private val provider = CryptographyProvider.Default

private class IosEncryptionService : EncryptionService {

    private val rsaOaep = provider.get(RSA.OAEP)
    private val aesGcm = provider.get(AES.GCM)

    private val keyPair: RSA.OAEP.KeyPair = runBlocking { loadOrGenerateKeyPair() }
    private val publicKey: RSA.OAEP.PublicKey get() = keyPair.publicKey
    private val privateKey: RSA.OAEP.PrivateKey get() = keyPair.privateKey

    init {
        // Touch keyPair so initialization failures surface at construction.
        keyPair.publicKey
    }

    private suspend fun loadOrGenerateKeyPair(): RSA.OAEP.KeyPair {
        val rsa = provider.get(RSA.OAEP)
        val existing = readKeychain("atlas.rsa.private.der")
        if (existing != null) {
            val priv = rsa.privateKeyDecoder(SHA256).decodeFromByteArray(RSA.PrivateKey.Format.DER, existing)
            return object : RSA.OAEP.KeyPair {
                override val publicKey: RSA.OAEP.PublicKey = priv.publicKey
                override val privateKey: RSA.OAEP.PrivateKey = priv
            }
        }
        val newPair = rsa.keyPairGenerator(2048.bits).generateKey()
        writeKeychain("atlas.rsa.private.der", newPair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.DER))
        return newPair
    }

    override val publicKeyBase64: String
        get() = runBlocking {
            publicKey.encodeToByteArray(RSA.PublicKey.Format.DER).encodeBase64()
        }

    override fun encrypt(plaintext: String, recipientPublicKeyBase64: String): EncryptedPayload = runBlocking {
        val recipientPublicKey = provider.get(RSA.OAEP)
            .publicKeyDecoder(SHA256)
            .decodeFromByteArray(RSA.PublicKey.Format.DER, recipientPublicKeyBase64.decodeBase64())

        val aesKey = aesGcm.keyGenerator().generateKey()
        val aesKeyBytes = aesKey.encodeToByteArray(AES.Key.Format.RAW)
        val encryptedKey = recipientPublicKey.encryptor().encrypt(aesKeyBytes)

        val iv = ByteArray(12).also { randomBytes(it) }
        val ciphertextWithTag = aesKey.cipher().encryptWithIv(
            iv = iv,
            plaintext = plaintext.encodeToByteArray(),
        )
        val (ciphertext, tag) = splitCiphertextAndTag(ciphertextWithTag)
        EncryptedPayload(
            encryptedKey = encryptedKey.encodeBase64(),
            iv = iv.encodeBase64(),
            ciphertext = ciphertext.encodeBase64(),
            tag = tag.encodeBase64(),
        )
    }

    override fun encryptForSelf(plaintext: String): EncryptedPayload =
        encrypt(plaintext, publicKeyBase64)

    override fun decrypt(payload: EncryptedPayload): String = runBlocking {
        val encryptedKeyBytes = payload.encryptedKey.decodeBase64()
        val iv = payload.iv.decodeBase64()
        val ciphertext = payload.ciphertext.decodeBase64()
        val tag = payload.tag.decodeBase64()

        val aesKeyBytes = privateKey.decryptor().decrypt(encryptedKeyBytes)
        val aesKey = provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, aesKeyBytes)

        val plaintext = aesKey.cipher().decryptWithIv(
            iv = iv,
            ciphertext = ciphertext + tag,
        )
        plaintext.decodeToString()
    }

    private fun splitCiphertextAndTag(combined: ByteArray): Pair<ByteArray, ByteArray> {
        require(combined.size >= 16) { "AES-GCM output too short" }
        val tagSize = 16
        val ct = combined.copyOfRange(0, combined.size - tagSize)
        val tag = combined.copyOfRange(combined.size - tagSize, combined.size)
        return ct to tag
    }

    private fun randomBytes(out: ByteArray) {
        out.usePinned { pinned ->
            platform.posix.arc4random_buf(pinned.addressOf(0), out.size.toULong())
        }
    }
}

private fun ByteArray.encodeBase64(): String =
    (this.usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong()) }
        .base64EncodedStringWithOptions(0u))

private fun String.decodeBase64(): ByteArray {
    val data = NSData.create(base64EncodedString = this, options = 0u)
        ?: throw IllegalArgumentException("Некорректный base64")
    val result = ByteArray(data.length.toInt())
    result.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return result
}

private fun readKeychain(account: String): ByteArray? {
    memScoped {
        val query = CFDictionaryCreateMutable(null, 4, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, "atlas.keys".cstr.ptr)
        CFDictionaryAddValue(query, kSecAttrAccount, account.cstr.ptr)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        val out = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, out.ptr)
        CFRelease(query)
        if (status != errSecSuccess || out.value == null) return null
        @Suppress("UNCHECKED_CAST")
        val data = out.value as CFDataRef
        val length = CFDataGetLength(data)
        if (length <= 0) return null
        val result = ByteArray(length.toInt())
        result.usePinned { pinned ->
            CFDataGetBytes(data, CFRangeMake(0, length), pinned.addressOf(0))
        }
        return result
    }
}

private fun writeKeychain(account: String, bytes: ByteArray) {
    memScoped {
        bytes.usePinned { pinned ->
            val data = CFDataCreate(null, pinned.addressOf(0), bytes.size.toLong())!!
            val query = CFDictionaryCreateMutable(null, 3, null, null)!!
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, "atlas.keys".cstr.ptr)
            CFDictionaryAddValue(query, kSecAttrAccount, account.cstr.ptr)
            SecItemDelete(query)
            CFDictionaryAddValue(query, kSecValueData, data)
            SecItemAdd(query, null)
            CFRelease(data)
            CFRelease(query)
        }
    }
}

actual fun createEncryptionService(): EncryptionService = IosEncryptionService()

actual fun initEncryptionService() {}
