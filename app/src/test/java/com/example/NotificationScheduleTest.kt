package com.example

import com.example.ui.components.parseSendAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/** Il campo "Programma invio" del dialogo notifiche accetta "gg/mm hh:mm" e "gg/mm/aaaa hh:mm". */
class NotificationScheduleTest {

    private fun expected(y: Int, m: Int, d: Int, h: Int, mi: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, mi, 0) }.timeInMillis

    @Test
    fun `data con anno esplicito`() {
        assertEquals(expected(2026, 11, 13, 19, 45), parseSendAt("13/11/2026 19:45"))
        assertEquals(expected(2026, 11, 13, 19, 45), parseSendAt(" 13/11/2026 19.45 "))
    }

    @Test
    fun `senza anno usa l'anno corrente`() {
        val now = Calendar.getInstance().apply { clear(); set(2026, 8, 25, 12, 0, 0) }
        assertEquals(expected(2026, 11, 13, 20, 30), parseSendAt("13/11 20:30", now))
    }

    @Test
    fun `testi non validi`() {
        assertNull(parseSendAt(""))
        assertNull(parseSendAt("domani alle 8"))
        assertNull(parseSendAt("32/11 19:45"))
        assertNull(parseSendAt("13/13 19:45"))
        assertNull(parseSendAt("13/11 25:00"))
        assertNull(parseSendAt("13/11"))
    }
}
