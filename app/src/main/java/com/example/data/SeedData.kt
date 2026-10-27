package com.example.data

import com.example.ui.MapPoint

// FILE GENERATO DA tools/gen-event-data.py a partire da pwa/shared/event-data.json.
// NON modificare a mano: cambia il JSON e rigenera con: python3 tools/gen-event-data.py
object SeedData {
    const val maxBusSeats: Int = 30

    // Date (AAAA-MM-GG) e orari (HH:MM, vuoto = da definire) dal blocco "schedule" del JSON
    val schedule: EventSchedule = EventSchedule(
        ceremonyDate = "2026-11-09",
        ceremonyTime = "09:00",
        partyDate = "2026-11-13",
        partyTime = "21:30",
        partyEndTime = "03:00",
        busDepartureTime = "",
        busReturnTime = "",
    )

    val graduates: List<String> = listOf(
        "Fedele Luisi",
        "Sebastiano Carlone",
        "Roberto Spiridione Prezioso",
        "Dalila Totaro",
        "Giorgia Ruta",
        "Lorenzo Parrulli",
        "Francesco Cusmai",
        "Chiara Esposto",
        "Donato Regina",

    )

    val mapPoints: List<MapPoint> = listOf(
        MapPoint(
            id = "seduta",
            title = "Seduta di Specializzazione & Proclamazione",
            subtitle = "Discussione Tesi & Brindisi",
            timeLabel = "9 Novembre - ore 09:00",
            address = "Aula Magna \"G. De Benedictis\", AOUC Policlinico di Bari, Piazza Giulio Cesare 11, Bari",
            latitude = 41.1122,
            longitude = 16.8655,
            iconType = "GRADUATION",
            description = "Aula Magna \"G. De Benedictis\" del Policlinico di Bari. Arrivare qualche minuto prima dell'orario di inizio (ore 09:00).",
            mapsUrl = "https://maps.app.goo.gl/RhZKgXuxfYgg2RgTA"
        ),
        MapPoint(
            id = "bus",
            title = "Partenza Autobus Navetta",
            subtitle = "Punto di Ritrovo Transfer Gratuito",
            timeLabel = "Venerdì 13 - ritrovo da definire",
            address = "Piazza Giulio Cesare, Bari (davanti all'ingresso del Policlinico)",
            latitude = 41.1131,
            longitude = 16.8663,
            iconType = "BUS",
            description = "Ritrovo in Piazza Giulio Cesare, davanti all'ingresso del Policlinico. Navetta riservata 30 posti verso Il Giardino dei Tempi - Orto Botanico (Via Giovanni Amendola 247, 70126 Bari). Orari di andata e ritorno saranno confermati prossimamente.",
            mapsUrl = "https://www.google.com/maps/search/?api=1&query=Piazza+Giulio+Cesare,+Bari"
        ),
        MapPoint(
            id = "festa",
            title = "Festa di Specializzazione",
            subtitle = "Aperitivo, Cena, Dj Set & Torta",
            timeLabel = "Venerdì 13 novembre - dalle 21:30 alle 03:00",
            address = "Il Giardino dei Tempi - Orto Botanico, Via Giovanni Amendola 247, 70126 Bari",
            latitude = 41.0985,
            longitude = 16.8945,
            iconType = "PARTY",
            description = "La festa di venerdì 13 novembre si terrà al Giardino dei Tempi - Orto Botanico (Via Giovanni Amendola 247, 70126 Bari), tra il verde dell'orto botanico e le sale per eventi. Dalle ore 21:30 fino alle 03:00. Dress code: Elegant Chic.",
            mapsUrl = "https://maps.app.goo.gl/zcwjBwscFm3VkhgM6"
        ),
    )

    val programBadge: String = "SPECIALIZZAZIONE IN NEUROLOGIA"
    val programTitle: String = "Seduta di Specializzazione & Festa"
    val programSubtitle: String = "9 Neo-Specialisti in Neurologia"
    val programDateLabel: String = "9 Novembre (seduta) • 13 Novembre (festa)"
    val programLocationLabel: String = "Aula Magna \"G. De Benedictis\" (seduta) • Il Giardino dei Tempi (festa)"

