// Repository + entità + pre-popolazione (mirror di EventRepository.kt)
import { getAll, count, add, put, bulkAdd, del, getMeta, setMeta } from "./db.js";

export const RsvpStatus = { CONFIRMED: "CONFIRMED", PENDING: "PENDING", DECLINED: "DECLINED" };

export const MAX_BUS_SEATS = 54;

export const GRADUATES = [
  "Fedele Luisi",
  "Sebastiano Carlone",
  "Roberto Spiridione Prezioso",
  "Dalila Totaro",
  "Giorgia Ruta",
  "Lorenzo Parrulli",
  "Francesco Cusmai",
  "Chiara Esposto"
];

export const MAP_POINTS = [
  {
    id: "seduta",
    title: "Seduta di Specializzazione & Proclamazione",
    subtitle: "Discussione Tesi & Brindisi",
    timeLabel: "9 Novembre - ora da definire",
    address: 'Aula Magna "G. De Benedictis", AOUC Policlinico di Bari, Piazza Giulio Cesare 11, Bari',
    latitude: 41.1173,
    longitude: 16.8719,
    iconType: "GRADUATION",
    description: 'Aula Magna "G. De Benedictis" del Policlinico di Bari. Arrivare qualche minuto prima dell\'orario di inizio (in via di definizione).'
  },
  {
    id: "bus",
    title: "Partenza Autobus Navetta",
    subtitle: "Punto di Ritrovo Transfer Gratuito",
    timeLabel: "Venerdì 13 - ritrovo da definire",
    address: "Piazzale Policlinico di Bari (fronte ingresso principale)",
    latitude: 41.1170,
    longitude: 16.8715,
    iconType: "BUS",
    description: "Navetta riservata 54 posti verso la location della festa (luogo da definire). Orari e fermate di andata e ritorno saranno confermati prossimamente."
  },
  {
    id: "festa",
    title: "Festa di Specializzazione",
    subtitle: "Aperitivo, Cena, Dj Set & Torta",
    timeLabel: "Venerdì 13 novembre - ora da definire",
    address: "Location da definire (Bari e dintorni)",
    latitude: 41.1100,
    longitude: 16.8600,
    iconType: "PARTY",
    description: "La location della festa di venerdì 13 novembre sarà comunicata a breve. Dress code: Elegant Chic."
  }
];

const initialWishes = [
  { authorName: "Prof. Giancarlo Bianchi", targetGraduate: "Tutti i Laureandi", message: "Congratulazioni di cuore ai nostri nuovi specialisti! Avete dimostrato dedizione, rigore scientifico e grande umanità. Buona strada in Neurologia!", emojiBadge: "🧠", heartCount: 24, createdAt: Date.now() - 3600000 * 8 },
  { authorName: "I colleghi di reparto", targetGraduate: "Fedele Luisi", message: "Grande Fedele! Tra turni di notte interminabili ed EEG complessi sei arrivato al traguardo più bello. Fieri di te!", emojiBadge: "⚡", heartCount: 18, createdAt: Date.now() - 3600000 * 7 },
  { authorName: "La tua famiglia", targetGraduate: "Sebastiano Carlone", message: "Sebastiano, vedere coronato il tuo sogno di diventare neurologo ci riempie di orgoglio e gioia immensa. Ti vogliamo bene!", emojiBadge: "❤️", heartCount: 31, createdAt: Date.now() - 3600000 * 6 },
  { authorName: "Gli amici della facoltà", targetGraduate: "Roberto Spiridione Prezioso", message: "Adesso sei ufficialmente un 'Brain Master'! Venerdì 13 si brinda senza sosta alla tua salute!", emojiBadge: "🥂", heartCount: 15, createdAt: Date.now() - 3600000 * 5 },
  { authorName: "I tuoi tutor ambulatoriali", targetGraduate: "Dalila Totaro", message: "Dalila, la tua sensibilità clinica e la cura dei pazienti sono state un esempio per tutti. Auguri di vera specialista!", emojiBadge: "⭐", heartCount: 12, createdAt: Date.now() - 3600000 * 4 },
  { authorName: "Le compagne di studio", targetGraduate: "Giorgia Ruta", message: "Giorgia, ce l'hai fatta! Da oggi neurologa a tutti gli effetti: orgogliose di te e del percorso condiviso.", emojiBadge: "🎉", heartCount: 20, createdAt: Date.now() - 3600000 * 3 },
  { authorName: "Mamma e Papà", targetGraduate: "Lorenzo Parrulli", message: "Lorenzo caro, ogni sacrificio di questi anni diventa oggi orgoglio immenso. Continua a curare i tuoi pazienti come hai curato il tuo sogno.", emojiBadge: "❤️", heartCount: 27, createdAt: Date.now() - 3600000 * 2 },
  { authorName: "Gli amici di sempre", targetGraduate: "Francesco Cusmai", message: "Francesco, neurologo e amico: una combinazione imbattibile. Venerdì 13 festa, e poi... si vedrà!", emojiBadge: "🎓", heartCount: 16, createdAt: Date.now() - 3600000 },
  { authorName: "La tua famiglia", targetGraduate: "Chiara Esposto", message: "Chiara, la tua determinazione e il tuo sorriso hanno illuminato il reparto. Auguri di cuore, dottoressa!", emojiBadge: "✨", heartCount: 22, createdAt: Date.now() - 600000 }
];

