package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.data.remote.RemoteClient
import com.example.data.remote.RemoteException
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

/** Stato della connessione al backend condiviso. */
data class ServerStatus(
    val configured: Boolean = false,
    val online: Boolean = false,
    val version: Long = -1L,
    val adminEnabled: Boolean = false,
    val lastError: String? = null
)

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EventRepository
    private val prefs: AppPrefs = AppPrefs(application)

    /** Messaggi per l'utente (errori del server, conferme) mostrati come Toast dalla MainActivity. */
    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    private val _serverStatus = MutableStateFlow(ServerStatus())
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private var syncJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = EventRepository(db)
        configureRemote()
        viewModelScope.launch {
            if (repository.remote == null) {
                repository.prepopulateIfNeeded()
            } else {
                pollServer(force = true)
            }
        }
        startWishTicker()
        startServerSync()
    }

    val serverUrl: String get() = prefs.apiBaseUrl
    val adminToken: String get() = prefs.adminToken
    val isAdmin: Boolean get() = prefs.adminToken.isNotBlank()

    val mapPoints: List<MapPoint> = SeedData.mapPoints

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
    val maxBusSeats = SeedData.maxBusSeats
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

    // ------------------------------------------------------------------ backend condiviso

    /** Crea (o rimuove) il client verso il server in base alle preferenze correnti. */
    private fun configureRemote() {
        val url = prefs.apiBaseUrl
        repository.remote = if (url.isBlank()) null
        else RemoteClient.create(url, prefs.clientId, prefs.adminToken, debugLogging = BuildConfig.DEBUG)
        _serverStatus.value = ServerStatus(configured = url.isNotBlank(), version = prefs.dataVersion)
    }

    /** Salva URL e token, riconnette e scarica subito i dati. */
    fun updateServerSettings(url: String, token: String) {
        val changedUrl = url.trim().trimEnd('/') != prefs.apiBaseUrl
        prefs.apiBaseUrl = url
        prefs.adminToken = token
        if (changedUrl) prefs.resetSyncState()
        configureRemote()
        viewModelScope.launch {
            if (repository.remote == null) {
                repository.prepopulateIfNeeded()
                _uiMessage.emit("Modalità locale attiva")
            } else if (pollServer(force = true)) {
                _uiMessage.emit("Connesso al server ✓")
            }
        }
    }

    /** Ricarica manualmente i dati dal server (es. pull-to-refresh). */
    fun refreshFromServer() {
        viewModelScope.launch { pollServer(force = true) }
    }

    private fun startServerSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (true) {
                delay(SYNC_INTERVAL_MS)
                pollServer(force = false)
            }
        }
    }

    /**
     * Interroga /api/state; se la versione dei dati è cambiata (o [force]) scarica lo snapshot,
     * aggiorna Room e mostra come notifiche di sistema gli avvisi non ancora visti.
     * @return true se la sincronizzazione è riuscita.
     */
    private suspend fun pollServer(force: Boolean): Boolean {
        val api = repository.remote ?: return false
        return try {
            val state = api.state()
            val changed = state.version != prefs.dataVersion
            if (changed || force) {
                repository.syncFromServer()
                prefs.dataVersion = state.version
                showUnseenNotifications(state.serverTime)
            }
            _serverStatus.value = ServerStatus(
                configured = true, online = true, version = state.version, adminEnabled = state.adminEnabled
            )
            true
        } catch (e: Exception) {
            val message = RemoteClient.toRemoteException(e).message
            _serverStatus.value = _serverStatus.value.copy(configured = true, online = false, lastError = message)
            if (force) _uiMessage.emit(message ?: "Server non raggiungibile")
            false
        }
    }

    private suspend fun showUnseenNotifications(serverTime: Long) {
        val lastSeen = prefs.lastSeenNotificationTs
        if (lastSeen == 0L) {
            // prima sincronizzazione: non rimostrare lo storico
            prefs.lastSeenNotificationTs = serverTime
            return
        }
        val fresh = repository.notificationsNewerThan(lastSeen)
        if (fresh.isEmpty()) return
        fresh.takeLast(MAX_SYSTEM_NOTIFICATIONS).forEach { n ->
            NotificationHelper.sendPushNotification(getApplication<Application>(), n.id.toInt(), n.title, n.message)
        }
        prefs.lastSeenNotificationTs = fresh.maxOf { it.timestamp }
    }

    /** Esegue un'operazione (locale o remota) e mostra l'eventuale errore all'utente. */
    private fun runAction(successMessage: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                if (successMessage != null && repository.remote != null) _uiMessage.emit(successMessage)
            } catch (e: RemoteException) {
                _uiMessage.emit(e.message ?: "Operazione non riuscita")
            } catch (e: Exception) {
                _uiMessage.emit(e.message ?: "Operazione non riuscita")
            }
        }
    }

    // ------------------------------------------------------------------ operazioni

    // Guest operations
    fun addGuest(
        fullName: String,
        category: String,
        status: RsvpStatus,
        guestsCount: Int,
        dietaryNotes: String,
        contactInfo: String
    ) {
        val guest = GuestEntity(
            fullName = fullName.trim(),
            category = category.trim().ifEmpty { "Invitato" },
            rsvpStatus = status,
            guestsCount = maxOf(1, guestsCount),
            dietaryNotes = dietaryNotes.trim(),
            contactInfo = contactInfo.trim()
        )
        runAction { repository.insertGuest(guest) }
    }

    fun updateGuestStatus(guest: GuestEntity, newStatus: RsvpStatus) {
        runAction { repository.updateGuestStatus(guest, newStatus) }
    }

    fun deleteGuest(guest: GuestEntity) {
        runAction { repository.deleteGuest(guest) }
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
        val requested = maxOf(1, seatsCount)
        if (!BusCapacity.canBook(bookedSeatsCount.value, requested, maxBusSeats)) {
            return false // posti esauriti o non sufficienti (il server rifà lo stesso controllo)
        }
        runAction {
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
        runAction { repository.deleteBusBooking(booking) }
    }

    // Wish operations
    fun postWish(author: String, target: String, text: String, emoji: String) {
        val wish = WishEntity(
            authorName = author.trim().ifEmpty { "Amico/a" },
            targetGraduate = target,
            message = text.trim(),
            emojiBadge = emoji.ifEmpty { "🎓" },
            heartCount = 1
        )
        runAction { repository.insertWish(wish) }
    }

    fun heartWish(wishId: Long) {
        runAction { repository.incrementWishHearts(wishId) }
    }

    // Photo operations
    fun uploadPhoto(author: String, caption: String, imageUri: String) {
        val photo = SharedPhotoEntity(
            authorName = author.trim().ifEmpty { "Invitato" },
            caption = caption.trim(),
            imageUri = imageUri,
            likesCount = 1
        )
        runAction("Foto condivisa con tutti ✓") {
            if (repository.remote == null) {
                repository.insertPhoto(photo)
            } else {
                val resolver = getApplication<Application>().contentResolver
                val uri = Uri.parse(imageUri)
                val bytes = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } }
                    ?: throw RemoteException("Impossibile leggere l'immagine selezionata")
                repository.insertPhoto(photo, bytes, resolver.getType(uri) ?: "image/jpeg")
            }
        }
    }

    fun likePhoto(photoId: Long) {
        runAction { repository.incrementPhotoLikes(photoId) }
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
        val contribution = GiftContributionEntity(
            donorName = if (isAnonymous) "Un invitato generoso" else donorName.trim().ifEmpty { "Invitato" },
            targetGraduateId = targetId,
            targetGraduateName = targetName,
            amount = amount,
            paymentMethod = paymentMethod,
            note = blessingNote.trim(),
            isAnonymous = isAnonymous
        )
        runAction { repository.addGiftContribution(contribution) }
    }

    /**
     * Notifica agli invitati: con il server viene salvata e inviata in push a tutti i dispositivi
     * (serve il token organizzatore); in ogni caso viene mostrata anche su questo telefono.
     */
    fun sendBroadcastNotification(
        context: Context,
        title: String,
        body: String,
        category: String
    ) {
        runAction("Notifica inviata a tutti ✓") {
            val notification = EventNotificationEntity(
                title = title.trim(),
                message = body.trim(),
                category = category,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            val id = repository.insertNotification(notification)
            if (repository.remote != null) {
                // già mostrata da noi: non rifarla scattare al prossimo polling
                prefs.lastSeenNotificationTs = maxOf(prefs.lastSeenNotificationTs, System.currentTimeMillis())
            }
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

    companion object {
        private const val SYNC_INTERVAL_MS = 20_000L
        private const val MAX_SYSTEM_NOTIFICATIONS = 3
    }
}

/** Regola di capienza della navetta, condivisa tra ViewModel e test. */
object BusCapacity {
    fun canBook(alreadyBooked: Int, requested: Int, maxSeats: Int): Boolean =
        requested >= 1 && alreadyBooked + requested <= maxSeats
}