    val programTimeline: List<ProgramTimelineEntry> = listOf(
        ProgramTimelineEntry(
            time = "Ore 09:00",
            title = "Seduta di Laurea & Proclamazione",
            location = "Aula Magna \"G. De Benedictis\" - Policlinico di Bari",
            details = "Discussione delle tesi e proclamazione dei 9 neo-specialisti in Neurologia. Inizio alle ore 09:00.",
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
            location = "Piazza Giulio Cesare (Policlinico di Bari)",
            details = "Venerdì 13 novembre: ritrovo dei partecipanti e transfer riservato 30 posti verso Il Giardino dei Tempi - Orto Botanico (Via Giovanni Amendola 247, 70126 Bari). Orario di partenza da definire.",
            hasMore = true
        ),
        ProgramTimelineEntry(
            time = "Ven 13 ore 21:30",
            title = "Festa di Specializzazione",
            location = "Il Giardino dei Tempi - Orto Botanico, Bari",
            details = "Venerdì 13 novembre al Giardino dei Tempi (Via Giovanni Amendola 247, 70126 Bari): aperitivo, cena a buffet e brindisi tutti insieme per festeggiare i 9 neo-neurologi. Dalle ore 21:30 fino alle 03:00.",
            hasMore = true
        ),
        ProgramTimelineEntry(
            time = "Ven 13",
            title = "Taglio della Torta & Dj Set",
            location = "Il Giardino dei Tempi - Orto Botanico, Bari",
            details = "Taglio della torta di specializzazione, musica e balli per chiudere in bellezza la serata.",
            hasMore = false
        ),
    )

