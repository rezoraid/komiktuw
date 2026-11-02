package com.komikta.feature.profile

import com.komikta.data.remote.dto.ActivityItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileUiTest {
    private fun item(chapter: String?, title: String = "") = ActivityItemDto(kind = "history", chapter = chapter, chapterTitle = title)

    @Test fun comicLevelItemsHaveNoChapterLabel() = assertNull(chapterLabel(item(null)))

    @Test fun usesChapterTitleWhenPresent() {
        assertEquals("Chapter 12", chapterLabel(item("12", "12")))
        assertEquals("Chapter 7: Pertemuan", chapterLabel(item("chapter-7", "Chapter 7: Pertemuan")))
    }

    @Test fun fallsBackToSlugNumber() {
        assertEquals("Chapter 5", chapterLabel(item("chapter-5")))
        assertEquals("Chapter 10.5", chapterLabel(item("chapter-10-5")))
    }

    @Test fun listKeysAreStablePerKind() {
        assertEquals("h:lumos:solo", ActivityItemDto(kind = "history", source = "lumos", slug = "solo").listKey())
        assertEquals("c:abc", ActivityItemDto(kind = "comment", id = "abc").listKey())
        assertEquals("l:comic:lumos:solo:null", ActivityItemDto(kind = "like", type = "comic", source = "lumos", slug = "solo").listKey())
    }
}
