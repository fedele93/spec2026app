package com.example.data

import androidx.room.withTransaction
import com.example.data.remote.BookingRequest
import com.example.data.remote.ContributionRequest
import com.example.data.remote.GuestRequest
import com.example.data.remote.GuestStatusRequest
import com.example.data.remote.NeuroPartyApi
import com.example.data.remote.NotificationRequest
import com.example.data.remote.RemoteClient
import com.example.data.remote.SnapshotDto
import com.example.data.remote.WishRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Unico punto di accesso ai dati.
 *
 * - Senza server ([remote] == null): tutto vive nel database Room locale (modalità demo, come in v1.x).
 * - Con server: ogni scrittura va al backend e subito dopo il DB locale viene riallineato
 *   con lo snapshot del server. Room resta la "cache" da cui la UI legge tramite Flow.
 */
class EventRepository(private val db: AppDatabase) {
    /** Client verso il backend; null = modalità locale. Impostato dal ViewModel in base alle preferenze. */
    @Volatile
    var remote: NeuroPartyApi? = null

    val allGuests: Flow<List<GuestEntity>> = db.guestDao().getAllGuests()
    val allBookings: Flow<List<BusBookingEntity>> = db.busBookingDao().getAllBookings()
    val totalBookedSeats: Flow<Int> = db.busBookingDao().getTotalBookedSeats().map { it ?: 0 }
    val allWishes: Flow<List<WishEntity>> = db.wishDao().getAllWishes()
    val allPhotos: Flow<List<SharedPhotoEntity>> = db.photoDao().getAllPhotos()
    val giftTargets: Flow<List<GiftTargetEntity>> = db.giftDao().getAllTargets()
    val giftContributions: Flow<List<GiftContributionEntity>> = db.giftDao().getAllContributions()
    val allNotifications: Flow<List<EventNotificationEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadCount()

    // ------------------------------------------------------------------ sincronizzazione

    /** Scarica lo snapshot dal server e sostituisce i dati locali. Lancia [com.example.data.remote.RemoteException]. */
    suspend fun syncFromServer(): SnapshotDto {
        val api = remote ?: throw IllegalStateException("Nessun server configurato")
        val snapshot = runRemote { api.snapshot() }
        applySnapshot(snapshot)
        return snapshot
    }

    /** Sostituisce le tabelle locali con i dati del server, conservando lo stato "letta" delle notifiche. */
    suspend fun applySnapshot(snapshot: SnapshotDto) {
        db.withTransaction {
            val readIds = db.notificationDao().getReadIds().toSet()

            db.guestDao().deleteAll()
            db.guestDao().insertGuests(snapshot.guests)

            db.busBookingDao().deleteAll()
            db.busBookingDao().insertBookings(snapshot.busBookings)

            db.wishDao().deleteAll()
            db.wishDao().insertWishes(snapshot.wishes)

            db.photoDao().deleteAll()
            db.photoDao().insertPhotos(snapshot.photos)

            db.giftDao().deleteAllContributions()
            db.giftDao().deleteAllTargets()
            db.giftDao().insertTargets(snapshot.giftTargets)
            db.giftDao().insertContributions(snapshot.giftContributions)

            db.notificationDao().deleteAll()
            db.notificationDao().insertNotifications(
                snapshot.notifications.map { it.copy(isRead = it.id in readIds) }
            )
        }
    }

    suspend fun notificationsNewerThan(since: Long): List<EventNotificationEntity> =
        db.notificationDao().getNewerThan(since)

    /** Esegue una chiamata remota convertendo gli errori in [com.example.data.remote.RemoteException]. */
    private suspend fun <T> runRemote(block: suspend () -> T): T = try {
        block()
    } catch (e: Exception) {
        throw RemoteClient.toRemoteException(e)
    }

    /** Scrittura sul server seguita dal riallineamento del DB locale. */
    private suspend fun <T> writeRemote(api: NeuroPartyApi, block: suspend (NeuroPartyApi) -> T): T {
        val result = runRemote { block(api) }
        applySnapshot(runRemote { api.snapshot() })
        return result
    }

    // ------------------------------------------------------------------ invitati

    suspend fun insertGuest(guest: GuestEntity) {
        val api = remote
        if (api == null) {
            db.guestDao().insertGuest(guest)
        } else {
            writeRemote(api) {
                it.addGuest(
                    GuestRequest(
                        fullName = guest.fullName,
                        category = guest.category,
                        rsvpStatus = guest.rsvpStatus,
                        guestsCount = guest.guestsCount,
                        dietaryNotes = guest.dietaryNotes,
                        contactInfo = guest.contactInfo
                    )
                )
            }
        }
    }

    suspend fun updateGuestStatus(guest: GuestEntity, newStatus: RsvpStatus) {
        val api = remote
        if (api == null) {
            db.guestDao().updateGuest(guest.copy(rsvpStatus = newStatus, updatedAt = System.currentTimeMillis()))
        } else {
            writeRemote(api) { it.updateGuestStatus(guest.id, GuestStatusRequest(newStatus)) }
        }
    }

