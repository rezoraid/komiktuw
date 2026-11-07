package com.komikta.ui.components

import com.komikta.data.remote.dto.ChapterSummaryDto
import com.komikta.data.remote.dto.ComicListItemDto
import com.komikta.data.remote.dto.absoluteUrl
import com.komikta.data.remote.dto.toDomain
import com.komikta.domain.model.Chapter
import com.komikta.domain.model.Comic
import com.komikta.domain.model.ComicDetail
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenWhenMissingTest {
    private val bare = Comic(title = "A", slug = "a", source = "manta")

    @Test fun missingDataIsAlwaysFilled() {
        assertEquals("Manhwa", genreText(bare.copy(source = "lumos")))
        assertEquals("Webtoon", genreText(bare))
        assertEquals("Chapter tersedia", latestChapterText(bare))
        assertEquals("Manta", latestAgoText(bare))
        assertEquals("Chapter tersedia · Manta", latestLine(bare))
        assertTrue(hasStats(bare))
        assertEquals("Baru", displayRating(null))
        assertEquals("Baru", displayRating(0.0))
        assertEquals("Tidak dicantumkan", displayPerson(null))
        assertEquals("Budi", displayPerson(" ", "Budi"))
        assertEquals("Ongoing", displayStatus(null, emptyList()))
    }

    @Test fun noDashOrBlankInDetailRows() {
        val rows = detailInfoRows(ComicDetail(title = "A", slug = "a", source = "shinigami"))
        assertEquals(8, rows.size)
        rows.forEach { (label, value) ->
            assertTrue(label, value.isNotBlank() && value != "-")
        }
        assertTrue(displaySynopsis(ComicDetail(title = "A", slug = "a")).isNotBlank())
    }

    @Test fun originAndCoverFallback() {
        assertEquals("manhwa", com.komikta.domain.model.originOf("webtoon"))
        assertEquals(fallbackHue("Solo"), fallbackHue("Solo"), 0f)
    }

    @Test fun partsAreJoined() {
        val c = bare.copy(chapters = listOf(Chapter("c1", "12")))
        assertTrue(latestLine(c).startsWith("Ch. 12"))
        val d = bare.copy(chapters = listOf(Chapter("c1", "12", null, "3 hours ago")))
        assertEquals("Ch. 12 · 3 jam lalu", latestLine(d))
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
