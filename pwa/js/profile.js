// Profilo personale dell'invitato (backend: /api/profile, link via email).
// - il link https://<app>/#profilo=<token> salva il token sul dispositivo e apre il profilo;
// - senza token: modulo "inserisci la tua email" (e il nome se non sei in lista);
// - con token: RSVP, contatti, accompagnatori con menu, consenso; salvando arriva l'email di conferma.
// - su iPhone l'app aggiunta alla schermata Home non condivide la memoria con Safari, dove si apre il link
//   dell'email: per entrare dall'app installata si digita il codice a 6 cifre che arriva insieme al link.
import { api, getGuestToken, setGuestToken } from "./api.js";
import { Repo, BUS_SCHEDULE, reconnect } from "./data.js";
import { toast, openModal, render } from "./app.js";
import { isIos, isInstalledPwa } from "./notify.js";

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
export const MENU_LABELS = { "": "Da scegliere", STANDARD: "Menu completo", VEGETARIANO: "Vegetariano", VEGANO: "Vegano", SENZA_GLUTINE: "Senza glutine", ALTRO: "Altro (vedi note)" };
const CATEGORIES = ["Familiari", "Colleghi Reparto", "Docenti e Reparto", "Specializzandi 1° e 2° anno", "Amici Università", "Invitato"];
const PRIVACY = "I dati (nome, contatti, menu e allergie, persone al seguito) servono solo a organizzare la festa, sono visibili agli organizzatori e al catering e vengono cancellati dopo l'evento. Puoi modificarli o chiederne la cancellazione in qualsiasi momento.";

export const hasProfile = () => !!getGuestToken();
// Su iPhone, in Safari con il profilo aperto: chi aggiunge l'app alla Home non si ritrova il profilo dentro l'app
export const iosInstallHint = () => isIos() && !isInstalledPwa() && hasProfile();

export async function openProfile() {
  if (Repo.mode !== "server") { toast("Il profilo personale funziona solo con il server della festa"); return; }
  if (!getGuestToken()) { requestLinkDialog(); return; }
  let data;
  try { data = await api.profileGet(); }
  catch (e) {
    if (e.status === 401) { setGuestToken(""); toast(e.message); requestLinkDialog(); return; }
    toast(e.message); return;
  }
  profileDialog(data.guest, data);
}

