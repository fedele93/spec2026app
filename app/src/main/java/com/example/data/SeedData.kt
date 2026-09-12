package com.example.data

import com.example.ui.MapPoint

// FILE GENERATO DA tools/gen-event-data.py a partire da pwa/shared/event-data.json.
// NON modificare a mano: cambia il JSON e rigenera con: python3 tools/gen-event-data.py
object SeedData {
    const val maxBusSeats: Int = 54

    val graduates: List<String> = listOf(
        "Fedele Luisi",
        "Sebastiano Carlone",
        "Roberto Spiridione Prezioso",
        "Dalila Totaro",
        "Giorgia Ruta",
        "Lorenzo Parrulli",
        "Francesco Cusmai",
        "Chiara Esposto",

    )

    val mapPoints: List<MapPoint> = listOf(
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
            latitude = 41.117,
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
            latitude = 41.11,
            longitude = 16.86,
            iconType = "PARTY",
            description = "La location della festa di venerdì 13 novembre sarà comunicata a breve. Dress code: Elegant Chic."
        ),
    )

    val programBadge: String = "SPECIALIZZAZIONE IN NEUROLOGIA"
    val programTitle: String = "Seduta di Specializzazione & Festa"
    val programSubtitle: String = "8 Neo-Specialisti in Neurologia"
    val programDateLabel: String = "9 Novembre (seduta) • 13 Novembre (festa)"
    val programLocationLabel: String = "Aula Magna \"G. De Benedictis\" - Policlinico di Bari"

    val programTimeline: List<ProgramTimelineEntry> = listOf(
        ProgramTimelineEntry(
            time = "Ore 9/10",
            title = "Seduta di Laurea & Proclamazione",
            location = "Aula Magna \"G. De Benedictis\" - Policlinico di Bari",
            details = "Discussione delle tesi e proclamazione degli 8 neo-specialisti in Neurologia. L'ora esatta della seduta sarà comunicata a breve.",
            hasMore = true
        ),
        ProgramTimelineEntry(
            time = "Dopo",
            title = "Brindisi Accademico & Foto di Rito",
            location = "Aula Magna \"G. De Benedictis\" - Policlinico di Bari",
            details = "Consegna dei diplomi, corona d'alloro e foto di rito con colleghi, docenti e parenti al termine della seduta del 9 novembre.",
            hasMore = true
        ),
        ProgramTimelineEntry(
            time = "Ven 13",
            title = "Ritrovo & Imbarco Autobus Navetta",
            location = "Piazzale Policlinico di Bari",
            details = "Venerdì 13 novembre: ritrovo dei partecipanti e transfer riservato 54 posti verso la location della festa (luogo da definire).",
            hasMore = true
        ),
        ProgramTimelineEntry(
            time = "Ven 13",
            title = "Festa di Specializzazione",
            location = "Location da definire (Bari e dintorni)",
            details = "Venerdì 13 novembre: aperitivo, cena a buffet e brindisi tutti insieme per festeggiare i Neo-neurologi. Luogo e ora saranno comunicati a breve.",
            hasMore = true
        ),
        ProgramTimelineEntry(
            time = "Ven 13",
            title = "Taglio della Torta & Dj Set",
            location = "Location da definire (Bari e dintorni)",
            details = "Taglio della torta di specializzazione, video celebrativo a sorpresa, musica e balli per chiudere in bellezza la serata.",
            hasMore = false
        ),
    )

    val busScheduleSubtitle: String = "Transfer andata e ritorno dal Policlinico di Bari alla sede della festa"
    val busAndata: BusTripInfo = BusTripInfo(
        timeLabel = "Ven 13 - da definire", from = "Policlinico di Bari (Piazzale Principale)", to = "Location della festa (da definire)", notes = "Venerdì 13 novembre: orario di partenza in via di definizione, confermato appena nota la location"
    )
    val busRitorno: BusTripInfo = BusTripInfo(
        timeLabel = "Ven 13 - notte", from = "Location della festa (da definire)", to = "Policlinico di Bari / Stazione Centrale", notes = "Rientro notturno garantito per tornare a casa in totale sicurezza"
    )
    val busPickupStops: List<String> = listOf(
        "Policlinico di Bari (Piazzale Principale)",
        "Policlinico di Bari (Fermata Metro/Navetta)",
        "Stazione Ferroviaria Centrale di Bari",

    )

    val guests: List<GuestEntity> = listOf(
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
        ),
    )

    val busBookings: List<BusBookingEntity> = listOf(
        BusBookingEntity(
            passengerName = "Dott.ssa Sofia Valenti",
            seatsCount = 1,
            pickupStop = "Policlinico di Bari (Piazzale Principale)",
            returnTripWanted = true,
            contactPhone = "333 4567891",
            notes = "Pronta subito dopo le proclamazioni"
        ),
        BusBookingEntity(
            passengerName = "Giulia Colombo & Gruppo Amici",
            seatsCount = 3,
            pickupStop = "Policlinico di Bari (Piazzale Principale)",
            returnTripWanted = true,
            contactPhone = "340 5566778",
            notes = "Con festoni e regali"
        ),
        BusBookingEntity(
            passengerName = "Specializzandi Neurologia Turno A",
            seatsCount = 6,
            pickupStop = "Policlinico di Bari (Fermata Metro/Navetta)",
            returnTripWanted = true,
            contactPhone = "349 1122339",
            notes = "Tutti insieme"
        ),
    )

    val wishes: List<WishEntity> = listOf(
        WishEntity(
            authorName = "Prof. Giancarlo Bianchi",
            targetGraduate = "Tutti i Laureandi",
            message = "Congratulazioni di cuore ai nostri nuovi specialisti! Avete dimostrato dedizione, rigore scientifico e grande umanità. Buona strada in Neurologia!",
            emojiBadge = "🧠",
            heartCount = 24,
            createdAt = (System.currentTimeMillis() - 28800000L)
        ),
        WishEntity(
            authorName = "I colleghi di reparto",
            targetGraduate = "Fedele Luisi",
            message = "Grande Fedele! Tra turni di notte interminabili ed EEG complessi sei arrivato al traguardo più bello. Fieri di te!",
            emojiBadge = "⚡",
            heartCount = 18,
            createdAt = (System.currentTimeMillis() - 25200000L)
        ),
        WishEntity(
            authorName = "La tua famiglia",
            targetGraduate = "Sebastiano Carlone",
            message = "Sebastiano, vedere coronato il tuo sogno di diventare neurologo ci riempie di orgoglio e gioia immensa. Ti vogliamo bene!",
            emojiBadge = "❤️",
            heartCount = 31,
            createdAt = (System.currentTimeMillis() - 21600000L)
        ),
        WishEntity(
            authorName = "Gli amici della facoltà",
            targetGraduate = "Roberto Spiridione Prezioso",
            message = "Adesso sei ufficialmente un 'Brain Master'! Venerdì 13 si brinda senza sosta alla tua salute!",
            emojiBadge = "🥂",
            heartCount = 15,
            createdAt = (System.currentTimeMillis() - 18000000L)
        ),
        WishEntity(
            authorName = "I tuoi tutor ambulatoriali",
            targetGraduate = "Dalila Totaro",
            message = "Dalila, la tua sensibilità clinica e la cura dei pazienti sono state un esempio per tutti. Auguri di vera specialista!",
            emojiBadge = "⭐",
            heartCount = 12,
            createdAt = (System.currentTimeMillis() - 14400000L)
        ),
        WishEntity(
            authorName = "Le compagne di studio",
            targetGraduate = "Giorgia Ruta",
            message = "Giorgia, ce l'hai fatta! Da oggi neurologa a tutti gli effetti: orgogliose di te e del percorso condiviso.",
            emojiBadge = "🎉",
            heartCount = 20,
            createdAt = (System.currentTimeMillis() - 10800000L)
        ),
        WishEntity(
            authorName = "Mamma e Papà",
            targetGraduate = "Lorenzo Parrulli",
            message = "Lorenzo caro, ogni sacrificio di questi anni diventa oggi orgoglio immenso. Continua a curare i tuoi pazienti come hai curato il tuo sogno.",
            emojiBadge = "❤️",
            heartCount = 27,
            createdAt = (System.currentTimeMillis() - 7200000L)
        ),
        WishEntity(
            authorName = "Gli amici di sempre",
            targetGraduate = "Francesco Cusmai",
            message = "Francesco, neurologo e amico: una combinazione imbattibile. Venerdì 13 festa, e poi... si vedrà!",
            emojiBadge = "🎓",
            heartCount = 16,
            createdAt = (System.currentTimeMillis() - 3600000L)
        ),
        WishEntity(
            authorName = "La tua famiglia",
            targetGraduate = "Chiara Esposto",
            message = "Chiara, la tua determinazione e il tuo sorriso hanno illuminato il reparto. Auguri di cuore, dottoressa!",
            emojiBadge = "✨",
            heartCount = 22,
            createdAt = (System.currentTimeMillis() - 612000L)
        ),
    )

    val photos: List<SharedPhotoEntity> = listOf(
        SharedPhotoEntity(
            authorName = "Staff Organizzazione",
            caption = "L'Aula Magna \"G. De Benedictis\" del Policlinico di Bari è pronta per la seduta di specializzazione del 9 novembre!",
            imageResId = 1,
            imageUri = "",
            likesCount = 28,
            createdAt = (System.currentTimeMillis() - 32400000L)
        ),
        SharedPhotoEntity(
            authorName = "Chiara Esposto",
            caption = "Tesi rilegata, corona d'alloro pronta: conto alla rovescia alla proclamazione!",
            imageResId = 2,
            imageUri = "",
            likesCount = 42,
            createdAt = (System.currentTimeMillis() - 21600000L)
        ),
        SharedPhotoEntity(
            authorName = "Fedele & Lorenzo",
            caption = "Ultimo turno insieme da specializzandi, da oggi Neurologi ufficiali!",
            imageResId = 3,
            imageUri = "",
            likesCount = 35,
            createdAt = (System.currentTimeMillis() - 10800000L)
        ),
    )

    val giftTargets: List<GiftTargetEntity> = listOf(
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
        ),
    )

    val giftContributions: List<GiftContributionEntity> = listOf(
        GiftContributionEntity(
            donorName = "Zia Laura & Famiglia",
            targetGraduateId = "carlone",
            targetGraduateName = "Dott. Sebastiano Carlone",
            amount = 150.0,
            paymentMethod = "IBAN",
            note = "Per il nostro neurologo preferito! Con affetto infinito.",
            isAnonymous = false,
            contributedAt = (System.currentTimeMillis() - 18000000L)
        ),
        GiftContributionEntity(
            donorName = "Colleghi Reparto Stroke",
            targetGraduateId = "prezioso",
            targetGraduateName = "Dott. Roberto Spiridione Prezioso",
            amount = 100.0,
            paymentMethod = "Satispay",
            note = "Per il futuro re delle trombolisi! Forza Roberto!",
            isAnonymous = false,
            contributedAt = (System.currentTimeMillis() - 14400000L)
        ),
        GiftContributionEntity(
            donorName = "Famiglia Luisi",
            targetGraduateId = "luisi",
            targetGraduateName = "Dott. Fedele Luisi",
            amount = 200.0,
            paymentMethod = "IBAN",
            note = "Orgogliosi del tuo percorso impeccabile.",
            isAnonymous = false,
            contributedAt = (System.currentTimeMillis() - 10800000L)
        ),
        GiftContributionEntity(
            donorName = "Amici del Liceo",
            targetGraduateId = "gruppo",
            targetGraduateName = "Regalo Comune Specializzandi",
            amount = 120.0,
            paymentMethod = "PayPal",
            note = "Brindiamo a tutti voi venerdì 13!",
            isAnonymous = false,
            contributedAt = (System.currentTimeMillis() - 7200000L)
        ),
    )

    val notifications: List<EventNotificationEntity> = listOf(
        EventNotificationEntity(
            title = "🎓 Benvenuti all'evento di Specializzazione!",
            message = "L'app ufficiale per la Specializzazione in Neurologia a Bari è attiva. Controlla il programma, prenota il bus e conferma il tuo RSVP!",
            category = "Organizzazione",
            timestamp = (System.currentTimeMillis() - 18000000L),
            isRead = false
        ),
        EventNotificationEntity(
            title = "📍 Seduta del 9 Novembre all'Aula Magna",
            message = "La seduta di proclamazione si terrà il 9 novembre presso l'Aula Magna \"G. De Benedictis\" del Policlinico di Bari (AOUC Policlinico di Bari). Ora da definirsi.",
            category = "Seduta",
            timestamp = (System.currentTimeMillis() - 10800000L),
            isRead = false
        ),
        EventNotificationEntity(
            title = "🎉 Festa di Venerdì 13 Novembre",
            message = "La festa di specializzazione è fissata per venerdì 13 novembre. Luogo e ora saranno comunicati a breve: resta in attesa!",
            category = "Festa",
            timestamp = (System.currentTimeMillis() - 7200000L),
            isRead = false
        ),
        EventNotificationEntity(
            title = "🚌 Prenotazione Bus Navetta Aperta",
            message = "Sono disponibili 54 posti gratuiti per il transfer dal Policlinico di Bari alla location della festa. Riserva il tuo posto!",
            category = "Navetta",
            timestamp = (System.currentTimeMillis() - 3600000L),
            isRead = false
        ),
    )
}

data class ProgramTimelineEntry(
    val time: String,
    val title: String,
    val location: String,
    val details: String,
    val hasMore: Boolean
)

data class BusTripInfo(
    val timeLabel: String,
    val from: String,
    val to: String,
    val notes: String
)
