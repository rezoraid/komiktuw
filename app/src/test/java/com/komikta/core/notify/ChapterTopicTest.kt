package com.komikta.core.notify

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterTopicTest {
    @Test fun matchesServerTopic() {
        assertEquals("c_8fcb32ab7066d597ce382278", chapterTopic("lumos", "solo-leveling"))
    }
}
