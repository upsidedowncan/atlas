package atlas.messenger.network

import atlas.messenger.data.ChatMessage
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
    data class MessageReceived(
        val id: String,
        val from: String,
        val to: String,
        val payload: atlas.messenger.data.EncryptedPayload,
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
}

data class HistoryEntry(
    val id: String,
    val from: String,
    val to: String,
    val payload: EncryptedPayload,
    val timestampMs: Long,
)

class WebSocketClient(private val httpClient: HttpClient) {

    private val _events = MutableSharedFlow<ServerEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<ServerEvent> = _events.asSharedFlow()

    private var session: DefaultClientWebSocketSession? = null
    private val json = Json { ignoreUnknownKeys = true }
    private var _connectionReady = CompletableDeferred<Unit>()
    val connectionReady: Deferred<Unit> = _connectionReady

    suspend fun connect(host: String, port: Int, onConnect: suspend () -> Unit = {}) {
        _connectionReady = CompletableDeferred()
        try {
            httpClient.webSocket(host = host, port = port, path = "/") {
                session = this
                println("DEBUG: WebSocket session established")
                _connectionReady.complete(Unit)

                // Perform initial actions (like auth) while inside the session block
                onConnect()

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            handleFrame(frame.readText())
                        }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Error in WebSocket frame loop: ${e.message}")
                    throw e
                } finally {
                    println("DEBUG: WebSocket session ending")
                    session = null
                    _events.emit(ServerEvent.Disconnected)
                }
            }
        } catch (e: Exception) {
            println("DEBUG: WebSocket connection failed: ${e.message}")
            _connectionReady.completeExceptionally(e)
            throw e
        }
    }

    suspend fun authRegister(username: String, password: String, publicKeyBase64: String) {
        sendRaw(buildJsonObject {
            put("type", "auth_register")
            put("username", username)
            put("password", password)
            put("publicKey", publicKeyBase64)
        })
    }

    suspend fun authLogin(username: String, password: String, publicKeyBase64: String) {
        sendRaw(buildJsonObject {
            put("type", "auth_login")
            put("username", username)
            put("password", password)
            put("publicKey", publicKeyBase64)
        })
    }

    suspend fun fetchPublicKey(username: String) {
        sendRaw(buildJsonObject {
            put("type", "fetch_key")
            put("username", username)
        })
    }

    suspend fun searchUsers(query: String) {
        sendRaw(buildJsonObject {
            put("type", "search_users")
            put("query", query)
        })
    }

    suspend fun updatePublicStatus(isPublic: Boolean) {
        sendRaw(buildJsonObject {
            put("type", "update_public_status")
            put("isPublic", isPublic)
        })
    }

    suspend fun fetchPublicUsers() {
        sendRaw(buildJsonObject {
            put("type", "fetch_public_users")
        })
    }

    suspend fun listAllUsers() {
        sendRaw(buildJsonObject {
            put("type", "list_all_users")
        })
    }

    suspend fun deleteConversation(peer: String) {
        sendRaw(buildJsonObject {
            put("type", "delete_conversation")
            put("peer", peer)
        })
    }

    suspend fun archiveConversation(peer: String, archived: Boolean) {
        sendRaw(buildJsonObject {
            put("type", "archive_conversation")
            put("peer", peer)
            put("archived", archived)
        })
    }

    suspend fun getUnread() {
        sendRaw(buildJsonObject {
            put("type", "get_unread")
        })
    }

    suspend fun clearUnread(peer: String) {
        sendRaw(buildJsonObject {
            put("type", "clear_unread")
            put("peer", peer)
        })
    }

    suspend fun updateAvatar(data: String) {
        sendRaw(buildJsonObject {
            put("type", "update_avatar")
            put("data", data)
        })
    }

    suspend fun fetchAvatar(username: String) {
        sendRaw(buildJsonObject {
            put("type", "fetch_avatar")
            put("username", username)
        })
    }

    suspend fun editMessage(id: String, payload: EncryptedPayload, senderPayload: EncryptedPayload) {
        sendRaw(buildJsonObject {
            put("type", "edit_message")
            put("id", id)
            put("payload", buildJsonObject {
                put("encryptedKey", payload.encryptedKey)
                put("iv", payload.iv)
                put("ciphertext", payload.ciphertext)
                put("tag", payload.tag)
            })
            put("senderPayload", buildJsonObject {
                put("encryptedKey", senderPayload.encryptedKey)
                put("iv", senderPayload.iv)
                put("ciphertext", senderPayload.ciphertext)
                put("tag", senderPayload.tag)
            })
        })
    }

    suspend fun deleteMessage(id: String) {
        sendRaw(buildJsonObject {
            put("type", "delete_message")
            put("id", id)
        })
    }

    suspend fun sendEncryptedMessage(id: String, to: String, payload: EncryptedPayload, senderPayload: EncryptedPayload, timestampMs: Long) {
        sendRaw(buildJsonObject {
            put("type", "message")
            put("id", id)
            put("to", to)
            put("timestampMs", timestampMs)
            put("payload", buildJsonObject {
                put("encryptedKey", payload.encryptedKey)
                put("iv", payload.iv)
                put("ciphertext", payload.ciphertext)
                put("tag", payload.tag)
            })
            put("senderPayload", buildJsonObject {
                put("encryptedKey", senderPayload.encryptedKey)
                put("iv", senderPayload.iv)
                put("ciphertext", senderPayload.ciphertext)
                put("tag", senderPayload.tag)
            })
        })
    }

    suspend fun sendAtlasDialog(id: String, text: String, imageUrl: String?, timestampMs: Long) {
        sendRaw(buildJsonObject {
            put("type", "atlas_broadcast_dialog")
            put("id", id)
            put("text", text)
            put("timestampMs", timestampMs)
            put("imageUrl", imageUrl)
        })
    }

    suspend fun sendAtlasBroadcastMessage(id: String, text: String, timestampMs: Long) {
        sendRaw(buildJsonObject {
            put("type", "atlas_broadcast_message")
            put("id", id)
            put("text", text)
            put("timestampMs", timestampMs)
        })
    }

    suspend fun updateDisplayName(displayName: String) {
        sendRaw(buildJsonObject {
            put("type", "update_display_name")
            put("displayName", displayName)
        })
    }

    fun disconnect() {
        session?.let { s ->
            CoroutineScope(Dispatchers.Default).launch { s.close() }
        }
    }

    private suspend fun sendRaw(obj: JsonObject) {
        val session = this.session
        if (session == null) {
            println("DEBUG: Cannot send message, session is null")
            throw IllegalStateException("WebSocket session is not connected")
        }
        try {
            session.send(Frame.Text(obj.toString()))
            println("DEBUG: Message sent successfully")
        } catch (e: Exception) {
            println("DEBUG: Failed to send message: ${e.message}")
            throw e
        }
    }

    private suspend fun handleFrame(raw: String) {
        runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: return

            val event: ServerEvent = when (type) {
                "auth_ok" -> ServerEvent.AuthOk(
                    username = obj["username"]!!.jsonPrimitive.content,
                    isPublic = obj["isPublic"]!!.jsonPrimitive.boolean,
                )
                "error" -> ServerEvent.ServerError(
                    message = obj["message"]!!.jsonPrimitive.content,
                )
                "public_key" -> ServerEvent.PublicKeyReceived(
                    username = obj["username"]!!.jsonPrimitive.content,
                    publicKey = obj["publicKey"]!!.jsonPrimitive.content,
                )
                "key_not_found" -> ServerEvent.KeyNotFound(
                    username = obj["username"]!!.jsonPrimitive.content,
                )
                "message" -> {
                    val p = obj["payload"]!!.jsonObject
                    ServerEvent.MessageReceived(
                        id = obj["id"]!!.jsonPrimitive.content,
                        from = obj["from"]!!.jsonPrimitive.content,
                        to = obj["to"]!!.jsonPrimitive.content,
                        payload = atlas.messenger.data.EncryptedPayload(
                            encryptedKey = p["encryptedKey"]!!.jsonPrimitive.content,
                            iv = p["iv"]!!.jsonPrimitive.content,
                            ciphertext = p["ciphertext"]!!.jsonPrimitive.content,
                            tag = p["tag"]!!.jsonPrimitive.content,
                        ),
                        timestampMs = obj["timestampMs"]!!.jsonPrimitive.long,
                    )
                }
                "message_history" -> {
                    val entries = obj["messages"]!!.jsonArray.map { el ->
                        val m = el.jsonObject
                        val p = m["payload"]!!.jsonObject
                        HistoryEntry(
                            id = m["id"]!!.jsonPrimitive.content,
                            from = m["from"]!!.jsonPrimitive.content,
                            to = m["to"]!!.jsonPrimitive.content,
                            payload = atlas.messenger.data.EncryptedPayload(
                                encryptedKey = p["encryptedKey"]!!.jsonPrimitive.content,
                                iv = p["iv"]!!.jsonPrimitive.content,
                                ciphertext = p["ciphertext"]!!.jsonPrimitive.content,
                                tag = p["tag"]!!.jsonPrimitive.content,
                            ),
                            timestampMs = m["timestampMs"]!!.jsonPrimitive.long,
                        )
                    }
                    ServerEvent.MessageHistory(entries)
                }
                "user_joined" -> ServerEvent.UserJoined(
                    username = obj["username"]!!.jsonPrimitive.content,
                )
                "user_left" -> ServerEvent.UserLeft(
                    username = obj["username"]!!.jsonPrimitive.content,
                )
                "user_list" -> ServerEvent.UserList(
                    users = obj["users"]!!.jsonArray.map { it.jsonPrimitive.content },
                )
                "search_results" -> ServerEvent.SearchResults(
                    users = obj["users"]!!.jsonArray.map { it.jsonPrimitive.content },
                )
                "public_users" -> {
                    val users = obj["users"]!!.jsonArray.map { el ->
                        val u = el.jsonObject
                        atlas.messenger.data.PublicUserInfo(
                            username = u["username"]!!.jsonPrimitive.content,
                            isOnline = u["isOnline"]!!.jsonPrimitive.boolean,
                        )
                    }
                    ServerEvent.PublicUsersReceived(users)
                }
                "conversation_deleted" -> ServerEvent.ConversationDeleted(
                    peer = obj["peer"]!!.jsonPrimitive.content,
                )
                "unread_counts" -> {
                    val counts = obj["counts"]!!.jsonObject.entries.associate { (k, v) ->
                        k to v.jsonPrimitive.content.toInt()
                    }
                    ServerEvent.UnreadCounts(counts)
                }
                "unread_cleared" -> ServerEvent.UnreadCleared(
                    peer = obj["peer"]!!.jsonPrimitive.content,
                )
                "avatar_response" -> ServerEvent.AvatarResponse(
                    username = obj["username"]!!.jsonPrimitive.content,
                    data = obj["data"]?.jsonPrimitive?.contentOrNull,
                )
                "message_edited" -> {
                    val p = obj["payload"]!!.jsonObject
                    ServerEvent.MessageEdited(
                        id = obj["id"]!!.jsonPrimitive.content,
                        from = obj["from"]!!.jsonPrimitive.content,
                        to = obj["to"]!!.jsonPrimitive.content,
                        payload = atlas.messenger.data.EncryptedPayload(
                            encryptedKey = p["encryptedKey"]!!.jsonPrimitive.content,
                            iv = p["iv"]!!.jsonPrimitive.content,
                            ciphertext = p["ciphertext"]!!.jsonPrimitive.content,
                            tag = p["tag"]!!.jsonPrimitive.content,
                        ),
                    )
                }
                "message_deleted" -> ServerEvent.MessageDeleted(
                    id = obj["id"]!!.jsonPrimitive.content,
                )
                "atlas_dialog" -> ServerEvent.AtlasDialogReceived(
                    id = obj["id"]!!.jsonPrimitive.content,
                    text = obj["text"]!!.jsonPrimitive.content,
                    imageUrl = obj["imageUrl"]?.jsonPrimitive?.contentOrNull,
                    timestampMs = obj["timestampMs"]!!.jsonPrimitive.long,
                )
                "atlas_message" -> ServerEvent.AtlasMessageReceived(
                    id = obj["id"]!!.jsonPrimitive.content,
                    from = obj["from"]!!.jsonPrimitive.content,
                    text = obj["text"]!!.jsonPrimitive.content,
                    timestampMs = obj["timestampMs"]!!.jsonPrimitive.long,
                )
                "display_names" -> {
                    val values = obj["values"]!!.jsonObject.entries.associate { (username, value) ->
                        username to value.jsonPrimitive.content
                    }
                    ServerEvent.DisplayNamesReceived(values)
                }
                "display_name_updated" -> ServerEvent.DisplayNameUpdated(
                    username = obj["username"]!!.jsonPrimitive.content,
                    displayName = obj["displayName"]!!.jsonPrimitive.content,
                )
                "all_users" -> ServerEvent.AllUsersReceived(
                    users = obj["users"]!!.jsonArray.map { it.jsonPrimitive.content },
                )
                "archived_conversations" -> ServerEvent.ArchivedConversationsReceived(
                    peers = obj["peers"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet(),
                )
                "conversation_archive_updated" -> ServerEvent.ConversationArchiveUpdated(
                    peer = obj["peer"]!!.jsonPrimitive.content,
                    archived = obj["archived"]!!.jsonPrimitive.boolean,
                )
                else -> return
            }

            _events.emit(event)
        }
    }
}
