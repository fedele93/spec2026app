package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.EventNotificationEntity
import com.example.data.EventRepository
import com.example.data.GiftContributionEntity
import com.example.data.GuestEntity
import com.example.data.RsvpStatus
import com.example.data.SeedData
import com.example.data.remote.RemoteClient
import com.example.data.remote.SnapshotDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Test del livello dati locale (Room in memoria) e della sincronizzazione con uno snapshot del server. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EventRepositoryRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: EventRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EventRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `prepopolazione demo inserisce i dati di SeedData una sola volta`() = runTest {
        repository.prepopulateIfNeeded()
        repository.prepopulateIfNeeded()
        assertEquals(SeedData.guests.size, repository.allGuests.first().size)
        assertEquals(SeedData.giftTargets.size, repository.giftTargets.first().size)
        assertEquals(SeedData.notifications.size, repository.allNotifications.first().size)
        val booked = SeedData.busBookings.sumOf { it.seatsCount }
        assertEquals(booked, repository.totalBookedSeats.first())
    }

    @Test
    fun `contributo regalo aggiorna il totale raccolto in modalita locale`() = runTest {
        repository.prepopulateIfNeeded()
        val before = repository.giftTargets.first().first { it.id == "luisi" }.collectedAmount
        repository.addGiftContribution(
            GiftContributionEntity(
                donorName = "Test", targetGraduateId = "luisi", targetGraduateName = "Dott. Fedele Luisi",
                amount = 75.0, paymentMethod = "IBAN"
            )
        )
        val after = repository.giftTargets.first().first { it.id == "luisi" }.collectedAmount
        assertEquals(before + 75.0, after, 0.001)
        assertEquals(SeedData.giftContributions.size + 1, repository.giftContributions.first().size)
    }

    @Test
    fun `stato RSVP e cancellazione invitato in modalita locale`() = runTest {
        repository.insertGuest(GuestEntity(fullName = "Mario Rossi", category = "Amici", rsvpStatus = RsvpStatus.PENDING))
        val guest = repository.allGuests.first().single()
        repository.updateGuestStatus(guest, RsvpStatus.CONFIRMED)
        assertEquals(RsvpStatus.CONFIRMED, repository.allGuests.first().single().rsvpStatus)
        repository.deleteGuest(repository.allGuests.first().single())
        assertTrue(repository.allGuests.first().isEmpty())
    }

    @Test
    fun `applySnapshot sostituisce i dati e conserva le notifiche gia lette`() = runTest {
        repository.prepopulateIfNeeded()
        // simula lettura di tutte le notifiche demo (id 1..n)
        repository.markAllNotificationsAsRead()
        assertEquals(0, repository.unreadNotificationsCount.first())

        val json = javaClass.classLoader!!.getResourceAsStream("snapshot.json")!!.bufferedReader().use { it.readText() }
        val snapshot = RemoteClient.moshi.adapter(SnapshotDto::class.java).fromJson(json)!!
        repository.applySnapshot(snapshot)

        assertEquals(snapshot.guests.size, repository.allGuests.first().size)
        assertEquals(snapshot.busBookings.sumOf { it.seatsCount }, repository.totalBookedSeats.first())
        assertEquals(snapshot.giftTargets.size, repository.giftTargets.first().size)
        assertEquals(snapshot.wishes.size, repository.allWishes.first().size)
        assertEquals(snapshot.notifications.size, repository.allNotifications.first().size)

        // gli id già letti localmente restano letti; le notifiche nuove (id non presenti prima) restano da leggere
        val readBefore = SeedData.notifications.indices.map { (it + 1).toLong() }.toSet()
        val expectedUnread = snapshot.notifications.count { it.id !in readBefore }
        assertEquals(expectedUnread, repository.unreadNotificationsCount.first())

        // gli id del server vengono mantenuti tali e quali (necessario per like/cuori/cancellazioni)
        val serverIds = snapshot.guests.map { it.id }.toSet()
        assertEquals(serverIds, repository.allGuests.first().map { it.id }.toSet())
    }

    @Test
    fun `notificationsNewerThan restituisce solo le notifiche successive`() = runTest {
        val now = System.currentTimeMillis()
        db.notificationDao().insertNotifications(
            listOf(
                EventNotificationEntity(title = "vecchia", message = "m", category = "Festa", timestamp = now - 10_000),
                EventNotificationEntity(title = "nuova", message = "m", category = "Navetta", timestamp = now + 10_000)
            )
        )
        val fresh = repository.notificationsNewerThan(now)
        assertEquals(listOf("nuova"), fresh.map { it.title })
    }
}
