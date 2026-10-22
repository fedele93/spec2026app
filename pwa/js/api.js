// Client HTTP verso il backend (repo neuroparty-backend).
// - X-Client-Id: identifica questo dispositivo (chi crea un record può cancellarlo)
// - X-Admin-Token: abilita le azioni da organizzatore (notifiche push, cancellazioni)
// - X-Treasurer-Token: cassiere delle quote uniche (cruscotto, stato "ricevuta", CSV)

const LS_API = "np.apiBase";
const LS_TOKEN = "np.adminToken";
const LS_CLIENT = "np.clientId";
const LS_TREASURER = "np.treasurerToken";
const LS_GRADUATE = "np.graduateToken";
const LS_GUEST = "np.guestToken";

function ls(key, fallback = "") {
  try { return localStorage.getItem(key) ?? fallback; } catch { return fallback; }
}
function lsSet(key, value) {
  try { value == null || value === "" ? localStorage.removeItem(key) : localStorage.setItem(key, value); } catch { /* storage bloccato */ }
}

export function getClientId() {
  let id = ls(LS_CLIENT);
  if (!id) {
    id = (crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2) + Date.now().toString(36));
    lsSet(LS_CLIENT, id);
  }
  return id;
}

// URL base dell'API. Vuoto = stessa origine della PWA (configurazione Caddy).
export function getApiBase() {
  const custom = ls(LS_API).trim().replace(/\/+$/, "");
  if (custom) return custom;
  if (location.protocol === "http:" || location.protocol === "https:") return location.origin;
  return "";
}
export function setApiBase(url) { lsSet(LS_API, (url || "").trim().replace(/\/+$/, "")); }

export function getAdminToken() { return ls(LS_TOKEN); }
export function setAdminToken(token) { lsSet(LS_TOKEN, (token || "").trim()); }
export function isAdmin() { return !!getAdminToken(); }
export function getGuestToken() { return ls(LS_GUEST); }
export function setGuestToken(token) { lsSet(LS_GUEST, (token || "").trim()); }
export function getTreasurerToken() { return ls(LS_TREASURER); }
export function setTreasurerToken(token) { lsSet(LS_TREASURER, (token || "").trim()); }
// Il cruscotto delle quote uniche è visibile al cassiere e agli organizzatori (il server accetta entrambi i token).
// Solo il token cassiere apre le quote uniche: il token organizzatore non basta (le cifre restano ai cassieri)
export function isTreasurer() { return !!getTreasurerToken(); }
// Festeggiato: GRADUATE_TOKEN_<ID> sul server apre l'area 🎓 (chi viene, auguri, coordinate, il proprio avatar)
export function getGraduateToken() { return ls(LS_GRADUATE); }
export function setGraduateToken(token) { lsSet(LS_GRADUATE, (token || "").trim()); }

// Link personali ricevuti via email: https://<app>/#<chiave>=<token>. Una tabella sola per tutti i ruoli:
// la chiave dice quale token salvare (i token dei cassieri vengono dal .env e possono essere corti).
const HASH_LINKS = {
  profilo: { set: setGuestToken, minLen: 10 },
  festeggiato: { set: setGraduateToken, minLen: 10 },
  cassiere: { set: setTreasurerToken, minLen: 4 },
};
/** Se l'URL contiene un link personale lo consuma: salva il token, pulisce l'indirizzo e ritorna la chiave
 *  ("profilo", "festeggiato", "cassiere"); altrimenti null. */
