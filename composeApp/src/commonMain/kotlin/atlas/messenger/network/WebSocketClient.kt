package atlas.messenger.network

import atlas.messenger.data.EncryptedPayload
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class ServerEvent {
    data class AuthOk(val username: String, val isPublic: Boolean) : ServerEvent()
    data class ServerError(val message: String) : ServerEvent()
    data class PublicKeyReceived(val username: String, val publicKey: String) : ServerEvent()
    data class KeyNotFound(val username: String) : ServerEvent()
    data class MessageReceived(
        val id: String,
        val from: String,
        val to: String,
        val payload: EncryptedPayload,
        val timestampMs: Long,
    ) : ServerEvent()
    data class MessageHistory(val messages: List<HistoryEntry>) : ServerEvent()
    data class UserJoined(val username: String) : ServerEvent()
    data class UserLeft(val username: String) : ServerEvent()
    data class UserList(val users: List<String>) : ServerEvent()
    data class SearchResults(val users: List<String>) : ServerEvent()
    data class PublicUsersReceived(val users: List<atlas.messenger.data.PublicUserInfo>) : ServerEvent()
    data object Disconnected : ServerEvent()
    data class ConversationDeleted(val peer: String) : ServerEvent()
    data class UnreadCounts(val counts: Map<String, Int>) : ServerEvent()
    data class UnreadCleared(val peer: String) : ServerEvent()
    data class AvatarResponse(val username: String, val data: String?) : ServerEvent()
    data class MessageEdited(val id: String, val from: String, val to: String, val payload: EncryptedPayload) : ServerEvent()
    data class MessageDeleted(val id: String) : ServerEvent()
    data class AtlasDialogReceived(val id: String, val text: String, val imageUrl: String?, val timestampMs: Long) : ServerEvent()
    data class AtlasMessageReceived(val id: String, val from: String, val text: String, val timestampMs: Long) : ServerEvent()
    data class DisplayNamesReceived(val values: Map<String, String>) : ServerEvent()
    data class DisplayNameUpdated(val username: String, val displayName: String) : ServerEvent()
    data class AllUsersReceived(val users: List<String>) : ServerEvent()
    data class ArchivedConversationsReceived(val peers: Set<String>) : ServerEvent()
    data class ConversationArchiveUpdated(val peer: String, val archived: Boolean) : ServerEvent()
    data class MiteChatDelta(val id: String, val delta: String) : ServerEvent()
    data class MiteChatReasoningDelta(val id: String, val delta: String) : ServerEvent()
    data class MiteChatDone(val id: String) : ServerEvent()
    data class MiteChatError(val id: String, val message: String) : ServerEvent()
    data class AtlasXImageReceived(val data: String?, val message: String?) : ServerEvent()
}

data class MiteChatContextMessage(val role: String, val content: String)

data class HistoryEntry(
    val id: String,
    val from: String,
    val to: String,
    val payload: EncryptedPayload,
    val timestampMs: Long,
)

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

class WebSocketClient(private val httpClient: HttpClient) {

    private val _events = MutableSharedFlow<ServerEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<ServerEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var _connectionReady = CompletableDeferred<Unit>()
    val connectionReady: Deferred<Unit> = _connectionReady

