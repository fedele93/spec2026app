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

// microfono finto per provare la registrazione dell'assistente vocale senza hardware
const browser = await chromium.launch({ args: ["--no-proxy-server", "--disable-dev-shm-usage", "--use-fake-ui-for-media-stream", "--use-fake-device-for-media-stream"] });
const context = await browser.newContext({
  viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true, locale: "it-IT",
  permissions: ["notifications", "microphone"]
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
check("orari: nessun segnaposto non risolto nella pagina", !/\{\w+\|/.test(await page.textContent(".screen")));
check("pulsante calendario punta a /api/event/calendar.ics", (await page.getAttribute("#cal-add", "href")).endsWith("/api/event/calendar.ics"));
const ics = await page.evaluate(async (b) => { const r = await fetch(b + "/api/event/calendar.ics"); return { ok: r.ok, ct: r.headers.get("content-type"), body: await r.text() }; }, BASE);
check("calendario .ics con seduta e festa", ics.ok && ics.ct.startsWith("text/calendar") && ics.body.split("BEGIN:VEVENT").length === 3);
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
check("riepilogo catering con menu speciali", (await page.textContent("#catering")).includes("Celiaco"));
await page.click("#catering summary");
check("riepilogo catering: categorie e nomi", (await page.textContent("#catering")).includes("Famigliari") && (await page.textContent("#catering")).includes("Matteo Moretti"));
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
await page.waitForSelector("[data-target]");
check("9 neo-specialisti con regalo diretto", (await page.$$("[data-target]")).length === 9);
check("nessuna cifra raccolta visibile", !/Raccolt|Obiettivo/.test(await page.textContent("#screen")));
check("nessun cruscotto cassiere senza token", (await page.$(".board")) === null);
check("pulsanti copia IBAN e PayPal/Satispay", (await page.$$("[data-copy-iban]")).length === 9 && (await page.$$(".gift .paypal")).length === 9 && (await page.$$(".gift .satispay")).length === 9);
check("nessuna opzione contanti per il regalo diretto", !(await page.textContent(".gift")).includes("Contanti"));
await page.click("#pool-open");
await page.waitForSelector("#q-save");
check("metodi cassa: IBAN, PayPal, Contanti (no Satispay)", (await Promise.all((await page.$$("#q-methods .chip")).map((c) => c.textContent()))).join(",") === "IBAN,PayPal,Contanti");
await page.fill("#q-donor", "Zio Playwright");
await page.click('#q-presets .chip[data-amt="150"]');
await page.click('[data-who="luisi"]'); // escludo un neo-specialista: 150 € su 8
check("anteprima parti uguali (18,75 € ciascuno)", (await page.textContent('[data-each="carlone"]')).includes("18,75"));
check("causale suggerita con il nome", (await page.textContent("#q-reason")).includes("Zio Playwright"));
const poolToast = await clickExpectToast("#q-save");
check("quota unica registrata", poolToast.includes("150,00"), poolToast);
await page.waitForTimeout(600);
check("la mia quota compare nella card del cassiere", (await page.textContent(".collector .mine")).includes("150,00 €"));
const myPool = await fetch(BASE + "/api/gifts/pool/mine", { headers: { "X-Client-Id": await page.evaluate(() => localStorage.getItem("np.clientId")) } }).then((r) => r.json());
check("ripartizione salvata sul server (8 parti)", myPool.length === 1 && myPool[0].allocations.length === 8 && !myPool[0].allocations.some((a) => a.graduateId === "luisi"));
check("le quote non sono pubbliche", (await fetch(BASE + "/api/gifts/pool")).status === 403);
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

console.log("7b) Notifica programmata ed export CSV (organizzatore)");
const publicBefore = (await fetch(BASE + "/api/notifications").then((r) => r.json())).length;
await page.click("#ntf-send");
await page.fill("#p-title", "🎂 Torta tra poco");
await page.fill("#p-body", "Programmata dal test e2e");
const inOneHour = new Date(Date.now() + 3600000);
const pad = (n) => String(n).padStart(2, "0");
await page.fill("#p-when", `${inOneHour.getFullYear()}-${pad(inOneHour.getMonth() + 1)}-${pad(inOneHour.getDate())}T${pad(inOneHour.getHours())}:${pad(inOneHour.getMinutes())}`);
check("toast notifica programmata", (await clickExpectToast("#p-send")).includes("programmata"));
await page.waitForTimeout(400);
const adminHeaders = { "X-Admin-Token": ADMIN_TOKEN };
const scheduled = await fetch(BASE + "/api/notifications/scheduled", { headers: adminHeaders }).then((r) => r.json());
check("una notifica in attesa sul server", scheduled.length === 1 && scheduled[0].title === "🎂 Torta tra poco");
check("gli invitati non la vedono", (await fetch(BASE + "/api/notifications").then((r) => r.json())).length === publicBefore);
await page.click("#ntf-send");
await page.waitForSelector("[data-cancel]");
check("programmata elencata nel dialogo", (await page.textContent("#p-scheduled")).includes("Torta tra poco"));
check("annullamento programmata", (await clickExpectToast("[data-cancel]")).includes("annullata"));
check("nessuna programmata dopo annullamento", (await fetch(BASE + "/api/notifications/scheduled", { headers: adminHeaders }).then((r) => r.json())).length === 0);
await page.click(".modal .close");
const csv = await page.evaluate(async (b) => { const r = await fetch(b + "/api/export/guests.csv", { headers: { "X-Admin-Token": localStorage.getItem("np.adminToken") } }); return { ok: r.ok, body: await r.text() }; }, BASE);
check("export CSV invitati", csv.ok && csv.body.replace(/^\ufeff/, "").startsWith("Nome;Categoria;Stato RSVP"));
check("export CSV negato senza token", (await fetch(BASE + "/api/export/bus.csv")).status === 403);
await page.click("#settings");
check("pulsanti export in Impostazioni", (await page.$("#s-exp-guests")) !== null && (await page.$("#s-exp-bus")) !== null && (await page.$("#s-exp-pool")) !== null);
await page.click(".modal .close");

console.log("7c) Cruscotto cassiere (il token organizzatore vale anche come cassiere)");
await tab("gifts");
await page.waitForSelector(".board");
const boardText = await page.textContent(".board");
check("totali per neo-specialista", boardText.includes("Carlone") && boardText.includes("18,75"));
check("quota di Zio Playwright elencata in attesa", boardText.includes("Zio Playwright") && boardText.includes("In attesa"));
const statusToast = await clickExpectToast('[data-pool-status][data-next="RECEIVED"]');
check("segnata come ricevuta", statusToast.includes("ricevuta"));
await page.waitForTimeout(600);
const poolCsv = await page.evaluate(async (b) => { const r = await fetch(b + "/api/export/gift-pool.csv", { headers: { "X-Admin-Token": localStorage.getItem("np.adminToken") } }); return { ok: r.ok, body: await r.text() }; }, BASE);
check("export CSV quote uniche", poolCsv.ok && poolCsv.body.replace(/^\ufeff/, "").startsWith("Data;Donatore;Contatto;Metodo") && poolCsv.body.includes("TOTALE;"));
check("la quota ricevuta non si può più annullare dal donatore", (await page.$("[data-pool-del]")) === null);
await shot("06b-cassa");

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

console.log("11) Assistente vocale (backend con ASSISTANT_FAKE)");
await page.waitForSelector("#assistant-fab", { timeout: 8000 });
check("pulsante assistente presente", (await page.$("#assistant-fab")) !== null);
await page.click("#assistant-fab");
await page.waitForSelector("#as-chat");
check("solo Fedele selezionabile all'inizio", (await Promise.all((await page.$$("[data-avatar]")).map((c) => c.textContent()))).join(",") === "Fedele Luisi");
await page.fill("#as-text", "Quando è la festa?");
await page.click("#as-send");
await page.waitForSelector(".as-msg.assistant", { timeout: 15000 });
check("risposta scritta dell'avatar", (await page.textContent(".as-msg.assistant .as-bubble")).includes("13 novembre"));
check("audio della risposta caricato", await page.evaluate(() => { const a = document.getElementById("assistant-audio"); return !!a && a.src.startsWith("data:audio/"); }));
await page.fill("#as-text", "Quanti posti ci sono sulla navetta?");
await page.click("#as-send");
await page.waitForFunction(() => document.querySelectorAll(".as-msg.assistant").length >= 2, null, { timeout: 15000 });
check("azione eseguita mostrata (posti navetta)", (await page.textContent("#as-chat")).includes("posti liberi"));
// registrazione con il microfono finto: tieni premuto ~1 s e rilascia
const mic = await page.$("#as-mic");
check("pulsante microfono disponibile", mic !== null);
if (mic) {
  const box = await mic.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(1200);
  await page.mouse.up();
  await page.waitForFunction(() => document.querySelectorAll(".as-msg.assistant").length >= 3, null, { timeout: 20000 });
  check("registrazione trascritta dal server", (await page.textContent("#as-chat")).includes("Trascrizione simulata"));
}
await shot("07-assistente");
await page.fill("#as-text", "Apri la sezione navetta");
await page.click("#as-send");
await page.waitForTimeout(1500);
check("apertura sezione via assistente", (await page.$(".tab.active[data-tab=\"bus\"]")) !== null && (await page.$(".modal-bg")) === null);

console.log("11b) Pannello organizzatori: campione vocale e abilitazione di un avatar");
await tab("program");
await page.click("#settings");
await page.waitForSelector("#s-assistant");
await page.click("#s-assistant");
await page.waitForSelector(".as-admin");
check("9 avatar nel pannello", (await page.$$(".as-admin")).length === 9);
await page.setInputFiles('.as-admin[data-id="carlone"] [data-file]', { name: "sebastiano.wav", mimeType: "audio/wav", buffer: Buffer.concat([Buffer.from("RIFF"), Buffer.alloc(4000)]) });
await page.waitForFunction(() => (document.querySelector('.as-admin[data-id="carlone"] .as-admin-status')?.textContent || "").includes("clonata"), null, { timeout: 15000 });
check("voce clonata dal campione", (await page.textContent('.as-admin[data-id="carlone"] .as-admin-status')).includes("clonata"));
await page.check('.as-admin[data-id="carlone"] [data-enabled]');
await page.fill('.as-admin[data-id="carlone"] [data-persona]', "Sei Sebastiano, pacato e preciso.");
check("avatar salvato e pronto", (await clickExpectToast('.as-admin[data-id="carlone"] [data-save]')).includes("pronto"));
await page.click(".modal .close");
await page.click("#assistant-fab");
await page.waitForSelector("#as-chat");
check("Sebastiano ora selezionabile", (await Promise.all((await page.$$("[data-avatar]")).map((c) => c.textContent()))).join(",") === "Fedele Luisi,Sebastiano Carlone");
await page.click('[data-avatar="carlone"]');
await page.fill("#as-text", "Ciao!");
await page.click("#as-send");
await page.waitForSelector(".as-msg.assistant", { timeout: 15000 });
check("risposta con il nuovo avatar", (await page.textContent(".as-msg.assistant .as-bubble")).includes("Hai detto"));
await page.click(".modal .close");
await shot("07b-assistente-admin");

check("nessun errore JavaScript in console", consoleErrors.length === 0, consoleErrors.join(" | "));

await browser.close();
console.log(`\n${results.filter((r) => r.ok).length}/${results.length} controlli superati`);
if (failures) { console.log("FALLITI:", results.filter((r) => !r.ok).map((r) => r.name).join("; ")); process.exit(1); }
