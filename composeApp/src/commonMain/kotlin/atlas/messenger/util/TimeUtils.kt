package atlas.messenger.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun formatTimestamp(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    return when {
        local.date == now.date -> "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
        local.year == now.year && local.dayOfYear == now.dayOfYear - 1 -> "Yesterday"
        local.year == now.year -> "${local.dayOfMonth} ${monthName(local.monthNumber)}"
        else -> "${local.dayOfMonth} ${monthName(local.monthNumber)} ${local.year}"
    }
}

fun formatRelativeTime(millis: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - millis
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> formatTimestamp(millis)
    }
}

fun formatTime(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}

private fun monthName(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
}