const initialPhotos = [
  { authorName: "Staff Organizzazione", caption: 'L\'Aula Magna "G. De Benedictis" del Policlinico di Bari è pronta per la seduta di specializzazione del 9 novembre!', imageUri: "", likesCount: 28, createdAt: Date.now() - 3600000 * 9 },
  { authorName: "Chiara Esposto", caption: "Tesi rilegata, corona d'alloro pronta: conto alla rovescia alla proclamazione!", imageUri: "", likesCount: 42, createdAt: Date.now() - 3600000 * 6 },
  { authorName: "Fedele & Lorenzo", caption: "Ultimo turno insieme da specializzandi, da oggi Neurologi ufficiali!", imageUri: "", likesCount: 35, createdAt: Date.now() - 3600000 * 3 }
];

const initialGiftTargets = [
  { id: "gruppo", name: "Regalo Comune Specializzandi", specialization: "Specializzazione in Neurologia 2026", roleTitle: "Fedele, Sebastiano, Roberto, Dalila, Giorgia, Lorenzo, Francesco, Chiara", giftTitle: "Viaggio Congresso Europeo di Neurologia & Brindisi di Classe", giftDescription: "Quota comune per sostenere la partecipazione al congresso EAN (European Academy of Neurology) e la festa di venerdì 13 novembre!", targetAmount: 4800.0, collectedAmount: 2100.0, iban: "IT78 K030 6909 6061 0000 1234 567", ibanHolder: "Comitato Festa Neurologia Bari", satispayUrl: "https://tag.satispay.com/festaneurologiabari", paypalMeUrl: "https://paypal.me/festaneurologiabari2026" },
  { id: "luisi", name: "Dott. Fedele Luisi", specialization: "Epilessia & Neurofisiologia Clinica", roleTitle: "Neo-Specialista in Neurologia", giftTitle: "Stetoscopio Digitale Littmann & Fellowship Clinica", giftDescription: "Contributo dedicato per lo strumento diagnostico avanzato e per l'inizio dell'attività ospedaliera di Fedele.", targetAmount: 900.0, collectedAmount: 520.0, iban: "IT44 X030 6909 6061 0000 9876 543", ibanHolder: "Fedele Luisi", satispayUrl: "https://tag.satispay.com/fedeleluisineuro", paypalMeUrl: "https://paypal.me/fedeleluisimd" },
  { id: "carlone", name: "Dott. Sebastiano Carlone", specialization: "Cefalee e Malattie Neurodegenerative", roleTitle: "Neo-Specialista in Neurologia", giftTitle: "Oftalmoscopio Professionale & Borsa Medico in Cuoio", giftDescription: "Regalo personalizzato per le visite ambulatoriali e il master in patologie neurodegenerative di Sebastiano.", targetAmount: 950.0, collectedAmount: 680.0, iban: "IT12 Y030 6909 6061 0000 4567 890", ibanHolder: "Sebastiano Carlone", satispayUrl: "https://tag.satispay.com/sebastianocarlone", paypalMeUrl: "https://paypal.me/sebastianocarlone" },
  { id: "prezioso", name: "Dott. Roberto Spiridione Prezioso", specialization: "Stroke Unit & Neuro-Vascolare", roleTitle: "Neo-Specialista in Neurologia", giftTitle: "Corso Neurosonologia Doppler & Attrezzatura Studio", giftDescription: "Regalo dedicato per la certificazione in ecocolordoppler transcranico e dotazione clinica di Roberto.", targetAmount: 850.0, collectedAmount: 430.0, iban: "IT99 Z030 6909 6061 0000 3210 987", ibanHolder: "Roberto Spiridione Prezioso", satispayUrl: "https://tag.satispay.com/robertoprezioso", paypalMeUrl: "https://paypal.me/robertoprezioso" },
  { id: "totaro", name: "Dott.ssa Dalila Totaro", specialization: "Sclerosi Multipla & Immunologia Neurologica", roleTitle: "Neo-Specialista in Neurologia", giftTitle: "Martello Riflessi Digitale & Corso RM Funzionale", giftDescription: "Contributo per la dotazione ambulatoriale e l'aggiornamento in neuroimaging funzionale di Dalila.", targetAmount: 800.0, collectedAmount: 350.0, iban: "IT55 A030 6909 6061 0000 1112 223", ibanHolder: "Dalila Totaro", satispayUrl: "https://tag.satispay.com/dalilatotaro", paypalMeUrl: "https://paypal.me/dalilatotaro" },
  { id: "ruta", name: "Dott.ssa Giorgia Ruta", specialization: "Neuropatologie Periferiche & EMG", roleTitle: "Neo-Specialista in Neurologia", giftTitle: "Elettromiografo Portatile & Stage Neurofisiologia", giftDescription: "Regalo per l'avvio dell'attività in elettrofisiologia clinica e lo studio delle neuropatie di Giorgia.", targetAmount: 880.0, collectedAmount: 410.0, iban: "IT66 B030 6909 6061 0000 3334 445", ibanHolder: "Giorgia Ruta", satispayUrl: "https://tag.satispay.com/gorgiaruta", paypalMeUrl: "https://paypal.me/giorgiaruta" },
  { id: "parrulli", name: "Dott. Lorenzo Parrulli", specialization: "Movimenti Patologici & Malattia di Parkinson", roleTitle: "Neo-Specialista in Neurologia", giftTitle: "Corso Tourette & Dispositivo Wearable Monitoring", giftDescription: "Regalo per la formazione sui disturbi del movimento e l'attività di ricerca clinica di Lorenzo.", targetAmount: 820.0, collectedAmount: 380.0, iban: "IT77 C030 6909 6061 0000 5556 667", ibanHolder: "Lorenzo Parrulli", satispayUrl: "https://tag.satispay.com/lorenzoparrulli", paypalMeUrl: "https://paypal.me/lorenzoparrulli" },
  { id: "cusmai", name: "Dott. Francesco Cusmai", specialization: "Neuroriabilitazione & Medicina Fisica", roleTitle: "Neo-Specialista in Neurologia", giftTitle: "Tappeto Rotante & Kit Valutazione Neurologica", giftDescription: "Contributo per la dotazione di neuroriabilitazione e l'attività ambulatoriale di Francesco.", targetAmount: 760.0, collectedAmount: 290.0, iban: "IT88 D030 6909 6061 0000 7778 889", ibanHolder: "Francesco Cusmai", satispayUrl: "https://tag.satispay.com/francescocusmai", paypalMeUrl: "https://paypal.me/francescocusmai" },
  { id: "esposto", name: "Dott.ssa Chiara Esposto", specialization: "Disturbi Cognitivi & Demenze", roleTitle: "Neo-Specialista in Neurologia", giftTitle: "Tablet Clinico & Corso Neuropsicologia", giftDescription: "Regalo per la valutazione neuropsicologica dei pazienti e l'aggiornamento sulle demenze di Chiara.", targetAmount: 790.0, collectedAmount: 360.0, iban: "IT33 E030 6909 6061 0000 9990 011", ibanHolder: "Chiara Esposto", satispayUrl: "https://tag.satispay.com/chiaraesposto", paypalMeUrl: "https://paypal.me/chiaraesposto" }
];

