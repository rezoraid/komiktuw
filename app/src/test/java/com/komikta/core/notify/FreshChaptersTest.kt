package com.komikta.core.notify

import com.komikta.data.remote.dto.ChapterSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshChaptersTest {
    private fun list(vararg slugs: String) = slugs.map { ChapterSummaryDto(slug = it, number = it) }

    @Test fun firstCheckOnlyRecordsBaseline() {
        assertTrue(freshChapters(list("3", "2", "1"), null).isEmpty())
    }

    @Test fun noChange() {
        assertTrue(freshChapters(list("3", "2", "1"), UpdateState.Seen("3", 3)).isEmpty())
    }

    @Test fun oneAndManyNewChapters() {
        assertEquals(listOf("4"), freshChapters(list("4", "3", "2", "1"), UpdateState.Seen("3", 3)).map { it.slug })
        assertEquals(listOf("5", "4"), freshChapters(list("5", "4", "3", "2", "1"), UpdateState.Seen("3", 3)).map { it.slug })
    }

    @Test fun missingPreviousFallsBackToCountDifference() {
        assertEquals(listOf("x"), freshChapters(list("x", "b", "a"), UpdateState.Seen("gone", 2)).map { it.slug })
        assertTrue(freshChapters(list("b", "a"), UpdateState.Seen("gone", 2)).isEmpty())
    }

    @Test fun emptyList() {
        assertTrue(freshChapters(emptyList(), UpdateState.Seen("3", 3)).isEmpty())
    }
}
