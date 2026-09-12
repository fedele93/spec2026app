// Repository + entit\u00e0 + pre-popolazione (mirror di EventRepository.kt)
// UNICA SORGENTE DATI: ../shared/event-data.json
// Modificare quel JSON per aggiornare sia la PWA sia l'app Android (via codegen).
import { getAll, count, add, put, bulkAdd, del, getMeta, setMeta } from "./db.js";

export const RsvpStatus = { CONFIRMED: "CONFIRMED", PENDING: "PENDING", DECLINED: "DECLINED" };

// --- Caricamento della sorgente dati condivisa ---------------------------
// fetch iniziale con fallback a dati minimi se la rete/cache fallisce.
let DATA;
try {
  const resp = await fetch("shared/event-data.json", { cache: "no-cache" });
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
  DATA = await resp.json();
} catch (e) {
  console.error("Impossibile caricare event-data.json, avvio con dati vuoti:", e);
  DATA = { meta: { maxBusSeats: 54 }, graduates: [], program: { timeline: [] }, busSchedule: { pickupStops: [] }, mapPoints: [], guests: [], busBookings: [], wishes: [], photos: [], giftTargets: [], giftContributions: [], notifications: [] };
}

// --- Costanti derivate dal JSON -----------------------------------------
export const MAX_BUS_SEATS = DATA.meta?.maxBusSeats ?? 54;
export const GRADUATES = DATA.graduates ?? [];
export const MAP_POINTS = DATA.mapPoints ?? [];
export const PROGRAM = DATA.program ?? { badge: "", title: "", subtitle: "", dateLabel: "", locationLabel: "", timeline: [] };
export const BUS_SCHEDULE = DATA.busSchedule ?? { andata: {}, ritorno: {}, pickupStops: [] };

// --- Conversione timestamp relativi (ageHours -> millisecondi) ---------
const now = Date.now();
const ts = (ageHours) => now - Math.round((ageHours || 0) * 3600000);

// --- Seed dati (deriva dal JSON, non pi\u00f9 hardcoded) ---------------------
const initialGuests = (DATA.guests ?? []).map((g) => ({
  fullName: g.fullName,
  category: g.category,
  rsvpStatus: g.rsvpStatus,
  guestsCount: g.guestsCount,
  dietaryNotes: g.dietaryNotes,
  contactInfo: g.contactInfo,
  updatedAt: now
}));

const initialBookings = DATA.busBookings ?? [];

const initialWishes = (DATA.wishes ?? []).map((w) => ({
  authorName: w.authorName,
  targetGraduate: w.targetGraduate,
  message: w.message,
  emojiBadge: w.emojiBadge,
  heartCount: w.heartCount,
  createdAt: ts(w.ageHours)
}));

const initialPhotos = (DATA.photos ?? []).map((p) => ({
  authorName: p.authorName,
  caption: p.caption,
  imageResId: p.imageResId ?? 0,
  imageUri: p.imageUri ?? "",
  likesCount: p.likesCount,
  createdAt: ts(p.ageHours)
}));

const initialGiftTargets = DATA.giftTargets ?? [];

const initialGiftContributions = (DATA.giftContributions ?? []).map((c) => ({
  donorName: c.donorName,
  targetGraduateId: c.targetGraduateId,
  targetGraduateName: c.targetGraduateName,
  amount: c.amount,
  paymentMethod: c.paymentMethod,
  note: c.note,
  isAnonymous: c.isAnonymous ?? false,
  contributedAt: ts(c.ageHours)
}));

const initialNotifications = (DATA.notifications ?? []).map((n) => ({
  title: n.title,
  message: n.message,
  category: n.category,
  timestamp: ts(n.ageHours),
  isRead: n.isRead ?? false
}));

export const Repo = {
  async prepopulateIfNeeded() {
    if (await getMeta("seeded")) return;
    if ((await count("guests")) === 0) await bulkAdd("guests", initialGuests);
    if ((await count("busBookings")) === 0) await bulkAdd("busBookings", initialBookings);
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
