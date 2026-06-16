@file:OptIn(DelicateCryptographyApi::class)

package atlas.messenger.crypto

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import kotlinx.coroutines.runBlocking
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.KeyFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals

class HybridEncryptionJvmTest {

    @Test
    fun rsaOaepAndAesGcm_roundtrip() = runBlocking {
        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.OAEP)
        val aes = provider.get(AES.GCM)

        val keyPair = rsa.keyPairGenerator(2048.bits).generateKey()
        val aesKey = aes.keyGenerator().generateKey()
        val aesKeyBytes = aesKey.encodeToByteArray(AES.Key.Format.RAW)
        val encryptedKey = keyPair.publicKey.encryptor().encrypt(aesKeyBytes)
        val iv = ByteArray(12) { it.toByte() }
        val combined = aesKey.cipher().encryptWithIv(iv = iv, plaintext = "hello".encodeToByteArray())
        val ct = combined.copyOfRange(0, combined.size - 16)
        val tag = combined.copyOfRange(combined.size - 16, combined.size)

        val decryptedKeyBytes = keyPair.privateKey.decryptor().decrypt(encryptedKey)
        val restoredKey = aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, decryptedKeyBytes)
        val plaintext = restoredKey.cipher().decryptWithIv(iv = iv, ciphertext = ct + tag)

        assertContentEquals("hello".encodeToByteArray(), plaintext)
    }

    @Test
    fun javaPublicKey_whyolegCanDecrypt() = runBlocking {
        val kpg = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val javaKeyPair = kpg.generateKeyPair()
        val javaPub = javaKeyPair.public.encoded
        val javaPriv = javaKeyPair.private.encoded

        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.OAEP)
        val aes = provider.get(AES.GCM)

        val pubKey = rsa.publicKeyDecoder(SHA256).decodeFromByteArray(RSA.PublicKey.Format.DER, javaPub)
        val aesKey = aes.keyGenerator().generateKey()
        val aesKeyBytes = aesKey.encodeToByteArray(AES.Key.Format.RAW)
        val encryptedKey = pubKey.encryptor().encrypt(aesKeyBytes)
        val iv = ByteArray(12) { (it * 3).toByte() }
        val combined = aesKey.cipher().encryptWithIv(iv = iv, plaintext = "from-whyoleg".encodeToByteArray())
        val ct = combined.copyOfRange(0, combined.size - 16)
        val tag = combined.copyOfRange(combined.size - 16, combined.size)

        val privKey = rsa.privateKeyDecoder(SHA256).decodeFromByteArray(RSA.PrivateKey.Format.DER, javaPriv)
        val decryptedKeyBytes = privKey.decryptor().decrypt(encryptedKey)
        val restoredKey = aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, decryptedKeyBytes)
        val plaintext = restoredKey.cipher().decryptWithIv(iv = iv, ciphertext = ct + tag)
        assertContentEquals("from-whyoleg".encodeToByteArray(), plaintext)
    }
}
