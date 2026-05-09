package atlas.messenger.data

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedPayload(
    val encryptedKey: String,
    val iv: String,
    val ciphertext: String,
    val tag: String,
)

@Serializable
data class ChatMessage(
    val id: String,
    val from: String,
    val to: String,
    val text: String,
    val timestampMs: Long,
    val isOwn: Boolean,
)

@Serializable
data class PublicUserInfo(
    val username: String,
    val isOnline: Boolean
)

@Serializable
sealed class ServerFrame {
    @Serializable
    data class AuthOk(val username: String, val isPublic: Boolean) : ServerFrame()

    @Serializable
    data class Error(val message: String) : ServerFrame()

    @Serializable
    data class PublicKey(val username: String, val publicKey: String) : ServerFrame()

    @Serializable
    data class KeyNotFound(val username: String) : ServerFrame()

    @Serializable
    data class IncomingMessage(val from: String, val payload: EncryptedPayload) : ServerFrame()

    @Serializable
    data class UserJoined(val username: String) : ServerFrame()

    @Serializable
    data class UserLeft(val username: String) : ServerFrame()

    @Serializable
    data class UserList(val users: List<String>) : ServerFrame()

    @Serializable
    data class SearchResults(val users: List<String>) : ServerFrame()

    @Serializable
    data class PublicUsers(val users: List<PublicUserInfo>) : ServerFrame()
}

@Serializable
sealed class ClientFrame {
    @Serializable
    data class AuthRegister(val username: String, val password: String, val publicKey: String) : ClientFrame()

    @Serializable
    data class AuthLogin(val username: String, val password: String, val publicKey: String) : ClientFrame()

    @Serializable
    data class SendMessage(val to: String, val payload: EncryptedPayload) : ClientFrame()

    @Serializable
    data class FetchKey(val username: String) : ClientFrame()

    @Serializable
    data class SearchUsers(val query: String) : ClientFrame()

    @Serializable
    data class UpdatePublicStatus(val isPublic: Boolean) : ClientFrame()

    @Serializable
    data class FetchPublicUsers(val dummy: Int = 0) : ClientFrame()
}
