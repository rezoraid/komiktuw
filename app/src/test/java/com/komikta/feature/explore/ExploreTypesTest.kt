package com.komikta.feature.explore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreTypesTest {
    @Test fun exploreStartsOnAll() {
        assertEquals(ALL_TYPES, ExploreUiState().type)
    }

    @Test fun allShowsManhwaManhuaMangaAndWebtoon() {
        assertTrue(matchesType("manhwa", ALL_TYPES))
        assertTrue(matchesType("manga", ALL_TYPES))
        assertTrue(matchesType("webtoon", ALL_TYPES))
        assertTrue(matchesType(null, ALL_TYPES))
        assertFalse(matchesType("novel", ALL_TYPES))
    }

    @Test fun specificTabKeepsOnlyItsType() {
        assertTrue(matchesType("webtoon", "manhwa"))
        assertFalse(matchesType("manga", "manhwa"))
        assertTrue(matchesType("manhua", "manhua"))
    }
}
