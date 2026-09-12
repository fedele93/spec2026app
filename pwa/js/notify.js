// Notifiche web locali - mirror di NotificationHelper.kt (NotificationCompat locale)
const CHANNEL = "Aggiornamenti Seduta & Festa Neurologia";

export async function ensurePermission() {
  if (!("Notification" in window)) return false;
  if (Notification.permission === "granted") return true;
  if (Notification.permission === "denied") return false;
  const res = await Notification.requestPermission();
  return res === "granted";
}

export async function sendPush(title, body) {
  const ok = await ensurePermission();
  if (!ok) return false;
  try {
    const n = new Notification(`NeuroParty · ${title}`, { body, icon: "icons/icon-192.png", badge: "icons/icon-192.png", tag: "neuroparty-" + Date.now() });
    setTimeout(() => n.close(), 8000);
    return true;
  } catch (e) {
    return false;
  }
}

export { CHANNEL };