const initialGiftContributions = [
  { donorName: "Zia Laura & Famiglia", targetGraduateId: "carlone", targetGraduateName: "Dott. Sebastiano Carlone", amount: 150.0, paymentMethod: "IBAN", note: "Per il nostro neurologo preferito! Con affetto infinito.", isAnonymous: false, contributedAt: Date.now() - 3600000 * 5 },
  { donorName: "Colleghi Reparto Stroke", targetGraduateId: "prezioso", targetGraduateName: "Dott. Roberto Spiridione Prezioso", amount: 100.0, paymentMethod: "Satispay", note: "Per il futuro re delle trombolisi! Forza Roberto!", isAnonymous: false, contributedAt: Date.now() - 3600000 * 4 },
  { donorName: "Famiglia Luisi", targetGraduateId: "luisi", targetGraduateName: "Dott. Fedele Luisi", amount: 200.0, paymentMethod: "IBAN", note: "Orgogliosi del tuo percorso impeccabile.", isAnonymous: false, contributedAt: Date.now() - 3600000 * 3 },
  { donorName: "Amici del Liceo", targetGraduateId: "gruppo", targetGraduateName: "Regalo Comune Specializzandi", amount: 120.0, paymentMethod: "PayPal", note: "Brindiamo a tutti voi venerdì 13!", isAnonymous: false, contributedAt: Date.now() - 3600000 * 2 }
];

