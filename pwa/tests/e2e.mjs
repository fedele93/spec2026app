// Test end-to-end della PWA con Playwright contro un backend locale.
// Uso:  BASE_URL=http://127.0.0.1:8765 ADMIN_TOKEN=test-token node pwa/tests/e2e.mjs
// Richiede: npm i playwright (o @playwright/test) e un backend avviato con PWA_DIR=pwa e SEED_DEMO_DATA=true.
import { chromium } from "playwright";
import fs from "node:fs";
import path from "node:path";

const BASE = process.env.BASE_URL || "http://127.0.0.1:8765";
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || "test-token";
const OUT = process.env.SCREENSHOT_DIR || "pwa/tests/screenshots";
fs.mkdirSync(OUT, { recursive: true });

let failures = 0;
const results = [];
function check(name, cond, extra = "") {
  results.push({ name, ok: !!cond });
  if (!cond) { failures++; console.log(`  ✗ ${name} ${extra}`); } else console.log(`  ✓ ${name}`);
}

const tinyPng = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAIAAABvFaqvAAAAIklEQVR4nGM8ISfHQA3ARBVTRg0aNWjUoFGDRg0aNYgiAACdgwE0j1vkvQAAAABJRU5ErkJggg==", "base64");

const browser = await chromium.launch({ args: ["--no-proxy-server", "--disable-dev-shm-usage"] });
const context = await browser.newContext({
  viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true, locale: "it-IT",
  permissions: ["notifications"]
});
const page = await context.newPage();
const consoleErrors = [];
page.on("pageerror", (e) => consoleErrors.push("pageerror: " + e.message));
// "Failed to load resource" arriva anche per le risposte 403/409 attese dai test negativi e per le tile mappa senza rete
page.on("console", (m) => { if (m.type() === "error" && !m.text().startsWith("Failed to load resource")) consoleErrors.push(m.text()); });
page.on("dialog", (d) => d.accept());

const shot = (name) => page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage: true });
const tab = async (name) => { await page.click(`.tab[data-tab="${name}"]`); await page.waitForTimeout(400); };
// Azzera il toast precedente prima di un'azione, così leggiamo davvero il nuovo messaggio.
const resetToast = () => page.evaluate(() => { const t = document.querySelector(".toast"); if (t) { t.classList.remove("show"); t.textContent = ""; } });
const toastText = async () => { await page.waitForSelector(".toast.show", { timeout: 8000 }); return page.textContent(".toast.show"); };
const clickExpectToast = async (selector) => { await resetToast(); await page.click(selector); return toastText(); };

console.log("1) Avvio e schermata Programma");
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForSelector(".hero h1");
check("hero con titolo evento", (await page.textContent(".hero h1")).includes("Seduta"));
check("9 laureandi nell'hero", (await page.textContent(".hero .graduates")).includes("Donato Regina"));
check("timeline con 5 tappe", (await page.$$(".timeline")).length === 5);
check("mappa Leaflet inizializzata", await page.$(".leaflet-container") !== null);
check("barra stato nascosta (modalità server online)", await page.$eval("#status-bar", (e) => e.classList.contains("hidden")));
check("badge notifiche non lette = 4", (await page.textContent("#ntf-show")).includes("4"));
await shot("01-programma");

console.log("2) Cronologia notifiche");
await page.click("#ntf-show");
await page.waitForSelector(".modal");
check("4 notifiche in cronologia", (await page.$$(".modal .card")).length === 4);
await page.click(".modal .close");
await page.waitForTimeout(300);
check("badge azzerato dopo lettura", !(await page.textContent("#ntf-show")).includes("4"));

console.log("3) Invitati & RSVP");
await tab("rsvp");
await page.waitForSelector("[data-guest]");
const guestsBefore = (await page.$$("[data-guest]")).length;
check("elenco invitati caricato dal server", guestsBefore >= 7);
await page.fill("#g-search", "Colombo");
await page.waitForTimeout(200);
check("ricerca filtra a 1", (await page.$$("[data-guest]")).length === 1);
await page.fill("#g-search", "");
await page.waitForTimeout(200);
await page.click("#add-guest");
await page.fill("#ag-name", "Test Playwright");
await page.fill("#ag-count", "3");
await page.fill("#ag-diet", "Vegetariano");
check("toast invitato aggiunto", (await clickExpectToast("#ag-save")).includes("aggiunto"));
await page.waitForTimeout(500);
check("un invitato in più dopo inserimento", (await page.$$("[data-guest]")).length === guestsBefore + 1);
const row = page.locator("[data-guest]", { hasText: "Test Playwright" });
await row.locator('[data-st="PENDING"]').click();
await page.waitForTimeout(500);
check("stato cambiato in PENDING", await page.locator("[data-guest]", { hasText: "Test Playwright" }).locator(".status-btn.pend").count() === 1);
await resetToast();
await page.locator("[data-guest]", { hasText: "Test Playwright" }).locator("[data-del]").click();
check("toast invitato rimosso (creatore)", (await toastText()).includes("rimosso"));
await page.waitForTimeout(500);
await resetToast();
await page.locator("[data-guest]", { hasText: "Giulia Colombo" }).locator("[data-del]").click();
check("rimozione record altrui negata (403)", (await toastText()).includes("organizzatore"));
await shot("02-invitati");

