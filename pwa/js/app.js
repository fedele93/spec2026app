// App shell: routing tab, init, registrazione service worker
import { Repo } from "./data.js";
import * as Screens from "./screens.js";

let currentTab = "program";
let mapInstance = null;

const screenEl = () => document.getElementById("screen");

async function render() {
  const el = screenEl();
  el.scrollTop = 0;
  // distruggi mappa precedente per evitare leak
  if (mapInstance) { mapInstance.remove(); mapInstance = null; }
  if (currentTab === "program") await Screens.program(el, goto);
  else if (currentTab === "rsvp") await Screens.rsvp(el, goto);
  else if (currentTab === "bus") await Screens.bus(el, goto);
  else if (currentTab === "wishes") await Screens.wishes(el, goto);
  else if (currentTab === "gifts") await Screens.gifts(el, goto);
}

function goto(tab) {
  if (tab === currentTab) return;
  currentTab = tab;
  document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("active", t.dataset.tab === tab));
  render();
}

export async function toast(msg) {
  let t = document.querySelector(".toast");
  if (!t) { t = document.createElement("div"); t.className = "toast"; document.body.appendChild(t); }
  t.textContent = msg;
  t.classList.add("show");
  clearTimeout(t._timer);
  t._timer = setTimeout(() => t.classList.remove("show"), 2200);
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

export function fmtTime(ts) {
  const d = new Date(ts);
  return d.toLocaleDateString("it-IT", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}

async function init() {
  await Repo.prepopulateIfNeeded();
  document.querySelectorAll(".tab").forEach((t) => {
    t.onclick = () => goto(t.dataset.tab);
  });
  document.querySelector('.tab[data-tab="program"]').classList.add("active");
  await render();

  if ("serviceWorker" in navigator) {
    try {
      await navigator.serviceWorker.register("sw.js");
    } catch (e) { console.warn("SW registration failed", e); }
  }
}

init().catch((e) => {
  screenEl().innerHTML = `<div class="empty">Errore di avvio: ${e.message}</div>`;
  console.error(e);
});

export { render, goto };
