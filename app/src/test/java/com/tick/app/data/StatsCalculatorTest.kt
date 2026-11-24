package com.tick.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StatsCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(
        year: Int = 2026,
        month: Int = 11,
        day: Int,
        hour: Int = 20,
        minute: Int = 0,
    ): Long = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `no events starts the clock at journey start`() {
        val now = at(day = 24)
        val journey = now - 5 * HOUR_MILLIS
        val stats = StatsCalculator.calculate(emptyList(), journey, now, zone)

        assertEquals(5 * HOUR_MILLIS, stats.streakMillis)
        assertEquals(0, stats.thisMonthCount)
        assertNull(stats.averageIntervalMillis)
        assertEquals(5 * HOUR_MILLIS, stats.longestStreakMillis)
        assertEquals(24 * HOUR_MILLIS, stats.nextTargetMillis)
        assertTrue(stats.targetProgress in 0f..1f)
    }

    @Test
    fun `relapses drive streak average longest and month count`() {
        val now = at(day = 24)
        val events = listOf(
            TickEvent(id = "1", type = EventType.RELAPSE, at = at(day = 1)),
            TickEvent(id = "2", type = EventType.RELAPSE, at = at(day = 10)),
            TickEvent(id = "3", type = EventType.RELAPSE, at = at(day = 20)),
            TickEvent(id = "4", type = EventType.CHECK_IN, at = at(day = 24, hour = 9)),
        )
        val stats = StatsCalculator.calculate(events, at(day = 1) - DAY_MILLIS, now, zone)

        assertEquals(at(day = 20), stats.streakStart)
        assertEquals(4 * DAY_MILLIS + 0, stats.streakMillis)
        assertEquals(3, stats.thisMonthCount)
        assertEquals((9.5 * DAY_MILLIS).toLong(), stats.averageIntervalMillis)
        assertEquals(10 * DAY_MILLIS, stats.longestStreakMillis)
        assertTrue(stats.checkedInToday)
        assertEquals(1, stats.totalCheckIns)
    }

    @Test
    fun `backdated events recalculate without special casing`() {
        val now = at(day = 24)
        val latest = at(day = 22)
        val events = listOf(
            TickEvent(id = "old", type = EventType.RELAPSE, at = at(day = 2)),
            TickEvent(id = "new", type = EventType.RELAPSE, at = latest),
            TickEvent(id = "backfill", type = EventType.RELAPSE, at = at(day = 15)),
        )
        val stats = StatsCalculator.calculate(events, now - 30 * DAY_MILLIS, now, zone)

        assertEquals(latest, stats.streakStart)
        assertEquals(2 * DAY_MILLIS, stats.streakMillis)
        assertEquals(3, stats.thisMonthCount)
        assertEquals(10 * DAY_MILLIS, stats.averageIntervalMillis)
        assertEquals(13 * DAY_MILLIS, stats.longestStreakMillis)
    }

    @Test
    fun `future events do not disturb the current clock`() {
        val now = at(day = 24)
        val events = listOf(
            TickEvent(id = "future", type = EventType.RELAPSE, at = now + DAY_MILLIS),
        )
        val stats = StatsCalculator.calculate(events, now - 2 * HOUR_MILLIS, now, zone)
        assertEquals(now - 2 * HOUR_MILLIS, stats.streakStart)
        assertEquals(2 * HOUR_MILLIS, stats.streakMillis)
        assertEquals(0, stats.thisMonthCount)
    }
}
