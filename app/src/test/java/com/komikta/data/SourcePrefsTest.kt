package com.komikta.data

import com.komikta.data.local.SourcePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcePrefsTest {
    @Test fun languageLabelKnownAndUnknown() {
        assertEquals("Indonesia", SourcePrefs.languageLabel("ind"))
        assertEquals("English", SourcePrefs.languageLabel("eng"))
        assertEquals("xx", SourcePrefs.languageLabel("xx"))
    }

    @Test fun mangaPlusIsSelectableAndDefaultLangExists() {
        assertTrue(SourcePrefs.SOURCES.any { it.id == SourcePrefs.MANGAPLUS })
        assertTrue(SourcePrefs.LANGUAGES.any { it.code == SourcePrefs.DEFAULT_LANG })
    }
}
