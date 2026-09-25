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
  meta: { maxBusSeats: 54 }, schedule: {}, graduates: [],
  program: { badge: "", title: "", subtitle: "", dateLabel: "", locationLabel: "", timeline: [] },
  busSchedule: { subtitle: "", andata: {}, ritorno: {}, pickupStops: [] }, mapPoints: []
};

// --- Costanti dell'evento (live bindings: vengono aggiornate da initData) -------------
export let MAX_BUS_SEATS = 54;
export let GRADUATES = [];
export let MAP_POINTS = [];
export let PROGRAM = EMPTY_EVENT.program;
export let BUS_SCHEDULE = EMPTY_EVENT.busSchedule;
export let SCHEDULE = getSchedule(EMPTY_EVENT);
export let EVENT = EMPTY_EVENT; // evento completo con i testi già risolti (per il calendario)

function applyEvent(ev) {
  const e = { ...EMPTY_EVENT, ...(ev || {}) };
  SCHEDULE = getSchedule(e);
  // Segnaposto degli orari ({partyTime|...}): già risolti dal server, da risolvere per il JSON locale.
  EVENT = { ...e, program: resolvePlaceholders(e.program ?? EMPTY_EVENT.program, SCHEDULE),
    busSchedule: resolvePlaceholders(e.busSchedule ?? EMPTY_EVENT.busSchedule, SCHEDULE),
    mapPoints: resolvePlaceholders(e.mapPoints ?? [], SCHEDULE) };
  MAX_BUS_SEATS = e.meta?.maxBusSeats ?? 54;
  GRADUATES = e.graduates ?? [];
  MAP_POINTS = EVENT.mapPoints;
  PROGRAM = EVENT.program;
  BUS_SCHEDULE = EVENT.busSchedule;
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

async function refreshSnapshot() {
  try {
    const snap = await api.snapshot();
    snapshot = snap;
    Status.online = true;
    Status.version = snap.version;
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
      Status.pushSubscriptions = s.pushSubscriptions ?? 0;
      if (s.version !== Status.version) {
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

  allPhotos: async () => (snapshot ?? await refreshSnapshot()).photos,
  addPhoto: ({ file, authorName, caption }) => write(() => api.uploadPhoto(file, authorName, caption)),
  likePhoto: (id) => write(() => api.likePhoto(id)),

  allGiftTargets: async () => (snapshot ?? await refreshSnapshot()).giftTargets,
  allGiftContributions: async () => (snapshot ?? await refreshSnapshot()).giftContributions,
  addContribution: (c) => write(() => api.addContribution(c)),

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
    if ((await count("giftContributions")) === 0) await bulkAdd("giftContributions", (D.giftContributions ?? []).map((c) => ({ ...c, contributedAt: ts(c.ageHours) })));
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

  allPhotos: () => getAll("photos"),
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
  allGiftContributions: () => getAll("giftContributions"),
  addContribution: async (c) => {
    const target = (await getAll("giftTargets")).find((t) => t.id === c.targetGraduateId);
    const donorName = c.isAnonymous ? "Un invitato generoso" : (c.donorName || "Invitato");
    const id = await add("giftContributions", { ...c, donorName, targetGraduateName: target?.name ?? "", contributedAt: Date.now() });
    if (target) { target.collectedAmount = (target.collectedAmount || 0) + (c.amount || 0); await put("giftTargets", target); }
    notify();
    return id;
  },

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
