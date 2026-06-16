package atlas.messenger.util

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val totalMinutes = totalSeconds / 60
    val hour = ((totalMinutes / 60) % 24).toInt()
    val minute = (totalMinutes % 60).toInt()
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

fun formatRelativeTime(millis: Long): String {
    val now = currentTimeMs()
    val diff = now - millis
    return when {
        diff < 60_000 -> "только что"
        diff < 3_600_000 -> "${diff / 60_000}м назад"
        diff < 86_400_000 -> "${diff / 3_600_000}ч назад"
        diff < 604_800_000 -> "${diff / 86_400_000}д назад"
        else -> formatTimestamp(millis)
    }
}

fun formatTimestamp(millis: Long): String {
    val totalSeconds = millis / 1000
    val totalMinutes = totalSeconds / 60
    val hour = ((totalMinutes / 60) % 24).toInt()
    val minute = (totalMinutes % 60).toInt()

    val daysSinceEpoch = (totalMinutes / 60 / 24).toInt()
    val (day, month, year) = daysToDate(daysSinceEpoch)

    val nowDays = (currentTimeMs() / 1000 / 60 / 60 / 24).toInt()
    val nowYear = daysToDate(nowDays).third

    return when {
        daysSinceEpoch == nowDays -> "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        year == nowYear -> "$day ${monthName(month)}"
        else -> "$day ${monthName(month)} $year"
    }
}

private fun daysToDate(days: Int): Triple<Int, Int, Int> {
    val l = days + 719468
    val era = if (l >= 0) l / 146097 else (l - 146096) / 146097
    val doe = l - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val yr = if (m <= 2) y + 1 else y
    return Triple(d, m, yr)
}

private fun monthName(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
}

expect fun currentTimeMs(): Long
