package com.komikta.data

import com.komikta.data.local.SourcePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcePrefsTest {
    @Test fun sourcesMatchBackend() {
        assertEquals(listOf("lumos", "manta", "shinigami"), SourcePrefs.SOURCES.map { it.id })
    }

    @Test fun capabilitiesFollowBackend() {
        assertEquals(listOf("lumos"), SourcePrefs.POPULAR_SOURCES)
        assertTrue("manhwa" in SourcePrefs.SERVER_TYPES.getValue("lumos"))
        assertEquals(setOf("manga"), SourcePrefs.SERVER_TYPES.getValue("manta"))
        assertFalse("shinigami" in SourcePrefs.SERVER_TYPES)
    }

    @Test fun lumosImageHostsGoThroughProxy() {
        assertTrue(SourcePrefs.isProxiedImageHost("lms.imgsvr.my.id"))
        assertTrue(SourcePrefs.isProxiedImageHost("03.lumosgg.com"))
        assertTrue(SourcePrefs.isProxiedImageHost("02.lumosgg.com"))
        assertTrue(SourcePrefs.isProxiedImageHost("lumoskomik.com"))
        assertTrue(SourcePrefs.isProxiedImageHost("cdn.shngm.id"))
        assertFalse(SourcePrefs.isProxiedImageHost("static.mantacdn.net"))
        assertFalse(SourcePrefs.isProxiedImageHost("notlumosgg.com"))
    }
}
