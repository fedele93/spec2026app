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
        if (db.guestDao().countGuests() == 0) {
            val initialGuests = listOf(
                GuestEntity(
                    fullName = "Prof. Giancarlo Bianchi",
                    category = "Docenti e Reparto",
                    rsvpStatus = RsvpStatus.CONFIRMED,
                    guestsCount = 2,
                    dietaryNotes = "Nessuna restrizione",
                    contactInfo = "giancarlo.bianchi@unipol.it"
                ),
                GuestEntity(
                    fullName = "Dott.ssa Sofia Valenti",
                    category = "Specializzandi 1° e 2° anno",
                    rsvpStatus = RsvpStatus.CONFIRMED,
                    guestsCount = 1,
                    dietaryNotes = "Opzione Vegetariana",
                    contactInfo = "333 4567891"
                ),
                GuestEntity(
                    fullName = "Matteo Moretti & Famiglia",
                    category = "Famigliari",
                    rsvpStatus = RsvpStatus.CONFIRMED,
                    guestsCount = 4,
                    dietaryNotes = "1 Celiaco (menu senza glutine)",
                    contactInfo = "340 1122334"
                ),
                GuestEntity(
                    fullName = "Dott. Luca Gatti",
                    category = "Colleghi Reparto",
                    rsvpStatus = RsvpStatus.PENDING,
                    guestsCount = 2,
                    dietaryNotes = "Nessuna",
                    contactInfo = "348 9988776"
                ),
                GuestEntity(
                    fullName = "Giulia Colombo",
                    category = "Amici Università",
                    rsvpStatus = RsvpStatus.CONFIRMED,
                    guestsCount = 1,
                    dietaryNotes = "Nessuna",
                    contactInfo = "giulia.colombo@gmail.com"
                ),
                GuestEntity(
                    fullName = "Dott. Roberto De Luca",
                    category = "Docenti e Reparto",
                    rsvpStatus = RsvpStatus.DECLINED,
                    guestsCount = 0,
                    dietaryNotes = "Turno di guardia in Neuro-Rianimazione",
                    contactInfo = "r.deluca@policlinico.it"
                ),
                GuestEntity(
                    fullName = "Martina Ferri",
                    category = "Famigliari",
                    rsvpStatus = RsvpStatus.CONFIRMED,
                    guestsCount = 2,
                    dietaryNotes = "Nessuna",
                    contactInfo = "329 5544332"
                )
            )
            db.guestDao().insertGuests(initialGuests)
        }

        if (db.busBookingDao().countBookings() == 0) {
            val initialBookings = listOf(
                BusBookingEntity(
                    passengerName = "Dott.ssa Sofia Valenti",
                    seatsCount = 1,
                    pickupStop = "Policlinico (Piazzale Principale)",
                    returnTripWanted = true,
                    contactPhone = "333 4567891",
                    notes = "Pronta subito dopo le proclamazioni"
                ),
                BusBookingEntity(
                    passengerName = "Giulia Colombo & Gruppo Amici",
                    seatsCount = 3,
                    pickupStop = "Policlinico (Piazzale Principale)",
                    returnTripWanted = true,
                    contactPhone = "340 5566778",
                    notes = "Con festoni e regali"
                ),
                BusBookingEntity(
                    passengerName = "Specializzandi Neurologia Turno A",
                    seatsCount = 6,
                    pickupStop = "Policlinico (Fermata Metro/Navetta)",
                    returnTripWanted = true,
                    contactPhone = "349 1122339",
                    notes = "Tutti insieme"
                )
            )
            db.busBookingDao().insertBookings(initialBookings)
        }

        if (db.wishDao().countWishes() == 0) {
            val initialWishes = listOf(
                WishEntity(
                    authorName = "Prof. Giancarlo Bianchi",
                    targetGraduate = "Tutti i Laureandi",
                    message = "Congratulazioni di cuore ai nostri nuovi specialisti! Avete dimostrato dedizione, rigore scientifico e grande umanità. Buona strada in Neurologia!",
                    emojiBadge = "🧠",
                    heartCount = 24
                ),
                WishEntity(
                    authorName = "I tuoi colleghi di reparto",
                    targetGraduate = "Dott. Andrea Riva",
                    message = "Grande Andrea! Tra turni di notte interminabili ed EEG complessi sei arrivato al traguardo più bello. Fieri di te!",
                    emojiBadge = "⚡",
                    heartCount = 18
                ),
                WishEntity(
                    authorName = "Mamma e Papà",
                    targetGraduate = "Dott.ssa Elena Moretti",
                    message = "Elena carissima, vedere coronato il tuo sogno di diventare neurologa ci riempie di orgoglio e gioia immensa. Ti vogliamo bene!",
                    emojiBadge = "❤️",
                    heartCount = 31
                ),
                WishEntity(
                    authorName = "Gli amici del calcetto e della facoltà",
                    targetGraduate = "Dott. Marco Ferri",
                    message = "Adesso sei ufficialmente un 'Brain Master'! Stasera si brinda senza sosta alla tua salute!",
                    emojiBadge = "🥂",
                    heartCount = 15
                )
            )
            db.wishDao().insertWishes(initialWishes)
        }

        if (db.photoDao().countPhotos() == 0) {
            val initialPhotos = listOf(
                SharedPhotoEntity(
                    authorName = "Staff Organizzazione",
                    caption = "L'Aula Magna del Policlinico è pronta per accogliere la seduta di specializzazione!",
                    imageResId = 1,
                    imageUri = "",
                    likesCount = 28
                ),
                SharedPhotoEntity(
                    authorName = "Elena Moretti",
                    caption = "Tesi rilegata, corona d'alloro pronta: conto alla rovescia!",
                    imageResId = 2,
                    imageUri = "",
                    likesCount = 42
                ),
                SharedPhotoEntity(
                    authorName = "Andrea & Marco",
                    caption = "Ultimo turno insieme da specializzandi, da stasera Neurologi ufficiali!",
                    imageResId = 3,
                    imageUri = "",
                    likesCount = 35
                )
            )
            db.photoDao().insertPhotos(initialPhotos)
        }

        if (db.giftDao().countContributions() == 0) {
            val initialTargets = listOf(
                GiftTargetEntity(
                    id = "gruppo",
                    name = "Regalo Comune Specializzandi",
                    specialization = "Specializzazione in Neurologia 2026",
                    roleTitle = "Andrea, Elena & Marco",
                    giftTitle = "Viaggio Congresso Europeo di Neurologia & Brindisi di Classe",
                    giftDescription = "Quota comune per sostenere la partecipazione al congresso EAN (European Academy of Neurology) e la festa di stasera!",
                    targetAmount = 2400.0,
                    collectedAmount = 1450.0,
                    iban = "IT78 K030 6909 6061 0000 1234 567",
                    ibanHolder = "Comitato Festa Neurologia (Andrea Riva)",
                    satispayUrl = "https://tag.satispay.com/festaneurologia",
                    paypalMeUrl = "https://paypal.me/festaneurologia2026"
                ),
                GiftTargetEntity(
                    id = "andrea",
                    name = "Dott. Andrea Riva",
                    specialization = "Epilessia & Neurofisiologia Clinica",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Stetoscopio Digitale Littmann & Fellowship Clinica",
                    giftDescription = "Contributo dedicato per lo strumento diagnostico avanzato e per l'inizio dell'attività ospedaliera di Andrea.",
                    targetAmount = 900.0,
                    collectedAmount = 520.0,
                    iban = "IT44 X030 6909 6061 0000 9876 543",
                    ibanHolder = "Andrea Riva",
                    satispayUrl = "https://tag.satispay.com/andrearivaneuro",
                    paypalMeUrl = "https://paypal.me/andrearivamd"
                ),
                GiftTargetEntity(
                    id = "elena",
                    name = "Dott.ssa Elena Moretti",
                    specialization = "Cefalee e Malattie Neurodegenerative",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Oftalmoscopio Professionale & Borsa Medico in Cuoio",
                    giftDescription = "Regalo personalizzato per le visite ambulatoriali e il master in patologie neurodegenerative di Elena.",
                    targetAmount = 950.0,
                    collectedAmount = 680.0,
                    iban = "IT12 Y030 6909 6061 0000 4567 890",
                    ibanHolder = "Elena Moretti",
                    satispayUrl = "https://tag.satispay.com/elenamorettimd",
                    paypalMeUrl = "https://paypal.me/elenamorettimd"
                ),
                GiftTargetEntity(
                    id = "marco",
                    name = "Dott. Marco Ferri",
                    specialization = "Stroke Unit & Neuro-Vascolare",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Corso Neurosonologia Doppler & Attrezzatura Studio",
                    giftDescription = "Regalo dedicato per la certificazione in ecocolordoppler transcranico e dotazione clinica di Marco.",
                    targetAmount = 850.0,
                    collectedAmount = 430.0,
                    iban = "IT99 Z030 6909 6061 0000 3210 987",
                    ibanHolder = "Marco Ferri",
                    satispayUrl = "https://tag.satispay.com/marcoferrimd",
                    paypalMeUrl = "https://paypal.me/marcoferrimd"
                )
            )
            db.giftDao().insertTargets(initialTargets)

            val initialContributions = listOf(
                GiftContributionEntity(
                    donorName = "Zia Laura & Famiglia",
                    targetGraduateId = "elena",
                    targetGraduateName = "Dott.ssa Elena Moretti",
                    amount = 150.0,
                    paymentMethod = "IBAN",
                    note = "Per la nostra neurologa preferita! Con affetto infinito."
                ),
                GiftContributionEntity(
                    donorName = "Colleghi Reparto Stroke",
                    targetGraduateId = "marco",
                    targetGraduateName = "Dott. Marco Ferri",
                    amount = 100.0,
                    paymentMethod = "Satispay",
                    note = "Per il futuro re delle trombolisi! Forza Marco!"
                ),
                GiftContributionEntity(
                    donorName = "Famiglia Riva",
                    targetGraduateId = "andrea",
                    targetGraduateName = "Dott. Andrea Riva",
                    amount = 200.0,
                    paymentMethod = "IBAN",
                    note = "Orgogliosi del tuo percorso impeccabile."
                ),
                GiftContributionEntity(
                    donorName = "Amici del Liceo",
                    targetGraduateId = "gruppo",
                    targetGraduateName = "Regalo Comune Specializzandi",
                    amount = 120.0,
                    paymentMethod = "PayPal",
                    note = "Brindiamo a tutti voi stasera!"
                )
            )
            for (c in initialContributions) {
                db.giftDao().insertContribution(c)
            }
        }

        if (db.notificationDao().countNotifications() == 0) {
            val initialNotifications = listOf(
                EventNotificationEntity(
                    title = "🎓 Benvenuti all'evento di Specializzazione!",
                    message = "L'app ufficiale per la Laurea in Neurologia è attiva. Controlla il programma, prenota il bus e conferma il tuo RSVP!",
                    category = "Organizzazione",
                    timestamp = System.currentTimeMillis() - 3600000 * 5,
                    isRead = false
                ),
                EventNotificationEntity(
                    title = "🚌 Prenotazione Bus Navetta Aperta",
                    message = "Sono disponibili 54 posti gratuiti per il transfer dal Policlinico a Villa Delle Rose. Riserva il tuo posto!",
                    category = "Navetta",
                    timestamp = System.currentTimeMillis() - 3600000 * 2,
                    isRead = false
                ),
                EventNotificationEntity(
                    title = "📍 Dettagli Seduta e Aula Magna",
                    message = "La discussione delle tesi inizierà puntuale alle 10:30 presso l'Aula Magna della Clinica Neurologica (Ingresso 4).",
                    category = "Seduta",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isRead = false
                )
            )
            db.notificationDao().insertNotifications(initialNotifications)
        }
    }
}
