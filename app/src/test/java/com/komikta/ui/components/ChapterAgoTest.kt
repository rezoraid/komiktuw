package com.komikta.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterAgoTest {
    private val now = java.time.Instant.parse("2026-11-07T03:00:00Z").toEpochMilli()
    private fun ago(raw: String?) = chapterAgo(raw, now)

    @Test fun englishRelative() {
        assertEquals("23 jam lalu", ago("23 hours ago"))
        assertEquals("3 hari lalu", ago("3 days ago"))
        assertEquals("1 jam lalu", ago("an hour ago"))
        assertEquals("45 menit lalu", ago("45 minutes ago"))
        assertEquals("baru saja", ago("just now"))
    }

    @Test fun indonesianRelative() {
        assertEquals("2 jam lalu", ago("2 jam lalu"))
        assertEquals("3 hari lalu", ago("3 hari yang lalu"))
        assertEquals("1 hari lalu", ago("kemarin"))
    }

    @Test fun abbreviations() {
        assertEquals("3 jam lalu", ago("3h"))
        assertEquals("5 menit lalu", ago("5m"))
        assertEquals("2 hari lalu", ago("2d"))
        assertEquals("2 bulan lalu", ago("2mo"))
    }

    @Test fun timestamps() {
        assertEquals("23 jam lalu", ago("2026-11-06T04:00:00Z"))
        assertEquals("3 hari lalu", ago("2026-11-04"))
        assertEquals("23 jam lalu", ago("1793937600000")) // epoch milidetik
        assertEquals("23 jam lalu", ago("1793937600"))    // epoch detik
    }

    @Test fun unknownIsHiddenNotShownRaw() {
        assertNull(ago("garbage"))
        assertNull(ago(""))
        assertNull(ago(null))
    }

    @Test fun freshMeansUnder24Hours() {
        assertTrue(isFresh("23 hours ago", now))
        assertTrue(isFresh("2026-11-06T04:00:00Z", now))
        assertFalse(isFresh("1 day ago", now))
        assertFalse(isFresh(null, now))
    }
}
