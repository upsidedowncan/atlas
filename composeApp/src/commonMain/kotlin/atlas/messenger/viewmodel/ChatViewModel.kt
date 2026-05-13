package atlas.messenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import atlas.messenger.crypto.EncryptionService
import atlas.messenger.crypto.createEncryptionService
import atlas.messenger.data.ChatMessage
import atlas.messenger.network.HistoryEntry
import atlas.messenger.network.ServerEvent
import atlas.messenger.network.WebSocketClient
import atlas.messenger.audio.createAudioLevelMonitor
import atlas.messenger.session.SessionStore
import atlas.messenger.session.UiPreferences
import atlas.messenger.session.createSessionStore
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class Screen { AUTH, CHAT }

data class ChatUiState(
    val screen: Screen = Screen.AUTH,
    val username: String = "",
    val passwordInput: String = "",
    val isRegistering: Boolean = false,
    val onlineUsers: List<String> = emptyList(),
    val allUsers: List<String> = emptyList(),
    val conversations: List<String> = emptyList(),
    val selectedPeer: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val inputText: String = "",
    val searchQuery: String = "",
    val searchResults: List<String> = emptyList(),
    val showSearch: Boolean = false,
    val allMessages: Map<String, List<ChatMessage>> = emptyMap(),
    val showSettings: Boolean = false,
    val textScale: Float = 1.0f,
    val accentColor: Int = 0xFF2196F3.toInt(),
    val colorPreset: ColorPreset = ColorPreset.DEFAULT,
    val contrast: Float = 1.0f,
    val publicKeyFingerprint: String = "",
    val serverUrl: String = "ws://127.0.0.1:8080",
    val showServerUrlDialog: Boolean = false,
    val isPublic: Boolean = false,
    val publicUsers: List<atlas.messenger.data.PublicUserInfo> = emptyList(),
    val showUserDiscovery: Boolean = false,
    val showAtlasBroadcast: Boolean = false,
    val activeCallPeer: String? = null,
    val callAudioLevel: Float = 0f,
    val micEnabled: Boolean = false,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val showEmojiPicker: Boolean = false,
    val avatars: Map<String, String?> = emptyMap(),
    val displayNames: Map<String, String> = emptyMap(),
    val displayNameInput: String = "",
    val avatarUploading: Boolean = false,
    val atlasDialogs: List<AtlasDialog> = emptyList(),
    val atlasBroadcastTitle: String = "",
    val atlasBroadcastDescription: String = "",
    val atlasBroadcastText: String = "",
    val atlasBroadcastImageUrl: String = "",
    val showArchive: Boolean = false,
    val archivedConversations: Set<String> = emptySet(),
)
data class AtlasDialog(
    val id: String,
    val title: String,
    val description: String,
    val text: String,
    val imageUrl: String?,
    val timestampMs: Long,
)

enum class ColorPreset { DEFAULT, VIBRANT, MUTED, PASTEL }

@OptIn(ExperimentalUuidApi::class)
class ChatViewModel : ViewModel() {
    companion object {
        const val EVERYONE_PEER = "__everyone__"
    }

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val encryption: EncryptionService = createEncryptionService()
    private val sessionStore: SessionStore = createSessionStore()

    private val client = HttpClient { install(WebSockets) }
    private val wsClient = WebSocketClient(client)

    private val recipientPublicKeys = mutableMapOf<String, String>()
    private val pendingKeyCallbacks = mutableMapOf<String, CompletableDeferred<String>>()
    private val requestedAvatarUsers = mutableSetOf<String>()

    private var connectionJob: Job? = null

    init {
        sessionStore.load()?.let { (username, password) ->
            _state.update { it.copy(username = username, passwordInput = password) }
            // Auto-connect if credentials exist
            if (username.isNotBlank() && password.isNotBlank()) {
                connect()
            }
        }
        sessionStore.loadPreferences()?.let { prefs ->
            _state.update {
                it.copy(
                    textScale = prefs.textScale,
                    accentColor = prefs.accentColor,
                    colorPreset = runCatching { ColorPreset.valueOf(prefs.colorPreset) }.getOrDefault(ColorPreset.DEFAULT),
                    contrast = prefs.contrast,
                    serverUrl = prefs.serverUrl,
                )
            }
        }
        _state.update { it.copy(publicKeyFingerprint = computeFingerprint(encryption.publicKeyBase64)) }
    }