    val busScheduleSubtitle: String = "Transfer andata e ritorno da Piazza Giulio Cesare (Policlinico di Bari) al Giardino dei Tempi"
    val busAndata: BusTripInfo = BusTripInfo(
        timeLabel = "Ven 13 - da definire", from = "Piazza Giulio Cesare (Policlinico di Bari)", to = "Il Giardino dei Tempi - Orto Botanico (Via Amendola 247)", notes = "Venerdì 13 novembre: orario di partenza in via di definizione, sarà confermato con l'orario della festa"
    )
    val busRitorno: BusTripInfo = BusTripInfo(
        timeLabel = "Ven 13 - notte", from = "Il Giardino dei Tempi - Orto Botanico (Via Amendola 247)", to = "Piazza Giulio Cesare (Policlinico di Bari)", notes = "Rientro notturno garantito per tornare a casa in totale sicurezza"
    )
    val busPickupStops: List<String> = listOf(
        "Piazza Giulio Cesare (Policlinico di Bari)",

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
            pickupStop = "Piazza Giulio Cesare (Policlinico di Bari)",
            returnTripWanted = true,
            contactPhone = "333 4567891",
            notes = "Pronta subito dopo le proclamazioni"
        ),
        BusBookingEntity(
            passengerName = "Giulia Colombo & Gruppo Amici",
            seatsCount = 3,
            pickupStop = "Piazza Giulio Cesare (Policlinico di Bari)",
            returnTripWanted = true,
            contactPhone = "340 5566778",
            notes = "Con festoni e regali"
        ),
        BusBookingEntity(
            passengerName = "Specializzandi Neurologia Turno A",
            seatsCount = 6,
            pickupStop = "Piazza Giulio Cesare (Policlinico di Bari)",
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
        WishEntity(
            authorName = "I colleghi della Stroke Unit",
            targetGraduate = "Donato Regina",
            message = "Donato, calma nelle urgenze e sorriso nelle guardie: un neurologo con cui tutti vorrebbero lavorare. Auguri!",
            emojiBadge = "🧠",
            heartCount = 14,
            createdAt = (System.currentTimeMillis() - 288000L)
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
            id = "luisi",
            name = "Dott. Fedele Luisi",
            specialization = "Epilessia & Neurofisiologia Clinica",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Stetoscopio Digitale Littmann & Fellowship Clinica",
            giftDescription = "Contributo dedicato per lo strumento diagnostico avanzato e per l'inizio dell'attività ospedaliera di Fedele.",
            iban = "IT13 U030 6941 5131 0000 0010 092",
            ibanHolder = "Fedele Luisi",
            satispayUrl = "https://web.satispay.com/download/qrcode/S6Y-CON--6F32BD93-1233-47D0-A362-4EB4C249ACE1?locale=it",
            paypalMeUrl = "https://www.paypal.com/qrcodes/p2pqrc/MBSHEM4F557R8",
            bancomatPay = "3469556702",
            paypalEmail = ""
        ),
        GiftTargetEntity(
            id = "carlone",
            name = "Dott. Sebastiano Carlone",
            specialization = "Cefalee e Malattie Neurodegenerative",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Oftalmoscopio Professionale & Borsa Medico in Cuoio",
            giftDescription = "Regalo personalizzato per le visite ambulatoriali e il master in patologie neurodegenerative di Sebastiano.",
            iban = "IT17 W076 0104 2000 0106 3223 976",
            ibanHolder = "Sebastiano Carlone",
            satispayUrl = "",
            paypalMeUrl = "",
            bancomatPay = "",
            paypalEmail = ""
        ),
        GiftTargetEntity(
            id = "prezioso",
            name = "Dott. Roberto Spiridione Prezioso",
            specialization = "Stroke Unit & Neuro-Vascolare",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Corso Neurosonologia Doppler & Attrezzatura Studio",
            giftDescription = "Regalo dedicato per la certificazione in ecocolordoppler transcranico e dotazione clinica di Roberto.",
            iban = "IT83 H030 6964 7161 0000 0060 343",
            ibanHolder = "Roberto Spiridione Prezioso",
            satispayUrl = "",
            paypalMeUrl = "https://www.paypal.me/RobertoPrezioso96",
            bancomatPay = "3889861472",
            paypalEmail = ""
        ),
        GiftTargetEntity(
            id = "totaro",
            name = "Dott.ssa Dalila Totaro",
            specialization = "Sclerosi Multipla & Immunologia Neurologica",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Martello Riflessi Digitale & Corso RM Funzionale",
            giftDescription = "Contributo per la dotazione ambulatoriale e l'aggiornamento in neuroimaging funzionale di Dalila.",
            iban = "IT89 Y020 0804 0240 0043 0727 126",
            ibanHolder = "Dalila Totaro",
            satispayUrl = "",
            paypalMeUrl = "https://www.paypal.me/dalilatotaro1996",
            bancomatPay = "",
            paypalEmail = ""
        ),
        GiftTargetEntity(
            id = "ruta",
            name = "Dott.ssa Giorgia Ruta",
            specialization = "Neuropatologie Periferiche & EMG",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Elettromiografo Portatile & Stage Neurofisiologia",
            giftDescription = "Regalo per l'avvio dell'attività in elettrofisiologia clinica e lo studio delle neuropatie di Giorgia.",
            iban = "IT04 G030 6904 0131 0000 0005 259",
            ibanHolder = "Giorgia Ruta",
            satispayUrl = "",
            paypalMeUrl = "",
            bancomatPay = "",
            paypalEmail = ""
        ),
        GiftTargetEntity(
            id = "parrulli",
            name = "Dott. Lorenzo Parrulli",
            specialization = "Movimenti Patologici & Malattia di Parkinson",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Corso Tourette & Dispositivo Wearable Monitoring",
            giftDescription = "Regalo per la formazione sui disturbi del movimento e l'attività di ricerca clinica di Lorenzo.",
            iban = "IT75 E053 8541 5020 0000 1672 967",
            ibanHolder = "Lorenzo Parrulli",
            satispayUrl = "",
            paypalMeUrl = "",
            bancomatPay = "",
            paypalEmail = ""
        ),
        GiftTargetEntity(
            id = "cusmai",
            name = "Dott. Francesco Cusmai",
            specialization = "Neuroriabilitazione & Medicina Fisica",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Tappeto Rotante & Kit Valutazione Neurologica",
            giftDescription = "Contributo per la dotazione di neuroriabilitazione e l'attività ambulatoriale di Francesco.",
            iban = "IT49 O010 3041 3400 0000 1593 702",
            ibanHolder = "Francesco Cusmai",
            satispayUrl = "",
            paypalMeUrl = "",
            bancomatPay = "",
            paypalEmail = "francescocusmai96@gmail.com"
        ),
        GiftTargetEntity(
            id = "esposto",
            name = "Dott.ssa Chiara Esposto",
            specialization = "Disturbi Cognitivi & Demenze",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Tablet Clinico & Corso Neuropsicologia",
            giftDescription = "Regalo per la valutazione neuropsicologica dei pazienti e l'aggiornamento sulle demenze di Chiara.",
            iban = "IT46 E030 6978 4941 0000 0001 908",
            ibanHolder = "Chiara Esposto",
            satispayUrl = "",
            paypalMeUrl = "",
            bancomatPay = "",
            paypalEmail = ""
        ),
        GiftTargetEntity(
            id = "regina",
            name = "Dott. Donato Regina",
            specialization = "Neurologia d'Urgenza & Neuro-Oncologia",
            roleTitle = "Neo-Specialista in Neurologia",
            giftTitle = "Ecografo Palmare & Corso Neuro-Oncologia",
            giftDescription = "Contributo per la dotazione diagnostica al letto del paziente e l'aggiornamento in neuro-oncologia di Donato.",
            iban = "IT05 S054 2404 0100 0000 1066 123",
            ibanHolder = "Donato Regina",
            satispayUrl = "",
            paypalMeUrl = "https://www.paypal.me/DonatoRegina",
            bancomatPay = "",
            paypalEmail = ""
        ),
    )

