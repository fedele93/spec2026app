// Notifiche: permesso, notifica locale di prova e sottoscrizione Web Push al backend.
import { api, getApiBase } from "./api.js";

const CHANNEL = "Aggiornamenti Seduta & Festa Neurologia";
const LS_PUSH = "np.pushEndpoint";

export function supportsNotifications() { return "Notification" in window; }
export function supportsPush() { return "serviceWorker" in navigator && "PushManager" in window; }
export function isInstalledPwa() {
  return window.matchMedia("(display-mode: standalone)").matches || navigator.standalone === true;
}
export function isIos() { return /iphone|ipad|ipod/i.test(navigator.userAgent); }

export async function ensurePermission() {
  if (!supportsNotifications()) return false;
  if (Notification.permission === "granted") return true;
  if (Notification.permission === "denied") return false;
  return (await Notification.requestPermission()) === "granted";
}

// Notifica mostrata solo su questo dispositivo (usata in modalità locale e come anteprima).
export async function showLocalNotification(title, body) {
  const ok = await ensurePermission();
  if (!ok) return false;
  try {
    const reg = await navigator.serviceWorker?.getRegistration();
    const opts = { body, icon: "icons/icon-192.png", badge: "icons/icon-192.png", tag: "neuroparty-" + Date.now() };
    if (reg && reg.showNotification) await reg.showNotification(`NeuroParty · ${title}`, opts);
    else new Notification(`NeuroParty · ${title}`, opts);
    return true;
  } catch { return false; }
}

function urlBase64ToUint8Array(base64String) {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(base64);
  return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
}

export function isPushSubscribed() { try { return !!localStorage.getItem(LS_PUSH); } catch { return false; } }

// Iscrive questo dispositivo alle notifiche push del server (Chrome/Edge/Firefox; iOS 16.4+ da PWA installata).
export async function subscribeToPush() {
  if (!supportsPush()) throw new Error("Questo browser non supporta le notifiche push");
  if (!getApiBase()) throw new Error("Nessun server configurato");
  const ok = await ensurePermission();
  if (!ok) throw new Error("Permesso notifiche negato");
  const reg = await navigator.serviceWorker.ready;
  const { publicKey } = await api.vapidPublicKey();
  let sub = await reg.pushManager.getSubscription();
  if (!sub) {
    sub = await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(publicKey) });
  }
  await api.pushSubscribe(sub.toJSON());
  try { localStorage.setItem(LS_PUSH, sub.endpoint); } catch { /* ignora */ }
  return sub;
}

export async function unsubscribeFromPush() {
  const reg = await navigator.serviceWorker?.getRegistration();
  const sub = await reg?.pushManager?.getSubscription();
  if (sub) {
    try { await api.pushUnsubscribe(sub.endpoint); } catch { /* server offline */ }
    await sub.unsubscribe();
  }
  try { localStorage.removeItem(LS_PUSH); } catch { /* ignora */ }
}

// Al riavvio, se il permesso c'è già, rinnova silenziosamente la sottoscrizione sul server.
export async function resyncPushSubscription() {
  if (!supportsPush() || !getApiBase() || Notification.permission !== "granted") return;
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();
    if (sub) { await api.pushSubscribe(sub.toJSON()); localStorage.setItem(LS_PUSH, sub.endpoint); }
  } catch (e) { console.warn("Rinnovo sottoscrizione push fallito:", e.message); }
}

export { CHANNEL };
