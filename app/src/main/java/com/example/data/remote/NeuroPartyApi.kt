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
    val canSeeLists: Boolean = true, // con i token di questo dispositivo il server manda gli elenchi completi
    val canSeeMemories: Boolean = true, // foto e auguri: solo invitati registrati (X-Guest-Token) e ruoli
    val pushSubscriptions: Int = 0,
    val roles: RolesDto? = null // ruoli sbloccati dai token di questa chiamata (server dalla 1.13 in poi)
)

/** Quali token inviati sono validi: admin/treasurer booleani, graduate = id del festeggiato o null. */
@JsonClass(generateAdapter = true)
data class RolesDto(
    val admin: Boolean = false,
    val treasurer: Boolean = false,
    val treasurerIds: List<String> = emptyList(),
    val graduate: String? = null
)

@JsonClass(generateAdapter = true)
data class ClearedDto(val deleted: Int = 0)

/** Tutte le collezioni pubbliche in una sola risposta (GET /api/snapshot).
 *  Del campo "event" si legge solo il cassiere delle quote uniche (il resto è nel JSON locale). */
@JsonClass(generateAdapter = true)
data class SnapshotDto(
    val version: Long,
    val serverTime: Long,
    val event: SnapshotEventDto? = null,
    val canSeeMemories: Boolean = true, // false = foto e auguri non inviati (serve il profilo invitato o un ruolo)
    val guests: List<GuestEntity> = emptyList(),
    val busBookings: List<BusBookingEntity> = emptyList(),
    val wishes: List<WishEntity> = emptyList(),
    val photos: List<SharedPhotoEntity> = emptyList(),
    val photosHasMore: Boolean = false, // lo snapshot porta solo le foto più recenti: le altre con photosPage
    val photosTotal: Int? = null,
    val giftTargets: List<GiftTargetEntity> = emptyList(),
    val notifications: List<EventNotificationEntity> = emptyList()
)

/** Una pagina della galleria (GET /api/photos/page), dalla più recente. */
@JsonClass(generateAdapter = true)
data class PhotosPageDto(val photos: List<SharedPhotoEntity> = emptyList(), val hasMore: Boolean = false, val total: Int = 0)

/** snapshot.event: i cassieri delle quote uniche (giftCollectors); giftCollector è il primo, per i server più vecchi. */
@JsonClass(generateAdapter = true)
data class SnapshotEventDto(val giftCollector: GiftCollectorDto? = null, val giftCollectors: List<GiftCollectorDto> = emptyList()) {
    fun collectors(): List<GiftCollectorDto> = giftCollectors.ifEmpty { listOfNotNull(giftCollector) }
}

