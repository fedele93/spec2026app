// Livello dati della PWA.
// Modalità "server": i dati vivono nel backend (condivisi da tutti gli invitati);
//   una copia dell'ultimo snapshot è salvata in localStorage per la consultazione offline.
// Modalità "locale": se nessun server è raggiungibile al primo avvio, la PWA funziona in
//   modalità demo con IndexedDB (dati solo su questo dispositivo), come l'app Android senza rete.
import { getAll, count, add, put, bulkAdd, del, getMeta, setMeta } from "./db.js";
import { api, ApiError, getApiBase } from "./api.js";
import { getSchedule, resolvePlaceholders } from "./schedule.js";

export const RsvpStatus = { CONFIRMED: "CONFIRMED", PENDING: "PENDING", DECLINED: "DECLINED" };

const EMPTY_EVENT = {
  meta: { maxBusSeats: 40 }, schedule: {}, graduates: [],
  program: { badge: "", title: "", subtitle: "", dateLabel: "", locationLabel: "", timeline: [] },
  busSchedule: { subtitle: "", andata: {}, ritorno: {}, pickupStops: [] }, mapPoints: [],
  giftCollectors: []
};
const COLLECTOR_DEFAULTS = { id: "", name: "", roleTitle: "", description: "", iban: "", ibanHolder: "", paypalMeUrl: "", paypalContact: "", paymentMethods: ["IBAN", "PayPal", "Contanti"], transferReason: "" };

// --- Costanti dell'evento (live bindings: vengono aggiornate da initData) -------------
export let MAX_BUS_SEATS = 40;
export let GRADUATES = [];
export let MAP_POINTS = [];
export let PROGRAM = EMPTY_EVENT.program;
export let BUS_SCHEDULE = EMPTY_EVENT.busSchedule;
export let SCHEDULE = getSchedule(EMPTY_EVENT);
export let EVENT = EMPTY_EVENT; // evento completo con i testi già risolti (per il calendario)
export let GIFT_COLLECTORS = []; // cassieri delle quote uniche (giftCollectors nel JSON; accetta anche il vecchio giftCollector singolo)

function applyEvent(ev) {
  const e = { ...EMPTY_EVENT, ...(ev || {}) };
  SCHEDULE = getSchedule(e);
  // Segnaposto degli orari ({partyTime|...}): già risolti dal server, da risolvere per il JSON locale.
  EVENT = { ...e, program: resolvePlaceholders(e.program ?? EMPTY_EVENT.program, SCHEDULE),
    busSchedule: resolvePlaceholders(e.busSchedule ?? EMPTY_EVENT.busSchedule, SCHEDULE),
    mapPoints: resolvePlaceholders(e.mapPoints ?? [], SCHEDULE) };
  MAX_BUS_SEATS = e.meta?.maxBusSeats ?? 40;
  GRADUATES = e.graduates ?? [];
  MAP_POINTS = EVENT.mapPoints;
  PROGRAM = EVENT.program;
  BUS_SCHEDULE = EVENT.busSchedule;
  const raw = Array.isArray(e.giftCollectors) ? e.giftCollectors : (e.giftCollector ? [e.giftCollector] : []);
  GIFT_COLLECTORS = raw.map((c, i) => ({ ...COLLECTOR_DEFAULTS, ...(c || {}), id: String((c && c.id) || `cassiere${i + 1}`).toLowerCase() }));
}
export const collectorById = (id) => GIFT_COLLECTORS.find((c) => c.id === id) || GIFT_COLLECTORS[0] || COLLECTOR_DEFAULTS;

// Divide i centesimi in parti uguali: i centesimi di resto vanno ai primi della lista (come sul server).
export function splitEqually(totalCents, count) {
  const base = Math.floor(totalCents / count), rest = totalCents % count;
  return Array.from({ length: count }, (_, i) => base + (i < rest ? 1 : 0));
}
const cents = (v) => Math.round(Number(v || 0) * 100);