    suspend fun deleteGuest(guest: GuestEntity) {
        val api = remote
        if (api == null) db.guestDao().deleteGuest(guest)
        else writeRemote(api) { it.deleteGuest(guest.id) }
    }

    // ------------------------------------------------------------------ navetta

    suspend fun insertBusBooking(booking: BusBookingEntity) {
        val api = remote
        if (api == null) {
            db.busBookingDao().insertBooking(booking)
        } else {
            writeRemote(api) {
                it.addBooking(
                    BookingRequest(
                        passengerName = booking.passengerName,
                        seatsCount = booking.seatsCount,
                        pickupStop = booking.pickupStop,
                        returnTripWanted = booking.returnTripWanted,
                        contactPhone = booking.contactPhone,
                        notes = booking.notes
                    )
                )
            }
        }
    }

    suspend fun deleteBusBooking(booking: BusBookingEntity) {
        val api = remote
        if (api == null) db.busBookingDao().deleteBooking(booking)
        else writeRemote(api) { it.deleteBooking(booking.id) }
    }

    // ------------------------------------------------------------------ auguri

    suspend fun insertWish(wish: WishEntity) {
        val api = remote
        if (api == null) {
            db.wishDao().insertWish(wish)
        } else {
            writeRemote(api) {
                it.addWish(WishRequest(wish.authorName, wish.targetGraduate, wish.message, wish.emojiBadge))
            }
        }
    }

    suspend fun incrementWishHearts(wishId: Long) {
        val api = remote
        if (api == null) db.wishDao().incrementHearts(wishId)
        else writeRemote(api) { it.heartWish(wishId) }
    }

    // ------------------------------------------------------------------ foto

    /**
     * Pubblica una foto. In modalità server servono i byte dell'immagine ([imageBytes]);
     * in modalità locale viene salvato solo l'URI del photo picker.
     */
    suspend fun insertPhoto(photo: SharedPhotoEntity, imageBytes: ByteArray? = null, mimeType: String = "image/jpeg") {
        val api = remote
        if (api == null) {
            db.photoDao().insertPhoto(photo)
        } else {
            val bytes = imageBytes ?: throw IllegalArgumentException("Immagine non disponibile")
            val filePart = MultipartBody.Part.createFormData(
                "file", "photo.jpg", bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            val text = "text/plain".toMediaTypeOrNull()
            writeRemote(api) {
                it.uploadPhoto(filePart, photo.authorName.toRequestBody(text), photo.caption.toRequestBody(text))
            }
        }
    }

    suspend fun incrementPhotoLikes(photoId: Long) {
        val api = remote
        if (api == null) db.photoDao().incrementLikes(photoId)
        else writeRemote(api) { it.likePhoto(photoId) }
    }

    // ------------------------------------------------------------------ regali

    suspend fun addGiftContribution(contribution: GiftContributionEntity) {
        val api = remote
        if (api == null) {
            db.giftDao().insertContribution(contribution)
            db.giftDao().addAmountToTarget(contribution.targetGraduateId, contribution.amount)
        } else {
            writeRemote(api) {
                it.addContribution(
                    ContributionRequest(
                        donorName = contribution.donorName,
                        targetGraduateId = contribution.targetGraduateId,
                        amount = contribution.amount,
                        paymentMethod = contribution.paymentMethod,
                        note = contribution.note,
                        isAnonymous = contribution.isAnonymous
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------ notifiche

    /** Salva la notifica; con il server la invia in push a tutti (richiede il token organizzatore). Ritorna l'id.
     *  Con sendAt nel futuro (solo con il server) la notifica viene programmata e pubblicata a quell'ora. */
    suspend fun insertNotification(notification: EventNotificationEntity, sendAt: Long? = null): Long {
        val api = remote
        return if (api == null) {
            db.notificationDao().insertNotification(notification)
        } else {
            val created = writeRemote(api) {
                it.sendNotification(NotificationRequest(notification.title, notification.message, notification.category, sendAt))
            }
            created.id
        }
    }

    suspend fun markAllNotificationsAsRead() = db.notificationDao().markAllAsRead()

    // ------------------------------------------------------------------ dati demo (modalità locale)

    suspend fun prepopulateIfNeeded() {
        // Sorgente dati unica: pwa/shared/event-data.json -> SeedData.kt (codegen)
        if (db.guestDao().countGuests() == 0) {
            db.guestDao().insertGuests(SeedData.guests)
        }
        if (db.busBookingDao().countBookings() == 0) {
            db.busBookingDao().insertBookings(SeedData.busBookings)
        }
        if (db.wishDao().countWishes() == 0) {
            db.wishDao().insertWishes(SeedData.wishes)
        }
        if (db.photoDao().countPhotos() == 0) {
            db.photoDao().insertPhotos(SeedData.photos)
        }
        if (db.giftDao().countContributions() == 0) {
            db.giftDao().insertTargets(SeedData.giftTargets)
            for (c in SeedData.giftContributions) {
                db.giftDao().insertContribution(c)
            }
        }
        if (db.notificationDao().countNotifications() == 0) {
            db.notificationDao().insertNotifications(SeedData.notifications)
        }
    }
}
