package atlas.messenger.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChatTile(
    username: String,
    displayName: String,
    avatarUrl: String?,
    supportingText: String,
    isOnline: Boolean,
    unreadCount: Int = 0,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: ((Offset) -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val onlineColor = if (colors.surface.luminance() > 0.5f) Color(0xFF2E7D32) else Color(0xFF4CAF50)
    val backgroundColor = if (selected) colors.secondaryContainer else colors.surface
    val titleColor = if (selected) colors.onSecondaryContainer else colors.onSurface
    val supportingColor = if (selected) colors.onSecondaryContainer.copy(alpha = 0.78f) else colors.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .pointerInput(onLongClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = if (onLongClick != null) {
                        { offset: Offset -> onLongClick.invoke(offset) }
                    } else null,
                )
            }
            .pointerInput(onLongClick) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press &&
                            event.buttons.isSecondaryPressed &&
                            onLongClick != null
                        ) {
                            onLongClick.invoke(event.changes.first().position)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BadgedBox(
            badge = {
                if (isOnline) {
                    Badge(
                        containerColor = onlineColor,
                        modifier = Modifier.size(12.dp),
                    )
                }
            },
        ) {
            BadgedBox(
                badge = { CircularUnreadBadge(unreadCount) },
            ) {
                AvatarBox(username, avatarUrl, 44)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