export function consumeHashLink() {
  const m = /[#&](profilo|festeggiato|cassiere)=([A-Za-z0-9_-]+)/.exec(location.hash || "");
  if (!m || m[2].length < HASH_LINKS[m[1]].minLen) return null;
  HASH_LINKS[m[1]].set(m[2]);
  try { history.replaceState(null, "", location.pathname + location.search); } catch { /* ignora */ }
  return m[1];
}
export function isGraduate() { return !!getGraduateToken(); }

export class ApiError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

function headers(extra = {}) {
  const h = { "X-Client-Id": getClientId(), ...extra };
  const t = getAdminToken();
  if (t) h["X-Admin-Token"] = t;
  const tt = getTreasurerToken();
  if (tt) h["X-Treasurer-Token"] = tt;
  const gt = getGuestToken();
  if (gt) h["X-Guest-Token"] = gt;
  const gr = getGraduateToken();
  if (gr) h["X-Graduate-Token"] = gr;
  return h;
}

async function parseError(res) {
  let msg = `Errore ${res.status}`;
  try {
    const body = await res.json();
    if (typeof body.detail === "string") msg = body.detail;
    else if (Array.isArray(body.detail) && body.detail[0]?.msg) msg = body.detail[0].msg.replace(/^Value error, /, "");
  } catch { /* corpo non JSON */ }
  return new ApiError(res.status, msg);
}

export async function request(path, { method = "GET", json, form, timeoutMs = 12000 } = {}) {
  const base = getApiBase();
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const init = { method, headers: headers(), signal: ctrl.signal };
    if (json !== undefined) { init.headers["Content-Type"] = "application/json"; init.body = JSON.stringify(json); }
    if (form) init.body = form; // multipart: il browser imposta il boundary
    const res = await fetch(`${base}${path}`, init);
    if (!res.ok) throw await parseError(res);
    if (res.status === 204) return null;
    return await res.json();
  } catch (e) {
    if (e instanceof ApiError) throw e;
    throw new ApiError(0, navigator.onLine === false ? "Sei offline" : "Server non raggiungibile");
  } finally {
    clearTimeout(timer);
  }
}