    suspend fun connect(host: String, port: Int, onConnect: suspend () -> Unit = {}) {
        _connectionReady = CompletableDeferred()
        _connectionState.value = ConnectionState.CONNECTING
        try {
            httpClient.webSocket(host = host, port = port, path = "/") {
                session = this
                _connectionReady.complete(Unit)
                _connectionState.value = ConnectionState.CONNECTED
                onConnect()

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            handleFrame(frame.readText())
                        }
                    }
                } catch (e: Exception) {
                    throw e
                } finally {
                    session = null
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _events.emit(ServerEvent.Disconnected)
                }
            }
        } catch (e: Exception) {
            _connectionReady.completeExceptionally(e)
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e
        }
    }

    suspend fun authRegister(username: String, password: String, publicKeyBase64: String) {
        sendRaw(buildJsonObject("auth_register") {
            put("username", username)
            put("password", password)
            put("publicKey", publicKeyBase64)
        })
    }

    suspend fun authLogin(username: String, password: String, publicKeyBase64: String) {
        sendRaw(buildJsonObject("auth_login") {
            put("username", username)
            put("password", password)
            put("publicKey", publicKeyBase64)
        })
    }

    suspend fun fetchPublicKey(username: String) {
        sendRaw(buildJsonObject("fetch_key") { put("username", username) })
    }

    suspend fun searchUsers(query: String) {
        sendRaw(buildJsonObject("search_users") { put("query", query) })
    }

    suspend fun updatePublicStatus(isPublic: Boolean) {
        sendRaw(buildJsonObject("update_public_status") { put("isPublic", isPublic) })
    }

    suspend fun fetchPublicUsers() {
        sendRaw(buildJsonObject("fetch_public_users"))
    }

    suspend fun listAllUsers() {
        sendRaw(buildJsonObject("list_all_users"))
    }

    suspend fun deleteConversation(peer: String) {
        sendRaw(buildJsonObject("delete_conversation") { put("peer", peer) })
    }

    suspend fun archiveConversation(peer: String, archived: Boolean) {
        sendRaw(buildJsonObject("archive_conversation") {
            put("peer", peer)
            put("archived", archived)
        })
    }

    suspend fun getUnread() {
        sendRaw(buildJsonObject("get_unread"))
    }

    suspend fun clearUnread(peer: String) {
        sendRaw(buildJsonObject("clear_unread") { put("peer", peer) })
    }

    suspend fun updateAvatar(data: String) {
        sendRaw(buildJsonObject("update_avatar") { put("data", data) })
    }

    suspend fun fetchAvatar(username: String) {
        sendRaw(buildJsonObject("fetch_avatar") { put("username", username) })
    }

    suspend fun editMessage(id: String, payload: EncryptedPayload, senderPayload: EncryptedPayload) {
        sendRaw(buildJsonObject("edit_message") {
            put("id", id)
            putJsonObject("payload") { putEncryptedPayload(payload) }
            putJsonObject("senderPayload") { putEncryptedPayload(senderPayload) }
        })
    }

    suspend fun deleteMessage(id: String) {
        sendRaw(buildJsonObject("delete_message") { put("id", id) })
    }

    suspend fun sendEncryptedMessage(id: String, to: String, payload: EncryptedPayload, senderPayload: EncryptedPayload, timestampMs: Long) {
        sendRaw(buildJsonObject("message") {
            put("id", id)
            put("to", to)
            put("timestampMs", timestampMs)
            putJsonObject("payload") { putEncryptedPayload(payload) }
            putJsonObject("senderPayload") { putEncryptedPayload(senderPayload) }
        })
    }

    suspend fun sendAtlasDialog(id: String, text: String, imageUrl: String?, timestampMs: Long) {
        sendRaw(buildJsonObject("atlas_broadcast_dialog") {
            put("id", id)
            put("text", text)
            put("timestampMs", timestampMs)
            put("imageUrl", imageUrl)
        })
    }

    suspend fun sendAtlasBroadcastMessage(id: String, text: String, timestampMs: Long) {
        sendRaw(buildJsonObject("atlas_broadcast_message") {
            put("id", id)
            put("text", text)
            put("timestampMs", timestampMs)
        })
    }

    suspend fun updateDisplayName(displayName: String) {
        sendRaw(buildJsonObject("update_display_name") { put("displayName", displayName) })
    }

    suspend fun sendMiteChatRequest(id: String, prompt: String, history: List<MiteChatContextMessage>) {
        sendRaw(buildJsonObject("mite_chat_request") {
            put("id", id)
            put("prompt", prompt)
            putJsonArray("history") {
                history.forEach { message ->
                    addJsonObject {
                        put("role", message.role)
                        put("content", message.content)
                    }
                }
            }
        })
    }

    suspend fun fetchServerImage(path: String) {
        sendRaw(buildJsonObject("fetch_server_image") { put("path", path) })
    }

    fun disconnect() {
        session?.let { s ->
            CoroutineScope(Dispatchers.Default).launch { s.close() }
        }
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private suspend fun sendRaw(obj: kotlinx.serialization.json.JsonObject) {
        val s = session ?: throw IllegalStateException("WebSocket session is not connected")
        s.send(Frame.Text(obj.toString()))
    }

    private suspend fun handleFrame(raw: String) {
        runCatching {
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: return

            val event: ServerEvent = when (type) {
                "auth_ok" -> ServerEvent.AuthOk(
                    username = obj.getString("username"),
                    isPublic = obj.getBoolean("isPublic"),
                )
                "error" -> ServerEvent.ServerError(
                    message = obj.getString("message"),
                )
                "public_key" -> ServerEvent.PublicKeyReceived(
                    username = obj.getString("username"),
                    publicKey = obj.getString("publicKey"),
                )
                "key_not_found" -> ServerEvent.KeyNotFound(
                    username = obj.getString("username"),
                )
                "message" -> {
                    val p = obj.getObject("payload")
                    ServerEvent.MessageReceived(
                        id = obj.getString("id"),
                        from = obj.getString("from"),
                        to = obj.getString("to"),
                        payload = parseEncryptedPayload(p),
                        timestampMs = obj.getLong("timestampMs"),
                    )
                }
                "message_history" -> {
                    val entries = obj.getArray("messages").map { el ->
                        val m = el.jsonObject
                        val p = m.getObject("payload")
                        HistoryEntry(
                            id = m.getString("id"),
                            from = m.getString("from"),
                            to = m.getString("to"),
                            payload = parseEncryptedPayload(p),
                            timestampMs = m.getLong("timestampMs"),
                        )
                    }
                    ServerEvent.MessageHistory(entries)
                }
                "user_joined" -> ServerEvent.UserJoined(obj.getString("username"))
                "user_left" -> ServerEvent.UserLeft(obj.getString("username"))
                "user_list" -> ServerEvent.UserList(obj.getStringArray("users"))
                "search_results" -> ServerEvent.SearchResults(obj.getStringArray("users"))
                "public_users" -> {
                    val users = obj.getArray("users").map { el ->
                        val u = el.jsonObject
                        atlas.messenger.data.PublicUserInfo(
                            username = u.getString("username"),
                            isOnline = u.getBoolean("isOnline"),
                        )
                    }
                    ServerEvent.PublicUsersReceived(users)
                }
                "conversation_deleted" -> ServerEvent.ConversationDeleted(obj.getString("peer"))
                "unread_counts" -> {
                    val counts = obj.getObject("counts").entries.associate { (k, v) ->
                        k to v.jsonPrimitive.content.toInt()
                    }
                    ServerEvent.UnreadCounts(counts)
                }
                "unread_cleared" -> ServerEvent.UnreadCleared(obj.getString("peer"))
                "avatar_response" -> ServerEvent.AvatarResponse(
                    username = obj.getString("username"),
                    data = obj["data"]?.jsonPrimitive?.contentOrNull,
                )
                "message_edited" -> {
                    val p = obj.getObject("payload")
                    ServerEvent.MessageEdited(
                        id = obj.getString("id"),
                        from = obj.getString("from"),
                        to = obj.getString("to"),
                        payload = parseEncryptedPayload(p),
                    )
                }
                "message_deleted" -> ServerEvent.MessageDeleted(obj.getString("id"))
                "atlas_dialog" -> ServerEvent.AtlasDialogReceived(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    imageUrl = obj["imageUrl"]?.jsonPrimitive?.contentOrNull,
                    timestampMs = obj.getLong("timestampMs"),
                )
                "atlas_message" -> ServerEvent.AtlasMessageReceived(
                    id = obj.getString("id"),
                    from = obj.getString("from"),
                    text = obj.getString("text"),
                    timestampMs = obj.getLong("timestampMs"),
                )
                "display_names" -> {
                    val values = obj.getObject("values").entries.associate { (username, value) ->
                        username to value.jsonPrimitive.content
                    }
                    ServerEvent.DisplayNamesReceived(values)
                }
                "display_name_updated" -> ServerEvent.DisplayNameUpdated(
                    username = obj.getString("username"),
                    displayName = obj.getString("displayName"),
                )
                "all_users" -> ServerEvent.AllUsersReceived(obj.getStringArray("users"))
                "archived_conversations" -> ServerEvent.ArchivedConversationsReceived(
                    obj.getStringArray("peers").toSet(),
                )
                "conversation_archive_updated" -> ServerEvent.ConversationArchiveUpdated(
                    peer = obj.getString("peer"),
                    archived = obj.getBoolean("archived"),
                )
                "mite_chat_delta" -> ServerEvent.MiteChatDelta(
                    id = obj.getString("id"),
                    delta = obj.getString("delta"),
                )
                "mite_chat_reasoning_delta" -> ServerEvent.MiteChatReasoningDelta(
                    id = obj.getString("id"),
                    delta = obj.getString("delta"),
                )
                "mite_chat_done" -> ServerEvent.MiteChatDone(obj.getString("id"))
                "mite_chat_error" -> ServerEvent.MiteChatError(
                    id = obj.getString("id"),
                    message = obj.getString("message"),
                )
                "atlas_x_image" -> ServerEvent.AtlasXImageReceived(
                    data = obj["data"]?.jsonPrimitive?.contentOrNull,
                    message = obj["message"]?.jsonPrimitive?.contentOrNull,
                )
                else -> return
            }

            _events.emit(event)
        }
    }
}

