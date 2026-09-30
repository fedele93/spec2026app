package com.example.data.remote

import com.example.data.BusBookingEntity
import com.example.data.EventNotificationEntity
import com.example.data.GiftCollectorEntity
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
import retrofit2.http.PATCH
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
    val treasurerEnabled: Boolean = false,
    val pushSubscriptions: Int = 0
)

/** Tutte le collezioni pubbliche in una sola risposta (GET /api/snapshot).
 *  Del campo "event" si legge solo il cassiere delle quote uniche (il resto è nel JSON locale). */
@JsonClass(generateAdapter = true)
data class SnapshotDto(
    val version: Long,
    val serverTime: Long,
    val event: SnapshotEventDto? = null,
    val guests: List<GuestEntity> = emptyList(),
    val busBookings: List<BusBookingEntity> = emptyList(),
    val wishes: List<WishEntity> = emptyList(),
    val photos: List<SharedPhotoEntity> = emptyList(),
    val giftTargets: List<GiftTargetEntity> = emptyList(),
    val notifications: List<EventNotificationEntity> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SnapshotEventDto(val giftCollector: GiftCollectorDto? = null)

/** Blocco "giftCollector" (GET /api/gifts/collector e snapshot.event). */
@JsonClass(generateAdapter = true)
data class GiftCollectorDto(
    val name: String = "",
    val roleTitle: String = "",
    val description: String = "",
    val iban: String = "",
    val ibanHolder: String = "",
    val paypalMeUrl: String = "",
    val paymentMethods: List<String> = listOf("IBAN", "PayPal", "Contanti"),
    val transferReason: String = ""
) {
    fun toEntity(): GiftCollectorEntity = GiftCollectorEntity(
        name = name, roleTitle = roleTitle, description = description, iban = iban, ibanHolder = ibanHolder,
        paypalMeUrl = paypalMeUrl, paymentMethodsCsv = paymentMethods.joinToString(","), transferReason = transferReason
    )
}

/** Parte di una quota unica destinata a un neo-specialista. */
@JsonClass(generateAdapter = true)
data class GiftPoolAllocation(val graduateId: String, val graduateName: String = "", val amount: Double)

/** Quota unica versata al cassiere e ripartita fra i neo-specialisti (POST/GET /api/gifts/pool...). */
@JsonClass(generateAdapter = true)
data class GiftPoolContribution(
    val id: Long = 0,
    val donorName: String,
    val contact: String = "",
    val paymentMethod: String = "IBAN", // IBAN | PayPal | Contanti
    val splitMode: String = "EQUAL", // EQUAL | CUSTOM
    val totalAmount: Double,
    val note: String = "",
    val status: String = "PENDING", // PENDING | RECEIVED
    val createdAt: Long = 0,
    val receivedAt: Long? = null,
    val allocations: List<GiftPoolAllocation> = emptyList()
)

val GiftPoolContribution.isReceived: Boolean get() = status == "RECEIVED"

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
data class PoolAllocationRequest(val graduateId: String, val amount: Double)

/** Quota unica: con splitMode EQUAL il server divide totalAmount fra graduateIds (vuoto = tutti);
 *  con CUSTOM usa allocations e il totale è la somma. */
@JsonClass(generateAdapter = true)
data class PoolContributionRequest(
    val donorName: String,
    val contact: String = "",
    val paymentMethod: String = "IBAN",
    val splitMode: String = "EQUAL",
    val totalAmount: Double? = null,
    val graduateIds: List<String> = emptyList(),
    val allocations: List<PoolAllocationRequest> = emptyList(),
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class PoolStatusRequest(val status: String)

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

    @GET("api/gifts/collector")
    suspend fun giftCollector(): GiftCollectorDto

    @POST("api/gifts/pool")
    suspend fun addPoolContribution(@Body body: PoolContributionRequest): GiftPoolContribution

    /** Le quote uniche registrate da questo dispositivo (X-Client-Id). */
    @GET("api/gifts/pool/mine")
    suspend fun myPoolContributions(): List<GiftPoolContribution>

    @DELETE("api/gifts/pool/{id}")
    suspend fun deletePoolContribution(@Path("id") id: Long): Response<Unit>

    /** Solo cassiere/organizzatori (il cruscotto completo è nella PWA). */
    @PATCH("api/gifts/pool/{id}/status")
    suspend fun setPoolStatus(@Path("id") id: Long, @Body body: PoolStatusRequest): GiftPoolContribution

    @GET("api/notifications")
    suspend fun notifications(@Query("since") since: Long = 0): List<EventNotificationEntity>

    @POST("api/notifications")
    suspend fun sendNotification(@Body body: NotificationRequest): EventNotificationEntity
}
