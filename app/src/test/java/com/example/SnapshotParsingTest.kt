package com.example

import com.example.data.RsvpStatus
import com.example.data.remote.RemoteClient
import com.example.data.remote.SnapshotDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica che i modelli Kotlin (entità Room + adapter Moshi generati) leggano davvero la
 * risposta di GET /api/snapshot del backend. Il file snapshot.json è stato catturato da un
 * server reale (repo neuroparty-backend) con i dati demo.
 */
class SnapshotParsingTest {

    private fun loadSnapshot(): SnapshotDto {
        val json = javaClass.classLoader!!.getResourceAsStream("snapshot.json")!!
            .bufferedReader().use { it.readText() }
        val adapter = RemoteClient.moshi.adapter(SnapshotDto::class.java)
        return adapter.fromJson(json)!!
    }

    @Test
    fun `snapshot del backend viene deserializzato nelle entita Room`() {
        val snap = loadSnapshot()
        assertTrue(snap.version >= 1)
        assertEquals(7, snap.guests.size)
        assertEquals(9, snap.giftTargets.size)
        assertTrue(snap.wishes.size >= 9)
        assertTrue(snap.notifications.size >= 4)

        val guest = snap.guests.first { it.fullName == "Dott. Luca Gatti" }
        assertEquals(RsvpStatus.PENDING, guest.rsvpStatus)
        assertEquals(2, guest.guestsCount)
        assertTrue(guest.id > 0)

        val gruppo = snap.giftTargets.first { it.id == "gruppo" }
        assertEquals(4800.0, gruppo.targetAmount, 0.001)
        assertNotNull(gruppo.iban)

        val uploaded = snap.photos.firstOrNull { it.imageUri.isNotBlank() }
        assertNotNull("il server deve restituire URL assoluti per le foto caricate", uploaded)
        assertTrue(uploaded!!.imageUri.startsWith("http"))

        // isRead è uno stato locale: dal server arriva sempre false
        assertTrue(snap.notifications.none { it.isRead })
    }

    @Test
    fun `messaggi di errore FastAPI vengono estratti dal corpo`() {
        assertEquals(
            "Posti non sufficienti (disponibili solo 3 posti).",
            RemoteClient.messageFromErrorBody("""{"detail":"Posti non sufficienti (disponibili solo 3 posti)."}""", 409)
        )
        assertEquals(
            "Il nome è obbligatorio",
            RemoteClient.messageFromErrorBody(
                """{"detail":[{"type":"value_error","loc":["body","fullName"],"msg":"Value error, Il nome è obbligatorio"}]}""",
                422
            )
        )
        assertEquals("Errore del server (500)", RemoteClient.messageFromErrorBody("<html>boom</html>", 500))
        assertEquals("Errore del server (502)", RemoteClient.messageFromErrorBody(null, 502))
    }
}
