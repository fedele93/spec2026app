// IndexedDB wrapper - mirror del layer Room dell'app Android
const DB_NAME = "neuroparty-db";
const DB_VERSION = 1;

const STORES = {
  guests: { keyPath: "id", autoIncrement: true },
  busBookings: { keyPath: "id", autoIncrement: true },
  wishes: { keyPath: "id", autoIncrement: true },
  photos: { keyPath: "id", autoIncrement: true },
  giftTargets: { keyPath: "id", autoIncrement: false },
  giftContributions: { keyPath: "id", autoIncrement: true },
  notifications: { keyPath: "id", autoIncrement: true },
  meta: { keyPath: "key" }
};

let _db = null;

function openDB() {
  return new Promise((resolve, reject) => {
    if (_db) return resolve(_db);
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = (e) => {
      const db = e.target.result;
      for (const [name, cfg] of Object.entries(STORES)) {
        if (!db.objectStoreNames.contains(name)) {
          db.createObjectStore(name, {
            keyPath: cfg.keyPath,
            autoIncrement: cfg.autoIncrement
          });
        }
      }
    };
    req.onsuccess = (e) => { _db = e.target.result; resolve(_db); };
    req.onerror = (e) => reject(e.target.error);
  });
}

async function tx(store, mode, fn) {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const t = db.transaction(store, mode);
    const os = t.objectStore(store);
    let result;
    const r = fn(os);
    if (r) r.onsuccess = () => { result = r.result; };
    t.oncomplete = () => resolve(result);
    t.onerror = () => reject(t.error);
  });
}

async function getAll(store) {
  return tx(store, "readonly", (os) => os.getAll());
}
async function count(store) {
  return tx(store, "readonly", (os) => os.count());
}
async function add(store, value) {
  return tx(store, "readwrite", (os) => os.add(value));
}
async function put(store, value) {
  return tx(store, "readwrite", (os) => os.put(value));
}
async function bulkAdd(store, values) {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const t = db.transaction(store, "readwrite");
    const os = t.objectStore(store);
    values.forEach((v) => os.add(v));
    t.oncomplete = () => resolve();
    t.onerror = () => reject(t.error);
  });
}
async function del(store, key) {
  return tx(store, "readwrite", (os) => os.delete(key));
}
async function clear(store) {
  return tx(store, "readwrite", (os) => os.clear());
}

async function getMeta(key) {
  const db = await openDB();
  return new Promise((resolve) => {
    const t = db.transaction("meta", "readonly");
    const r = t.objectStore("meta").get(key);
    r.onsuccess = () => resolve(r.result ? r.result.value : null);
    r.onerror = () => resolve(null);
  });
}
async function setMeta(key, value) {
  return put("meta", { key, value });
}

export { openDB, getAll, count, add, put, bulkAdd, del, clear, getMeta, setMeta };
