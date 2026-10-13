package com.example

import com.example.data.remote.DashboardDto
import com.example.data.remote.GraduateMeDto
import com.example.data.remote.ImportResultDto
import com.example.data.remote.RemoteClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gli adapter Moshi generati leggono le risposte di /api/graduate/me, /api/admin/dashboard e /api/guests/import. */
class GraduateMeParsingTest {
    @Test
    fun `area festeggiato con campi extra e avatar nullo`() {
        val json = """{"graduate":{"id":"luisi","name":"Dott. Fedele Luisi","shortName":"Fedele Luisi","iban":"IT00","ibanHolder":"Fedele","paypalMeUrl":"https://paypal.me/x","satispayUrl":"","email":null,"giftTitle":"x"},
            "guests":{"confirmedGuests":2,"covers":5,"pendingGuests":1,"pendingCovers":1,"declinedGuests":0,"byMenu":[],"confirmed":[{"fullName":"Anna","category":"Amici","guestsCount":2,"companions":["Luca"]}],"pending":["Bruno"]},
            "wishes":[{"id":1,"authorName":"Nonna","targetGraduate":"Fedele Luisi","message":"Bravo","emojiBadge":"🎓","heartCount":3,"createdAt":1700000000000}],
            "wishesForMe":1,"bus":{"maxSeats":54,"bookedSeats":10,"availableSeats":44,"waitlistSeats":0,"waitlistBookings":0,"checkedInSeats":0,"busDeadline":null,"busLocked":false,"passengers":10},
            "avatar":null,"assistantConfigured":true}"""
        val me = RemoteClient.moshi.adapter(GraduateMeDto::class.java).fromJson(json)
        assertNotNull(me)
        assertEquals("luisi", me!!.graduate.id)
        assertNull(me.graduate.email)
        assertEquals(listOf("Luca"), me.guests.confirmed[0].companions)
        assertEquals("Nonna", me.wishes[0].authorName)
        assertEquals(10, me.bus.passengers)
        assertNull(me.avatar)
        assertTrue(me.assistantConfigured)
    }

    @Test
    fun `cruscotto e risultato import`() {
        val dash = RemoteClient.moshi.adapter(DashboardDto::class.java).fromJson(
            """{"generatedAt":1,"guests":{"confirmed":3,"covers":7,"missingMenu":2,"byMenu":[{"menu":"VEGANO","covers":1}]},"bus":{"maxSeats":54},
               "mail":{"enabled":false,"counts":{"DELIVERED":2,"FAILED":1},"failed":[]},"pool":{"contributions":1,"pending":1,"receivedAmount":0},
               "wishes":4,"photos":2,"pushSubscriptions":0,"assistant":{"configured":true,"avatars":9,"ready":1},"deadlines":{"menuDeadline":"2026-11-06"},
               "todo":[{"key":"pending","text":"3 invitati in attesa","section":"rsvp"},{"key":"mail_off","text":"Email disattivate","action":"mail"}]}"""
        )!!
        assertEquals(7, dash.guests.covers)
        assertEquals(1, dash.mail.counts["FAILED"])
        assertEquals("rsvp", dash.todo[0].section)
        assertEquals("mail", dash.todo[1].action)
        assertEquals("2026-11-06", dash.deadlines.menuDeadline)

        val res = RemoteClient.moshi.adapter(ImportResultDto::class.java).fromJson(
            """{"dryRun":true,"created":1,"updated":0,"unchanged":0,"skipped":1,"invited":0,"rows":[{"fullName":"A","email":"a@x.it","action":"created","changes":[],"willInvite":true},{"fullName":"","action":"skipped","reason":"nome mancante"}]}"""
        )!!
        assertTrue(res.dryRun)
        assertEquals("created", res.rows[0].action)
        assertEquals("nome mancante", res.rows[1].reason)
    }
}
