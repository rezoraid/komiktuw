package com.komikta.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompleteComicTest {
    private val sparse = Comic(title = "A", slug = "a", source = "manta", rating = 8.0)
    private val detail = ComicDetail(
        title = "A", slug = "a", source = "manta", rating = 5.0, viewCount = 1200L, type = "Manhwa", status = "ongoing", coverUrl = "https://x.test/c.jpg",
        genres = listOf("action", "FANTASY"), chapters = listOf(Chapter("c2", "2", null, "2 jam lalu"), Chapter("c1", "1"))
    )

    @Test fun fillsOnlyMissingFields() {
        val c = sparse.completedWith(detail)
        assertEquals(8.0, c.rating!!, 0.0) // data daftar tidak ditimpa
        assertEquals(1200L, c.viewCount)
        assertEquals("manhwa", c.type)
        assertEquals(listOf("Action", "Fantasy"), c.genres)
        assertEquals("c2", c.chapters.first().slug)
        assertFalse(c.isIncomplete)
    }

    @Test fun sparseIsIncomplete() = assertTrue(sparse.isIncomplete)

    @Test fun viewsAloneDoNotMakeACardIncomplete() {
        val c = sparse.copy(viewCount = null, coverUrl = "https://x.test/c.jpg", status = "ongoing", genres = listOf("Action"), type = "manhwa", chapters = listOf(Chapter("c1", "1")))
        assertFalse(c.isIncomplete)
    }

    @Test fun reusesEarlierEnrichment() {
        val done = sparse.completedWith(detail)
        assertEquals(done.viewCount, sparse.fillMissingFrom(done).viewCount)
    }
}
