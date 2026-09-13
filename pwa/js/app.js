// App shell: routing tab, init, service worker, polling aggiornamenti dal server
import { Repo, Status, initData, onDataChange } from "./data.js";
import { resyncPushSubscription } from "./notify.js";
import * as Screens from "./screens.js";

let currentTab = "program";

const screenEl = () => document.getElementById("screen");

async function render() {
  const el = screenEl();
  el.scrollTop = 0;
  try {
    if (currentTab === "program") await Screens.program(el, goto);
    else if (currentTab === "rsvp") await Screens.rsvp(el, goto);
    else if (currentTab === "bus") await Screens.bus(el, goto);
    else if (currentTab === "wishes") await Screens.wishes(el, goto);
    else if (currentTab === "gifts") await Screens.gifts(el, goto);
  } catch (e) {
    console.error(e);
    el.innerHTML = `<div class="empty">Errore: ${e.message}</div>`;
  }
  updateStatusBar();
}

function goto(tab) {
  if (tab === currentTab) return;
  currentTab = tab;
  document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("active", t.dataset.tab === tab));
  render();
}

export function updateStatusBar() {
  let bar = document.getElementById("status-bar");
  if (!bar) { bar = document.createElement("div"); bar.id = "status-bar"; bar.className = "status-bar"; document.getElementById("app").prepend(bar); }
  if (Status.mode === "locale") {
    bar.textContent = "Modalità demo locale: i dati restano solo su questo dispositivo";
    bar.className = "status-bar warn";
  } else if (!Status.online) {
    bar.textContent = "Offline: mostro gli ultimi dati ricevuti dal server";
    bar.className = "status-bar warn";
  } else {
    bar.className = "status-bar hidden";
  }
}

export async function toast(msg) {
  let t = document.querySelector(".toast");
  if (!t) { t = document.createElement("div"); t.className = "toast"; document.body.appendChild(t); }
  t.textContent = msg;
  t.classList.add("show");
  clearTimeout(t._timer);
  t._timer = setTimeout(() => t.classList.remove("show"), 2600);
}

export function openModal(html, onMount) {
  const bg = document.createElement("div");
  bg.className = "modal-bg";
  bg.innerHTML = `<div class="modal"><button class="close" aria-label="Chiudi">×</button>${html}</div>`;
  document.body.appendChild(bg);
  const close = () => bg.remove();
  bg.querySelector(".close").onclick = close;
  bg.addEventListener("click", (e) => { if (e.target === bg) close(); });
  if (onMount) onMount(bg, close);
  return bg;
}

export function isModalOpen() { return !!document.querySelector(".modal-bg"); }

export function fmtTime(ts) {
  const d = new Date(ts);
  return d.toLocaleDateString("it-IT", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}

// Polling leggero: se la versione dei dati sul server cambia, ridisegna la schermata corrente.
const POLL_MS = 20000;
async function pollLoop() {
  if (document.visibilityState === "visible" && !isModalOpen()) {
    const changed = await Repo.pollState();
    updateStatusBar();
    if (changed) { await render(); }
  }
  setTimeout(pollLoop, POLL_MS);
}

async function init() {
  document.querySelectorAll(".tab").forEach((t) => { t.onclick = () => goto(t.dataset.tab); });
  document.querySelector('.tab[data-tab="program"]').classList.add("active");

  if ("serviceWorker" in navigator) {
    try { await navigator.serviceWorker.register("sw.js"); }
    catch (e) { console.warn("SW registration failed", e); }
  }

  await initData();
  await render();

  // ridisegna quando i dati cambiano in seguito a una scrittura (se nessun modale è aperto)
  onDataChange(() => { if (!isModalOpen()) render(); });
  document.addEventListener("visibilitychange", () => { if (document.visibilityState === "visible") Repo.pollState().then((c) => { updateStatusBar(); if (c) render(); }); });
  window.addEventListener("online", () => Repo.pollState().then(() => { updateStatusBar(); render(); }));
  window.addEventListener("offline", updateStatusBar);
  setTimeout(pollLoop, POLL_MS);
  resyncPushSubscription();
}

init().catch((e) => {
  screenEl().innerHTML = `<div class="empty">Errore di avvio: ${e.message}</div>`;
  console.error(e);
});

export { render, goto };
