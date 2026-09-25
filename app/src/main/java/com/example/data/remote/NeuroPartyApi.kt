package com.example.data.remote

import com.example.data.BusBookingEntity
import com.example.data.EventNotificationEntity
import com.example.data.GiftContributionEntity
import com.example.data.GiftTargetEntity
import com.example.data.GuestEntity
import com.example.data.RsvpStatus
import com.example.data.SharedPhotoEntity
import com.example.data.WishEntity
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// ---- Modelli di risposta -------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class StateDto(
    val version: Long,
    val serverTime: Long,
    val adminEnabled: Boolean = false,
    val pushSubscriptions: Int = 0
)

/** Tutte le collezioni in una sola risposta (GET /api/snapshot). Il campo "event" viene ignorato. */
@JsonClass(generateAdapter = true)
data class SnapshotDto(
    val version: Long,
    val serverTime: Long,
    val guests: List<GuestEntity> = emptyList(),
    val busBookings: List<BusBookingEntity> = emptyList(),
    val wishes: List<WishEntity> = emptyList(),
    val photos: List<SharedPhotoEntity> = emptyList(),
    val giftTargets: List<GiftTargetEntity> = emptyList(),
    val giftContributions: List<GiftContributionEntity> = emptyList(),
    val notifications: List<EventNotificationEntity> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ContributionResponse(
    val contribution: GiftContributionEntity,
    val target: GiftTargetEntity
)

@JsonClass(generateAdapter = true)
data class BusSummaryDto(val maxSeats: Int, val bookedSeats: Int, val availableSeats: Int)

// ---- Corpi delle richieste ----------------------------------------------------------

@JsonClass(generateAdapter = true)
data class GuestRequest(
    val fullName: String,
    val category: String,
    val rsvpStatus: RsvpStatus,
    val guestsCount: Int,
    val dietaryNotes: String,
    val contactInfo: String
)

@JsonClass(generateAdapter = true)
data class GuestStatusRequest(val rsvpStatus: RsvpStatus)

@JsonClass(generateAdapter = true)
data class BookingRequest(
    val passengerName: String,
    val seatsCount: Int,
    val pickupStop: String,
    val returnTripWanted: Boolean,
    val contactPhone: String,
    val notes: String
)

@JsonClass(generateAdapter = true)
data class WishRequest(
    val authorName: String,
    val targetGraduate: String,
    val message: String,
    val emojiBadge: String
)

@JsonClass(generateAdapter = true)
data class ContributionRequest(
    val donorName: String,
    val targetGraduateId: String,
    val amount: Double,
    val paymentMethod: String,
    val note: String,
    val isAnonymous: Boolean
)

@JsonClass(generateAdapter = true)
/** sendAt (ms): se nel futuro il server programma la notifica invece di inviarla subito. */
data class NotificationRequest(val title: String, val message: String, val category: String, val sendAt: Long? = null)

// ---- Interfaccia Retrofit -----------------------------------------------------------

interface NeuroPartyApi {
    @GET("api/state")
    suspend fun state(): StateDto

    @GET("api/snapshot")
    suspend fun snapshot(): SnapshotDto

    @GET("api/bus/summary")
    suspend fun busSummary(): BusSummaryDto

    @POST("api/guests")
    suspend fun addGuest(@Body body: GuestRequest): GuestEntity

    @PUT("api/guests/{id}")
    suspend fun updateGuestStatus(@Path("id") id: Long, @Body body: GuestStatusRequest): GuestEntity

    @DELETE("api/guests/{id}")
    suspend fun deleteGuest(@Path("id") id: Long): Response<Unit>

    @POST("api/bus/bookings")
    suspend fun addBooking(@Body body: BookingRequest): BusBookingEntity

    @DELETE("api/bus/bookings/{id}")
    suspend fun deleteBooking(@Path("id") id: Long): Response<Unit>

    @POST("api/wishes")
    suspend fun addWish(@Body body: WishRequest): WishEntity

    @POST("api/wishes/{id}/heart")
    suspend fun heartWish(@Path("id") id: Long): WishEntity

    @Multipart
    @POST("api/photos")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Part("authorName") authorName: RequestBody,
        @Part("caption") caption: RequestBody
    ): SharedPhotoEntity

    @POST("api/photos/{id}/like")
    suspend fun likePhoto(@Path("id") id: Long): SharedPhotoEntity

    @POST("api/gifts/contributions")
    suspend fun addContribution(@Body body: ContributionRequest): ContributionResponse

    @GET("api/notifications")
    suspend fun notifications(@Query("since") since: Long = 0): List<EventNotificationEntity>

    @POST("api/notifications")
    suspend fun sendNotification(@Body body: NotificationRequest): EventNotificationEntity
}