console.log("4) Navetta");
await tab("bus");
await page.waitForSelector("#book-bus");
const summary = await (await fetch(BASE + "/api/bus/summary")).json();
check("posti liberi coerenti con /api/bus/summary", (await page.textContent(".pill")).includes(`${summary.availableSeats} liberi`));
await page.click("#book-bus");
await page.fill("#bk-name", "Gruppo troppo grande");
await page.fill("#bk-seats", String(summary.availableSeats + 1));
check("overbooking rifiutato (409)", (await clickExpectToast("#bk-save")).includes("non sufficienti"));
await page.fill("#bk-seats", "2");
check("prenotazione ok", (await clickExpectToast("#bk-save")).includes("prenotato"));
await page.waitForTimeout(500);
check("posti liberi diminuiti di 2", (await page.textContent(".pill")).includes(`${summary.availableSeats - 2} liberi`));
await shot("03-navetta");

console.log("5) Auguri & Foto");
await tab("wishes");
await page.waitForSelector("#add-wish");
const wishesBefore = (await page.$$(".wish")).length;
check("auguri caricati dal server", wishesBefore >= 9);
await page.click("#add-wish");
await page.fill("#w-author", "Playwright");
await page.click('#w-targets .chip[data-tg="Fedele Luisi"]');
await page.fill("#w-msg", "Auguri automatici!");
check("augurio pubblicato", (await clickExpectToast("#w-send")).includes("pubblicato"));
await page.waitForTimeout(500);
check("nuovo augurio per primo", (await page.textContent(".wish")).includes("Auguri automatici") && (await page.$$(".wish")).length === wishesBefore + 1);
const firstHeart = page.locator(".wish [data-heart]").first();
const before = Number((await firstHeart.textContent()).replace(/\D/g, ""));
await firstHeart.click();
await page.waitForTimeout(600);
check("cuore incrementato", Number((await page.locator(".wish [data-heart]").first().textContent()).replace(/\D/g, "")) === before + 1);
await page.click("#add-photo");
await page.setInputFiles("#ph-file", { name: "test.png", mimeType: "image/png", buffer: tinyPng });
await page.fill("#ph-author", "Bot");
await page.fill("#ph-caption", "Foto di prova");
const photoToast = await clickExpectToast("#ph-save");
check("foto caricata sul server", photoToast.includes("pubblicata"), photoToast);
await page.waitForTimeout(600);
const img = await page.$(".photo img.thumb");
check("thumbnail servita da /uploads/", img !== null && (await img.getAttribute("src")).includes("/uploads/"));
await shot("04-auguri-foto");

console.log("6) Regali");
await tab("gifts");
await page.waitForSelector("[data-cont]");
check("10 destinatari regalo", (await page.$$("[data-cont]")).length === 10);
await page.click('[data-cont="luisi"]');
await page.click('#c-presets .chip[data-amt="100"]');
await page.fill("#c-donor", "Zio Playwright");
await page.fill("#c-note", "Bravo!");
check("contributo registrato", (await clickExpectToast("#c-save")).includes("100€"));
await page.waitForTimeout(600);
check("dedica visibile nelle ultime quote", (await page.textContent("#screen")).includes("Zio Playwright"));
await shot("05-regali");

console.log("7) Invio notifica push (solo organizzatore)");
await tab("program");
await page.waitForSelector("#settings");
check("pulsante invio nascosto senza token", (await page.$("#ntf-send")) === null);
await page.click("#settings");
await page.fill("#s-token", ADMIN_TOKEN);
await page.click("#s-save");
await page.waitForTimeout(600);
check("pulsante invio visibile con token", (await page.$("#ntf-send")) !== null);
await page.click("#ntf-send");
await page.click('.modal [data-tpl="1"]');
check("notifica inviata a tutti", (await clickExpectToast("#p-send")).includes("inviata"));
await page.waitForTimeout(600);
check("badge notifiche = 1 nuova", (await page.textContent("#ntf-show")).includes("1"));
await shot("06-notifica");

console.log("8) Persistenza: ricarico la pagina e i dati restano (server)");
await page.reload({ waitUntil: "networkidle" });
await tab("bus");
await page.waitForSelector("#book-bus");
check("prenotazione persistita dopo reload", (await page.textContent("#screen")).includes("Gruppo troppo grande"));

console.log("9) Polling: una modifica fatta via API compare senza ricaricare");
const res = await fetch(BASE + "/api/wishes", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ authorName: "API", message: "Arrivato dal server" }) });
check("POST /api/wishes ok", res.status === 201);
await page.evaluate(() => { document.dispatchEvent(new Event("visibilitychange")); });
await page.waitForTimeout(1200);
await tab("wishes");
await page.waitForSelector(".wish");
check("augurio dal server visibile", (await page.textContent("#screen")).includes("Arrivato dal server"));

console.log("10) Service worker + manifest");
const swOk = await page.evaluate(async () => !!(await navigator.serviceWorker.getRegistration()));
check("service worker registrato", swOk);
const manifest = await (await fetch(BASE + "/manifest.webmanifest")).json();
check("manifest installabile (standalone, icone 192/512)", manifest.display === "standalone" && manifest.icons.length >= 2);
const vapid = await (await fetch(BASE + "/api/push/vapid-public-key")).json();
check("chiave VAPID esposta", vapid.publicKey && vapid.publicKey.length > 80);

check("nessun errore JavaScript in console", consoleErrors.length === 0, consoleErrors.join(" | "));

await browser.close();
console.log(`\n${results.filter((r) => r.ok).length}/${results.length} controlli superati`);
if (failures) { console.log("FALLITI:", results.filter((r) => !r.ok).map((r) => r.name).join("; ")); process.exit(1); }