private fun kotlinx.serialization.json.JsonObject.getString(key: String): String =
    this[key]?.jsonPrimitive?.content ?: ""

private fun kotlinx.serialization.json.JsonObject.getBoolean(key: String): Boolean =
    this[key]?.jsonPrimitive?.boolean ?: false

private fun kotlinx.serialization.json.JsonObject.getLong(key: String): Long =
    this[key]?.jsonPrimitive?.long ?: 0L

private fun kotlinx.serialization.json.JsonObject.getObject(key: String): kotlinx.serialization.json.JsonObject =
    this[key]?.jsonObject ?: kotlinx.serialization.json.buildJsonObject {}

private fun kotlinx.serialization.json.JsonObject.getArray(key: String): List<kotlinx.serialization.json.JsonElement> =
    this[key]?.jsonArray ?: emptyList()

private fun kotlinx.serialization.json.JsonObject.getStringArray(key: String): List<String> =
    getArray(key).map { it.jsonPrimitive.content }

private fun parseEncryptedPayload(obj: kotlinx.serialization.json.JsonObject): EncryptedPayload =
    EncryptedPayload(
        encryptedKey = obj.getString("encryptedKey"),
        iv = obj.getString("iv"),
        ciphertext = obj.getString("ciphertext"),
        tag = obj.getString("tag"),
    )

private fun kotlinx.serialization.json.JsonObjectBuilder.putEncryptedPayload(payload: EncryptedPayload) {
    put("encryptedKey", payload.encryptedKey)
    put("iv", payload.iv)
    put("ciphertext", payload.ciphertext)
    put("tag", payload.tag)
}

private fun buildJsonObject(
    type: String,
    builder: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {},
): kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.buildJsonObject {
    put("type", type)
    builder()
}
