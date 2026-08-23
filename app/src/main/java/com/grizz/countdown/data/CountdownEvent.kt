package com.grizz.countdown.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * A single tracked date. [colorArgb] is a packed ARGB int widened to Long so it
 * survives JSON round-trips without sign surprises.
 */
@Serializable
data class CountdownEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val date: String = LocalDate.now().toString(),
    val colorArgb: Long = 0xFF3B6EF3,
    val note: String = ""
) {
    val localDate: LocalDate
        get() = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())

    /** Days from [today] to the event: positive ahead, negative behind, 0 today. */
    fun daysFrom(today: LocalDate): Int =
        ChronoUnit.DAYS.between(today, localDate).toInt()
}

/** Ordering helper: the event a widget should show — soonest ahead, else most recent behind. */
fun List<CountdownEvent>.nextUp(today: LocalDate): CountdownEvent? {
    val upcoming = filter { it.daysFrom(today) >= 0 }.minByOrNull { it.daysFrom(today) }
    return upcoming ?: maxByOrNull { it.daysFrom(today) }
}
