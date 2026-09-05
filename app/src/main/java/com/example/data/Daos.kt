package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestDao {
    @Query("SELECT * FROM guests ORDER BY fullName ASC")
    fun getAllGuests(): Flow<List<GuestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuest(guest: GuestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuests(guests: List<GuestEntity>)

    @Update
    suspend fun updateGuest(guest: GuestEntity)

    @Delete
    suspend fun deleteGuest(guest: GuestEntity)

    @Query("SELECT COUNT(*) FROM guests")
    suspend fun countGuests(): Int
}

@Dao
interface BusBookingDao {
    @Query("SELECT * FROM bus_bookings ORDER BY bookedAt DESC")
    fun getAllBookings(): Flow<List<BusBookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BusBookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BusBookingEntity>)

    @Delete
    suspend fun deleteBooking(booking: BusBookingEntity)

    @Query("SELECT SUM(seatsCount) FROM bus_bookings")
    fun getTotalBookedSeats(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM bus_bookings")
    suspend fun countBookings(): Int
}

@Dao
interface WishDao {
    @Query("SELECT * FROM wishes ORDER BY createdAt DESC")
    fun getAllWishes(): Flow<List<WishEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWish(wish: WishEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishes(wishes: List<WishEntity>)

    @Query("UPDATE wishes SET heartCount = heartCount + 1 WHERE id = :wishId")
    suspend fun incrementHearts(wishId: Long)

    @Query("SELECT COUNT(*) FROM wishes")
    suspend fun countWishes(): Int
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM shared_photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<SharedPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: SharedPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<SharedPhotoEntity>)

    @Query("UPDATE shared_photos SET likesCount = likesCount + 1 WHERE id = :photoId")
    suspend fun incrementLikes(photoId: Long)

    @Query("SELECT COUNT(*) FROM shared_photos")
    suspend fun countPhotos(): Int
}

@Dao
interface GiftDao {
    @Query("SELECT * FROM gift_targets ORDER BY id ASC")
    fun getAllTargets(): Flow<List<GiftTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<GiftTargetEntity>)

    @Update
    suspend fun updateTarget(target: GiftTargetEntity)

    @Query("SELECT * FROM gift_contributions ORDER BY contributedAt DESC")
    fun getAllContributions(): Flow<List<GiftContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: GiftContributionEntity): Long

    @Query("UPDATE gift_targets SET collectedAmount = collectedAmount + :amount WHERE id = :targetId")
    suspend fun addAmountToTarget(targetId: String, amount: Double)

    @Query("SELECT COUNT(*) FROM gift_contributions")
    suspend fun countContributions(): Int
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM event_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<EventNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: EventNotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<EventNotificationEntity>)

    @Query("UPDATE event_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("SELECT COUNT(*) FROM event_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM event_notifications")
    suspend fun countNotifications(): Int
}
