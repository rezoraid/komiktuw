package com.komikta.ui.components

import com.komikta.data.remote.dto.ChapterSummaryDto
import com.komikta.data.remote.dto.ComicListItemDto
import com.komikta.data.remote.dto.absoluteUrl
import com.komikta.data.remote.dto.toDomain
import com.komikta.domain.model.Chapter
import com.komikta.domain.model.Comic
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenWhenMissingTest {
    private val bare = Comic(title = "A", slug = "a", source = "manta")

    @Test fun missingDataProducesNullNotDash() {
        assertNull(genreText(bare))
        assertNull(latestChapterText(bare))
        assertNull(latestAgoText(bare))
        assertNull(latestLine(bare))
        assertFalse(hasStats(bare))
    }

    @Test fun onlyAvailablePartsAreJoined() {
        val c = bare.copy(chapters = listOf(Chapter("c1", "12")))
        assertEquals("Ch. 12", latestLine(c))
        val d = bare.copy(chapters = listOf(Chapter("c1", "12", null, "3 hours ago")))
        assertEquals("Ch. 12 · 3 jam lalu", latestLine(d))
    }

    @Test fun statsNeedRealValues() {
        assertFalse(hasStats(bare.copy(rating = 0.0, viewCount = 0L)))
        assertTrue(hasStats(bare.copy(viewCount = 5L)))
        assertTrue(hasStats(bare.copy(rating = 8.2)))
    }

    @Test fun scraperViewsAreIgnoredAndKomiktaViewCountIsUsed() {
        // `views` dari sumber tidak ada lagi di DTO; hanya viewCount (database Komikta) yang dipakai.
        val withCount = ComicListItemDto(title = "A", slug = "a", viewCount = JsonPrimitive(1500)).toDomain("manta")
        assertEquals(1500L, withCount.viewCount)
        val without = ComicListItemDto(title = "A", slug = "a").toDomain("manta")
        assertNull(without.viewCount)
        assertNull(ComicListItemDto(title = "A", slug = "a", viewCount = JsonPrimitive(0)).toDomain("manta").viewCount)
    }

    @Test fun chapterViewCountMaps() {
        assertEquals(7L, ChapterSummaryDto(slug = "c", viewCount = JsonPrimitive("7")).toDomain().viewCount)
        assertNull(ChapterSummaryDto(slug = "c").toDomain().viewCount)
    }

    @Test fun relativeMediaPathBecomesAbsolute() {
        assertNull(absoluteUrl(null))
        assertNull(absoluteUrl("  "))
        assertEquals("https://x.test/a.jpg", absoluteUrl("https://x.test/a.jpg"))
        assertTrue(absoluteUrl("/media/abc")!!.endsWith("/media/abc"))
        assertTrue(absoluteUrl("/media/abc")!!.startsWith("http"))
    }
}
