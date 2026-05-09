package atlas.messenger.crypto

import android.content.Context
import android.content.SharedPreferences
import atlas.messenger.data.EncryptedPayload
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

private var androidContext: Context? = null

fun initEncryptionService(context: Context) {
    androidContext = context.applicationContext
}

private class AndroidEncryptionService : EncryptionService {

    private val context = androidContext ?: throw IllegalStateException("Call initEncryptionService(context) first")
    private val prefs: SharedPreferences = context.getSharedPreferences("atlas_keys", Context.MODE_PRIVATE)

    private val keyPair = loadOrGenerateKeyPair()
    private val privateKey: PrivateKey = keyPair.private

    override val publicKeyBase64: String =
        Base64.getEncoder().encodeToString(keyPair.public.encoded)

    private val oaepSpec = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        PSource.PSpecified.DEFAULT
    )

    private fun loadOrGenerateKeyPair(): java.security.KeyPair {
        val privBase64 = prefs.getString("privateKey", null)
        val pubBase64 = prefs.getString("publicKey", null)

        if (privBase64 != null && pubBase64 != null) {
            runCatching {
                val privBytes = Base64.getDecoder().decode(privBase64)
                val pubBytes = Base64.getDecoder().decode(pubBase64)
                val privKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(PKCS8EncodedKeySpec(privBytes))
                val pubKey = KeyFactory.getInstance("RSA")
                    .generatePublic(X509EncodedKeySpec(pubBytes))
                return java.security.KeyPair(pubKey, privKey)
            }
        }

        val newPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.generateKeyPair()

        prefs.edit()
            .putString("privateKey", Base64.getEncoder().encodeToString(newPair.private.encoded))
            .putString("publicKey", Base64.getEncoder().encodeToString(newPair.public.encoded))
            .apply()

        return newPair
    }

    override fun encrypt(plaintext: String, recipientPublicKeyBase64: String): EncryptedPayload {
        val recipientPublicKey = decodeRsaPublicKey(recipientPublicKeyBase64)

        val aesKey: SecretKey = KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()

        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey, oaepSpec)
        val encryptedKey = rsaCipher.doFinal(aesKey.encoded)

        val gcmSpec = GCMParameterSpec(128, iv)
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)

        val ciphertextWithTag = aesCipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ciphertext = ciphertextWithTag.copyOf(ciphertextWithTag.size - 16)
        val tag = ciphertextWithTag.copyOfRange(ciphertextWithTag.size - 16, ciphertextWithTag.size)

        return EncryptedPayload(
            encryptedKey = Base64.getEncoder().encodeToString(encryptedKey),
            iv = Base64.getEncoder().encodeToString(iv),
            ciphertext = Base64.getEncoder().encodeToString(ciphertext),
            tag = Base64.getEncoder().encodeToString(tag),
        )
    }

    override fun encryptForSelf(plaintext: String): EncryptedPayload =
        encrypt(plaintext, publicKeyBase64)

    override fun decrypt(payload: EncryptedPayload): String {
        val encryptedKeyBytes = Base64.getDecoder().decode(payload.encryptedKey)
        val iv = Base64.getDecoder().decode(payload.iv)
        val ciphertext = Base64.getDecoder().decode(payload.ciphertext)
        val tag = Base64.getDecoder().decode(payload.tag)

        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSpec)
        val aesKeyBytes = rsaCipher.doFinal(encryptedKeyBytes)

        val aesKey = SecretKeySpec(aesKeyBytes, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)

        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)

        val plainBytes = aesCipher.doFinal(ciphertext + tag)
        return plainBytes.toString(Charsets.UTF_8)
    }

    private fun decodeRsaPublicKey(base64: String): PublicKey {
        val bytes = Base64.getDecoder().decode(base64)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}

actual fun createEncryptionService(): EncryptionService = AndroidEncryptionService()

actual fun initEncryptionService() {
    throw IllegalStateException("Use initEncryptionService(context: Context) on Android")
}