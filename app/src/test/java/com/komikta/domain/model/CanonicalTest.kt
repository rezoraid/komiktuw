package com.komikta.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalTest {
    @Test fun typeFromDifferentSpellings() {
        assertEquals("manhwa", canonicalType("Manhwa"))
        assertEquals("manhua", canonicalType("MANHUA"))
        assertEquals("manga", canonicalType("manga"))
        assertEquals("webtoon", canonicalType("Korean Webtoon"))
        assertNull(canonicalType(""))
        assertNull(canonicalType(null))
        assertEquals("manhua", canonicalType("", listOf("Action", "Manhua")))
    }

    @Test fun statusFromDifferentSpellings() {
        assertEquals("ongoing", canonicalStatus("Ongoing"))
        assertEquals("ongoing", canonicalStatus("Berjalan"))
        assertEquals("completed", canonicalStatus("Tamat"))
        assertEquals("hiatus", canonicalStatus("On Hiatus"))
        assertNull(canonicalStatus("entah"))
        assertNull(canonicalStatus(null))
    }

    @Test fun genresAreTidyAndUnique() {
        assertEquals(listOf("Action", "Slice of Life", "Sci-Fi"), canonicalGenres(listOf("action", "ACTION", " slice of life ", "Sci-Fi", "")))
    }

    @Test fun unknownTypeHasNoKomikLabel() {
        assertNull(com.komikta.ui.components.typeLabel("comic"))
        assertNull(com.komikta.ui.components.typeLabel(null))
        assertEquals("Manhwa", com.komikta.ui.components.typeLabel("Manhwa"))
        assertEquals("Manhua", com.komikta.ui.components.typeLabel("manhua"))
        assertEquals("Manga", com.komikta.ui.components.typeLabel("manga"))
    }
}
