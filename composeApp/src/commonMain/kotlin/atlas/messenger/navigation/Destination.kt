package atlas.messenger.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object Auth : Destination

    @Serializable
    data object ConversationList : Destination

    @Serializable
    data class Chat(val peer: String) : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object UserDiscovery : Destination

    @Serializable
    data object Archive : Destination

    @Serializable
    data object MiteChats : Destination

    @Serializable
    data class MiteChat(val chatId: String) : Destination

    @Serializable
    data object AtlasX : Destination

    @Serializable
    data object AtlasXPayment : Destination

    @Serializable
    data object AtlasXActivated : Destination
}