// Ripartizione di una quota unica calcolata in locale (modalità demo): stessa logica del server.
export function buildAllocations(c, targets) {
  const byId = new Map(targets.map((t) => [t.id, t]));
  if (c.splitMode === "CUSTOM") {
    const rows = (c.allocations || []).filter((a) => byId.has(a.graduateId) && cents(a.amount) > 0)
      .map((a) => ({ graduateId: a.graduateId, graduateName: byId.get(a.graduateId).name, amount: cents(a.amount) / 100 }));
    if (!rows.length) throw new ApiError(422, "Indica almeno un importo personalizzato");
    return { allocations: rows, totalAmount: rows.reduce((s, a) => s + cents(a.amount), 0) / 100 };
  }
  const ids = (c.graduateIds?.length ? c.graduateIds : targets.map((t) => t.id)).filter((id) => byId.has(id));
  const total = cents(c.totalAmount);
  if (!ids.length) throw new ApiError(422, "Nessun neo-specialista da includere nella quota");
  if (total < ids.length) throw new ApiError(422, "Importo troppo basso per essere diviso fra i neo-specialisti scelti");
  const parts = splitEqually(total, ids.length);
  return { allocations: ids.map((id, i) => ({ graduateId: id, graduateName: byId.get(id).name, amount: parts[i] / 100 })), totalAmount: total / 100 };
}

async function loadLocalEventFile() {
  try {
    const resp = await fetch("shared/event-data.json", { cache: "no-cache" });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    return await resp.json();
  } catch (e) {
    console.error("Impossibile caricare event-data.json:", e);
    return { ...EMPTY_EVENT };
  }
}

// --- Cache offline dello snapshot del server ---------------------------------------
const LS_SNAPSHOT = "np.snapshot";
const LS_SEEN = "np.notificationsSeenAt"; // notifiche viste (stato per dispositivo)
function readCache() { try { return JSON.parse(localStorage.getItem(LS_SNAPSHOT) || "null"); } catch { return null; } }
function writeCache(snap) { try { localStorage.setItem(LS_SNAPSHOT, JSON.stringify(snap)); } catch { /* quota piena */ } }
export function getSeenAt() { return Number(localStorage.getItem(LS_SEEN) || 0); }
export function markNotificationsSeen(ts = Date.now()) { try { localStorage.setItem(LS_SEEN, String(ts)); } catch { /* ignora */ } }

// --- Stato della connessione ----------------------------------------------------------
export const Status = {
  mode: "server",      // "server" | "locale"
  online: true,        // ultimo tentativo verso il server riuscito
  version: 0,          // versione dati del server (per il polling)
  adminEnabled: false, // il server ha un ADMIN_TOKEN configurato
  treasurerEnabled: false, // il server ha un TREASURER_TOKEN[_<ID>] per il cruscotto delle quote uniche
  canSeeLists: true,       // con i token di questo dispositivo il server mostra gli elenchi completi di invitati e navetta
  canSeeMemories: true,    // foto e auguri: solo invitati registrati (link personale) e ruoli; false = schermata "Accedi"
  roles: null,             // ruoli sbloccati dai token di questo dispositivo {admin, treasurer, treasurerIds, graduate}; null = server senza il campo
  pushSubscriptions: 0,
  lastError: ""
};

const listeners = new Set();
export function onDataChange(fn) { listeners.add(fn); return () => listeners.delete(fn); }
function notify() { listeners.forEach((fn) => { try { fn(); } catch (e) { console.error(e); } }); }

// ======================================================================================
//  Repository "server"
// ======================================================================================
let snapshot = null; // ultimo snapshot noto (server o cache)
// Foto più vecchie caricate a richiesta ("Mostra foto precedenti"): lo snapshot porta solo le più recenti,
// le pagine successive arrivano già in ordine e più vecchie di tutto ciò che c'è nello snapshot.
// hasMore null = lo dice lo snapshot (photosHasMore) finché non si è chiesta nessuna pagina.
let older = { photos: [], hasMore: null };
const newestFirst = (a, b) => (b.createdAt - a.createdAt) || (b.id - a.id);
// snapshot + pagine vecchie, senza doppioni (dopo una cancellazione lo snapshot può scorrere su una foto già caricata)
function galleryPhotos(snap) {
  const seen = new Set((snap.photos || []).map((p) => p.id));
  return (snap.photos || []).concat(older.photos.filter((p) => !seen.has(p.id)));
}