const fmtDeadline = (iso) => { try { return new Date(iso + "T12:00:00").toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" }); } catch { return iso; } };

export function requestLinkDialog() {
  const installed = isInstalledPwa();
  openModal(`<h3>👤 Il mio profilo</h3>
    <div class="muted" style="margin-bottom:10px;">Con il profilo confermi la partecipazione, indichi chi viene con te e il menu di ciascuno, e ricevi le conferme via email. Niente password: ti mandiamo un link personale e un codice.</div>
    <div class="field"><label>La tua email *</label><input id="pl-email" type="email" inputmode="email" autocomplete="email" placeholder="nome@esempio.it"></div>
    <div class="field"><label>Nome e cognome (solo se non sei ancora in lista)</label><input id="pl-name" autocomplete="name" placeholder="es. Maria Rossi"></div>
    <button class="btn btn-primary" id="pl-send">Inviami il link e il codice</button>
    <div class="muted as-hint" id="pl-msg" style="margin-top:10px;"></div>
    <div class="muted as-hint">${installed ? "Dall'app installata entra con il codice: il link dell'email si aprirebbe nel browser, non qui." : "Hai già ricevuto l'email? Apri il link: il profilo si apre da solo. Oppure usa il codice qui sotto."}</div>
    <div class="card" style="margin-top:12px;" id="pl-code-card">
      <b style="font-size:13px;">🔢 Entra con il codice</b>
      <div class="muted" style="margin:4px 0 8px;">Il codice a 6 cifre è nell'email insieme al link (vale 15 minuti). Serve soprattutto a chi ha aggiunto l'app alla schermata Home dell'iPhone.</div>
      <div class="field"><label>Codice ricevuto</label><input id="pl-code" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9 ]*" placeholder="123456"></div>
      <button class="btn btn-gold" id="pl-verify">Entra</button>
    </div>`,
    (bg, close) => {
      const email = () => bg.querySelector("#pl-email").value.trim();
      bg.querySelector("#pl-send").onclick = async () => {
        const fullName = bg.querySelector("#pl-name").value.trim();
        if (!email().includes("@")) { toast("Inserisci un'email valida"); return; }
        const btn = bg.querySelector("#pl-send"); btn.disabled = true;
        try {
          const r = await api.profileRequestLink({ email: email(), fullName });
          bg.querySelector("#pl-msg").textContent = r.message;
          if (r.queued) { toast("Email inviata: controlla la posta"); bg.querySelector("#pl-code").focus(); }
        } catch (e) { toast(e.message); }
        finally { btn.disabled = false; }
      };
      bg.querySelector("#pl-verify").onclick = async () => {
        const code = bg.querySelector("#pl-code").value.replace(/\D/g, "");
        if (!email().includes("@")) { toast("Scrivi prima la tua email"); return; }
        if (code.length !== 6) { toast("Il codice ha 6 cifre"); return; }
        const btn = bg.querySelector("#pl-verify"); btn.disabled = true;
        try {
          const r = await api.profileVerifyCode({ email: email(), code });
          setGuestToken(r.token);
          close();
          await reconnect(); // con il token il server manda anche il proprio record: si ricarica tutto
          await render();
          toast("Profilo aperto su questo dispositivo ✓");
          openProfile();
        } catch (e) { toast(e.message); }
        finally { btn.disabled = false; }
      };
    });
}

function companionRow(c = {}, i = 0) {
  return `<div class="pf-comp" data-comp="${i}">
    <input data-cname placeholder="Nome accompagnatore" value="${esc(c.name || "")}">
    <select data-cmenu>${Object.entries(MENU_LABELS).map(([k, v]) => `<option value="${k}" ${k === (c.menu || "") ? "selected" : ""}>${v}</option>`).join("")}</select>
    <input data-cnotes placeholder="Allergie/note" value="${esc(c.notes || "")}">
    <button class="icon-btn" data-cdel title="Rimuovi">🗑️</button>
  </div>`;
}

function busSection(data) {
  const b = data.busBooking, bus = data.bus || {}, dl = data.deadlines || {};
  const stops = BUS_SCHEDULE.pickupStops && BUS_SCHEDULE.pickupStops.length ? BUS_SCHEDULE.pickupStops : ["Policlinico di Bari"];
  const status = b ? (b.status === "WAITLIST" ? `<span class="pill pill-pend">in lista d'attesa</span>` : `<span class="pill pill-conf">confermata</span>`) : "";
  const u = !b && data.unclaimedBooking;
  return `<div class="pf-bus" id="pf-bus">
    <b>🚌 Navetta</b> ${status}
    ${u ? `<div class="card" id="pb-unclaimed" style="margin:6px 0;"><b style="font-size:13px;">C'è una prenotazione a tuo nome</b>
      <div class="muted" style="margin:4px 0 8px;">${esc(u.passengerName)}: ${u.seatsCount} ${u.seatsCount === 1 ? "posto" : "posti"}${u.pickupStop ? ` da ${esc(u.pickupStop)}` : ""}, codice <b>${esc(u.code)}</b>, fatta senza profilo. È la tua? Collegandola ricevi la conferma via email e non ne crei una seconda.</div>
      <button class="btn btn-gold" id="pb-claim">Sì, è la mia</button></div>` : ""}
    ${b ? `<div class="muted">${b.seatsCount} ${b.seatsCount === 1 ? "posto" : "posti"} · codice <b>${esc(b.code)}</b>${b.pickupStop ? ` · da ${esc(b.pickupStop)}` : ""}${b.checkedInAt ? " · ✓ salito" : ""}</div>` : `<div class="muted">Posti liberi: ${bus.availableSeats ?? "-"} su ${bus.maxSeats ?? "-"}${bus.waitlistSeats ? ` · ${bus.waitlistSeats} in lista d'attesa` : ""}</div>`}
    ${dl.busLocked ? `<div class="muted">Il termine per prenotare o modificare la navetta è passato: per cambiamenti contatta gli organizzatori.</div>` : `
    <div class="as-row" style="margin-top:6px;"><div class="field" style="flex:0 0 90px;"><label>Posti</label><input id="pb-seats" type="number" min="1" max="40" inputmode="numeric" value="${b ? b.seatsCount : ""}" placeholder="auto"></div>
      <div class="field" style="flex:1;"><label>Fermata di salita</label><select id="pb-stop">${stops.map((st) => `<option ${b && b.pickupStop === st ? "selected" : ""}>${esc(st)}</option>`).join("")}</select></div></div>
    <label class="as-check"><input type="checkbox" id="pb-return" ${!b || b.returnTripWanted ? "checked" : ""}> Mi serve anche il ritorno notturno</label>
    <div class="btn-row"><button class="btn btn-primary" id="pb-save">${b ? "Aggiorna navetta" : "Prenota la navetta"}</button>${b ? `<button class="btn btn-ghost" id="pb-cancel">Annulla prenotazione</button>` : ""}</div>
    ${dl.busDeadline ? `<div class="muted as-hint">Modificabile fino a ${fmtDeadline(dl.busDeadline)}. Posti vuoti = le persone del profilo. Ricevi la conferma via email con il codice e l'evento per il calendario.</div>` : ""}`}
  </div>`;
}

function profileDialog(g, data = {}) {
  const firstTime = !g.consentAt;
  const dl = data.deadlines || {};
  openModal(`<h3>👤 Il mio profilo</h3>
    <div class="muted" style="margin-bottom:10px;">Ciao ${esc(g.fullName)}! Qui trovi la tua partecipazione, chi viene con te e il menu. Salvando ricevi un'email di conferma.</div>
    <div class="field"><label>Partecipazione</label>
      <div class="chips" id="pf-status">${[["CONFIRMED", "✅ Ci sarò"], ["PENDING", "🤔 Non so ancora"], ["DECLINED", "❌ Non posso"]].map(([k, v]) => `<button class="chip ${g.rsvpStatus === k ? "active" : ""}" data-st="${k}">${v}</button>`).join("")}</div></div>
    <div class="field"><label>Nome e cognome *</label><input id="pf-name" value="${esc(g.fullName)}"></div>
    <div class="field"><label>Chi sei per i festeggiati</label><select id="pf-cat">${CATEGORIES.map((c) => `<option ${g.category === c ? "selected" : ""}>${c}</option>`).join("")}${CATEGORIES.includes(g.category) ? "" : `<option selected>${esc(g.category)}</option>`}</select></div>
    <div class="as-row"><div class="field" style="flex:1;"><label>Email</label><input id="pf-email" type="email" value="${esc(g.email || "")}"></div><div class="field" style="flex:1;"><label>Telefono</label><input id="pf-phone" type="tel" value="${esc(g.phone || "")}"></div></div>
    ${dl.menuLocked ? `<div class="muted" style="margin-bottom:8px;">⏰ Il termine per cambiare partecipazione, menu e accompagnatori è passato (${fmtDeadline(dl.menuDeadline)}): per modifiche contatta gli organizzatori. Puoi ancora aggiornare email e telefono.</div>` : dl.menuDeadline ? `<div class="muted as-hint" style="margin-bottom:8px;">Menu e accompagnatori modificabili fino a ${fmtDeadline(dl.menuDeadline)}.</div>` : ""}
    <div id="pf-food">
      <div class="field"><label>Il tuo menu</label><select id="pf-menu">${Object.entries(MENU_LABELS).map(([k, v]) => `<option value="${k}" ${k === (g.menu || "") ? "selected" : ""}>${v}</option>`).join("")}</select></div>
      <div class="field"><label>Allergie o note per la cucina</label><input id="pf-diet" value="${esc(g.dietaryNotes || "")}" placeholder="es. allergia alle noci"></div>
      <div class="field"><label>Chi viene con te</label><div id="pf-comps">${(g.companions || []).map(companionRow).join("")}</div>
        <button class="btn btn-ghost" id="pf-add" style="margin-top:6px;">＋ Aggiungi una persona</button></div>
      <div class="muted" id="pf-count" style="margin-bottom:8px;"></div>
    </div>
    ${g.consentAt && g.rsvpStatus !== "DECLINED" && Repo.mode === "server" ? busSection(data) : ""}
    <label class="as-check" style="align-items:flex-start;"><input type="checkbox" id="pf-consent" ${g.consentAt ? "checked disabled" : ""}> <span>${firstTime ? "Acconsento al trattamento dei dati. " : "Consenso dato. "}<span class="muted">${PRIVACY}</span></span></label>
    <div class="btn-row"><button class="btn btn-primary" id="pf-save">Salva e ricevi conferma</button><button class="btn btn-ghost" id="pf-logout" title="Il link ricevuto smette di valere">Esci</button></div>`,
    (bg, close) => {
      const $ = (sel) => bg.querySelector(sel);
      let status = g.rsvpStatus;
      const count = () => { const n = [...bg.querySelectorAll("[data-cname]")].filter((i) => i.value.trim()).length; $("#pf-count").textContent = status === "DECLINED" ? "" : `${1 + n} ${1 + n === 1 ? "persona" : "persone"} in totale`; };
      const paintStatus = () => { $("#pf-food").style.display = status === "DECLINED" ? "none" : ""; count(); };
      bg.querySelectorAll("#pf-status .chip").forEach((c) => c.onclick = () => { bg.querySelectorAll("#pf-status .chip").forEach((x) => x.classList.remove("active")); c.classList.add("active"); status = c.dataset.st; paintStatus(); });
      const wireComps = () => {
        bg.querySelectorAll("[data-cdel]").forEach((b) => b.onclick = () => { b.closest(".pf-comp").remove(); count(); });
        bg.querySelectorAll("[data-cname]").forEach((i) => i.oninput = count);
      };
      $("#pf-add").onclick = () => { $("#pf-comps").insertAdjacentHTML("beforeend", companionRow({}, bg.querySelectorAll(".pf-comp").length)); wireComps(); count(); };
      wireComps(); paintStatus();
      $("#pf-save").onclick = async () => {
        const body = {
          fullName: $("#pf-name").value.trim(), category: $("#pf-cat").value, rsvpStatus: status,
          email: $("#pf-email").value.trim(), phone: $("#pf-phone").value.trim(), menu: $("#pf-menu").value, dietaryNotes: $("#pf-diet").value.trim(),
          companions: [...bg.querySelectorAll(".pf-comp")].map((r) => ({ name: r.querySelector("[data-cname]").value.trim(), menu: r.querySelector("[data-cmenu]").value, notes: r.querySelector("[data-cnotes]").value.trim() })).filter((c) => c.name),
          consent: $("#pf-consent").checked,
        };
        if (!body.fullName) { toast("Inserisci il tuo nome"); return; }
        if (!body.consent) { toast("Serve il consenso al trattamento dei dati"); return; }
        $("#pf-save").disabled = true;
        try {
          const r = await api.profilePut(body);
          const mail = r.mail;
          toast(mail && (mail.status === "DELIVERED" || mail.status === "QUEUED") ? "Profilo salvato: ti arriva l'email di conferma ✓" : "Profilo salvato ✓");
          close();
          await Repo.pollState(); // la versione dati è cambiata: ricarica lo snapshot (lista invitati, riepilogo)
          render();
        } catch (e) { toast(e.message); }
        finally { const b = $("#pf-save"); if (b) b.disabled = false; }
      };
      const pbClaim = $("#pb-claim");
      if (pbClaim) pbClaim.onclick = async () => {
        pbClaim.disabled = true;
        try {
          const r = await api.profileBusClaim(data.unclaimedBooking.id);
          toast("Prenotazione collegata al tuo profilo ✓ (conferma via email)");
          close(); await Repo.pollState(); render(); profileDialog(r.guest, r);
        } catch (e) { toast(e.message); pbClaim.disabled = false; }
      };
      const pbSave = $("#pb-save");
      if (pbSave) pbSave.onclick = async () => {
        const seats = Number($("#pb-seats").value) || null;
        pbSave.disabled = true;
        try {
          const r = await api.profileBusPut({ seatsCount: seats, pickupStop: $("#pb-stop").value, returnTripWanted: $("#pb-return").checked, contactPhone: $("#pf-phone").value.trim() });
          const b = r.busBooking;
          toast(b && b.status === "WAITLIST" ? "Posti esauriti: sei in lista d'attesa, ti avvisiamo via email" : "Navetta prenotata ✓ (conferma via email)");
          close(); await Repo.pollState(); render(); profileDialog(r.guest, r);
        } catch (e) { toast(e.message); pbSave.disabled = false; }
      };
      const pbCancel = $("#pb-cancel");
      if (pbCancel) pbCancel.onclick = async () => {
        if (!confirm("Annullare la prenotazione della navetta?")) return;
        try { const r = await api.profileBusDelete(); toast("Prenotazione annullata"); close(); await Repo.pollState(); render(); profileDialog(r.guest, r); }
        catch (e) { toast(e.message); }
      };
      $("#pf-logout").onclick = async () => {
        if (!confirm("Uscire dal profilo su questo dispositivo? Il link ricevuto non varrà più: potrai chiederne uno nuovo con la tua email.")) return;
        try { await api.profileLogout(); } catch { /* token già non valido */ }
        setGuestToken(""); close(); await Repo.pollState(); render(); toast("Sei uscito dal profilo");
      };
    });
}

// Pannello organizzatori: stato delle email (n8n), outbox con reinvio, email di prova.
export async function openMailAdmin() {
  let st, box;
  try { [st, box] = await Promise.all([api.mailStatus(), api.mailOutbox(50)]); } catch (e) { toast(e.message); return; }
  const pill = (s) => `<span class="pill ${s === "DELIVERED" ? "pill-conf" : s === "FAILED" ? "pill-decl" : "pill-pend"}">${s === "DELIVERED" ? "consegnata" : s === "FAILED" ? "fallita" : s === "SKIPPED" ? "non inviata" : "in coda"}</span>`;
  openModal(`<h3>✉️ Email tramite n8n</h3>
    <div class="card" style="margin-bottom:10px;"><b style="font-size:13px;">Stato</b>
      <div class="muted">${st.enabled ? `Collegato: le email passano dal webhook n8n (${esc(st.webhook.url || "")}).` : "Non attivo: imposta N8N_WEBHOOK_URL nel .env del server e importa il workflow aggiornato. Nel frattempo i link personali si copiano dalla lista invitati."}</div>
      <div class="muted">${Object.entries(st.counts || {}).map(([k, v]) => `${v} ${k.toLowerCase()}`).join(" · ") || "nessuna email ancora"}</div>
      <div class="as-row" style="margin-top:8px;"><input id="ml-to" type="email" placeholder="Email di prova a…"><button class="btn btn-ghost" id="ml-test">Invia prova</button></div>
      <div class="btn-row" style="margin-top:8px;"><button class="btn btn-ghost" id="ml-remind-menu">🍽️ Promemoria menu mancante</button><button class="btn btn-ghost" id="ml-remind-bus">🚌 Promemoria navetta (domani)</button></div>
      <div class="muted as-hint">I promemoria si possono anche programmare da n8n (POST /api/mail/reminders/menu e /bus con il token automazioni).</div></div>
    <b style="font-size:13px;">Ultime email</b>
    ${box.length ? box.map((m) => `<div class="row"><div class="grow"><div class="name" style="font-size:13px;">${esc(m.to)} ${pill(m.status)}</div><div class="cat">${esc(st.kinds[m.kind] || m.kind)} · ${esc(m.subject)} · ${new Date(m.createdAt).toLocaleString("it-IT", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })}${m.error ? ` · <span style="color:var(--error)">${esc(m.error)}</span>` : ""}</div></div>${m.status !== "DELIVERED" ? `<button class="btn btn-ghost" style="width:auto;padding:6px 10px;font-size:12px;" data-resend="${m.id}">↻</button>` : ""}</div>`).join("") : `<div class="muted">Nessuna email.</div>`}`,
    (bg, close) => {
      bg.querySelector("#ml-test").onclick = async () => {
        const to = bg.querySelector("#ml-to").value.trim(); if (!to.includes("@")) { toast("Inserisci un'email"); return; }
        try { const r = await api.mailTest(to); toast(r.status === "DELIVERED" || r.status === "QUEUED" ? "Email di prova inviata a n8n" : `Non inviata: ${r.error}`); close(); openMailAdmin(); } catch (e) { toast(e.message); }
      };
      bg.querySelector("#ml-remind-menu").onclick = async () => {
        if (!confirm("Mandare il promemoria a chi non ha ancora scelto il menu?")) return;
        try { const r = await api.mailRemindMenu(); toast(r.skipped ? `Nessun invio: ${r.skipped}` : `Promemoria menu: ${r.sent} email`); close(); openMailAdmin(); } catch (e) { toast(e.message); }
      };
      bg.querySelector("#ml-remind-bus").onclick = async () => {
        if (!confirm("Mandare il promemoria della navetta a tutte le prenotazioni confermate con email?")) return;
        try { const r = await api.mailRemindBus(); toast(`Promemoria navetta: ${r.sent} email`); close(); openMailAdmin(); } catch (e) { toast(e.message); }
      };
      bg.querySelectorAll("[data-resend]").forEach((b) => b.onclick = async () => {
        try { const r = await api.mailResend(Number(b.dataset.resend)); toast(r.status === "DELIVERED" ? "Consegnata ✓" : `Stato: ${r.status}`); close(); openMailAdmin(); } catch (e) { toast(e.message); }
      });
    });
}
