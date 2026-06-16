package atlas.messenger.network

import atlas.messenger.data.EncryptedPayload
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

sealed class ServerEvent {
    data class AuthOk(val username: String, val isPublic: Boolean) : ServerEvent()
    data class ServerError(val message: String) : ServerEvent()
    data class PublicKeyReceived(val username: String, val publicKey: String) : ServerEvent()
    data class KeyNotFound(val username: String) : ServerEvent()
    data class MessageReceived(val id: String, val from: String, val to: String, val payload: EncryptedPayload, val timestampMs: Long) : ServerEvent()
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

data class HistoryEntry(val id: String, val from: String, val to: String, val payload: EncryptedPayload, val timestampMs: Long)

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
                        if (frame is Frame.Text) handleFrame(frame.readText())
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

    suspend fun authRegister(username: String, password: String, publicKeyBase64: String) =
        send(buildJsonObject {
            put("type", "auth_register"); put("username", username); put("password", password); put("publicKey", publicKeyBase64)
        })

    suspend fun authLogin(username: String, password: String, publicKeyBase64: String) =
        send(buildJsonObject {
            put("type", "auth_login"); put("username", username); put("password", password); put("publicKey", publicKeyBase64)
        })

    suspend fun fetchPublicKey(username: String) =
        send(buildJsonObject { put("type", "fetch_key"); put("username", username) })

    suspend fun searchUsers(query: String) =
        send(buildJsonObject { put("type", "search_users"); put("query", query) })

    suspend fun updatePublicStatus(isPublic: Boolean) =
        send(buildJsonObject { put("type", "update_public_status"); put("isPublic", isPublic) })

    suspend fun fetchPublicUsers() =
        send(buildJsonObject { put("type", "fetch_public_users") })

    suspend fun listAllUsers() =
        send(buildJsonObject { put("type", "list_all_users") })

    suspend fun deleteConversation(peer: String) =
        send(buildJsonObject { put("type", "delete_conversation"); put("peer", peer) })

    suspend fun archiveConversation(peer: String, archived: Boolean) =
        send(buildJsonObject { put("type", "archive_conversation"); put("peer", peer); put("archived", archived) })

    suspend fun getUnread() =
        send(buildJsonObject { put("type", "get_unread") })

    suspend fun clearUnread(peer: String) =
        send(buildJsonObject { put("type", "clear_unread"); put("peer", peer) })

    suspend fun updateAvatar(data: String) =
        send(buildJsonObject { put("type", "update_avatar"); put("data", data) })

    suspend fun fetchAvatar(username: String) =
        send(buildJsonObject { put("type", "fetch_avatar"); put("username", username) })

    suspend fun editMessage(id: String, payload: EncryptedPayload, senderPayload: EncryptedPayload) =
        send(buildJsonObject {
            put("type", "edit_message"); put("id", id)
            put("payload", payload.toJson())
            put("senderPayload", senderPayload.toJson())
        })

    suspend fun deleteMessage(id: String) =
        send(buildJsonObject { put("type", "delete_message"); put("id", id) })

    suspend fun sendEncryptedMessage(id: String, to: String, payload: EncryptedPayload, senderPayload: EncryptedPayload, timestampMs: Long) =
        send(buildJsonObject {
            put("type", "message"); put("id", id); put("to", to); put("timestampMs", timestampMs)
            put("payload", payload.toJson())
            put("senderPayload", senderPayload.toJson())
        })

    suspend fun sendAtlasDialog(id: String, text: String, imageUrl: String?, timestampMs: Long) =
        send(buildJsonObject {
            put("type", "atlas_broadcast_dialog"); put("id", id); put("text", text); put("timestampMs", timestampMs); put("imageUrl", imageUrl)
        })

    suspend fun sendAtlasBroadcastMessage(id: String, text: String, timestampMs: Long) =
        send(buildJsonObject { put("type", "atlas_broadcast_message"); put("id", id); put("text", text); put("timestampMs", timestampMs) })

    suspend fun updateDisplayName(displayName: String) =
        send(buildJsonObject { put("type", "update_display_name"); put("displayName", displayName) })

    suspend fun sendMiteChatRequest(id: String, prompt: String, history: List<MiteChatContextMessage>) =
        send(buildJsonObject {
            put("type", "mite_chat_request"); put("id", id); put("prompt", prompt)
            put("history", JsonArray(history.map { msg ->
                buildJsonObject { put("role", msg.role); put("content", msg.content) }
            }))
        })

    suspend fun fetchServerImage(path: String) =
        send(buildJsonObject { put("type", "fetch_server_image"); put("path", path) })

