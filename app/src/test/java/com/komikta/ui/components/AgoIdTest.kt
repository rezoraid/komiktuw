package com.komikta.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AgoIdTest {
    private val now = 1_700_000_000_000L
    private fun ago(seconds: Long) = agoId(now - seconds * 1000, now)

    @Test fun recent() {
        assertEquals("baru saja", ago(30))
        assertEquals("5 menit lalu", ago(5 * 60))
        assertEquals("3 jam lalu", ago(3 * 3600))
    }

    @Test fun yesterdayAndOlder() {
        assertEquals("kemarin", ago(86_400 + 3600))
        assertEquals("2 hari lalu", ago(2 * 86_400))
        assertEquals("2 bulan lalu", ago(65L * 86_400))
        assertEquals("2 tahun lalu", ago(800L * 86_400))
    }

    @Test fun futureTimestampNeverNegative() {
        assertEquals("baru saja", agoId(now + 99_000, now))
    }
}
