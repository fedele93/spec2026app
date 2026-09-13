package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

// Le entità Room sono anche i modelli JSON dell'API del backend (stessi nomi di campo, camelCase).

enum class RsvpStatus {
    CONFIRMED,
    PENDING,
    DECLINED
}

@Entity(tableName = "guests")
@JsonClass(generateAdapter = true)
data class GuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val category: String, // es. "Colleghi Reparto", "Famiglia", "Amici Università"
    val rsvpStatus: RsvpStatus,
    val guestsCount: Int = 1, // compreso l'invitato
    val dietaryNotes: String = "", // es. Celiaco, Vegetariano, Nessuna
    val contactInfo: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bus_bookings")
@JsonClass(generateAdapter = true)
data class BusBookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val passengerName: String,
    val seatsCount: Int = 1,
    val pickupStop: String = "Policlinico (Piazzale Principale)",
    val returnTripWanted: Boolean = true,
    val contactPhone: String = "",
    val notes: String = "",
    val bookedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wishes")
@JsonClass(generateAdapter = true)
data class WishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val targetGraduate: String, // "Tutti i Laureandi" o nome specifico
    val message: String,
    val emojiBadge: String = "🎓",
    val heartCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shared_photos")
@JsonClass(generateAdapter = true)
data class SharedPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val caption: String,
    val imageResId: Int = 0, // se drawable
    val imageUri: String = "", // se da photo picker / camera
    val likesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "gift_targets")
@JsonClass(generateAdapter = true)
data class GiftTargetEntity(
    @PrimaryKey val id: String, // es. "andrea", "elena", "marco", "gruppo"
    val name: String,
    val specialization: String,
    val roleTitle: String,
    val giftTitle: String,
    val giftDescription: String,
    val targetAmount: Double,
    val collectedAmount: Double,
    val iban: String,
    val ibanHolder: String,
    val satispayUrl: String,
    val paypalMeUrl: String
)

@Entity(tableName = "gift_contributions")
@JsonClass(generateAdapter = true)
data class GiftContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val donorName: String,
    val targetGraduateId: String,
    val targetGraduateName: String,
    val amount: Double,
    val paymentMethod: String, // "IBAN", "Satispay", "PayPal", "Contanti"
    val note: String = "",
    val isAnonymous: Boolean = false,
    val contributedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "event_notifications")
@JsonClass(generateAdapter = true)
data class EventNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String, // "Seduta", "Festa", "Navetta", "Organizzazione"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