const initialGuests = [
  { fullName: "Prof. Giancarlo Bianchi", category: "Docenti e Reparto", rsvpStatus: RsvpStatus.CONFIRMED, guestsCount: 2, dietaryNotes: "Nessuna restrizione", contactInfo: "giancarlo.bianchi@unipol.it", updatedAt: Date.now() },
  { fullName: "Dott.ssa Sofia Valenti", category: "Specializzandi 1° e 2° anno", rsvpStatus: RsvpStatus.CONFIRMED, guestsCount: 1, dietaryNotes: "Opzione Vegetariana", contactInfo: "333 4567891", updatedAt: Date.now() },
  { fullName: "Matteo Moretti & Famiglia", category: "Famigliari", rsvpStatus: RsvpStatus.CONFIRMED, guestsCount: 4, dietaryNotes: "1 Celiaco (menu senza glutine)", contactInfo: "340 1122334", updatedAt: Date.now() },
  { fullName: "Dott. Luca Gatti", category: "Colleghi Reparto", rsvpStatus: RsvpStatus.PENDING, guestsCount: 2, dietaryNotes: "Nessuna", contactInfo: "348 9988776", updatedAt: Date.now() }
];

const initialNotifications = [
  { title: "🎓 Benvenuti all'evento di Specializzazione!", message: "L'app ufficiale per la Specializzazione in Neurologia a Bari è attiva. Controlla il programma, prenota il bus e conferma il tuo RSVP!", category: "Organizzazione", timestamp: Date.now() - 3600000 * 5, isRead: false },
  { title: "📍 Seduta del 9 Novembre all'Aula Magna", message: 'La seduta di proclamazione si terrà il 9 novembre presso l\'Aula Magna "G. De Benedictis" del Policlinico di Bari (AOUC Policlinico di Bari). Ora da definirsi.', category: "Seduta", timestamp: Date.now() - 3600000 * 3, isRead: false },
  { title: "🎉 Festa di Venerdì 13 Novembre", message: "La festa di specializzazione è fissata per venerdì 13 novembre. Luogo e ora saranno comunicati a breve: resta in attesa!", category: "Festa", timestamp: Date.now() - 3600000 * 2, isRead: false },
  { title: "🚌 Prenotazione Bus Navetta Aperta", message: "Sono disponibili 54 posti gratuiti per il transfer dal Policlinico di Bari alla location della festa. Riserva il tuo posto!", category: "Navetta", timestamp: Date.now() - 3600000, isRead: false }
];

