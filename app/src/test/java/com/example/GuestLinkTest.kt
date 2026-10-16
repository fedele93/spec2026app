package com.example

import com.example.data.GuestLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Link personale incollato nell'app: se ne ricava il token del profilo (X-Guest-Token). */
class GuestLinkTest {
    @Test
    fun extractsTokenFromLinkOrBareCode() {
        val token = "AbCdEf0123456789_-xyzXYZ0123456789"
        assertEquals(token, GuestLink.extractToken("https://festa.example.org/#profilo=$token"))
        assertEquals(token, GuestLink.extractToken("  https://festa.example.org/?x=1#profilo=$token\n"))
        assertEquals(token, GuestLink.extractToken(token))
    }

    @Test
    fun graduateTokenFromLinkOrPlainToken() {
        val token = "Zz9_-AbCdEf0123456789abcdefghij"
        assertEquals(token, GuestLink.graduateTokenFrom("https://festa.example.org/#festeggiato=$token"))
        assertEquals("grad-luisi", GuestLink.graduateTokenFrom("  grad-luisi "))
        assertEquals("", GuestLink.graduateTokenFrom(""))
    }

    @Test
    fun rejectsTextThatIsNotALink() {
        assertNull(GuestLink.extractToken(""))
        assertNull(GuestLink.extractToken("https://festa.example.org/"))
        assertNull(GuestLink.extractToken("ciao come stai"))
        assertNull(GuestLink.extractToken("#profilo=corto"))
    }
}
