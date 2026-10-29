package com.yaddashty.app.ui

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

fun toPersianDigits(value: String): String = buildString {
    for (character in value) {
        append(if (character in '0'..'9') ('۰' + (character - '0')) else character)
    }
}

/**
 * Search key used by the notes list. Persian text is often typed with the
 * Arabic forms of yeh/kaf, with ZWNJ, diacritics or Arabic-Indic digits, so a
 * plain `contains` misses many legitimate matches. Folding the text first keeps
 * search predictable without changing what is stored in the database.
 */
fun searchKey(value: String): String {
    val folded = buildString {
        value.forEach { character ->
            when (character) {
                'ي', 'ى', 'ئ' -> append('ی')
                'ك' -> append('ک')
                'ة', 'ۀ' -> append('ه')
                'أ', 'إ', 'ٱ', 'آ' -> append('ا')
                'ؤ' -> append('و')
                '\u200c', '\u200e', '\u200f', '\u0640' -> Unit
                in '\u064B'..'\u065F' -> Unit
                in '\u06F0'..'\u06F9' -> append('0' + (character - '\u06F0'))
                in '\u0660'..'\u0669' -> append('0' + (character - '\u0660'))
                else -> append(character)
            }
        }
    }
    return folded.lowercase(Locale.ROOT).trim()
}

/** Gregorian → Jalali conversion (the same arithmetic used by jalaali-js). */
private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val monthDays = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
        ((gy2 + 399) / 400) + gd + monthDays[gm - 1]
    var jy = -1595 + (33 * (days / 12053))
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    return if (days < 186) {
        Triple(jy, 1 + days / 31, 1 + days % 31)
    } else {
        Triple(jy, 7 + (days - 186) / 30, 1 + (days - 186) % 30)
    }
}

fun formatCalendarDate(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val (year, month, day) = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
    val text = "%04d/%02d/%02d".format(Locale.ROOT, year, month, day)
    return toPersianDigits(text)
}

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