    val giftCollectors: List<GiftCollectorEntity> = listOf(
        GiftCollectorEntity(
            id = "roberto",
            sortOrder = 0,
            name = "Dott. Paolo Roberto",
            roleTitle = "Cassiere delle quote uniche",
            description = "Se preferisci fare un solo versamento per tutti i neo-specialisti, Paolo raccoglie la tua quota e la gira a ciascuno di loro: scegli tu se dividerla in parti uguali o personalizzarla.",
            iban = "IT89 T036 6901 6002 8511 6017 627",
            ibanHolder = "Paolo Roberto",
            paypalMeUrl = "",
            paypalContact = "+39 348 276 7437",
            paymentMethodsCsv = "IBAN,PayPal,Contanti",
            transferReason = "Regalo specializzazione Neurologia 2026"
        ),
        GiftCollectorEntity(
            id = "aki",
            sortOrder = 1,
            name = "Dott. Akilesh Bhunjun",
            roleTitle = "Cassiere delle quote uniche",
            description = "In alternativa puoi versare la tua quota unica ad Akilesh, che la ripartisce allo stesso modo fra i neo-specialisti.",
            iban = "IT96 E010 0504 0010 0000 0003 247",
            ibanHolder = "Akilesh Bhunjun",
            paypalMeUrl = "https://www.paypal.me/AkileshBhunjun",
            paypalContact = "",
            paymentMethodsCsv = "IBAN,PayPal,Contanti",
            transferReason = "Regalo specializzazione Neurologia 2026"
        )
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
            message = "La festa di specializzazione si terrà venerdì 13 novembre al Giardino dei Tempi - Orto Botanico (Via Giovanni Amendola 247, Bari), dalle 21:30 alle 03:00. Vi aspettiamo!",
            category = "Festa",
            timestamp = (System.currentTimeMillis() - 7200000L),
            isRead = false
        ),
        EventNotificationEntity(
            title = "🚌 Prenotazione Bus Navetta Aperta",
            message = "Sono disponibili 30 posti gratuiti per il transfer dal Policlinico di Bari al Giardino dei Tempi. Riserva il tuo posto!",
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

data class EventSchedule(
    val ceremonyDate: String,
    val ceremonyTime: String,
    val partyDate: String,
    val partyTime: String,
    val partyEndTime: String,
    val busDepartureTime: String,
    val busReturnTime: String
)

data class BusTripInfo(
    val timeLabel: String,
    val from: String,
    val to: String,
    val notes: String
)