/** Un cassiere (GET /api/gifts/collectors, GET /api/gifts/collector e snapshot.event). */
@JsonClass(generateAdapter = true)
data class GiftCollectorDto(
    val id: String = "",
    val name: String = "",
    val roleTitle: String = "",
    val description: String = "",
    val iban: String = "",
    val ibanHolder: String = "",
    val paypalMeUrl: String = "",
    val paypalContact: String = "",
    val paymentMethods: List<String> = listOf("IBAN", "PayPal", "Contanti"),
    val transferReason: String = ""
) {
    fun toEntity(sortOrder: Int = 0): GiftCollectorEntity = GiftCollectorEntity(
        id = id.ifBlank { name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') }, sortOrder = sortOrder,
        name = name, roleTitle = roleTitle, description = description, iban = iban, ibanHolder = ibanHolder,
        paypalMeUrl = paypalMeUrl, paypalContact = paypalContact, paymentMethodsCsv = paymentMethods.joinToString(","), transferReason = transferReason
    )
}

/** Parte di una quota unica destinata a un neo-specialista. */
@JsonClass(generateAdapter = true)
data class GiftPoolAllocation(val graduateId: String, val graduateName: String = "", val amount: Double)

/** Quota unica versata al cassiere e ripartita fra i neo-specialisti (POST/GET /api/gifts/pool...). */
@JsonClass(generateAdapter = true)
data class GiftPoolContribution(
    val id: Long = 0,
    val collectorId: String = "",
    val collectorName: String = "",
    val donorName: String,
    val contact: String = "",
    val email: String = "",
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

/** Richiesta del link personale via email (POST /api/profile/request-link). */
@JsonClass(generateAdapter = true)
data class ProfileLinkRequest(val email: String, val fullName: String = "")

@JsonClass(generateAdapter = true)
data class ProfileLinkResponse(val queued: Boolean = false, val created: Boolean = false, val message: String = "")

/** Il proprio profilo (GET /api/profile con X-Guest-Token): qui serve solo il nome. */
@JsonClass(generateAdapter = true)
data class ProfileDto(val guest: ProfileGuestDto)

@JsonClass(generateAdapter = true)
data class ProfileGuestDto(val id: Long = 0, val fullName: String = "")

@JsonClass(generateAdapter = true)
data class PoolAllocationRequest(val graduateId: String, val amount: Double)

/** Quota unica: con splitMode EQUAL il server divide totalAmount fra graduateIds (vuoto = tutti);
 *  con CUSTOM usa allocations e il totale è la somma. */
@JsonClass(generateAdapter = true)
data class PoolContributionRequest(
    val collectorId: String = "",
    val donorName: String,
    val contact: String = "",
    val email: String = "",
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

// ---- Organizzatori: cruscotto e import invitati (header X-Admin-Token) ----------------

@JsonClass(generateAdapter = true)
data class DashboardGuestsDto(
    val confirmed: Int = 0, val covers: Int = 0, val pending: Int = 0, val pendingCovers: Int = 0, val declined: Int = 0,
    val total: Int = 0, val withProfile: Int = 0, val withEmail: Int = 0, val consented: Int = 0, val missingMenu: Int = 0,
    val allergies: Int = 0, val updatedLastWeek: Int = 0, val byMenu: List<DashboardMenuDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DashboardMenuDto(val menu: String = "", val covers: Int = 0)

@JsonClass(generateAdapter = true)
data class DashboardBusDto(
    val maxSeats: Int = 0, val bookedSeats: Int = 0, val availableSeats: Int = 0, val waitlistSeats: Int = 0,
    val waitlistBookings: Int = 0, val checkedInSeats: Int = 0, val bookings: Int = 0, val busLocked: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DashboardMailDto(val enabled: Boolean = false, val counts: Map<String, Int> = emptyMap())

@JsonClass(generateAdapter = true)
data class DashboardPoolDto(
    val contributions: Int = 0, val pending: Int = 0, val received: Int = 0, val totalAmount: Double = 0.0, val receivedAmount: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DashboardAssistantDto(val configured: Boolean = false, val avatars: Int = 0, val ready: Int = 0)

@JsonClass(generateAdapter = true)
data class DashboardDeadlinesDto(
    val menuDeadline: String? = null, val menuLocked: Boolean = false, val busDeadline: String? = null, val busLocked: Boolean = false
)

/** Una cosa da fare: section = tab dell'app da aprire, action = pannello (mail, import, assistant, remind_menu). */
@JsonClass(generateAdapter = true)
data class DashboardTodoDto(val key: String = "", val text: String = "", val section: String? = null, val action: String? = null)

@JsonClass(generateAdapter = true)
data class DashboardDto(
    val generatedAt: Long = 0,
    val guests: DashboardGuestsDto = DashboardGuestsDto(),
    val bus: DashboardBusDto = DashboardBusDto(),
    val mail: DashboardMailDto = DashboardMailDto(),
    val pool: DashboardPoolDto = DashboardPoolDto(),
    val wishes: Int = 0,
    val photos: Int = 0,
    val pushSubscriptions: Int = 0,
    val assistant: DashboardAssistantDto = DashboardAssistantDto(),
    val deadlines: DashboardDeadlinesDto = DashboardDeadlinesDto(),
    val todo: List<DashboardTodoDto> = emptyList()
)

/** Una riga della lista invitati da importare (campi vuoti = non toccare). */
@JsonClass(generateAdapter = true)
data class ImportRowRequest(
    val fullName: String = "",
    val category: String = "",
    val email: String = "",
    val phone: String = "",
    val guestsCount: Int? = null,
    val rsvpStatus: RsvpStatus? = null
)

@JsonClass(generateAdapter = true)
data class ImportRequest(val rows: List<ImportRowRequest>, val sendLinks: Boolean = false, val dryRun: Boolean = false)

@JsonClass(generateAdapter = true)
data class ImportRowResultDto(
    val fullName: String = "", val email: String? = null, val action: String = "", val reason: String? = null,
    val changes: List<String> = emptyList(), val willInvite: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ImportResultDto(
    val dryRun: Boolean = false, val created: Int = 0, val updated: Int = 0, val unchanged: Int = 0, val skipped: Int = 0,
    val invited: Int = 0, val inviteFailed: Int = 0, val rows: List<ImportRowResultDto> = emptyList()
)

// ---- Area festeggiato (header X-Graduate-Token) -----------------------------------------

@JsonClass(generateAdapter = true)
data class GraduateInfoDto(
    val id: String = "", val name: String = "", val shortName: String = "", val specialization: String = "",
    val iban: String = "", val ibanHolder: String = "", val satispayUrl: String = "", val paypalMeUrl: String = "", val email: String? = null,
    val bancomatPay: String = "",
    val paypalEmail: String = ""
)

@JsonClass(generateAdapter = true)
data class GraduateGuestDto(val fullName: String = "", val category: String = "", val guestsCount: Int = 1, val companions: List<String> = emptyList())

@JsonClass(generateAdapter = true)
data class GraduateGuestsDto(
    val confirmedGuests: Int = 0, val covers: Int = 0, val pendingGuests: Int = 0, val pendingCovers: Int = 0,
    val confirmed: List<GraduateGuestDto> = emptyList(), val pending: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GraduateBusDto(val maxSeats: Int = 0, val bookedSeats: Int = 0, val availableSeats: Int = 0, val waitlistSeats: Int = 0, val passengers: Int = 0)

@JsonClass(generateAdapter = true)
data class GraduateAvatarDto(
    val id: String = "", val name: String = "", val shortName: String = "", val persona: String = "",
    val voiceKind: String = "", val enabled: Boolean = false, val ready: Boolean = false, val sampleFilename: String = ""
)

/** GET /api/graduate/me: chi viene, auguri, coordinate di pagamento, navetta e avatar del festeggiato. */
@JsonClass(generateAdapter = true)
data class GraduateMeDto(
    val graduate: GraduateInfoDto = GraduateInfoDto(),
    val guests: GraduateGuestsDto = GraduateGuestsDto(),
    val wishes: List<WishEntity> = emptyList(),
    val wishesForMe: Int = 0,
    val bus: GraduateBusDto = GraduateBusDto(),
    val avatar: GraduateAvatarDto? = null,
    val assistantConfigured: Boolean = false
)

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

    /** Manda all'email il link personale (crea l'invitato se c'è anche il nome). */
    @POST("api/profile/request-link")
    suspend fun requestProfileLink(@Body body: ProfileLinkRequest): ProfileLinkResponse

    /** Manda al festeggiato il link della sua area, se l'email è la sua (la risposta non lo rivela). */
    @POST("api/graduate/request-link")
    suspend fun requestGraduateLink(@Body body: ProfileLinkRequest): ProfileLinkResponse

    /** Il proprio profilo (header X-Guest-Token); 401 se il link non vale più. */
    @GET("api/profile")
    suspend fun profile(): ProfileDto

    @POST("api/wishes")
    suspend fun addWish(@Body body: WishRequest): WishEntity

    @POST("api/wishes/{id}/heart")
    suspend fun heartWish(@Path("id") id: Long): WishEntity

    /** Solo organizzatori. */
    @DELETE("api/wishes/{id}")
    suspend fun deleteWish(@Path("id") id: Long): Response<Unit>

    /** Solo organizzatori. */
    @DELETE("api/photos/{id}")
    suspend fun deletePhoto(@Path("id") id: Long): Response<Unit>

    /** Solo organizzatori: svuota la cronologia delle notifiche pubblicate. */
    @DELETE("api/notifications")
    suspend fun clearNotifications(): ClearedDto

    @Multipart
    @POST("api/photos")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Part("authorName") authorName: RequestBody,
        @Part("caption") caption: RequestBody
    ): SharedPhotoEntity

    /** Foto più vecchie di quella indicata (createdAt + id), a pagine. */
    @GET("api/photos/page")
    suspend fun photosPage(
        @Query("beforeTs") beforeTs: Long?, @Query("beforeId") beforeId: Long?, @Query("limit") limit: Int = 24
    ): PhotosPageDto

    @POST("api/photos/{id}/like")
    suspend fun likePhoto(@Path("id") id: Long): SharedPhotoEntity

    @GET("api/gifts/collector")
    suspend fun giftCollector(): GiftCollectorDto

    @GET("api/gifts/collectors")
    suspend fun giftCollectors(): List<GiftCollectorDto>

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

    /** Area festeggiato (solo con X-Graduate-Token, oppure organizzatori con ?graduate_id=). */
    @GET("api/graduate/me")
    suspend fun graduateMe(): GraduateMeDto

    /** Cruscotto organizzatori (solo con X-Admin-Token). */
    @GET("api/admin/dashboard")
    suspend fun dashboard(): DashboardDto

    /** Import della lista invitati (solo organizzatori): dryRun = anteprima senza scritture. */
    @POST("api/guests/import")
    suspend fun importGuests(@Body body: ImportRequest): ImportResultDto

    @GET("api/notifications")
    suspend fun notifications(@Query("since") since: Long = 0): List<EventNotificationEntity>

    @POST("api/notifications")
    suspend fun sendNotification(@Body body: NotificationRequest): EventNotificationEntity
}
