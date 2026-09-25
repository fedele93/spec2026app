// Client HTTP verso il backend (repo neuroparty-backend).
// - X-Client-Id: identifica questo dispositivo (chi crea un record può cancellarlo)
// - X-Admin-Token: abilita le azioni da organizzatore (notifiche push, cancellazioni)

const LS_API = "np.apiBase";
const LS_TOKEN = "np.adminToken";
const LS_CLIENT = "np.clientId";

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

export class ApiError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

function headers(extra = {}) {
  const h = { "X-Client-Id": getClientId(), ...extra };
  const t = getAdminToken();
  if (t) h["X-Admin-Token"] = t;
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
  uploadPhoto: (file, authorName, caption) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("authorName", authorName);
    fd.append("caption", caption);
    return request("/api/photos", { method: "POST", form: fd, timeoutMs: 60000 });
  },
  likePhoto: (id) => request(`/api/photos/${id}/like`, { method: "POST" }),

  giftTargets: () => request("/api/gifts/targets"),
  contributions: () => request("/api/gifts/contributions"),
  addContribution: (c) => request("/api/gifts/contributions", { method: "POST", json: c }),

  notifications: (since = 0) => request(`/api/notifications${since ? `?since=${since}` : ""}`),
  sendNotification: (n) => request("/api/notifications", { method: "POST", json: n }),
  scheduledNotifications: () => request("/api/notifications/scheduled"),
  deleteNotification: (id) => request(`/api/notifications/${id}`, { method: "DELETE" }),
  guestsSummary: () => request("/api/guests/summary"),

  vapidPublicKey: () => request("/api/push/vapid-public-key"),
  pushSubscribe: (sub) => request("/api/push/subscribe", { method: "POST", json: sub }),
  pushUnsubscribe: (endpoint) => request("/api/push/unsubscribe", { method: "POST", json: { endpoint } })
};

// Scarica un CSV riservato agli organizzatori (il token viaggia nell'header, non nell'URL).
export async function downloadCsv(kind) {
  const res = await fetch(`${getApiBase()}/api/export/${kind}.csv`, { headers: headers() });
  if (!res.ok) throw await parseError(res);
  const blob = await res.blob();
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = kind === "guests" ? "invitati.csv" : "navetta.csv";
  document.body.appendChild(a);
  a.click();
  setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 1000);
}
