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

    suspend fun connect(host: String, port: Int) {
        httpClient.webSocket(host = host, port = port, path = "/") {
            session = this
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        handleFrame(frame.readText())
                    }
                }
            } finally {
                session = null
                _events.emit(ServerEvent.Disconnected)
            }
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

    fun disconnect() {
        session?.let { s ->
            CoroutineScope(Dispatchers.Default).launch { s.close() }
        }
    }

    private suspend fun sendRaw(obj: JsonObject) {
        session?.send(Frame.Text(obj.toString()))
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
                else -> return
            }

            _events.emit(event)
        }
    }
}
