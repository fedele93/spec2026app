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
                    authorName = "I colleghi di reparto",
                    targetGraduate = "Fedele Luisi",
                    message = "Grande Fedele! Tra turni di notte interminabili ed EEG complessi sei arrivato al traguardo più bello. Fieri di te!",
                    emojiBadge = "⚡",
                    heartCount = 18
                ),
                WishEntity(
                    authorName = "La tua famiglia",
                    targetGraduate = "Sebastiano Carlone",
                    message = "Sebastiano, vedere coronato il tuo sogno di diventare neurologo ci riempie di orgoglio e gioia immensa. Ti vogliamo bene!",
                    emojiBadge = "❤️",
                    heartCount = 31
                ),
                WishEntity(
                    authorName = "Gli amici della facoltà",
                    targetGraduate = "Roberto Spiridione Prezioso",
                    message = "Adesso sei ufficialmente un 'Brain Master'! Venerdì 13 si brinda senza sosta alla tua salute!",
                    emojiBadge = "🥂",
                    heartCount = 15
                ),
                WishEntity(
                    authorName = "I tuoi tutor ambulatoriali",
                    targetGraduate = "Dalila Totaro",
                    message = "Dalila, la tua sensibilità clinica e la cura dei pazienti sono state un esempio per tutti. Auguri di vera specialista!",
                    emojiBadge = "⭐",
                    heartCount = 12
                ),
                WishEntity(
                    authorName = "Le compagne di studio",
                    targetGraduate = "Giorgia Ruta",
                    message = "Giorgia, ce l'hai fatta! Da oggi neurologa a tutti gli effetti: orgogliose di te e del percorso condiviso.",
                    emojiBadge = "🎉",
                    heartCount = 20
                ),
                WishEntity(
                    authorName = "Mamma e Papà",
                    targetGraduate = "Lorenzo Parrulli",
                    message = "Lorenzo caro, ogni sacrificio di questi anni diventa oggi orgoglio immenso. Continua a curare i tuoi pazienti come hai curato il tuo sogno.",
                    emojiBadge = "❤️",
                    heartCount = 27
                ),
                WishEntity(
                    authorName = "Gli amici di sempre",
                    targetGraduate = "Francesco Cusmai",
                    message = "Francesco, neurologo e amico: una combinazione imbattibile. Venerdì 13 festa, e poi... si vedrà!",
                    emojiBadge = "🎓",
                    heartCount = 16
                ),
                WishEntity(
                    authorName = "La tua famiglia",
                    targetGraduate = "Chiara Esposto",
                    message = "Chiara, la tua determinazione e il tuo sorriso hanno illuminato il reparto. Auguri di cuore, dottoressa!",
                    emojiBadge = "✨",
                    heartCount = 22
                )
            )
            db.wishDao().insertWishes(initialWishes)
        }

        if (db.photoDao().countPhotos() == 0) {
            val initialPhotos = listOf(
                SharedPhotoEntity(
                    authorName = "Staff Organizzazione",
                    caption = "L'Aula Magna \"G. De Benedictis\" del Policlinico di Bari è pronta per la seduta di specializzazione del 9 novembre!",
                    imageResId = 1,
                    imageUri = "",
                    likesCount = 28
                ),
                SharedPhotoEntity(
                    authorName = "Chiara Esposto",
                    caption = "Tesi rilegata, corona d'alloro pronta: conto alla rovescia alla proclamazione!",
                    imageResId = 2,
                    imageUri = "",
                    likesCount = 42
                ),
                SharedPhotoEntity(
                    authorName = "Fedele & Lorenzo",
                    caption = "Ultimo turno insieme da specializzandi, da oggi Neurologi ufficiali!",
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
                    roleTitle = "Fedele, Sebastiano, Roberto, Dalila, Giorgia, Lorenzo, Francesco, Chiara",
                    giftTitle = "Viaggio Congresso Europeo di Neurologia & Brindisi di Classe",
                    giftDescription = "Quota comune per sostenere la partecipazione al congresso EAN (European Academy of Neurology) e la festa di venerdì 13 novembre!",
                    targetAmount = 4800.0,
                    collectedAmount = 2100.0,
                    iban = "IT78 K030 6909 6061 0000 1234 567",
                    ibanHolder = "Comitato Festa Neurologia Bari",
                    satispayUrl = "https://tag.satispay.com/festaneurologiabari",
                    paypalMeUrl = "https://paypal.me/festaneurologiabari2026"
                ),
                GiftTargetEntity(
                    id = "luisi",
                    name = "Dott. Fedele Luisi",
                    specialization = "Epilessia & Neurofisiologia Clinica",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Stetoscopio Digitale Littmann & Fellowship Clinica",
                    giftDescription = "Contributo dedicato per lo strumento diagnostico avanzato e per l'inizio dell'attività ospedaliera di Fedele.",
                    targetAmount = 900.0,
                    collectedAmount = 520.0,
                    iban = "IT44 X030 6909 6061 0000 9876 543",
                    ibanHolder = "Fedele Luisi",
                    satispayUrl = "https://tag.satispay.com/fedeleluisineuro",
                    paypalMeUrl = "https://paypal.me/fedeleluisimd"
                ),
                GiftTargetEntity(
                    id = "carlone",
                    name = "Dott. Sebastiano Carlone",
                    specialization = "Cefalee e Malattie Neurodegenerative",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Oftalmoscopio Professionale & Borsa Medico in Cuoio",
                    giftDescription = "Regalo personalizzato per le visite ambulatoriali e il master in patologie neurodegenerative di Sebastiano.",
                    targetAmount = 950.0,
                    collectedAmount = 680.0,
                    iban = "IT12 Y030 6909 6061 0000 4567 890",
                    ibanHolder = "Sebastiano Carlone",
                    satispayUrl = "https://tag.satispay.com/sebastianocarlone",
                    paypalMeUrl = "https://paypal.me/sebastianocarlone"
                ),
                GiftTargetEntity(
                    id = "prezioso",
                    name = "Dott. Roberto Spiridione Prezioso",
                    specialization = "Stroke Unit & Neuro-Vascolare",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Corso Neurosonologia Doppler & Attrezzatura Studio",
                    giftDescription = "Regalo dedicato per la certificazione in ecocolordoppler transcranico e dotazione clinica di Roberto.",
                    targetAmount = 850.0,
                    collectedAmount = 430.0,
                    iban = "IT99 Z030 6909 6061 0000 3210 987",
                    ibanHolder = "Roberto Spiridione Prezioso",
                    satispayUrl = "https://tag.satispay.com/robertoprezioso",
                    paypalMeUrl = "https://paypal.me/robertoprezioso"
                ),
                GiftTargetEntity(
                    id = "totaro",
                    name = "Dott.ssa Dalila Totaro",
                    specialization = "Sclerosi Multipla & Immunologia Neurologica",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Martello Riflessi Digitale & Corso RM Funzionale",
                    giftDescription = "Contributo per la dotazione ambulatoriale e l'aggiornamento in neuroimaging funzionale di Dalila.",
                    targetAmount = 800.0,
                    collectedAmount = 350.0,
                    iban = "IT55 A030 6909 6061 0000 1112 223",
                    ibanHolder = "Dalila Totaro",
                    satispayUrl = "https://tag.satispay.com/dalilatotaro",
                    paypalMeUrl = "https://paypal.me/dalilatotaro"
                ),
                GiftTargetEntity(
                    id = "ruta",
                    name = "Dott.ssa Giorgia Ruta",
                    specialization = "Neuropatologie Periferiche & EMG",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Elettromiografo Portatile & Stage Neurofisiologia",
                    giftDescription = "Regalo per l'avvio dell'attività in elettrofisiologia clinica e lo studio delle neuropatie di Giorgia.",
                    targetAmount = 880.0,
                    collectedAmount = 410.0,
                    iban = "IT66 B030 6909 6061 0000 3334 445",
                    ibanHolder = "Giorgia Ruta",
                    satispayUrl = "https://tag.satispay.com/gorgiaruta",
                    paypalMeUrl = "https://paypal.me/giorgiaruta"
                ),
                GiftTargetEntity(
                    id = "parrulli",
                    name = "Dott. Lorenzo Parrulli",
                    specialization = "Movimenti Patologici & Malattia di Parkinson",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Corso Tourette & Dispositivo Wearable Monitoring",
                    giftDescription = "Regalo per la formazione sui disturbi del movimento e l'attività di ricerca clinica di Lorenzo.",
                    targetAmount = 820.0,
                    collectedAmount = 380.0,
                    iban = "IT77 C030 6909 6061 0000 5556 667",
                    ibanHolder = "Lorenzo Parrulli",
                    satispayUrl = "https://tag.satispay.com/lorenzoparrulli",
                    paypalMeUrl = "https://paypal.me/lorenzoparrulli"
                ),
                GiftTargetEntity(
                    id = "cusmai",
                    name = "Dott. Francesco Cusmai",
                    specialization = "Neuroriabilitazione & Medicina Fisica",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Tappeto Rotante & Kit Valutazione Neurologica",
                    giftDescription = "Contributo per la dotazione di neuroriabilitazione e l'attività ambulatoriale di Francesco.",
                    targetAmount = 760.0,
                    collectedAmount = 290.0,
                    iban = "IT88 D030 6909 6061 0000 7778 889",
                    ibanHolder = "Francesco Cusmai",
                    satispayUrl = "https://tag.satispay.com/francescocusmai",
                    paypalMeUrl = "https://paypal.me/francescocusmai"
                ),
                GiftTargetEntity(
                    id = "esposto",
                    name = "Dott.ssa Chiara Esposto",
                    specialization = "Disturbi Cognitivi & Demenze",
                    roleTitle = "Neo-Specialista in Neurologia",
                    giftTitle = "Tablet Clinico & Corso Neuropsicologia",
                    giftDescription = "Regalo per la valutazione neuropsicologica dei pazienti e l'aggiornamento sulle demenze di Chiara.",
                    targetAmount = 790.0,
                    collectedAmount = 360.0,
                    iban = "IT33 E030 6909 6061 0000 9990 011",
                    ibanHolder = "Chiara Esposto",
                    satispayUrl = "https://tag.satispay.com/chiaraesposto",
                    paypalMeUrl = "https://paypal.me/chiaraesposto"
                )
            )
            db.giftDao().insertTargets(initialTargets)

            val initialContributions = listOf(
                GiftContributionEntity(
                    donorName = "Zia Laura & Famiglia",
                    targetGraduateId = "carlone",
                    targetGraduateName = "Dott. Sebastiano Carlone",
                    amount = 150.0,
                    paymentMethod = "IBAN",
                    note = "Per il nostro neurologo preferito! Con affetto infinito."
                ),
                GiftContributionEntity(
                    donorName = "Colleghi Reparto Stroke",
                    targetGraduateId = "prezioso",
                    targetGraduateName = "Dott. Roberto Spiridione Prezioso",
                    amount = 100.0,
                    paymentMethod = "Satispay",
                    note = "Per il futuro re delle trombolisi! Forza Roberto!"
                ),
                GiftContributionEntity(
                    donorName = "Famiglia Luisi",
                    targetGraduateId = "luisi",
                    targetGraduateName = "Dott. Fedele Luisi",
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
                    note = "Brindiamo a tutti voi venerdì 13!"
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
                    message = "L'app ufficiale per la Specializzazione in Neurologia a Bari è attiva. Controlla il programma, prenota il bus e conferma il tuo RSVP!",
                    category = "Organizzazione",
                    timestamp = System.currentTimeMillis() - 3600000 * 5,
                    isRead = false
                ),
                EventNotificationEntity(
                    title = "📍 Seduta del 9 Novembre all'Aula Magna",
                    message = "La seduta di proclamazione si terrà il 9 novembre presso l'Aula Magna \"G. De Benedictis\" del Policlinico di Bari (AOUC Policlinico di Bari). Ora da definirsi.",
                    category = "Seduta",
                    timestamp = System.currentTimeMillis() - 3600000 * 3,
                    isRead = false
                ),
                EventNotificationEntity(
                    title = "🎉 Festa di Venerdì 13 Novembre",
                    message = "La festa di specializzazione è fissata per venerdì 13 novembre. Luogo e ora saranno comunicati a breve: resta in attesa!",
                    category = "Festa",
                    timestamp = System.currentTimeMillis() - 3600000 * 2,
                    isRead = false
                ),
                EventNotificationEntity(
                    title = "🚌 Prenotazione Bus Navetta Aperta",
                    message = "Sono disponibili 54 posti gratuiti per il transfer dal Policlinico di Bari alla location della festa. Riserva il tuo posto!",
                    category = "Navetta",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isRead = false
                )
            )
            db.notificationDao().insertNotifications(initialNotifications)
        }
    }
}
