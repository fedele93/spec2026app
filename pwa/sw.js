const CACHE = "neuroparty-v2.0.0";
const ASSETS = [
  "./",
  "./index.html",
  "./manifest.webmanifest",
  "./css/app.css",
  "./js/db.js",
  "./js/api.js",
  "./js/data.js",
  "./js/notify.js",
  "./js/app.js",
  "./js/screens.js",
  "./shared/event-data.json",
  "./icons/icon-192.png",
  "./icons/icon-512.png",
  "./vendor/leaflet/leaflet.css",
  "./vendor/leaflet/leaflet.js",
  "./vendor/leaflet/images/marker-icon.png",
  "./vendor/leaflet/images/marker-icon-2x.png",
  "./vendor/leaflet/images/marker-shadow.png"
];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(ASSETS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

// Strategia: le chiamate API vanno sempre in rete (i dati sono condivisi e cambiano);
// i file dell'app usano la cache (con aggiornamento in background).
self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.pathname.startsWith("/api/") || url.pathname.startsWith("/uploads/") || url.hostname.includes("tile.openstreetmap.org")) {
    return; // rete diretta (niente cache)
  }
  e.respondWith(
    caches.match(req).then((cached) => {
      const network = fetch(req)
        .then((res) => {
          if (res && res.status === 200 && (url.protocol === "http:" || url.protocol === "https:")) {
            const copy = res.clone();
            caches.open(CACHE).then((c) => c.put(req, copy));
          }
          return res;
        })
        .catch(() => cached);
      return cached || network;
    })
  );
});

// Notifica push in arrivo dal backend
self.addEventListener("push", (e) => {
  let data = { title: "NeuroParty", body: "Nuovo aggiornamento sull'evento", url: "./index.html" };
  try { data = { ...data, ...e.data.json() }; } catch { if (e.data) data.body = e.data.text(); }
  e.waitUntil(
    self.registration.showNotification(`NeuroParty · ${data.title}`, {
      body: data.body,
      icon: "icons/icon-192.png",
      badge: "icons/icon-192.png",
      tag: "neuroparty-" + (data.id || Date.now()),
      data: { url: data.url }
    })
  );
});

self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  e.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((list) => {
      for (const c of list) { if ("focus" in c) return c.focus(); }
      return self.clients.openWindow(e.notification.data?.url || "./");
    })
  );
});

self.addEventListener("message", (e) => {
  if (e.data && e.data.type === "SKIP_WAITING") self.skipWaiting();
});