    fun disconnect() {
        session?.let { s -> CoroutineScope(Dispatchers.Default).launch { s.close() } }
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private suspend fun send(obj: JsonObject) {
        val s = session ?: throw IllegalStateException("WebSocket session is not connected")
        s.send(Frame.Text(obj.toString()))
    }

    private suspend fun handleFrame(raw: String) {
        runCatching {
            val obj = Json.parseToJsonElement(raw).jsonObject
            val type = obj.string("type") ?: return

            val event: ServerEvent = when (type) {
                "auth_ok" -> ServerEvent.AuthOk(obj.string("username")!!, obj.boolean("isPublic"))
                "error" -> ServerEvent.ServerError(obj.string("message")!!)
                "public_key" -> ServerEvent.PublicKeyReceived(obj.string("username")!!, obj.string("publicKey")!!)
                "key_not_found" -> ServerEvent.KeyNotFound(obj.string("username")!!)
                "message" -> {
                    val p = obj.obj("payload")
                    ServerEvent.MessageReceived(
                        obj.string("id")!!, obj.string("from")!!, obj.string("to")!!,
                        p.toEncryptedPayload(), obj.long("timestampMs"),
                    )
                }
                "message_history" -> {
                    val entries = obj.array("messages")?.map { el ->
                        val m = el.jsonObject
                        HistoryEntry(m.string("id")!!, m.string("from")!!, m.string("to")!!, m.obj("payload").toEncryptedPayload(), m.long("timestampMs"))
                    }.orEmpty()
                    ServerEvent.MessageHistory(entries)
                }
                "user_joined" -> ServerEvent.UserJoined(obj.string("username")!!)
                "user_left" -> ServerEvent.UserLeft(obj.string("username")!!)
                "user_list" -> ServerEvent.UserList(obj.stringList("users"))
                "search_results" -> ServerEvent.SearchResults(obj.stringList("users"))
                "public_users" -> {
                    val users = obj.array("users")?.map { el ->
                        val u = el.jsonObject
                        atlas.messenger.data.PublicUserInfo(u.string("username")!!, u.boolean("isOnline"))
                    }.orEmpty()
                    ServerEvent.PublicUsersReceived(users)
                }
                "conversation_deleted" -> ServerEvent.ConversationDeleted(obj.string("peer")!!)
                "unread_counts" -> {
                    val counts = obj.obj("counts")?.entries?.associate { (k, v) ->
                        k to (v.jsonPrimitive.content.toIntOrNull() ?: 0)
                    }.orEmpty()
                    ServerEvent.UnreadCounts(counts)
                }
                "unread_cleared" -> ServerEvent.UnreadCleared(obj.string("peer")!!)
                "avatar_response" -> ServerEvent.AvatarResponse(obj.string("username")!!, obj.string("data"))
                "message_edited" -> {
                    val p = obj.obj("payload")
                    ServerEvent.MessageEdited(obj.string("id")!!, obj.string("from")!!, obj.string("to")!!, p.toEncryptedPayload())
                }
                "message_deleted" -> ServerEvent.MessageDeleted(obj.string("id")!!)
                "atlas_dialog" -> ServerEvent.AtlasDialogReceived(
                    obj.string("id")!!, obj.string("text")!!, obj.string("imageUrl"), obj.long("timestampMs"),
                )
                "atlas_message" -> ServerEvent.AtlasMessageReceived(
                    obj.string("id")!!, obj.string("from")!!, obj.string("text")!!, obj.long("timestampMs"),
                )
                "display_names" -> {
                    val values = obj.obj("values")?.entries?.associate { (k, v) ->
                        k to v.jsonPrimitive.content
                    }.orEmpty()
                    ServerEvent.DisplayNamesReceived(values)
                }
                "display_name_updated" -> ServerEvent.DisplayNameUpdated(obj.string("username")!!, obj.string("displayName")!!)
                "all_users" -> ServerEvent.AllUsersReceived(obj.stringList("users"))
                "archived_conversations" -> ServerEvent.ArchivedConversationsReceived(obj.stringList("peers").toSet())
                "conversation_archive_updated" -> ServerEvent.ConversationArchiveUpdated(obj.string("peer")!!, obj.boolean("archived"))
                "mite_chat_delta" -> ServerEvent.MiteChatDelta(obj.string("id")!!, obj.string("delta")!!)
                "mite_chat_reasoning_delta" -> ServerEvent.MiteChatReasoningDelta(obj.string("id")!!, obj.string("delta")!!)
                "mite_chat_done" -> ServerEvent.MiteChatDone(obj.string("id")!!)
                "mite_chat_error" -> ServerEvent.MiteChatError(obj.string("id")!!, obj.string("message")!!)
                "atlas_x_image" -> ServerEvent.AtlasXImageReceived(obj.string("data"), obj.string("message"))
                else -> return
            }
            _events.emit(event)
        }
    }
}

private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false
private fun JsonObject.long(key: String): Long = this[key]?.jsonPrimitive?.longOrNull ?: 0L
private fun JsonObject.obj(key: String): JsonObject = this[key]?.jsonObject ?: buildJsonObject {}
private fun JsonObject.array(key: String): JsonArray? = this[key]?.jsonArray
private fun JsonObject.stringList(key: String): List<String> = array(key)?.map { it.jsonPrimitive.content }.orEmpty()

private fun JsonObject.toEncryptedPayload(): EncryptedPayload = EncryptedPayload(
    encryptedKey = string("encryptedKey").orEmpty(),
    iv = string("iv").orEmpty(),
    ciphertext = string("ciphertext").orEmpty(),
    tag = string("tag").orEmpty(),
)

private fun EncryptedPayload.toJson(): JsonObject = buildJsonObject {
    put("encryptedKey", encryptedKey); put("iv", iv); put("ciphertext", ciphertext); put("tag", tag)
}
