package com.komikta.data

import com.komikta.data.remote.dto.ApiResponse
import com.komikta.data.remote.dto.BroadcastFeedDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BroadcastDtoTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test fun feedParsesBackendShape() {
        val text = """{"ok":true,"data":{"latestId":"0190aaaaaaaabbbbbbbbbbbb","items":[
            {"id":"0190aaaaaaaabbbbbbbbbbbb","title":"Halo","body":"Isi pesan","source":"","slug":"","createdAt":1730000000000}]}}"""
        val r = json.decodeFromString(ApiResponse.serializer(serializer<BroadcastFeedDto>()), text)
        assertTrue(r.ok)
        assertEquals(1, r.data!!.items.size)
        assertEquals("Halo", r.data!!.items[0].title)
        assertEquals(1730000000000L, r.data!!.items[0].createdAt)
        assertTrue(r.data!!.items[0].source.isEmpty())
    }

    @Test fun baselineResponseHasNoItems() {
        val r = json.decodeFromString(ApiResponse.serializer(serializer<BroadcastFeedDto>()), """{"ok":true,"data":{"latestId":"000000000000000000000000","items":[]}}""")
        assertTrue(r.data!!.items.isEmpty())
        assertEquals(24, r.data!!.latestId.length)
    }
}
