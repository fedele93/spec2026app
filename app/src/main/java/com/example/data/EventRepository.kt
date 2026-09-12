package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(private val db: AppDatabase) {
    val allGuests: Flow<List<GuestEntity>> = db.guestDao().getAllGuests()
    val allBookings: Flow<List<BusBookingEntity>> = db.busBookingDao().getAllBookings()
    val totalBookedSeats: Flow<Int> = db.busBookingDao().getTotalBookedSeats().map { it ?: 0 }
    val allWishes: Flow<List<WishEntity>> = db.wishDao().getAllWishes()
    val allPhotos: Flow<List<SharedPhotoEntity>> = db.photoDao().getAllPhotos()
    val giftTargets: Flow<List<GiftTargetEntity>> = db.giftDao().getAllTargets()
    val giftContributions: Flow<List<GiftContributionEntity>> = db.giftDao().getAllContributions()
    val allNotifications: Flow<List<EventNotificationEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadCount()

    suspend fun insertGuest(guest: GuestEntity) = db.guestDao().insertGuest(guest)
    suspend fun updateGuest(guest: GuestEntity) = db.guestDao().updateGuest(guest)
    suspend fun deleteGuest(guest: GuestEntity) = db.guestDao().deleteGuest(guest)

    suspend fun insertBusBooking(booking: BusBookingEntity) = db.busBookingDao().insertBooking(booking)
    suspend fun deleteBusBooking(booking: BusBookingEntity) = db.busBookingDao().deleteBooking(booking)

    suspend fun insertWish(wish: WishEntity) = db.wishDao().insertWish(wish)
    suspend fun incrementWishHearts(wishId: Long) = db.wishDao().incrementHearts(wishId)

    suspend fun insertPhoto(photo: SharedPhotoEntity) = db.photoDao().insertPhoto(photo)
    suspend fun incrementPhotoLikes(photoId: Long) = db.photoDao().incrementLikes(photoId)

    suspend fun addGiftContribution(contribution: GiftContributionEntity) {
        db.giftDao().insertContribution(contribution)
        db.giftDao().addAmountToTarget(contribution.targetGraduateId, contribution.amount)
    }

    suspend fun insertNotification(notification: EventNotificationEntity) =
        db.notificationDao().insertNotification(notification)

    suspend fun markAllNotificationsAsRead() = db.notificationDao().markAllAsRead()

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