async function refreshSnapshot() {
  try {
    const snap = await api.snapshot();
    snapshot = snap;
    Status.online = true;
    Status.version = snap.version;
    Status.canSeeLists = snap.canSeeLists !== false;
    Status.canSeeMemories = snap.canSeeMemories !== false;
    if (!Status.canSeeMemories) older = { photos: [], hasMore: null }; // bacheca chiusa: via anche le pagine caricate
    applyEvent(snap.event);
    writeCache(snap);
    return snap;
  } catch (e) {
    Status.online = false;
    Status.lastError = e.message;
    if (!snapshot) snapshot = readCache();
    if (!snapshot) throw e;
    return snapshot;
  }
}

// Esegue una scrittura sul server e poi ricarica lo snapshot.
async function write(fn) {
  const result = await fn();
  await refreshSnapshot();
  notify();
  return result;
}

const ServerRepo = {
  mode: "server",
  async prepopulateIfNeeded() {
    await refreshSnapshot();
  },
  async pollState() {
    try {
      const s = await api.state();
      Status.online = true;
      Status.adminEnabled = !!s.adminEnabled;
      Status.treasurerEnabled = !!s.treasurerEnabled;
      // token cambiati (profilo aperto o chiuso, ruolo inserito): cambia cosa manda il server anche a versione uguale
      const accessChanged = (s.canSeeLists !== false) !== Status.canSeeLists || (s.canSeeMemories !== false) !== Status.canSeeMemories;
      Status.canSeeLists = s.canSeeLists !== false;
      Status.canSeeMemories = s.canSeeMemories !== false;
      Status.roles = s.roles || null;
      Status.pushSubscriptions = s.pushSubscriptions ?? 0;
      if (s.version !== Status.version || accessChanged) {
        await refreshSnapshot();
        notify();
        return true;
      }
      return false;
    } catch (e) {
      Status.online = false;
      Status.lastError = e.message;
      return false;
    }
  },

  allGuests: async () => (snapshot ?? await refreshSnapshot()).guests,
  addGuest: (g) => write(() => api.addGuest(g)),
  updateGuest: (g) => write(() => api.updateGuest(g.id, {
    fullName: g.fullName, category: g.category, rsvpStatus: g.rsvpStatus,
    guestsCount: g.guestsCount, dietaryNotes: g.dietaryNotes, contactInfo: g.contactInfo
  })),
  updateGuestStatus: (id, rsvpStatus) => write(() => api.updateGuest(id, { rsvpStatus })),
  deleteGuest: (id) => write(() => api.deleteGuest(id)),

  allBookings: async () => (snapshot ?? await refreshSnapshot()).busBookings,
  totalBookedSeats: async () => (await ServerRepo.allBookings()).reduce((s, b) => s + (b.seatsCount || 0), 0),
  addBooking: (b) => write(() => api.addBooking(b)),
  deleteBooking: (id) => write(() => api.deleteBooking(id)),

  allWishes: async () => (snapshot ?? await refreshSnapshot()).wishes,
  addWish: (w) => write(() => api.addWish(w)),
  heartWish: (id) => write(() => api.heartWish(id)),
  deleteWish: (id) => write(() => api.deleteWish(id)),
  deletePhoto: (id) => { older.photos = older.photos.filter((p) => p.id !== id); return write(() => api.deletePhoto(id)); },
  clearNotifications: () => write(() => api.clearNotifications()),

  // galleria: prima pagina dallo snapshot + le pagine più vecchie chieste dall'utente, dalla più recente
  allPhotos: async () => galleryPhotos(snapshot ?? await refreshSnapshot()),
  photosTotal: () => snapshot?.photosTotal ?? (snapshot?.photos || []).length,
  photosHasMore: () => (older.hasMore ?? !!snapshot?.photosHasMore),
  /** Carica la pagina successiva (a ritroso dall'ultima foto mostrata) e ritorna le foto nuove. */
  loadOlderPhotos: async () => {
    const shown = galleryPhotos(snapshot ?? await refreshSnapshot());
    const page = await api.photosPage(shown[shown.length - 1]);
    const seen = new Set(shown.map((p) => p.id));
    const fresh = (page.photos || []).filter((p) => !seen.has(p.id));
    older = { photos: older.photos.concat(fresh), hasMore: !!page.hasMore };
    return fresh;
  },
  addPhoto: ({ file, authorName, caption }) => write(() => api.uploadPhoto(file, authorName, caption)),
  likePhoto: (id) => write(() => api.likePhoto(id)),

  allGiftTargets: async () => (snapshot ?? await refreshSnapshot()).giftTargets,
  // quota unica al cassiere: il server calcola la ripartizione e la conserva; le proprie quote si leggono a parte
  addPoolContribution: (c) => write(() => api.addPoolContribution(c)),
  myPoolContributions: async () => { try { return await api.myPoolContributions(); } catch { return []; } },
  deletePoolContribution: (id) => write(() => api.deletePoolContribution(id)),
  poolBoard: () => api.poolBoard(),
  setPoolStatus: (id, status) => write(() => api.setPoolStatus(id, status)),
  setPoolStatusBatch: (ids, status) => write(() => api.setPoolStatusBatch(ids, status)),
  setPoolNote: (id, note) => api.setPoolNote(id, note),

  allNotifications: async () => {
    const seen = getSeenAt();
    return (snapshot ?? await refreshSnapshot()).notifications.map((n) => ({ ...n, isRead: n.timestamp <= seen }));
  },
  addNotification: (n) => write(() => api.sendNotification(n)),
  unreadCount: async () => (await ServerRepo.allNotifications()).filter((n) => !n.isRead).length,
  markAllRead: async () => { markNotificationsSeen(); notify(); }
};

