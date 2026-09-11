package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MapPoint(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val iconType: String, // "GRADUATION", "BUS", "PARTY"
    val description: String
)

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EventRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = EventRepository(db)
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
        }
        startWishTicker()
    }

    val mapPoints = listOf(
        MapPoint(
            id = "seduta",
            title = "Seduta di Specializzazione & Proclamazione",
            subtitle = "Discussione Tesi & Brindisi",
            timeLabel = "9 Novembre - ora da definire",
            address = "Aula Magna \"G. De Benedictis\", AOUC Policlinico di Bari, Piazza Giulio Cesare 11, Bari",
            latitude = 41.1173,
            longitude = 16.8719,
            iconType = "GRADUATION",
            description = "Aula Magna \"G. De Benedictis\" del Policlinico di Bari. Arrivare qualche minuto prima dell'orario di inizio (in via di definizione)."
        ),
        MapPoint(
            id = "bus",
            title = "Partenza Autobus Navetta",
            subtitle = "Punto di Ritrovo Transfer Gratuito",
            timeLabel = "Venerdì 13 - ritrovo da definire",
            address = "Piazzale Policlinico di Bari (fronte ingresso principale)",
            latitude = 41.1170,
            longitude = 16.8715,
            iconType = "BUS",
            description = "Navetta riservata 54 posti verso la location della festa (luogo da definire). Orari e fermate di andata e ritorno saranno confermati prossimamente."
        ),
        MapPoint(
            id = "festa",
            title = "Festa di Specializzazione",
            subtitle = "Aperitivo, Cena, Dj Set & Torta",
            timeLabel = "Venerdì 13 novembre - ora da definire",
            address = "Location da definire (Bari e dintorni)",
            latitude = 41.1100,
            longitude = 16.8600,
            iconType = "PARTY",
            description = "La location della festa di venerdì 13 novembre sarà comunicata a breve. Dress code: Elegant Chic."
        )
    )

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedMapPoint = MutableStateFlow(mapPoints[0])
    val selectedMapPoint: StateFlow<MapPoint> = _selectedMapPoint.asStateFlow()

    // Guests & RSVP
    val allGuests = repository.allGuests.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _guestFilter = MutableStateFlow<RsvpStatus?>(null)
    val guestFilter: StateFlow<RsvpStatus?> = _guestFilter.asStateFlow()

    private val _guestSearch = MutableStateFlow("")
    val guestSearch: StateFlow<String> = _guestSearch.asStateFlow()

    val filteredGuests: StateFlow<List<GuestEntity>> = combine(
        allGuests,
        _guestFilter,
        _guestSearch
    ) { guests, filter, query ->
        guests.filter { guest ->
            val matchesFilter = filter == null || guest.rsvpStatus == filter
            val matchesQuery = query.isBlank() ||
                    guest.fullName.contains(query, ignoreCase = true) ||
                    guest.category.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bus Bookings
    val maxBusSeats = 54
    val busBookings = repository.allBookings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val bookedSeatsCount = repository.totalBookedSeats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    // Wishes & Ticker
    val wishes = repository.allWishes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _tickerIndex = MutableStateFlow(0)
    val tickerIndex: StateFlow<Int> = _tickerIndex.asStateFlow()

    // Photos
    val photos = repository.allPhotos.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Gifts
    val giftTargets = repository.giftTargets.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val giftContributions = repository.giftContributions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Notifications
    val notifications = repository.allNotifications.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val unreadNotifications = repository.unreadNotificationsCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSelectedMapPoint(point: MapPoint) {
        _selectedMapPoint.value = point
    }

    fun setGuestFilter(filter: RsvpStatus?) {
        _guestFilter.value = filter
    }

    fun setGuestSearch(query: String) {
        _guestSearch.value = query
    }

    // Ticker loop: switches wishes periodically every 5 seconds
    private fun startWishTicker() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                val currentList = wishes.value
                if (currentList.isNotEmpty()) {
                    _tickerIndex.value = (_tickerIndex.value + 1) % currentList.size
                }
            }
        }
    }

    // Guest operations
    fun addGuest(
        fullName: String,
        category: String,
        status: RsvpStatus,
        guestsCount: Int,
        dietaryNotes: String,
        contactInfo: String
    ) {
        viewModelScope.launch {
            val guest = GuestEntity(
                fullName = fullName.trim(),
                category = category.trim().ifEmpty { "Invitato" },
                rsvpStatus = status,
                guestsCount = maxOf(1, guestsCount),
                dietaryNotes = dietaryNotes.trim(),
                contactInfo = contactInfo.trim()
            )
            repository.insertGuest(guest)
        }
    }

    fun updateGuestStatus(guest: GuestEntity, newStatus: RsvpStatus) {
        viewModelScope.launch {
            repository.updateGuest(
                guest.copy(
                    rsvpStatus = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteGuest(guest: GuestEntity) {
        viewModelScope.launch {
            repository.deleteGuest(guest)
        }
    }

    // Bus booking operations
    fun bookBus(
        passengerName: String,
        seatsCount: Int,
        pickupStop: String,
        returnTrip: Boolean,
        phone: String,
        notes: String
    ): Boolean {
        val currentBooked = bookedSeatsCount.value
        val requested = maxOf(1, seatsCount)
        if (currentBooked + requested > maxBusSeats) {
            return false // posti esauriti o non sufficienti
        }
        viewModelScope.launch {
            repository.insertBusBooking(
                BusBookingEntity(
                    passengerName = passengerName.trim(),
                    seatsCount = requested,
                    pickupStop = pickupStop,
                    returnTripWanted = returnTrip,
                    contactPhone = phone.trim(),
                    notes = notes.trim()
                )
            )
        }
        return true
    }

    fun cancelBusBooking(booking: BusBookingEntity) {
        viewModelScope.launch {
            repository.deleteBusBooking(booking)
        }
    }

    // Wish operations
    fun postWish(author: String, target: String, text: String, emoji: String) {
        viewModelScope.launch {
            val wish = WishEntity(
                authorName = author.trim().ifEmpty { "Amico/a" },
                targetGraduate = target,
                message = text.trim(),
                emojiBadge = emoji.ifEmpty { "🎓" },
                heartCount = 1
            )
            repository.insertWish(wish)
        }
    }

    fun heartWish(wishId: Long) {
        viewModelScope.launch {
            repository.incrementWishHearts(wishId)
        }
    }

    // Photo operations
    fun uploadPhoto(author: String, caption: String, imageUri: String) {
        viewModelScope.launch {
            val photo = SharedPhotoEntity(
                authorName = author.trim().ifEmpty { "Invitato" },
                caption = caption.trim(),
                imageUri = imageUri,
                likesCount = 1
            )
            repository.insertPhoto(photo)
        }
    }

    fun likePhoto(photoId: Long) {
        viewModelScope.launch {
            repository.incrementPhotoLikes(photoId)
        }
    }

    // Gift contribution operations
    fun contributeToGift(
        donorName: String,
        targetId: String,
        targetName: String,
        amount: Double,
        paymentMethod: String,
        blessingNote: String,
        isAnonymous: Boolean
    ) {
        viewModelScope.launch {
            val contribution = GiftContributionEntity(
                donorName = if (isAnonymous) "Un invitato generoso" else donorName.trim().ifEmpty { "Invitato" },
                targetGraduateId = targetId,
                targetGraduateName = targetName,
                amount = amount,
                paymentMethod = paymentMethod,
                note = blessingNote.trim(),
                isAnonymous = isAnonymous
            )
            repository.addGiftContribution(contribution)
        }
    }

    // Real Push Notification trigger
    fun sendBroadcastNotification(
        context: Context,
        title: String,
        body: String,
        category: String
    ) {
        viewModelScope.launch {
            val notification = EventNotificationEntity(
                title = title.trim(),
                message = body.trim(),
                category = category,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            val id = repository.insertNotification(notification)
            // Trigger actual system notification
            NotificationHelper.sendPushNotification(
                context = context,
                notificationId = id.toInt(),
                title = title,
                body = body
            )
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }
}
