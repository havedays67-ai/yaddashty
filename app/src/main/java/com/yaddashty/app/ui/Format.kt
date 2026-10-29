package com.yaddashty.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun toPersianDigits(value: String): String = buildString {
    for (character in value) {
        append(if (character in '0'..'9') ('۰' + (character - '0')) else character)
    }
}

fun formatCalendarDate(timestamp: Long): String =
    toPersianDigits(SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(timestamp)))

fun formatRelative(timestamp: Long): String {
    val diffMillis = System.currentTimeMillis() - timestamp
    if (diffMillis < 0) return formatCalendarDate(timestamp)
    val diffMinutes = diffMillis / 60_000
    return when {
        diffMinutes < 1 -> "همین حالا"
        diffMinutes < 60 -> "${toPersianDigits(diffMinutes.toString())} دقیقه پیش"
        diffMinutes < 24 * 60 -> "${toPersianDigits((diffMinutes / 60).toString())} ساعت پیش"
        diffMinutes < 7 * 24 * 60 -> "${toPersianDigits((diffMinutes / (24 * 60)).toString())} روز پیش"
        else -> formatCalendarDate(timestamp)
    }
}

fun wordCount(body: String): Int =
    body.trim()
        .split(Regex("[\\s،.,؛:!؟()\\[\\]«»\"'…\\-]+"))
        .count { word -> word.any { character -> character.isLetterOrDigit() } }
