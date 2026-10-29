package com.example

import com.example.data.GuestImport
import com.example.data.RsvpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestImportTest {
    @Test
    fun `riconosce intestazione, separatore e colonne in qualsiasi ordine`() {
        val p = GuestImport.parse("Email;Nome;Persone;Stato;Telefono\nmaria@esempio.it;Maria Rossi;2;confermato;333 1\n\"x@y.it\";Senza Persone;;in attesa;\n")
        assertTrue(p.hasHeader)
        assertEquals(';', p.separator)
        assertEquals(2, p.rows.size)
        assertEquals("Maria Rossi", p.rows[0].fullName)
        assertEquals("maria@esempio.it", p.rows[0].email)
        assertEquals(2, p.rows[0].guestsCount)
        assertEquals(RsvpStatus.CONFIRMED, p.rows[0].rsvpStatus)
        assertEquals("333 1", p.rows[0].phone)
        assertNull(p.rows[1].guestsCount)
        assertEquals(RsvpStatus.PENDING, p.rows[1].rsvpStatus)
    }

    @Test
    fun `senza intestazione usa l'ordine fisso e salta le righe vuote`() {
        val p = GuestImport.parse("Luca Bianchi\tAmici\tLUCA@x.it\t\t3\n\n\t\t\t\t\n")
        assertFalse(p.hasHeader)
        assertEquals('\t', p.separator)
        assertEquals(1, p.rows.size)
        assertEquals("luca@x.it", p.rows[0].email)
        assertEquals("Amici", p.rows[0].category)
        assertEquals(3, p.rows[0].guestsCount)
    }

    @Test
    fun `intestazione con accenti e virgole`() {
        val p = GuestImport.parse("NOME,Categorìa,E-mail\nAnna,Familiari,anna@x.it")
        assertTrue(p.hasHeader)
        assertEquals("Familiari", p.rows[0].category)
        assertEquals("anna@x.it", p.rows[0].email)
        assertTrue(GuestImport.parse("").rows.isEmpty())
    }
}