// ======================================================================================
//  Repository "locale" (IndexedDB, modalità demo senza server)
// ======================================================================================
let localData = null;
const now = Date.now();
const ts = (ageHours) => now - Math.round((ageHours || 0) * 3600000);

async function updateRecord(store, id, mutate) {
  const all = await getAll(store);
  const rec = all.find((x) => x.id === id);
  if (!rec) return null;
  mutate(rec);
  await put(store, rec);
  return rec;
}

const LocalRepo = {
  mode: "locale",
  async prepopulateIfNeeded() {
    const D = localData;
    if (await getMeta("seeded")) return;
    if ((await count("guests")) === 0) await bulkAdd("guests", (D.guests ?? []).map((g) => ({ ...g, updatedAt: now })));
    if ((await count("busBookings")) === 0) await bulkAdd("busBookings", (D.busBookings ?? []).map((b) => ({ ...b, bookedAt: now })));
    if ((await count("wishes")) === 0) await bulkAdd("wishes", (D.wishes ?? []).map((w) => ({ ...w, createdAt: ts(w.ageHours) })));
    if ((await count("photos")) === 0) await bulkAdd("photos", (D.photos ?? []).map((p) => ({ ...p, imageUri: p.imageUri ?? "", createdAt: ts(p.ageHours) })));
    if ((await count("giftTargets")) === 0) await bulkAdd("giftTargets", D.giftTargets ?? []);
    if ((await count("giftPool")) === 0) {
      const names = new Map((D.giftTargets ?? []).map((t) => [t.id, t.name]));
      await bulkAdd("giftPool", (D.giftPoolContributions ?? []).map((c) => {
        const allocations = (c.allocations ?? []).map((a) => ({ ...a, graduateName: names.get(a.graduateId) ?? "" }));
        const received = c.status === "RECEIVED";
        const collector = collectorById(c.collectorId);
        return { ...c, collectorId: collector.id, collectorName: collector.name, allocations, totalAmount: allocations.reduce((s, a) => s + (a.amount || 0), 0),
          status: received ? "RECEIVED" : "PENDING", clientId: "demo", createdAt: ts(c.ageHours), receivedAt: received ? ts(c.ageHours) : null };
      }));
    }
    if ((await count("notifications")) === 0) await bulkAdd("notifications", (D.notifications ?? []).map((n) => ({ ...n, timestamp: ts(n.ageHours) })));
    await setMeta("seeded", true);
  },
  async pollState() { return false; },

  allGuests: () => getAll("guests"),
  addGuest: (g) => add("guests", { ...g, updatedAt: Date.now() }).then(notify),
  updateGuest: (g) => put("guests", { ...g, updatedAt: Date.now() }).then(notify),
  updateGuestStatus: (id, rsvpStatus) => updateRecord("guests", id, (g) => { g.rsvpStatus = rsvpStatus; g.updatedAt = Date.now(); }).then(notify),
  deleteGuest: (id) => del("guests", id).then(notify),

  allBookings: () => getAll("busBookings"),
  totalBookedSeats: async () => (await getAll("busBookings")).reduce((s, b) => s + (b.seatsCount || 0), 0),
  addBooking: async (b) => {
    const booked = await LocalRepo.totalBookedSeats();
    if (booked + b.seatsCount > MAX_BUS_SEATS) throw new ApiError(409, `Posti non sufficienti (disponibili solo ${Math.max(0, MAX_BUS_SEATS - booked)} posti).`);
    return add("busBookings", { ...b, bookedAt: Date.now() }).then(notify);
  },
  deleteBooking: (id) => del("busBookings", id).then(notify),

  allWishes: () => getAll("wishes"),
  addWish: (w) => add("wishes", { ...w, heartCount: 1, createdAt: Date.now() }).then(notify),
  heartWish: (id) => updateRecord("wishes", id, (w) => { w.heartCount = (w.heartCount || 0) + 1; }).then(notify),
  deleteWish: (id) => del("wishes", id).then(notify),
  deletePhoto: (id) => del("photos", id).then(notify),
  clearNotifications: async () => { for (const n of await getAll("notifications")) await del("notifications", n.id); notify(); return { deleted: 0 }; },

  allPhotos: async () => (await getAll("photos")).sort(newestFirst),
  photosTotal: () => null,
  photosHasMore: () => false, // in modalità locale le foto sono poche e tutte sul dispositivo
  loadOlderPhotos: async () => [],
  addPhoto: async ({ file, authorName, caption }) => {
    let imageUri = "";
    if (file) {
      if (file.size > 4_500_000) throw new ApiError(413, "Immagine troppo grande (max ~4.5MB in modalità locale)");
      imageUri = await new Promise((res) => { const r = new FileReader(); r.onload = () => res(r.result); r.readAsDataURL(file); });
    }
    return add("photos", { authorName, caption, imageUri, likesCount: 1, createdAt: Date.now() }).then(notify);
  },
  likePhoto: (id) => updateRecord("photos", id, (p) => { p.likesCount = (p.likesCount || 0) + 1; }).then(notify),

  allGiftTargets: () => getAll("giftTargets"),
  addPoolContribution: async (c) => {
    const { allocations, totalAmount } = buildAllocations(c, await getAll("giftTargets"));
    const collector = collectorById(c.collectorId);
    if (!(collector.paymentMethods || []).includes(c.paymentMethod || "IBAN")) throw new ApiError(422, `${collector.name} non accetta ${c.paymentMethod}`);
    const rec = { collectorId: collector.id, collectorName: collector.name, donorName: (c.donorName || "").trim(), contact: c.contact || "", email: (c.email || "").trim().toLowerCase(), paymentMethod: c.paymentMethod || "IBAN", splitMode: c.splitMode || "EQUAL",
      totalAmount, note: c.note || "", treasurerNote: "", status: "PENDING", clientId: "local", createdAt: Date.now(), receivedAt: null, allocations };
    if (!rec.donorName) throw new ApiError(422, "Il nome è obbligatorio: serve al cassiere per riconoscere il versamento");
    rec.id = await add("giftPool", rec);
    notify();
    return rec;
  },
  myPoolContributions: async () => (await getAll("giftPool")).filter((c) => c.clientId === "local").sort((a, b) => b.createdAt - a.createdAt),
  deletePoolContribution: async (id) => {
    const rec = (await getAll("giftPool")).find((c) => c.id === id);
    if (rec && rec.status === "RECEIVED") throw new ApiError(409, "Quota già ricevuta dal cassiere: contattalo per modificarla");
    return del("giftPool", id).then(notify);
  },
  poolBoard: async () => {
    const rows = (await getAll("giftPool")).sort((a, b) => b.createdAt - a.createdAt);
    const targets = await getAll("giftTargets");
    const r2 = (v) => Math.round(v * 100) / 100;
    const summarize = (list) => {
      const byGraduate = targets.map((t) => ({ graduateId: t.id, graduateName: t.name, amount: 0, receivedAmount: 0, contributions: 0 }));
      const idx = new Map(byGraduate.map((g) => [g.graduateId, g]));
      let totalAmount = 0, receivedAmount = 0;
      for (const c of list) {
        totalAmount += c.totalAmount; if (c.status === "RECEIVED") receivedAmount += c.totalAmount;
        for (const a of c.allocations) { const g = idx.get(a.graduateId); if (!g) continue; g.amount += a.amount; g.contributions++; if (c.status === "RECEIVED") g.receivedAmount += a.amount; }
      }
      byGraduate.forEach((g) => { g.amount = r2(g.amount); g.receivedAmount = r2(g.receivedAmount); });
      return { contributions: list.length, received: list.filter((c) => c.status === "RECEIVED").length, pending: list.filter((c) => c.status !== "RECEIVED").length,
        totalAmount: r2(totalAmount), receivedAmount: r2(receivedAmount), pendingAmount: r2(totalAmount - receivedAmount), byGraduate, byMethod: [] };
    };
    const summary = summarize(rows);
    summary.byCollector = GIFT_COLLECTORS.map((c) => ({ collectorId: c.id, collectorName: c.name, ...summarize(rows.filter((r) => (r.collectorId || GIFT_COLLECTORS[0]?.id) === c.id)) }));
    return { collectors: GIFT_COLLECTORS.map((c) => ({ id: c.id, name: c.name })), contributions: rows, summary };
  },
  setPoolStatus: (id, status) => updateRecord("giftPool", id, (c) => { c.status = status; c.receivedAt = status === "RECEIVED" ? Date.now() : null; }).then(notify),
  setPoolStatusBatch: async (ids, status) => { for (const id of ids) await updateRecord("giftPool", id, (c) => { c.status = status; c.receivedAt = status === "RECEIVED" ? Date.now() : null; }); notify(); return { updated: ids.length, skipped: [] }; },
  setPoolNote: (id, note) => updateRecord("giftPool", id, (c) => { c.treasurerNote = note; }),

  allNotifications: () => getAll("notifications"),
  addNotification: ({ sendAt, ...n }) => add("notifications", { ...n, timestamp: Date.now(), isRead: false }).then(notify), // niente programmazione in demo
  unreadCount: async () => (await getAll("notifications")).filter((n) => !n.isRead).length,
  markAllRead: async () => {
    const all = await getAll("notifications");
    for (const n of all) { if (!n.isRead) { n.isRead = true; await put("notifications", n); } }
    notify();
  }
};