    fun onUsernameChanged(value: String) {
        _state.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(passwordInput = value, errorMessage = null) }
    }

    fun onInputTextChanged(value: String) {
        _state.update { it.copy(inputText = value) }
    }

    fun onSearchQueryChanged(value: String) {
        _state.update { it.copy(searchQuery = value, searchResults = emptyList()) }
    }

    fun toggleAuthMode() {
        _state.update { it.copy(isRegistering = !it.isRegistering, errorMessage = null) }
    }

    fun setAuthMode(registering: Boolean) {
        println("DEBUG setAuthMode: was ${state.value.isRegistering}, setting to $registering")
        _state.update { it.copy(isRegistering = registering, errorMessage = null) }
    }

    fun onUserSelected(peer: String) {
        _state.update { s ->
            val history = s.allMessages[peer] ?: emptyList()
            s.copy(
                selectedPeer = peer,
                messages = history,
                screen = Screen.CHAT,
                showSearch = false,
                searchQuery = "",
                searchResults = emptyList(),
                conversations = if (peer in s.conversations) s.conversations else s.conversations + peer,
            )
        }
        ensureAvatarLoaded(peer)
    }

    fun openSearch() {
        _state.update { it.copy(showSearch = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeSearch() {
        _state.update { it.copy(showSearch = false) }
    }

    fun openSettings() {
        _state.update { it.copy(showSettings = true, showUserDiscovery = false, showAtlasBroadcast = false, showArchive = false) }
    }

    fun closeSettings() {
        _state.update { it.copy(showSettings = false) }
    }

    fun openAtlasBroadcast() {
        _state.update { it.copy(showAtlasBroadcast = true, showSettings = false, showUserDiscovery = false, showArchive = false) }
    }

    fun closeAtlasBroadcast() {
        _state.update { it.copy(showAtlasBroadcast = false) }
    }

    fun closeChat() {
        _state.update { it.copy(selectedPeer = null) }
    }

    fun openUserDiscovery() {
        _state.update { it.copy(showUserDiscovery = true, showSettings = false, showAtlasBroadcast = false, showArchive = false) }
        viewModelScope.launch {
            wsClient.fetchPublicUsers()
            state.value.conversations.forEach { ensureAvatarLoaded(it) }
        }
    }

    fun refreshPublicUsers() {
        viewModelScope.launch { wsClient.fetchPublicUsers() }
    }

    fun closeUserDiscovery() {
        _state.update { it.copy(showUserDiscovery = false) }
    }

    fun openArchive() {
        _state.update { it.copy(showArchive = true, showSettings = false, showUserDiscovery = false, showAtlasBroadcast = false) }
    }

    fun closeArchive() {
        _state.update { it.copy(showArchive = false) }
    }

    fun onPublicStatusChanged(isPublic: Boolean) {
        _state.update { it.copy(isPublic = isPublic) }
        viewModelScope.launch { wsClient.updatePublicStatus(isPublic) }
    }

    fun startCall(peer: String) {
        _state.update { it.copy(activeCallPeer = peer) }
        // In a real app, we would send a signal via WebSocket
        // Here we just simulate the call starting
        simulateAudioLevels()
    }

    fun endCall() {
        _state.update { it.copy(activeCallPeer = null, callAudioLevel = 0f) }
    }

    private var audioSimulationJob: Job? = null
    private val audioMonitor = createAudioLevelMonitor()

    private fun simulateAudioLevels() {
        audioSimulationJob?.cancel()
        audioSimulationJob = viewModelScope.launch {
            if (state.value.micEnabled) {
                audioMonitor.start()
                while (isActive) {
                    val level = audioMonitor.currentLevel()
                    _state.update { it.copy(callAudioLevel = level) }
                    delay(50)
                }
            } else {
                audioMonitor.stop()
                // Static idle animation: very subtle breathing
                var t = 0f
                while (isActive) {
                    t += 0.05f
                    val idle = (kotlin.math.sin(t) * 0.5f + 0.5f) * 0.08f
                    _state.update { it.copy(callAudioLevel = idle) }
                    delay(50)
                }
            }
        }
    }

    fun onMicEnabledChanged(enabled: Boolean) {
        _state.update { it.copy(micEnabled = enabled) }
        if (state.value.activeCallPeer != null) {
            simulateAudioLevels()
        }
    }

    fun deleteConversation(peer: String) {
        viewModelScope.launch { wsClient.deleteConversation(peer) }
    }

    fun archiveConversation(peer: String) {
        setConversationArchived(peer, archived = true)
    }

    fun unarchiveConversation(peer: String) {
        setConversationArchived(peer, archived = false)
    }

    private fun setConversationArchived(peer: String, archived: Boolean) {
        _state.update { s ->
            val nextArchived = if (archived) s.archivedConversations + peer else s.archivedConversations - peer
            s.copy(archivedConversations = nextArchived)
        }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { wsClient.archiveConversation(peer, archived) }
                .onFailure { e -> _state.update { it.copy(errorMessage = "Ошибка архивации: ${e.message}") } }
        }
    }

    fun fetchUnreadCounts() {
        viewModelScope.launch { wsClient.getUnread() }
    }

    fun clearUnreadForPeer(peer: String) {
        viewModelScope.launch { wsClient.clearUnread(peer) }
    }

    fun toggleEmojiPicker() {
        _state.update { it.copy(showEmojiPicker = !it.showEmojiPicker) }
    }

    fun hideEmojiPicker() {
        _state.update { it.copy(showEmojiPicker = false) }
    }

    fun insertEmoji(emoji: String) {
        _state.update { it.copy(inputText = it.inputText + emoji, showEmojiPicker = false) }
    }

    fun updateAvatar(imageData: String) {
        _state.update { it.copy(avatarUploading = true) }
        viewModelScope.launch {
            wsClient.updateAvatar(imageData)
            _state.update { it.copy(avatarUploading = false) }
        }
    }

    fun fetchAvatar(username: String) {
        ensureAvatarLoaded(username, force = true)
    }

    fun onTextScaleChanged(scale: Float) {
        _state.update { it.copy(textScale = scale) }
        persistPreferences()
    }

    fun onAccentColorChanged(color: Int) {
        _state.update { it.copy(accentColor = color) }
        persistPreferences()
    }

    fun onColorPresetChanged(preset: ColorPreset) {
        _state.update { 
            runCatching { it.copy(colorPreset = preset) }.getOrElse { 
                ChatUiState(colorPreset = preset) 
            }
        }
        persistPreferences()
    }

    fun onContrastChanged(contrast: Float) {
        _state.update { 
            runCatching { it.copy(contrast = contrast) }.getOrElse { 
                ChatUiState(contrast = contrast) 
            }
        }
        persistPreferences()
    }

    fun onServerUrlChanged(url: String) {
        _state.update { it.copy(serverUrl = url) }
        persistPreferences()
    }

    fun onDisplayNameChanged(value: String) {
        _state.update { it.copy(displayNameInput = value) }
    }

    fun submitDisplayName() {
        val candidate = state.value.displayNameInput.trim()
        if (candidate.isEmpty()) {
            _state.update { it.copy(errorMessage = "Отображаемое имя не может быть пустым") }
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                wsClient.updateDisplayName(candidate)
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = "Ошибка смены имени: ${e.message}") }
            }
        }
    }

    fun onAtlasBroadcastTextChanged(value: String) {
        _state.update { it.copy(atlasBroadcastText = value) }
    }

    fun onAtlasBroadcastTitleChanged(value: String) {
        _state.update { it.copy(atlasBroadcastTitle = value) }
    }

    fun onAtlasBroadcastDescriptionChanged(value: String) {
        _state.update { it.copy(atlasBroadcastDescription = value) }
    }

    fun onAtlasBroadcastImageUrlChanged(value: String) {
        _state.update { it.copy(atlasBroadcastImageUrl = value) }
    }

    fun sendAtlasBroadcastDialog() {
        val currentState = state.value
        val title = currentState.atlasBroadcastTitle.trim()
        val description = currentState.atlasBroadcastDescription.trim()
        val bodyText = currentState.atlasBroadcastText.trim()
        if (title.isEmpty() && description.isEmpty() && bodyText.isEmpty()) return
        val imageUrl = currentState.atlasBroadcastImageUrl.trim().ifBlank { null }
        val id = Uuid.random().toString()
        val timestampMs = currentTimeMs()
        val text = buildString {
            if (title.isNotBlank()) append("Заголовок: ").append(title).append('\n')
            if (description.isNotBlank()) append("Описание: ").append(description).append('\n')
            if (bodyText.isNotBlank()) append(bodyText)
        }.trim()

        _state.update {
            it.copy(
                atlasBroadcastTitle = "",
                atlasBroadcastDescription = "",
                atlasBroadcastText = "",
                atlasBroadcastImageUrl = "",
            )
        }

        // Optimistic local insert so Atlas sees the dialog immediately.
        _state.update {
            it.copy(
                atlasDialogs = (it.atlasDialogs + AtlasDialog(
                    id = id,
                    title = title,
                    description = description,
                    text = text,
                    imageUrl = imageUrl,
                    timestampMs = timestampMs,
                )).sortedByDescending { d -> d.timestampMs },
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                wsClient.sendAtlasDialog(
                    id = id,
                    text = text,
                    imageUrl = imageUrl,
                    timestampMs = timestampMs,
                )
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = "Ошибка отправки Atlas-диалога: ${e.message}") }
            }
        }
    }

    private fun persistPreferences() {
        val s = state.value
        sessionStore.savePreferences(
            UiPreferences(
                textScale = s.textScale,
                accentColor = s.accentColor,
                colorPreset = s.colorPreset.name,
                contrast = s.contrast,
                serverUrl = s.serverUrl,
            ),
        )
    }

    fun openServerUrlDialog() {
        _state.update { it.copy(showServerUrlDialog = true) }
    }

    fun closeServerUrlDialog() {
        _state.update { it.copy(showServerUrlDialog = false) }
    }

    fun submitSearch() {
        val query = state.value.searchQuery.trim()
        if (query.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            wsClient.searchUsers(query)
        }
    }

    fun connect() {
        val username = state.value.username.trim()
        val password = state.value.passwordInput

        if (username.length < 2) {
            _state.update { it.copy(errorMessage = "Имя должно быть минимум 2 символа.") }
            return
        }
        if (password.length < 4) {
            _state.update { it.copy(errorMessage = "Пароль должен быть минимум 4 символа.") }
            return
        }

        // Parse server URL from state (format: ws://host:port or wss://host:port)
        val serverUrl = state.value.serverUrl
        val (host, port) = parseServerUrl(serverUrl)

        connectionJob?.cancel()

        _state.update { it.copy(isConnecting = true, errorMessage = null) }

        connectionJob = viewModelScope.launch(Dispatchers.Default) {
            launch { wsClient.events.collect { event -> handleServerEvent(event) } }

            runCatching {
                println("DEBUG: Starting WebSocket connection to $host:$port")
                wsClient.connect(host, port) {
                    println("DEBUG: Connection ready, sending auth")
                    val isReg = state.value.isRegistering
                    if (isReg) {
                        wsClient.authRegister(username, password, encryption.publicKeyBase64)
                    } else {
                        wsClient.authLogin(username, password, encryption.publicKeyBase64)
                    }
                }
                println("DEBUG: WebSocket connection completed")
            }.onFailure { e ->
                println("DEBUG: WebSocket connection failed: ${e.message}")
                _state.update { 
                    runCatching { it.copy(isConnecting = false, errorMessage = "Ошибка подключения: ${e.message}") }
                        .getOrElse { ChatUiState(isConnecting = false, errorMessage = "Ошибка подключения: ${e.message}") }
                }
            }
        }
    }

    private fun parseServerUrl(url: String): Pair<String, Int> {
        return try {
            val cleanUrl = url.trim()
            val wsUrl = if (cleanUrl.startsWith("ws://") || cleanUrl.startsWith("wss://")) {
                cleanUrl
            } else {
                "ws://$cleanUrl"
            }
            val parsed = java.net.URI(wsUrl)
            val host = parsed.host ?: "127.0.0.1"
            val port = if (parsed.port > 0) parsed.port else 8080
            Pair(host, port)
        } catch (e: Exception) {
            Pair("127.0.0.1", 8080)
        }
    }

    fun sendMessage() {
        val currentState = state.value
        val text = currentState.inputText.trim()
        val peer = currentState.selectedPeer ?: return
        if (text.isEmpty()) return

        _state.update { it.copy(inputText = "") }

        if (peer == EVERYONE_PEER && currentState.username.equals("atlas", ignoreCase = true)) {
            val id = Uuid.random().toString()
            val ts = currentTimeMs()
            _state.update { s ->
                val everyoneMessages = s.allMessages[EVERYONE_PEER].orEmpty() + ChatMessage(
                    id = id,
                    from = "atlas",
                    to = EVERYONE_PEER,
                    text = text,
                    timestampMs = ts,
                    isOwn = true,
                )
                s.copy(
                    messages = everyoneMessages,
                    allMessages = s.allMessages + (EVERYONE_PEER to everyoneMessages),
                    archivedConversations = s.archivedConversations + EVERYONE_PEER,
                )
            }
            viewModelScope.launch(Dispatchers.Default) {
                runCatching { wsClient.archiveConversation(EVERYONE_PEER, true) }
            }
            viewModelScope.launch(Dispatchers.Default) {
                runCatching {
                    val recipients = state.value.allUsers
                        .filter { it.isNotBlank() && !it.equals("atlas", ignoreCase = true) }
                        .distinct()
                    for (recipient in recipients) {
                        val recipientKey = resolvePublicKey(recipient)
                        val payload = encryption.encrypt(text, recipientKey)
                        val senderPayload = encryption.encryptForSelf(text)
                        wsClient.sendEncryptedMessage(
                            id = "${id}_$recipient",
                            to = recipient,
                            payload = payload,
                            senderPayload = senderPayload,
                            timestampMs = ts,
                        )
                    }
                }.onFailure { e ->
                    _state.update { it.copy(errorMessage = "Ошибка отправки в общий чат: ${e.message}") }
                }
            }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val recipientKey = resolvePublicKey(peer)
                val payload = encryption.encrypt(text, recipientKey)
                val senderPayload = encryption.encryptForSelf(text)
                val id = Uuid.random().toString()
                val ts = currentTimeMs()

                wsClient.sendEncryptedMessage(id, peer, payload, senderPayload, ts)
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = "Ошибка отправки: ${e.message}") }
            }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        val currentState = state.value
        val peer = currentState.selectedPeer ?: return

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val recipientKey = resolvePublicKey(peer)
                val payload = encryption.encrypt(newText, recipientKey)
                val senderPayload = encryption.encryptForSelf(newText)
                wsClient.editMessage(messageId, payload, senderPayload)
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = "Ошибка редактирования: ${e.message}") }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                wsClient.deleteMessage(messageId)
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = "Ошибка удаления: ${e.message}") }
            }
        }
    }

    fun disconnect() {
        wsClient.disconnect()
        connectionJob?.cancel()
        sessionStore.clear()
        _state.update { ChatUiState() }
    }

    private suspend fun resolvePublicKey(username: String): String {
        recipientPublicKeys[username]?.let { return it }

        val deferred = CompletableDeferred<String>()
        pendingKeyCallbacks[username] = deferred
        wsClient.fetchPublicKey(username)

        return withTimeout(5_000) { deferred.await() }
    }

    private suspend fun handleServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.AuthOk -> {
                sessionStore.save(event.username, state.value.passwordInput)
                _state.update {
                    val switchedAccount = it.username != event.username
                    val withEveryone = if (event.username.equals("atlas", ignoreCase = true) &&
                        EVERYONE_PEER !in it.conversations
                    ) it.conversations + EVERYONE_PEER else it.conversations
                    it.copy(
                        screen = Screen.CHAT,
                        username = event.username,
                        isConnecting = false,
                        errorMessage = null,
                        isPublic = event.isPublic,
                        avatars = if (switchedAccount) emptyMap() else it.avatars,
                        conversations = withEveryone,
                        displayNameInput = it.displayNames[event.username] ?: event.username,
                    )
                }
                viewModelScope.launch { wsClient.fetchAvatar(event.username) }
                viewModelScope.launch { wsClient.getUnread() }
                if (event.username.equals("atlas", ignoreCase = true)) {
                    viewModelScope.launch { wsClient.listAllUsers() }
                }
                requestedAvatarUsers.clear()
            }

            is ServerEvent.ServerError -> {
                _state.update { it.copy(isConnecting = false, errorMessage = event.message) }
            }

            is ServerEvent.PublicKeyReceived -> {
                recipientPublicKeys[event.username] = event.publicKey
                pendingKeyCallbacks.remove(event.username)?.complete(event.publicKey)
            }

            is ServerEvent.KeyNotFound -> {
                pendingKeyCallbacks.remove(event.username)?.completeExceptionally(
                    IllegalStateException("Публичный ключ пользователя «${event.username}» не найден."),
                )
            }

            is ServerEvent.MessageHistory -> {
                val owner = state.value.username
                val decrypted = event.messages.mapNotNull { entry ->
                    runCatching { decryptHistoryEntry(entry, owner) }.getOrNull()
                }

                val grouped = decrypted.groupBy { msg ->
                    if (msg.isOwn) msg.to else msg.from
                }

                _state.update { s ->
                    val newAllMessages = s.allMessages.toMutableMap()
                    for ((peer, msgs) in grouped) {
                        val existing = newAllMessages[peer] ?: emptyList()
                        val existingIds = existing.map { it.id }.toSet()
                        val fresh = msgs.filter { it.id !in existingIds }
                        newAllMessages[peer] = (existing + fresh).sortedBy { it.timestampMs }
                    }
                    val newConversations = (s.conversations + newAllMessages.keys)
                        .distinct()
                    val updatedMessages = if (s.selectedPeer != null)
                        newAllMessages[s.selectedPeer] ?: s.messages
                    else s.messages
                    s.copy(
                        allMessages = newAllMessages,
                        conversations = newConversations,
                        messages = updatedMessages,
                    )
                }
                grouped.keys.forEach { ensureAvatarLoaded(it) }
            }

            is ServerEvent.MessageReceived -> {
                val owner = state.value.username
                runCatching {
                    val isOwn = event.from == owner
                    val plaintext = encryption.decrypt(event.payload)
                    val peer = if (isOwn) event.to else event.from
                    val shouldAutoArchive = peer == EVERYONE_PEER ||
                        (!isOwn && event.from.equals("atlas", ignoreCase = true) && event.id.contains("_"))
                    val message = ChatMessage(
                        id = event.id,
                        from = event.from,
                        to = event.to,
                        text = plaintext,
                        timestampMs = event.timestampMs,
                        isOwn = isOwn,
                    )

                    _state.update { s ->
                        val updatedConversations =
                            if (peer in s.conversations) s.conversations else s.conversations + peer
                        val peerMessages = (s.allMessages[peer] ?: emptyList())
                        val alreadyExists = peerMessages.any { it.id == message.id }
                        if (alreadyExists) return@update s
                        val newPeerMessages = peerMessages + message
                        val newAllMessages = s.allMessages + (peer to newPeerMessages)
                        val updatedMessages =
                            if (s.selectedPeer == peer) newPeerMessages else s.messages
                        s.copy(
                            conversations = updatedConversations,
                            allMessages = newAllMessages,
                            messages = updatedMessages,
                            archivedConversations = if (shouldAutoArchive) s.archivedConversations + peer else s.archivedConversations,
                        )
                    }
                    if (shouldAutoArchive) {
                        viewModelScope.launch(Dispatchers.Default) {
                            runCatching { wsClient.archiveConversation(peer, true) }
                        }
                    }
                    ensureAvatarLoaded(peer)
                }.onFailure { e ->
                    _state.update { it.copy(errorMessage = "Ошибка расшифровки: ${e.message}") }
                }
            }

            is ServerEvent.UserJoined -> {
                _state.update { s ->
                    if (event.username !in s.onlineUsers) s.copy(onlineUsers = s.onlineUsers + event.username)
                    else s
                }
            }

            is ServerEvent.UserLeft -> {
                _state.update { s ->
                    s.copy(
                        onlineUsers = s.onlineUsers.filter { it != event.username },
                        selectedPeer = if (s.selectedPeer == event.username) null else s.selectedPeer,
                    )
                }
            }

            is ServerEvent.UserList -> {
                _state.update { s ->
                    s.copy(onlineUsers = event.users.filter { it != s.username })
                }
                event.users.filter { it != state.value.username }.forEach { ensureAvatarLoaded(it) }
            }

            is ServerEvent.SearchResults -> {
                _state.update { it.copy(searchResults = event.users) }
                event.users.forEach { ensureAvatarLoaded(it) }
            }

            is ServerEvent.PublicUsersReceived -> {
                _state.update { it.copy(publicUsers = event.users) }
                event.users.forEach { user -> ensureAvatarLoaded(user.username) }
            }

            is ServerEvent.ConversationDeleted -> {
                val peer = event.peer
                val newConversations = _state.value.conversations.filter { it != peer }
                val newAllMessages = _state.value.allMessages.toMutableMap()
                newAllMessages.remove(peer)
                val newUnread = _state.value.unreadCounts.toMutableMap()
                newUnread.remove(peer)
                _state.update {
                    it.copy(
                        conversations = newConversations,
                        allMessages = newAllMessages,
                        unreadCounts = newUnread,
                        archivedConversations = it.archivedConversations - peer,
                        selectedPeer = if (_state.value.selectedPeer == peer) null else _state.value.selectedPeer,
                    )
                }
            }

            is ServerEvent.UnreadCounts -> {
                _state.update { it.copy(unreadCounts = event.counts) }
            }

            is ServerEvent.UnreadCleared -> {
                val newUnread = _state.value.unreadCounts.toMutableMap()
                newUnread.remove(event.peer)
                _state.update { it.copy(unreadCounts = newUnread) }
            }

            is ServerEvent.AvatarResponse -> {
                requestedAvatarUsers.remove(event.username)
                val newAvatars = _state.value.avatars.toMutableMap()
                newAvatars[event.username] = event.data
                _state.update { it.copy(avatars = newAvatars) }
            }

            is ServerEvent.MessageEdited -> {
                val owner = state.value.username
                runCatching {
                    val newText = encryption.decrypt(event.payload)
                    _state.update { s ->
                        val newAllMessages = s.allMessages.toMutableMap()
                        for ((peerKey, messages) in newAllMessages) {
                            val updatedMessages = messages.map { msg ->
                                if (msg.id == event.id) {
                                    msg.copy(text = newText, isEdited = true)
                                } else msg
                            }
                            newAllMessages[peerKey] = updatedMessages
                        }
                        val updatedMessages = if (s.selectedPeer != null) newAllMessages[s.selectedPeer] ?: s.messages else s.messages
                        s.copy(allMessages = newAllMessages, messages = updatedMessages)
                    }
                }.onFailure { e ->
                    _state.update { it.copy(errorMessage = "Ошибка расшифровки редактирования: ${e.message}") }
                }
            }

            is ServerEvent.MessageDeleted -> {
                _state.update { s ->
                    val newAllMessages = s.allMessages.toMutableMap()
                    for ((peerKey, messages) in newAllMessages) {
                        val updatedMessages = messages.map { msg ->
                            if (msg.id == event.id) msg.copy(isDeleted = true) else msg
                        }
                        newAllMessages[peerKey] = updatedMessages
                    }
                    val updatedMessages = if (s.selectedPeer != null) newAllMessages[s.selectedPeer] ?: s.messages else s.messages
                    s.copy(allMessages = newAllMessages, messages = updatedMessages)
                }
            }
            is ServerEvent.AtlasDialogReceived -> {
                val lines = event.text.lines()
                val title = lines.firstOrNull()
                    ?.removePrefix("Заголовок: ")
                    ?.takeIf { lines.firstOrNull()?.startsWith("Заголовок: ") == true }
                    .orEmpty()
                val description = lines.getOrNull(1)
                    ?.removePrefix("Описание: ")
                    ?.takeIf { lines.getOrNull(1)?.startsWith("Описание: ") == true }
                    .orEmpty()
                _state.update {
                    val withoutSameId = it.atlasDialogs.filterNot { d -> d.id == event.id }
                    val atlasDialogs = (withoutSameId + AtlasDialog(
                        id = event.id,
                        title = title,
                        description = description,
                        text = event.text,
                        imageUrl = event.imageUrl,
                        timestampMs = event.timestampMs,
                    )).sortedByDescending { d -> d.timestampMs }
                    if (!it.username.equals("atlas", ignoreCase = true)) {
                        it.copy(atlasDialogs = atlasDialogs)
                    } else {
                        val msg = ChatMessage(
                            id = event.id,
                            from = "atlas",
                            to = EVERYONE_PEER,
                            text = event.text,
                            timestampMs = event.timestampMs,
                            isOwn = true,
                        )
                        val everyoneMessages = (it.allMessages[EVERYONE_PEER].orEmpty().filterNot { m -> m.id == event.id } + msg)
                            .sortedBy { m -> m.timestampMs }
                        val conv = if (EVERYONE_PEER !in it.conversations) it.conversations + EVERYONE_PEER else it.conversations
                        it.copy(
                            atlasDialogs = atlasDialogs,
                            allMessages = it.allMessages + (EVERYONE_PEER to everyoneMessages),
                            messages = if (it.selectedPeer == EVERYONE_PEER) everyoneMessages else it.messages,
                            conversations = conv,
                        )
                    }
                }
            }

            is ServerEvent.DisplayNamesReceived -> {
                _state.update { s ->
                    s.copy(
                        displayNames = event.values,
                        displayNameInput = event.values[s.username] ?: s.displayNameInput,
                    )
                }
            }

            is ServerEvent.DisplayNameUpdated -> {
                _state.update { s ->
                    val next = s.displayNames + (event.username to event.displayName)
                    s.copy(
                        displayNames = next,
                        displayNameInput = if (event.username == s.username) event.displayName else s.displayNameInput,
                    )
                }
            }

            is ServerEvent.AllUsersReceived -> {
                _state.update { it.copy(allUsers = event.users.distinct()) }
            }

            is ServerEvent.AtlasMessageReceived -> {
                _state.update { s ->
                    val isAtlas = s.username.equals(event.from, ignoreCase = true)
                    val peer = if (isAtlas) EVERYONE_PEER else event.from
                    val msg = ChatMessage(
                        id = event.id,
                        from = event.from,
                        to = if (isAtlas) EVERYONE_PEER else s.username,
                        text = event.text,
                        timestampMs = event.timestampMs,
                        isOwn = isAtlas,
                    )
                    val peerMessages = (s.allMessages[peer].orEmpty().filterNot { it.id == event.id } + msg)
                        .sortedBy { it.timestampMs }
                    val conv = if (peer !in s.conversations) s.conversations + peer else s.conversations
                    s.copy(
                        allMessages = s.allMessages + (peer to peerMessages),
                        messages = if (s.selectedPeer == peer) peerMessages else s.messages,
                        conversations = conv,
                        archivedConversations = s.archivedConversations + peer,
                    )
                }
            }

            is ServerEvent.ArchivedConversationsReceived -> {
                _state.update { it.copy(archivedConversations = event.peers) }
            }

            is ServerEvent.ConversationArchiveUpdated -> {
                _state.update { s ->
                    val next = if (event.archived) {
                        s.archivedConversations + event.peer
                    } else {
                        s.archivedConversations - event.peer
                    }
                    s.copy(archivedConversations = next)
                }
            }

            ServerEvent.Disconnected -> {
                _state.update {
                    it.copy(screen = Screen.AUTH, errorMessage = "Соединение прервано.", isConnecting = false)
                }
            }
        }
    }

    private fun computeFingerprint(publicKeyBase64: String): String {
        val bytes = java.util.Base64.getDecoder().decode(publicKeyBase64)
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }.take(23) + "…"
    }

    private fun decryptHistoryEntry(entry: HistoryEntry, owner: String): ChatMessage {
        val isOwn = entry.from == owner
        val plaintext = encryption.decrypt(entry.payload)
        return ChatMessage(
            id = entry.id,
            from = entry.from,
            to = entry.to,
            text = plaintext,
            timestampMs = entry.timestampMs,
            isOwn = isOwn,
        )
    }

    private fun ensureAvatarLoaded(username: String, force: Boolean = false) {
        val user = username.trim()
        val me = state.value.username
        if (user.isEmpty() || user == me || user == EVERYONE_PEER) return
        if (!force && (user in state.value.avatars || user in requestedAvatarUsers)) return
        requestedAvatarUsers += user
        viewModelScope.launch {
            wsClient.fetchAvatar(user)
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
        client.close()
    }
}

expect fun currentTimeMs(): Long
