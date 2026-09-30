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
  busSchedule: { subtitle: "", andata: {}, ritorno: {}, pickupStops: [] }, mapPoints: [],
  giftCollector: { name: "", roleTitle: "", description: "", iban: "", ibanHolder: "", paypalMeUrl: "", paymentMethods: ["IBAN", "PayPal", "Contanti"], transferReason: "" }
};

// --- Costanti dell'evento (live bindings: vengono aggiornate da initData) -------------
export let MAX_BUS_SEATS = 54;
export let GRADUATES = [];
export let MAP_POINTS = [];
export let PROGRAM = EMPTY_EVENT.program;
export let BUS_SCHEDULE = EMPTY_EVENT.busSchedule;
export let SCHEDULE = getSchedule(EMPTY_EVENT);
export let EVENT = EMPTY_EVENT; // evento completo con i testi già risolti (per il calendario)
export let GIFT_COLLECTOR = EMPTY_EVENT.giftCollector; // cassiere delle quote uniche

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
  GIFT_COLLECTOR = { ...EMPTY_EVENT.giftCollector, ...(e.giftCollector || {}) };
}

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
  treasurerEnabled: false, // il server ha un TREASURER_TOKEN (o ADMIN_TOKEN) per il cruscotto delle quote uniche
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
      Status.treasurerEnabled = !!s.treasurerEnabled;
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
  // quota unica al cassiere: il server calcola la ripartizione e la conserva; le proprie quote si leggono a parte
  addPoolContribution: (c) => write(() => api.addPoolContribution(c)),
  myPoolContributions: async () => { try { return await api.myPoolContributions(); } catch { return []; } },
  deletePoolContribution: (id) => write(() => api.deletePoolContribution(id)),
  poolBoard: () => api.poolBoard(),
  setPoolStatus: (id, status) => write(() => api.setPoolStatus(id, status)),

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
        return { ...c, allocations, totalAmount: allocations.reduce((s, a) => s + (a.amount || 0), 0), status: received ? "RECEIVED" : "PENDING",
          clientId: "demo", createdAt: ts(c.ageHours), receivedAt: received ? ts(c.ageHours) : null };
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
  addPoolContribution: async (c) => {
    const { allocations, totalAmount } = buildAllocations(c, await getAll("giftTargets"));
    const rec = { donorName: (c.donorName || "").trim(), contact: c.contact || "", paymentMethod: c.paymentMethod || "IBAN", splitMode: c.splitMode || "EQUAL",
      totalAmount, note: c.note || "", status: "PENDING", clientId: "local", createdAt: Date.now(), receivedAt: null, allocations };
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
    const byGraduate = targets.map((t) => ({ graduateId: t.id, graduateName: t.name, amount: 0, receivedAmount: 0, contributions: 0 }));
    const idx = new Map(byGraduate.map((g) => [g.graduateId, g]));
    let totalAmount = 0, receivedAmount = 0;
    for (const c of rows) {
      totalAmount += c.totalAmount; if (c.status === "RECEIVED") receivedAmount += c.totalAmount;
      for (const a of c.allocations) { const g = idx.get(a.graduateId); if (!g) continue; g.amount += a.amount; g.contributions++; if (c.status === "RECEIVED") g.receivedAmount += a.amount; }
    }
    const r2 = (v) => Math.round(v * 100) / 100;
    byGraduate.forEach((g) => { g.amount = r2(g.amount); g.receivedAmount = r2(g.receivedAmount); });
    return { contributions: rows, summary: { contributions: rows.length, received: rows.filter((c) => c.status === "RECEIVED").length,
      pending: rows.filter((c) => c.status !== "RECEIVED").length, totalAmount: r2(totalAmount), receivedAmount: r2(receivedAmount), pendingAmount: r2(totalAmount - receivedAmount), byGraduate, byMethod: [] } };
  },
  setPoolStatus: (id, status) => updateRecord("giftPool", id, (c) => { c.status = status; c.receivedAt = status === "RECEIVED" ? Date.now() : null; }).then(notify),

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