export const api = {
  health: () => request("/api/health", { timeoutMs: 5000 }),
  state: () => request("/api/state", { timeoutMs: 6000 }),
  event: () => request("/api/event"),
  snapshot: () => request("/api/snapshot"),

  guests: () => request("/api/guests"),
  addGuest: (g) => request("/api/guests", { method: "POST", json: g }),
  updateGuest: (id, patch) => request(`/api/guests/${id}`, { method: "PUT", json: patch }),
  deleteGuest: (id) => request(`/api/guests/${id}`, { method: "DELETE" }),

  busSummary: () => request("/api/bus/summary"),
  bookings: () => request("/api/bus/bookings"),
  addBooking: (b) => request("/api/bus/bookings", { method: "POST", json: b }),
  deleteBooking: (id) => request(`/api/bus/bookings/${id}`, { method: "DELETE" }),

  wishes: () => request("/api/wishes"),
  addWish: (w) => request("/api/wishes", { method: "POST", json: w }),
  heartWish: (id) => request(`/api/wishes/${id}/heart`, { method: "POST" }),

  photos: () => request("/api/photos"),
  // a ritroso: le foto più vecchie dell'ultima già mostrata (createdAt + id)
  photosPage: (before, limit = 24) => request(`/api/photos/page?limit=${limit}` + (before ? `&beforeTs=${before.createdAt}&beforeId=${before.id}` : "")),
  uploadPhoto: (file, authorName, caption) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("authorName", authorName);
    fd.append("caption", caption);
    return request("/api/photos", { method: "POST", form: fd, timeoutMs: 60000 });
  },
  likePhoto: (id) => request(`/api/photos/${id}/like`, { method: "POST" }),

  giftTargets: () => request("/api/gifts/targets"),
  giftCollectors: () => request("/api/gifts/collectors"),
  // quota unica al cassiere: registrazione, le proprie quote, cancellazione (finché in attesa)
  addPoolContribution: (c) => request("/api/gifts/pool", { method: "POST", json: c }),
  myPoolContributions: () => request("/api/gifts/pool/mine"),
  deletePoolContribution: (id) => request(`/api/gifts/pool/${id}`, { method: "DELETE" }),
  // cruscotto del cassiere (token cassiere o organizzatore)
  poolBoard: () => request("/api/gifts/pool"),
  setPoolStatus: (id, status) => request(`/api/gifts/pool/${id}/status`, { method: "PATCH", json: { status } }),
  setPoolStatusBatch: (ids, status) => request("/api/gifts/pool/status", { method: "POST", json: { ids, status } }),
  setPoolNote: (id, treasurerNote) => request(`/api/gifts/pool/${id}`, { method: "PATCH", json: { treasurerNote } }),
  mailRemindPool: (days = 7) => request(`/api/mail/reminders/pool?days=${days}`, { method: "POST", timeoutMs: 60000 }),
  graduateMe: () => request("/api/graduate/me"),
  graduateRequestLink: (body) => request("/api/graduate/request-link", { method: "POST", json: body, timeoutMs: 30000 }),
  graduateVerifyCode: (body) => request("/api/graduate/verify-code", { method: "POST", json: body }),
  // cassiere: link e codice alla TREASURER_EMAIL_<ID> del server, poi accesso con il codice (app installata su iPhone)
  treasurerRequestLink: (body) => request("/api/gifts/collector/request-link", { method: "POST", json: body, timeoutMs: 30000 }),
  treasurerVerifyCode: (body) => request("/api/gifts/collector/verify-code", { method: "POST", json: body }),
  adminDashboard: () => request("/api/admin/dashboard"),
  // navetta: prenotazioni senza invitato e riconciliazione (sempre con la conferma dell'organizzatore)
  adminBusOrphans: () => request("/api/admin/bus/orphans"),
  adminBusLink: (bookingId, guestId) => request(`/api/admin/bus/${bookingId}/link`, { method: "POST", json: { guestId }, timeoutMs: 30000 }),
  adminBusCreateGuest: (bookingId) => request(`/api/admin/bus/${bookingId}/create-guest`, { method: "POST", timeoutMs: 30000 }),
  adminInvites: (body) => request("/api/admin/invites", { method: "POST", json: body, timeoutMs: 60000 }),
  adminResetTestData: (confirm) => request("/api/admin/reset-test-data", { method: "POST", json: { confirm }, timeoutMs: 60000 }),
  importGuests: (rows, { sendLinks = false, dryRun = false } = {}) => request("/api/guests/import", { method: "POST", json: { rows, sendLinks, dryRun }, timeoutMs: 120000 }),

  notifications: (since = 0) => request(`/api/notifications${since ? `?since=${since}` : ""}`),
  sendNotification: (n) => request("/api/notifications", { method: "POST", json: n }),
  scheduledNotifications: () => request("/api/notifications/scheduled"),
  deleteNotification: (id) => request(`/api/notifications/${id}`, { method: "DELETE" }),
  clearNotifications: () => request("/api/notifications", { method: "DELETE" }),
  deleteWish: (id) => request(`/api/wishes/${id}`, { method: "DELETE" }),
  deletePhoto: (id) => request(`/api/photos/${id}`, { method: "DELETE" }),
  guestsSummary: () => request("/api/guests/summary"),

  // profilo personale dell'invitato (vedi profile.js) ed email tramite n8n
  profileRequestLink: (body) => request("/api/profile/request-link", { method: "POST", json: body, timeoutMs: 30000 }),
  profileVerifyCode: (body) => request("/api/profile/verify-code", { method: "POST", json: body }),
  profileGet: () => request("/api/profile"),
  profilePut: (body) => request("/api/profile", { method: "PUT", json: body, timeoutMs: 30000 }),
  profileLogout: () => request("/api/profile/logout", { method: "POST" }),
  profileBusPut: (body) => request("/api/profile/bus", { method: "PUT", json: body, timeoutMs: 30000 }),
  profileBusClaim: (bookingId) => request("/api/profile/bus/claim", { method: "POST", json: { bookingId }, timeoutMs: 30000 }),
  profileBusDelete: () => request("/api/profile/bus", { method: "DELETE", timeoutMs: 30000 }),
  busCheckin: (id, checkedIn) => request(`/api/bus/bookings/${id}/checkin`, { method: "POST", json: { checkedIn } }),
  mailRemindMenu: () => request("/api/mail/reminders/menu", { method: "POST", timeoutMs: 60000 }),
  mailRemindBus: () => request("/api/mail/reminders/bus", { method: "POST", timeoutMs: 60000 }),
  guestLink: (id) => request(`/api/guests/${id}/link`),
  guestInvite: (id) => request(`/api/guests/${id}/invite`, { method: "POST", timeoutMs: 30000 }),
  mailStatus: () => request("/api/mail/status"),
  mailOutbox: (limit = 50) => request(`/api/mail/outbox?limit=${limit}`),
  mailResend: (id) => request(`/api/mail/outbox/${id}/resend`, { method: "POST", timeoutMs: 30000 }),
  mailTest: (to) => request("/api/mail/test", { method: "POST", json: { to }, timeoutMs: 30000 }),

  // assistente vocale con avatar (vedi assistant.js)
  assistantStatus: () => request("/api/assistant/status", { timeoutMs: 8000 }),
  assistantAvatars: () => request("/api/assistant/avatars"),
  assistantTalk: ({ avatarId, text, audio, audioExt, history, wantAudio = true }) => {
    const fd = new FormData();
    fd.append("avatarId", avatarId);
    if (text) fd.append("text", text);
    if (audio) fd.append("audio", audio, `voce.${audioExt || "webm"}`);
    fd.append("history", JSON.stringify(history || []));
    fd.append("wantAudio", wantAudio ? "true" : "false");
    return request("/api/assistant/talk", { method: "POST", form: fd, timeoutMs: 120000 });
  },
  assistantAdminAvatars: () => request("/api/assistant/admin/avatars"),
  assistantAdminVoices: () => request("/api/assistant/admin/voices"),
  assistantAdminUpdate: (id, patch) => request(`/api/assistant/admin/avatars/${id}`, { method: "PUT", json: patch }),
  assistantAdminUploadSample: (id, blob, filename) => {
    const fd = new FormData();
    fd.append("file", blob, filename);
    return request(`/api/assistant/admin/avatars/${id}/sample`, { method: "POST", form: fd, timeoutMs: 120000 });
  },
  assistantAdminDeleteSample: (id) => request(`/api/assistant/admin/avatars/${id}/sample`, { method: "DELETE" }),
  assistantAdminPreview: (id, text = "") => request(`/api/assistant/admin/avatars/${id}/preview`, { method: "POST", json: { text }, timeoutMs: 60000 }),

  vapidPublicKey: () => request("/api/push/vapid-public-key"),
  pushSubscribe: (sub) => request("/api/push/subscribe", { method: "POST", json: sub }),
  pushUnsubscribe: (endpoint) => request("/api/push/unsubscribe", { method: "POST", json: { endpoint } })
};

// Scarica un file riservato (es. campione vocale di un avatar) come URL locale riproducibile.
export async function fetchBlobUrl(path) {
  const res = await fetch(`${getApiBase()}${path}`, { headers: headers() });
  if (!res.ok) throw await parseError(res);
  return URL.createObjectURL(await res.blob());
}

const CSV_NAMES = { guests: "invitati.csv", bus: "navetta.csv", "gift-pool": "quote-uniche.csv" };

// Scarica un CSV riservato (organizzatori, o cassiere per le quote uniche): il token viaggia nell'header, non nell'URL.
export async function downloadCsv(kind) {
  const res = await fetch(`${getApiBase()}/api/export/${kind}.csv`, { headers: headers() });
  if (!res.ok) throw await parseError(res);
  const blob = await res.blob();
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = CSV_NAMES[kind] || `${kind}.csv`;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 1000);
}