// ======================================================================================
//  Selezione della modalità e facciata "Repo"
// ======================================================================================
let impl = ServerRepo;

export const Repo = new Proxy({}, {
  get(_t, prop) {
    if (prop === "mode") return impl.mode;
    const v = impl[prop];
    return typeof v === "function" ? v.bind(impl) : v;
  }
});

export async function initData() {
  // 1) prova il server (stessa origine, oppure l'URL impostato nelle impostazioni)
  if (getApiBase()) {
    try {
      const s = await api.state();
      Status.mode = "server";
      Status.online = true;
      Status.adminEnabled = !!s.adminEnabled;
      Status.treasurerEnabled = !!s.treasurerEnabled;
      Status.canSeeLists = s.canSeeLists !== false;
      Status.canSeeMemories = s.canSeeMemories !== false;
      Status.roles = s.roles || null;
      Status.pushSubscriptions = s.pushSubscriptions ?? 0;
      impl = ServerRepo;
      await ServerRepo.prepopulateIfNeeded();
      return Status;
    } catch (e) {
      console.warn("Server non raggiungibile:", e.message);
      const cached = readCache();
      if (cached) {
        // offline ma con dati già visti: modalità server in sola lettura
        snapshot = cached;
        Status.mode = "server";
        Status.online = false;
        Status.lastError = e.message;
        Status.roles = null; // non confermabili offline
        Status.version = cached.version || 0;
        applyEvent(cached.event);
        impl = ServerRepo;
        return Status;
      }
    }
  }
  // 2) nessun server: modalità demo locale
  localData = await loadLocalEventFile();
  applyEvent(localData);
  Status.mode = "locale";
  Status.online = false;
  Status.roles = null; // nessun server: nessun ruolo confermato
  impl = LocalRepo;
  await LocalRepo.prepopulateIfNeeded();
  return Status;
}

// Riprova a passare alla modalità server (es. dopo aver impostato l'URL nelle impostazioni).
export async function reconnect() {
  try { localStorage.removeItem(LS_SNAPSHOT); } catch { /* ignora */ }
  snapshot = null;
  return initData();
}