export const Repo = {
  async prepopulateIfNeeded() {
    if (await getMeta("seeded")) return;
    if ((await count("guests")) === 0) await bulkAdd("guests", initialGuests);
    if ((await count("wishes")) === 0) await bulkAdd("wishes", initialWishes);
    if ((await count("photos")) === 0) await bulkAdd("photos", initialPhotos);
    if ((await count("giftTargets")) === 0) await bulkAdd("giftTargets", initialGiftTargets);
    if ((await count("giftContributions")) === 0) await bulkAdd("giftContributions", initialGiftContributions);
    if ((await count("notifications")) === 0) await bulkAdd("notifications", initialNotifications);
    await setMeta("seeded", true);
  },

  // Guests
  allGuests: () => getAll("guests"),
  addGuest: (g) => add("guests", { ...g, updatedAt: Date.now() }),
  updateGuest: (g) => put("guests", { ...g, updatedAt: Date.now() }),
  deleteGuest: (id) => del("guests", id),

  // Bus
  allBookings: () => getAll("busBookings"),
  totalBookedSeats: async () => (await getAll("busBookings")).reduce((s, b) => s + (b.seatsCount || 0), 0),
  addBooking: (b) => add("busBookings", { ...b, bookedAt: Date.now() }),
  deleteBooking: (id) => del("busBookings", id),

  // Wishes
  allWishes: () => getAll("wishes"),
  addWish: (w) => add("wishes", { ...w, heartCount: 1, createdAt: Date.now() }),
  heartWish: async (id) => {
    const w = await new Promise((res) => {
      const r = indexedDB.open("neuroparty-db");
      r.onsuccess = () => {
        const t = r.result.transaction("wishes", "readwrite");
        const get = t.objectStore("wishes").get(id);
        get.onsuccess = () => {
          const wish = get.result;
          wish.heartCount = (wish.heartCount || 0) + 1;
          t.objectStore("wishes").put(wish);
          res(wish);
        };
      };
    });
    return w;
  },

  // Photos
  allPhotos: () => getAll("photos"),
  addPhoto: (p) => add("photos", { ...p, likesCount: 1, createdAt: Date.now() }),
  likePhoto: async (id) => {
    return new Promise((res) => {
      const r = indexedDB.open("neuroparty-db");
      r.onsuccess = () => {
        const t = r.result.transaction("photos", "readwrite");
        const get = t.objectStore("photos").get(id);
        get.onsuccess = () => {
          const p = get.result;
          p.likesCount = (p.likesCount || 0) + 1;
          t.objectStore("photos").put(p);
          res(p);
        };
      };
    });
  },

  // Gifts
  allGiftTargets: () => getAll("giftTargets"),
  allGiftContributions: () => getAll("giftContributions"),
  addContribution: async (c) => {
    const id = await add("giftContributions", { ...c, contributedAt: Date.now() });
    const target = await new Promise((res) => {
      const r = indexedDB.open("neuroparty-db");
      r.onsuccess = () => {
        const t = r.result.transaction("giftTargets", "readwrite");
        const get = t.objectStore("giftTargets").get(c.targetGraduateId);
        get.onsuccess = () => {
          const tg = get.result;
          tg.collectedAmount = (tg.collectedAmount || 0) + (c.amount || 0);
          t.objectStore("giftTargets").put(tg);
          res(tg);
        };
      };
    });
    return id;
  },

  // Notifications
  allNotifications: () => getAll("notifications"),
  addNotification: (n) => add("notifications", { ...n, timestamp: Date.now(), isRead: false }),
  unreadCount: async () => (await getAll("notifications")).filter((n) => !n.isRead).length,
  markAllRead: async () => {
    const all = await getAll("notifications");
    const db = await new Promise((res) => {
      const r = indexedDB.open("neuroparty-db");
      r.onsuccess = () => res(r.result);
    });
    return new Promise((res) => {
      const t = db.transaction("notifications", "readwrite");
      all.forEach((n) => { n.isRead = true; t.objectStore("notifications").put(n); });
      t.oncomplete = () => res();
      res();
    });
  }
};
