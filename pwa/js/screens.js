// Schermate PWA - mirror delle 5 schermate Compose dell'app Android
import { Repo, Status, RsvpStatus, MAX_BUS_SEATS, GRADUATES, MAP_POINTS, PROGRAM, BUS_SCHEDULE, EVENT, GIFT_COLLECTOR, splitEqually, reconnect } from "./data.js";
import { downloadIcs } from "./schedule.js";
import { toast, openModal, fmtTime, goto, render, updateStatusBar } from "./app.js";
import { api, getApiBase, setApiBase, getAdminToken, setAdminToken, isAdmin, getTreasurerToken, setTreasurerToken, isTreasurer, getClientId, downloadCsv } from "./api.js";
import { showLocalNotification, subscribeToPush, unsubscribeFromPush, isPushSubscribed, supportsPush, supportsNotifications, isIos, isInstalledPwa } from "./notify.js";

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

// Esegue un'azione sul repository mostrando l'eventuale errore del server (403, 409, offline...)
async function tryAction(fn, okMsg) {
  try {
    await fn();
    if (okMsg) toast(okMsg);
    return true;
  } catch (e) {
    toast(e.message || "Operazione non riuscita");
    return false;
  }
}

// ============================ PROGRAMMA ============================
export async function program(el) {
  const wishes = await Repo.allWishes();
  const notifications = await Repo.allNotifications();
  const unread = notifications.filter((n) => !n.isRead).length;
  const ticker = wishes.length ? wishes[Math.floor(Math.random() * wishes.length)] : null;
  const P = PROGRAM;
  const serverMode = Repo.mode === "server";
  const pushOn = isPushSubscribed() && supportsNotifications() && Notification.permission === "granted";

  el.innerHTML = `
    <div class="hero">
      <div class="top">
        <div class="badge">🎓 ${esc(P.badge)}</div>
        <div style="display:flex;gap:6px;">
          <button class="icon-round" id="ntf-show" aria-label="Notifiche">🔔${unread > 0 ? `<span class="dot">${unread}</span>` : ""}</button>
          <button class="icon-round" id="settings" aria-label="Impostazioni">⚙️</button>
        </div>
      </div>
      <h1>${esc(P.title)}</h1>
      <div class="sub">${esc(P.subtitle)}</div>
      <div class="graduates">${GRADUATES.map(esc).join(" • ")}</div>
      <div class="meta">📅 ${esc(P.dateLabel)}</div>
      <div class="meta">📍 ${esc(P.locationLabel)}</div>
      <a class="btn btn-ghost cal-btn" id="cal-add" href="${serverMode ? esc(getApiBase() + "/api/event/calendar.ics") : "#"}" ${serverMode ? 'target="_blank" rel="noopener"' : ""}>📅 Aggiungi al calendario</a>
    </div>

    ${ticker ? `<div class="ticker" id="ticker"><span class="emoji">${esc(ticker.emojiBadge || "🎓")}</span>
      <span class="txt"><b>${esc(ticker.authorName)}</b> → ${esc(ticker.targetGraduate)}<br>“${esc(ticker.message)}”</span>
      <button class="heart" data-heart="${ticker.id}">❤️ ${ticker.heartCount}</button></div>` : ""}

    <div class="card push-card">
      <b>${serverMode ? "🔔 Notifiche in tempo reale" : "🔔 Notifiche"}</b>
      <div class="muted">${pushCardText(serverMode, pushOn)}</div>
      <div class="btn-row" style="margin-top:10px;">
        ${serverMode && supportsPush() ? (pushOn
          ? `<button class="btn btn-ghost" id="push-off">Disattiva su questo dispositivo</button>`
          : `<button class="btn btn-primary" id="push-on">Attiva notifiche push</button>`) : ""}
        ${isAdmin() || !serverMode ? `<button class="btn btn-gold" id="ntf-send">Invia notifica</button>` : ""}
      </div>
      ${serverMode && !isAdmin() ? `<div class="muted" style="margin-top:8px;">Sei un organizzatore? Inserisci il token in ⚙️ Impostazioni per inviare notifiche a tutti.</div>` : ""}
    </div>

    <h2 class="section">Programma dell'Evento</h2>
    <div class="card">
      ${(P.timeline ?? []).map((t, i) => timelineItem(t.time, timelineIcon(i), t.title, t.location, t.details, !!t.more, i === 2)).join("")}
    </div>

    <h2 class="section">Mappa Interattiva Punti di Ritrovo</h2>
    <div class="card">
      <div class="chips" id="mp-chips">
        ${MAP_POINTS.map((p, i) => `<button class="chip ${i === 0 ? "active" : ""}" data-mp="${esc(p.id)}">${mapChipLabel(p)}</button>`).join("")}
      </div>
      <div id="map"></div>
      <div id="mp-detail" style="margin-top:10px;"></div>
    </div>
  `;

  const hb = el.querySelector("#ticker .heart");
  if (hb) hb.onclick = () => tryAction(() => Repo.heartWish(Number(hb.dataset.heart)), "Cuore inviato ❤️");

  el.querySelector("#ntf-show").onclick = async () => {
    await Repo.markAllRead();
    showNotifications(await Repo.allNotifications());
  };
  el.querySelector("#settings").onclick = () => settingsDialog();
  const calBtn = el.querySelector("#cal-add");
  if (calBtn && !serverMode) calBtn.onclick = (e) => { e.preventDefault(); downloadIcs(EVENT); toast("File calendario scaricato: aprilo per aggiungere seduta e festa 📅"); };
  const sendBtn = el.querySelector("#ntf-send");
  if (sendBtn) sendBtn.onclick = () => sendPushDialog();
  const onBtn = el.querySelector("#push-on");
  if (onBtn) onBtn.onclick = async () => {
    onBtn.disabled = true;
    const ok = await tryAction(() => subscribeToPush(), "Notifiche push attivate ✓");
    onBtn.disabled = false;
    if (ok) render();
  };
  const offBtn = el.querySelector("#push-off");
  if (offBtn) offBtn.onclick = async () => { await tryAction(() => unsubscribeFromPush(), "Notifiche push disattivate"); render(); };
  const busLink = el.querySelector("[data-goto-bus]");
  if (busLink) busLink.onclick = (e) => { e.preventDefault(); goto("bus"); };

  if (MAP_POINTS.length) {
    initMap(el, 0);
    el.querySelectorAll("#mp-chips .chip").forEach((c) => {
      c.onclick = () => {
        el.querySelectorAll("#mp-chips .chip").forEach((x) => x.classList.remove("active"));
        c.classList.add("active");
        initMap(el, MAP_POINTS.findIndex((p) => p.id === c.dataset.mp));
      };
    });
  }
}

function pushCardText(serverMode, pushOn) {
  if (!serverMode) return "In modalità demo le notifiche restano su questo dispositivo.";
  if (!supportsPush()) {
    if (isIos() && !isInstalledPwa()) return "Su iPhone le notifiche funzionano solo dopo aver aggiunto l'app alla schermata Home (Condividi → Aggiungi alla schermata Home).";
    return "Questo browser non supporta le notifiche push.";
  }
  return pushOn
    ? "Ricevi gli aggiornamenti degli organizzatori (seduta, navetta, festa) anche ad app chiusa."
    : "Attiva le notifiche per ricevere orari, partenza navetta e aggiornamenti della festa in tempo reale.";
}

const TIMELINE_ICONS = ["🎓", "🎉", "🚌", "🌃", "🎂"];
function timelineIcon(i) { return TIMELINE_ICONS[i] ?? "🎓"; }

function timelineItem(time, icon, title, loc, det, more, busAction) {
  return `<div class="timeline">
    <div class="col-time"><div class="time">${esc(time)}</div><div class="dot">${icon}</div>${more ? '<div class="line"></div>' : ""}</div>
    <div class="body"><div class="t-title">${esc(title)}</div><div class="t-loc">📍 ${esc(loc)}</div><div class="t-det">${esc(det)}</div>
      ${busAction ? `<a href="#" data-goto-bus style="display:inline-block;margin-top:6px;font-size:12px;font-weight:700;color:var(--primary-2);">Prenota Posto →</a>` : ""}
    </div>
  </div>`;
}

function mapChipLabel(p) {
  return p.id === "seduta" ? "🎓 Seduta" : p.id === "bus" ? "🚌 Fermata Bus" : "🎉 Festa";
}

let _map = null;
function initMap(el, idx) {
  const p = MAP_POINTS[idx] || MAP_POINTS[0];
  const det = el.querySelector("#mp-detail");
  const gmaps = `https://www.google.com/maps/search/?api=1&query=${p.latitude},${p.longitude}`;
  const amaps = `https://maps.apple.com/?q=${encodeURIComponent(p.address)}&ll=${p.latitude},${p.longitude}`;
  det.innerHTML = `<b style="font-size:14px;">${esc(p.title)}</b><div class="muted">${esc(p.subtitle)} · ${esc(p.timeLabel)}</div>
    <div class="muted" style="margin-top:4px;">📍 ${esc(p.address)}</div><div class="muted" style="margin-top:4px;">${esc(p.description)}</div>
    <div class="btn-row" style="margin-top:8px;">
      <a class="btn btn-primary" style="text-decoration:none;" target="_blank" rel="noopener" href="${isIos() ? amaps : gmaps}">🧭 Apri nel navigatore</a>
    </div>`;
  if (_map) { _map.remove(); _map = null; }
  const mapEl = el.querySelector("#map");
  if (typeof L === "undefined" || !mapEl) return;
  _map = L.map(mapEl).setView([p.latitude, p.longitude], 15);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", { attribution: "© OpenStreetMap", maxZoom: 19 }).addTo(_map);
  const colorMap = { GRADUATION: "#005FB0", BUS: "#0284C7", PARTY: "#D97706" };
  MAP_POINTS.forEach((q, i) => {
    const color = colorMap[q.iconType] || "#005FB0";
    const m = L.circleMarker([q.latitude, q.longitude], { radius: i === idx ? 12 : 8, color, fillColor: color, fillOpacity: 0.9, weight: i === idx ? 3 : 1 }).addTo(_map);
    m.bindPopup(`<div class="mp-title">${esc(q.title)}</div><div class="mp-sub">${esc(q.timeLabel)}</div>`);
  });
}

function showNotifications(notifications) {
  const sorted = [...notifications].sort((a, b) => b.timestamp - a.timestamp);
  const pill = (c) => c === "Seduta" ? "pill-conf" : c === "Navetta" ? "pill-pend" : "pill-decl";
  openModal(`<h3>🔔 Aggiornamenti & Notifiche</h3>${sorted.length === 0 ? '<div class="empty">Nessuna notifica inviata finora.</div>' : sorted.map((n) => `
    <div class="card" style="margin-bottom:10px;">
      <div style="display:flex;justify-content:space-between;gap:8px;"><b style="font-size:13px;">${esc(n.title)}</b><span class="pill ${pill(n.category)}">${esc(n.category)}</span></div>
      <div class="muted" style="margin-top:6px;">${esc(n.message)}</div>
      <div class="muted" style="margin-top:6px;font-size:11px;">${fmtTime(n.timestamp)}</div>
    </div>`).join("")}`);
  render();
}

function sendPushDialog() {
  const templates = [
    ["🎓 Seduta del 9 Novembre!", 'La seduta di proclamazione è in corso all\'Aula Magna "G. De Benedictis" del Policlinico di Bari.', "Seduta"],
    ["🚌 Partenza Navetta Imminente", "L'autobus è in sosta al Piazzale Principale del Policlinico di Bari. Partenza tra 15 minuti!", "Navetta"],
    ["🥂 Benvenuti alla Festa!", "Venerdì 13 novembre al Giardino dei Tempi: aperitivo di benvenuto aperto! Vi aspettiamo per il primo brindisi insieme.", "Festa"],
    ["🎂 Taglio della Torta & Dj Set", "Tutti attorno alla torta di specializzazione per il momento più atteso della serata!", "Festa"],
    ["📸 Caricate le vostre foto!", "Aprite la sezione Foto dell'app e condividete gli scatti più belli con i neo-specialisti!", "Festa"]
  ];
  const cats = ["Seduta", "Festa", "Navetta", "Organizzazione"];
  const serverMode = Repo.mode === "server";
  openModal(`<h3>📡 Invia Notifica Push</h3>
    <div class="muted" style="margin-bottom:10px;">${serverMode
      ? `Il messaggio viene salvato nella cronologia e inviato in push a tutti i dispositivi iscritti (${Status.pushSubscriptions}).`
      : "In modalità demo la notifica viene mostrata solo su questo dispositivo."}</div>
    <div class="muted" style="margin:6px 0;">Preset rapidi:</div>
    <div class="chips">${templates.map((t, i) => `<button class="chip" data-tpl="${i}">${esc(t[0])}</button>`).join("")}</div>
    <div class="field"><label>Titolo</label><input id="p-title" placeholder="Titolo notifica (es. 🚌 Partenza)"></div>
    <div class="field"><label>Messaggio</label><textarea id="p-body" rows="3" placeholder="Messaggio / dettagli per gli invitati"></textarea></div>
    <div class="field"><label>Categoria</label><select id="p-cat">${cats.map((c) => `<option>${c}</option>`).join("")}</select></div>
    ${serverMode ? `<div class="field"><label>Programma l'invio (opzionale)</label><input id="p-when" type="datetime-local" min="${localDateTimeValue(Date.now())}">
      <div class="muted" style="font-size:11px;margin-top:4px;">Vuoto = invio immediato. Con una data futura la notifica parte da sola all'ora indicata (es. partenza navetta, taglio della torta).</div></div>
    <div id="p-scheduled"></div>` : ""}
    <button class="btn btn-primary" id="p-send" style="margin-top:12px;">Invia Push</button>`,
    (bg, close) => {
      bg.querySelectorAll("[data-tpl]").forEach((c) => c.onclick = () => {
        const t = templates[Number(c.dataset.tpl)];
        bg.querySelector("#p-title").value = t[0];
        bg.querySelector("#p-body").value = t[1];
        bg.querySelector("#p-cat").value = t[2];
      });
      const whenInput = bg.querySelector("#p-when");
      const paintScheduled = async () => {
        const box = bg.querySelector("#p-scheduled");
        if (!box || !isAdmin()) return;
        let list = [];
        try { list = await api.scheduledNotifications(); } catch { return; }
        box.innerHTML = list.length ? `<div class="muted" style="margin:8px 0 4px;">⏳ Programmate (${list.length}):</div>` + list.map((n) => `
          <div class="row" style="align-items:center;gap:8px;" data-sched="${n.id}">
            <div class="grow"><div class="name" style="font-size:13px;">${esc(n.title)}</div><div class="muted" style="font-size:11px;">${esc(fmtDateTime(n.scheduledAt))} · ${esc(n.category)}</div></div>
            <button class="btn btn-ghost" style="width:auto;padding:6px 10px;" data-cancel="${n.id}">Annulla</button></div>`).join("") : "";
        box.querySelectorAll("[data-cancel]").forEach((b) => b.onclick = async () => {
          if (await tryAction(() => api.deleteNotification(Number(b.dataset.cancel)), "Notifica programmata annullata")) paintScheduled();
        });
      };
      paintScheduled();
      bg.querySelector("#p-send").onclick = async () => {
        const title = bg.querySelector("#p-title").value.trim();
        const body = bg.querySelector("#p-body").value.trim();
        const category = bg.querySelector("#p-cat").value;
        if (!title || !body) { toast("Inserisci titolo e messaggio"); return; }
        const sendAt = whenInput && whenInput.value ? new Date(whenInput.value).getTime() : null;
        if (sendAt && sendAt < Date.now() - 60000) { toast("La data di invio è nel passato"); return; }
        const btn = bg.querySelector("#p-send"); btn.disabled = true;
        const payload = { title, message: body, category };
        if (sendAt) payload.sendAt = sendAt;
        const ok = await tryAction(() => Repo.addNotification(payload));
        btn.disabled = false;
        if (!ok) return;
        if (!serverMode) await showLocalNotification(title, body);
        close();
        toast(!serverMode ? "Notifica mostrata ✓" : sendAt ? `Notifica programmata per ${fmtDateTime(sendAt)} ⏳` : "Notifica inviata a tutti ✓");
        render();
      };
    });
}

// "2026-11-13T19:45" per <input type="datetime-local"> nel fuso del dispositivo
function localDateTimeValue(ts) {
  const d = new Date(ts);
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}
function fmtDateTime(ts) {
  return new Date(ts).toLocaleString("it-IT", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function settingsDialog() {
  const serverMode = Repo.mode === "server";
  openModal(`<h3>⚙️ Impostazioni</h3>
    <div class="card" style="margin-bottom:12px;">
      <b style="font-size:13px;">Stato</b>
      <div class="muted">Modalità: <b>${serverMode ? "server condiviso" : "demo locale"}</b>${serverMode ? ` · ${Status.online ? "online" : "offline"} · versione dati ${Status.version}` : ""}</div>
      <div class="muted">Server: <span class="mono">${esc(getApiBase() || "nessuno")}</span></div>
      <div class="muted">ID dispositivo: <span class="mono">${esc(getClientId())}</span></div>
    </div>
    <div class="field"><label>Token organizzatore (X-Admin-Token)</label><input id="s-token" type="password" value="${esc(getAdminToken())}" placeholder="Solo per gli organizzatori"></div>
    <div class="muted" style="margin-bottom:10px;">Con il token puoi inviare notifiche push a tutti e cancellare qualsiasi invitato o prenotazione.</div>
    <div class="field"><label>Token cassiere quote uniche (X-Treasurer-Token)</label><input id="s-treasurer" type="password" value="${esc(getTreasurerToken())}" placeholder="Solo per chi raccoglie le quote uniche"></div>
    <div class="muted" style="margin-bottom:10px;">Con il token cassiere la tab Regali mostra il cruscotto delle quote uniche (chi, quanto, per chi) e il CSV per Excel.</div>
    ${serverMode && isAdmin() ? `<div class="card" style="margin-bottom:12px;"><b style="font-size:13px;">📄 Esporta per ristorante e autista</b>
      <div class="muted" style="margin-bottom:8px;">File CSV (si aprono con Excel) con tutti gli invitati e le prenotazioni della navetta.</div>
      <div class="btn-row"><button class="btn btn-ghost" id="s-exp-guests">Esporta invitati</button><button class="btn btn-ghost" id="s-exp-bus">Esporta navetta</button></div></div>` : ""}
    ${serverMode && isTreasurer() ? `<div class="card" style="margin-bottom:12px;"><b style="font-size:13px;">📒 Esporta quote uniche per il cassiere</b>
      <div class="muted" style="margin-bottom:8px;">CSV con una riga per quota, una colonna per neo-specialista e i totali: si apre con Excel.</div>
      <button class="btn btn-ghost" id="s-exp-pool">Esporta quote uniche</button></div>` : ""}
    <div class="field"><label>URL del server (avanzato)</label><input id="s-api" value="${esc(localStorage.getItem("np.apiBase") || "")}" placeholder="vuoto = stesso dominio della PWA"></div>
    <div class="btn-row"><button class="btn btn-primary" id="s-save">Salva</button><button class="btn btn-ghost" id="s-test">Prova connessione</button></div>`,
    (bg, close) => {
      const expG = bg.querySelector("#s-exp-guests"), expB = bg.querySelector("#s-exp-bus");
      if (expG) expG.onclick = () => tryAction(() => downloadCsv("guests"), "invitati.csv scaricato");
      if (expB) expB.onclick = () => tryAction(() => downloadCsv("bus"), "navetta.csv scaricato");
      const expP = bg.querySelector("#s-exp-pool");
      if (expP) expP.onclick = () => tryAction(() => downloadCsv("gift-pool"), "quote-uniche.csv scaricato");
      bg.querySelector("#s-test").onclick = async () => {
        const prev = getApiBase();
        setApiBase(bg.querySelector("#s-api").value);
        try { const s = await api.state(); toast(`Server OK (versione ${s.version}${s.adminEnabled ? ", token attivo" : ""})`); }
        catch (e) { toast(e.message); }
        finally { setApiBase(localStorage.getItem("np.apiBase") === prev ? prev : bg.querySelector("#s-api").value); }
      };
      bg.querySelector("#s-save").onclick = async () => {
        const changedApi = (bg.querySelector("#s-api").value.trim().replace(/\/+$/, "")) !== (localStorage.getItem("np.apiBase") || "");
        setAdminToken(bg.querySelector("#s-token").value);
        setTreasurerToken(bg.querySelector("#s-treasurer").value);
        setApiBase(bg.querySelector("#s-api").value);
        close();
        if (changedApi || !serverMode) { await reconnect(); updateStatusBar(); }
        toast("Impostazioni salvate");
        render();
      };
    });
}

// ============================ RSVP / INVITATI ============================
export async function rsvp(el) {
  let guests = await Repo.allGuests();
  let filter = null;
  let query = "";

  async function paint() {
    let list = guests;
    if (filter) list = list.filter((g) => g.rsvpStatus === filter);
    if (query.trim()) {
      const q = query.toLowerCase();
      list = list.filter((g) => g.fullName.toLowerCase().includes(q) || g.category.toLowerCase().includes(q));
    }
    const confirmed = guests.filter((g) => g.rsvpStatus === RsvpStatus.CONFIRMED);
    const pending = guests.filter((g) => g.rsvpStatus === RsvpStatus.PENDING);
    const declined = guests.filter((g) => g.rsvpStatus === RsvpStatus.DECLINED);
    const covers = confirmed.reduce((s, g) => s + (g.guestsCount || 0), 0);
    const special = confirmed.filter((g) => g.dietaryNotes && !/nessuna/i.test(g.dietaryNotes)).length;
    el.innerHTML = `
      <h2 class="section">Gestione Invitati & RSVP</h2>
      <div class="muted" style="margin-top:-4px;margin-bottom:12px;">Monitoraggio in tempo reale delle presenze e coperti per la festa</div>
      <div class="btn-row" style="margin-bottom:12px;">
        ${statCard("Coperti Totali", covers, `${confirmed.length} invitati`, "#10B981")}
        ${statCard("In Attesa", pending.length, "da confermare", "#D97706")}
        ${statCard("Menu Speciali", special, "intolleranze/diete", "#0284C7")}
      </div>
      ${cateringSummary(confirmed, pending)}
      <div class="card">
        <input id="g-search" placeholder="🔍 Cerca invitato o categoria..." value="${esc(query)}">
        <div class="chips" style="margin-top:10px;">
          <button class="chip ${!filter ? "active" : ""}" data-f="">Tutti (${guests.length})</button>
          <button class="chip ${filter === RsvpStatus.CONFIRMED ? "active" : ""}" data-f="${RsvpStatus.CONFIRMED}">Confermati (${confirmed.length})</button>
          <button class="chip ${filter === RsvpStatus.PENDING ? "active" : ""}" data-f="${RsvpStatus.PENDING}">In attesa (${pending.length})</button>
          <button class="chip ${filter === RsvpStatus.DECLINED ? "active" : ""}" data-f="${RsvpStatus.DECLINED}">Declinati (${declined.length})</button>
        </div>
      </div>
      <div class="card">
        ${list.length === 0 ? '<div class="empty">Nessun invitato corrisponde ai filtri.</div>' : list.map((g) => `
          <div class="row" style="flex-direction:column;align-items:stretch;" data-guest="${g.id}">
            <div style="display:flex;align-items:center;gap:10px;">
              <div class="grow">
                <div class="name">${esc(g.fullName)}</div>
                <div class="cat">${esc(g.category)}${g.guestsCount > 1 ? ` · ${g.guestsCount} persone` : ""}${g.dietaryNotes ? " · 🍽️ " + esc(g.dietaryNotes) : ""}${g.contactInfo ? " · ☎ " + esc(g.contactInfo) : ""}</div>
              </div>
              <button class="icon-btn" data-del="${g.id}" title="Rimuovi invitato">🗑️</button>
            </div>
            <div class="status-row">
              <button class="status-btn ${g.rsvpStatus === RsvpStatus.CONFIRMED ? "conf" : ""}" data-st="${RsvpStatus.CONFIRMED}">Confermato</button>
              <button class="status-btn ${g.rsvpStatus === RsvpStatus.PENDING ? "pend" : ""}" data-st="${RsvpStatus.PENDING}">In Attesa</button>
              <button class="status-btn ${g.rsvpStatus === RsvpStatus.DECLINED ? "decl" : ""}" data-st="${RsvpStatus.DECLINED}">Declinato</button>
            </div>
          </div>`).join("")}
      </div>
      <button class="fab" id="add-guest">＋ Aggiungi Invitato</button>
    `;
    const search = el.querySelector("#g-search");
    search.oninput = (e) => { query = e.target.value; const pos = e.target.selectionStart; paint(); const s2 = el.querySelector("#g-search"); s2.focus(); s2.setSelectionRange(pos, pos); };
    el.querySelectorAll("[data-f]").forEach((c) => c.onclick = () => { filter = c.dataset.f || null; paint(); });
    el.querySelectorAll("[data-guest]").forEach((row) => {
      const id = Number(row.dataset.guest);
      row.querySelectorAll("[data-st]").forEach((b) => b.onclick = async () => {
        if (await tryAction(() => Repo.updateGuestStatus(id, b.dataset.st))) { guests = await Repo.allGuests(); paint(); }
      });
    });
    el.querySelectorAll("[data-del]").forEach((b) => b.onclick = async () => {
      if (!confirm("Rimuovere questo invitato?")) return;
      if (await tryAction(() => Repo.deleteGuest(Number(b.dataset.del)), "Invitato rimosso")) { guests = await Repo.allGuests(); paint(); }
    });
    el.querySelector("#add-guest").onclick = () => guestDialog(async () => { guests = await Repo.allGuests(); paint(); });
  }
  paint();
}

// Riepilogo per il ristorante: coperti per categoria ed esigenze alimentari con i nomi.
// Stessa logica di GET /api/guests/summary nel backend (calcolata qui per funzionare anche offline/demo).
const NO_DIET = new Set(["", "nessuna", "nessuna restrizione", "no", "-", "niente", "nessuno"]);
function cateringSummary(confirmed, pending) {
  const byCat = new Map();
  for (const g of confirmed) {
    const c = byCat.get(g.category) || { category: g.category, guests: 0, covers: 0 };
    c.guests += 1; c.covers += g.guestsCount || 0; byCat.set(g.category, c);
  }
  const diets = new Map();
  for (const g of confirmed) {
    const note = (g.dietaryNotes || "").trim();
    if (NO_DIET.has(note.toLowerCase())) continue;
    const d = diets.get(note.toLowerCase()) || { note, guests: [], covers: 0 };
    d.guests.push(g.fullName); d.covers += g.guestsCount || 0; diets.set(note.toLowerCase(), d);
  }
  const cats = [...byCat.values()].sort((a, b) => b.covers - a.covers);
  const dts = [...diets.values()].sort((a, b) => b.covers - a.covers);
  const pendingCovers = pending.reduce((s, g) => s + (g.guestsCount || 0), 0);
  return `<details class="card catering" id="catering">
    <summary><b>🍽️ Riepilogo per il catering</b> <span class="muted">· ${confirmed.reduce((s, g) => s + (g.guestsCount || 0), 0)} coperti confermati${pendingCovers ? ` (+${pendingCovers} in attesa)` : ""}</span></summary>
    <div class="muted" style="margin:8px 0 4px;font-size:12px;"><b>Per categoria</b></div>
    ${cats.length ? cats.map((c) => `<div class="row" style="padding:4px 0;"><div class="grow">${esc(c.category)}</div><div>${c.covers} coperti <span class="muted">(${c.guests} inv.)</span></div></div>`).join("") : '<div class="muted">Nessun confermato.</div>'}
    <div class="muted" style="margin:10px 0 4px;font-size:12px;"><b>Esigenze alimentari</b></div>
    ${dts.length ? dts.map((d) => `<div class="row" style="padding:4px 0;flex-direction:column;align-items:stretch;"><div><b>${esc(d.note)}</b> · ${d.covers} ${d.covers === 1 ? "persona" : "persone"}</div><div class="muted" style="font-size:12px;">${d.guests.map(esc).join(", ")}</div></div>`).join("") : '<div class="muted">Nessun menu speciale segnalato.</div>'}
    <div class="muted" style="margin-top:8px;font-size:11px;">Gli organizzatori possono scaricare il CSV completo da ⚙️ Impostazioni.</div>
  </details>`;
}

function statCard(title, value, sub, color) {
  return `<div class="card" style="flex:1;padding:12px;margin:0;"><div class="muted" style="font-size:11px;">${title}</div><div style="font-size:24px;font-weight:800;color:${color};">${value}</div><div class="muted" style="font-size:11px;">${sub}</div></div>`;
}

function guestDialog(onDone) {
  const cats = ["Colleghi Reparto", "Docenti & Medici", "Famigliari", "Amici Università", "Specializzandi"];
  openModal(`<h3>＋ Aggiungi Nuovo Invitato</h3>
    <div class="field"><label>Nome e cognome *</label><input id="ag-name"></div>
    <div class="field"><label>Categoria / Gruppo</label><div class="chips" id="ag-cats">${cats.map((c, i) => `<button class="chip ${i === 0 ? "active" : ""}" data-cat="${esc(c)}">${esc(c)}</button>`).join("")}</div></div>
    <div class="field"><label>N. Persone (coperti, compreso l'invitato)</label><input id="ag-count" type="number" min="1" value="1"></div>
    <div class="field"><label>Esigenze alimentari (Celiaco, Veg...)</label><input id="ag-diet" placeholder="es. Celiaco, Vegetariano, Nessuna"></div>
    <div class="field"><label>Contatto (telefono o email)</label><input id="ag-contact"></div>
    <div class="field"><label>Stato RSVP</label><select id="ag-status"><option value="${RsvpStatus.CONFIRMED}">Confermato</option><option value="${RsvpStatus.PENDING}">In attesa</option><option value="${RsvpStatus.DECLINED}">Declinato</option></select></div>
    <button class="btn btn-primary" id="ag-save" style="margin-top:10px;">Salva Invitato</button>`,
    (bg, close) => {
      let category = cats[0];
      bg.querySelectorAll("#ag-cats .chip").forEach((c) => c.onclick = () => {
        bg.querySelectorAll("#ag-cats .chip").forEach((x) => x.classList.remove("active")); c.classList.add("active"); category = c.dataset.cat;
      });
      bg.querySelector("#ag-save").onclick = async () => {
        const name = bg.querySelector("#ag-name").value.trim();
        if (!name) { toast("Inserisci il nome"); return; }
        const ok = await tryAction(() => Repo.addGuest({
          fullName: name,
          category,
          guestsCount: Math.max(1, Number(bg.querySelector("#ag-count").value) || 1),
          dietaryNotes: bg.querySelector("#ag-diet").value.trim(),
          contactInfo: bg.querySelector("#ag-contact").value.trim(),
          rsvpStatus: bg.querySelector("#ag-status").value
        }), "Invitato aggiunto ✓");
        if (ok) { close(); onDone(); }
      };
    });
}

// ============================ NAVETTA ============================
export async function bus(el) {
  const bookings = await Repo.allBookings();
  const booked = bookings.reduce((s, b) => s + (b.seatsCount || 0), 0);
  const available = Math.max(0, MAX_BUS_SEATS - booked);
  const pct = Math.min(100, (booked / MAX_BUS_SEATS) * 100);
  el.innerHTML = `
    <h2 class="section">Navetta Autobus Riservata</h2>
    <div class="muted" style="margin-top:-4px;margin-bottom:12px;">${esc(BUS_SCHEDULE.subtitle || "")}</div>
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <div><b>🚌 Pullman Gran Turismo</b><div class="muted">Capienza totale: ${MAX_BUS_SEATS} posti</div></div>
        <span class="pill ${available > 10 ? "pill-conf" : "pill-decl"}">${available} liberi</span>
      </div>
      <div class="meter"><span style="width:${pct}%;${pct > 90 ? "background:#EF4444;" : ""}"></span></div>
      <div style="display:flex;justify-content:space-between;" class="muted"><span>Adesioni raccolte: ${booked} posti</span><span>${pct.toFixed(0)}% occupato</span></div>
    </div>
    <div class="card">
      <b style="font-size:14px;">Orari & Fermate Transfer</b>
      ${busTripRow("ANDATA", BUS_SCHEDULE.andata)}
      ${busTripRow("RITORNO", BUS_SCHEDULE.ritorno)}
    </div>
    <h2 class="section">Elenco Passeggeri Prenotati (${bookings.length})</h2>
    <div class="card">
      ${bookings.length === 0 ? '<div class="empty">Nessun passeggero ha ancora prenotato la navetta.</div>' : bookings.map((b) => `
        <div class="row">
          <div style="width:36px;height:36px;border-radius:50%;background:#E0F2FE;color:#0284C7;font-weight:800;display:flex;align-items:center;justify-content:center;flex-shrink:0;">${b.seatsCount}</div>
          <div class="grow">
            <div class="name">${esc(b.passengerName)}</div>
            <div class="cat">Fermata: ${esc(b.pickupStop)}${b.contactPhone ? " · " + esc(b.contactPhone) : ""}</div>
            ${b.returnTripWanted ? `<div class="cat" style="color:#10B981;font-weight:600;">Include corsa di rientro notturna</div>` : ""}
            ${b.notes ? `<div class="cat" style="font-style:italic;">Note: ${esc(b.notes)}</div>` : ""}
          </div>
          <button class="icon-btn" data-cancel="${b.id}" title="Cancella prenotazione">🗑️</button>
        </div>`).join("")}
    </div>
    <button class="fab" id="book-bus">🚌 Aderisci all'Autobus</button>
  `;
  el.querySelectorAll("[data-cancel]").forEach((b) => b.onclick = async () => {
    if (!confirm("Cancellare questa prenotazione?")) return;
    if (await tryAction(() => Repo.deleteBooking(Number(b.dataset.cancel)), "Prenotazione annullata")) bus(el);
  });
  el.querySelector("#book-bus").onclick = () => busDialog(() => bus(el));
}

function busTripRow(label, trip) {
  if (!trip) return "";
  return `<div style="margin-top:12px;border-top:1px solid var(--outline);padding-top:12px;">
    <div style="display:flex;justify-content:space-between;"><b style="font-size:12px;">${label}</b><span class="pill ${label === "ANDATA" ? "pill-conf" : "pill-pend"}">${esc(trip.timeLabel || "")}</span></div>
    <div class="muted" style="margin-top:4px;color:var(--primary);font-weight:600;">${esc(trip.from || "")} ➔ ${esc(trip.to || "")}</div>
    <div class="muted" style="font-size:11px;">${esc(trip.notes || "")}</div>
  </div>`;
}

function busDialog(onDone) {
  const stops = BUS_SCHEDULE.pickupStops && BUS_SCHEDULE.pickupStops.length ? BUS_SCHEDULE.pickupStops : ["Policlinico di Bari"];
  openModal(`<h3>🚌 Prenota Posto Autobus Navetta</h3>
    <div class="field"><label>Nome e cognome passeggero *</label><input id="bk-name"></div>
    <div class="field"><label>Numero di posti richiesti</label><input id="bk-seats" type="number" min="1" value="1"></div>
    <div class="field"><label>Fermata di salita</label><select id="bk-stop">${stops.map((s) => `<option>${esc(s)}</option>`).join("")}</select></div>
    <label style="display:flex;gap:8px;align-items:center;font-size:13px;margin-bottom:10px;"><input id="bk-return" type="checkbox" checked style="width:auto;"> Richiedo anche il transfer di rientro notturno</label>
    <div class="field"><label>Telefono per comunicazioni corsa</label><input id="bk-phone"></div>
    <div class="field"><label>Note o esigenze speciali</label><textarea id="bk-notes" rows="2" placeholder="es. Pronto subito dopo le proclamazioni"></textarea></div>
    <button class="btn btn-primary" id="bk-save" style="margin-top:12px;">Conferma Adesione</button>`,
    (bg, close) => {
      bg.querySelector("#bk-save").onclick = async () => {
        const name = bg.querySelector("#bk-name").value.trim();
        if (!name) { toast("Inserisci il nome del passeggero"); return; }
        const seats = Math.max(1, Number(bg.querySelector("#bk-seats").value) || 1);
        const ok = await tryAction(() => Repo.addBooking({
          passengerName: name,
          seatsCount: seats,
          pickupStop: bg.querySelector("#bk-stop").value,
          contactPhone: bg.querySelector("#bk-phone").value.trim(),
          notes: bg.querySelector("#bk-notes").value.trim(),
          returnTripWanted: bg.querySelector("#bk-return").checked
        }), "Posto prenotato ✓");
        if (ok) { close(); onDone(); }
      };
    });
}

// ============================ AUGURI & FOTO ============================
export async function wishes(el) {
  const list = await Repo.allWishes();
  const photos = await Repo.allPhotos();
  const wSorted = [...list].sort((a, b) => b.createdAt - a.createdAt);
  el.innerHTML = `
    <h2 class="section">Bacheca Auguri & Momenti</h2>
    <div class="muted" style="margin-top:-4px;margin-bottom:12px;">Condividi congratulazioni e scatti indimenticabili con tutti gli invitati</div>
    <div class="btn-row" style="margin-bottom:12px;">
      <button class="btn btn-gold" id="add-wish">✍️ Scrivi un Augurio</button>
      <button class="btn btn-primary" id="add-photo">📷 Carica Foto</button>
    </div>
    <h2 class="section" style="margin-top:6px;">Tutti i Messaggi di Auguri (${wSorted.length})</h2>
    ${wSorted.length === 0 ? '<div class="empty">Nessun augurio ancora. Scrivi il primo!</div>' : wSorted.map((w) => `
      <div class="wish">
        <div class="head"><span class="author">${esc(w.emojiBadge || "🎓")} ${esc(w.authorName)}</span><span class="target">Per: ${esc(w.targetGraduate)}</span></div>
        <div class="msg">${esc(w.message)}</div>
        <div class="foot"><span class="muted" style="font-size:11px;">${fmtTime(w.createdAt)}</span>
          <button class="btn btn-ghost" style="width:auto;padding:6px 12px;color:#F43F5E;" data-heart="${w.id}">❤️ ${w.heartCount}</button></div>
      </div>`).join("")}

    <h2 class="section">Galleria Foto (${photos.length})</h2>
    ${photos.length === 0 ? '<div class="empty">Nessuna foto condivisa ancora.</div>' : photos.map((p) => `
      <div class="photo">
        ${p.imageUri ? `<img class="thumb" src="${esc(p.imageUri)}" alt="${esc(p.caption)}" loading="lazy">` : `<div class="placeholder">🎓 Neurologia 2026</div>`}
        <div class="name" style="font-weight:700;font-size:13px;">${esc(p.caption)}</div>
        <div class="cat">di ${esc(p.authorName)} · ${fmtTime(p.createdAt)}</div>
        <div style="margin-top:6px;"><button class="btn btn-ghost" style="width:auto;padding:6px 12px;color:#F43F5E;" data-like="${p.id}">❤️ ${p.likesCount}</button></div>
      </div>`).join("")}
  `;
  el.querySelector("#add-wish").onclick = () => wishDialog(() => wishes(el));
  el.querySelector("#add-photo").onclick = () => photoDialog(() => wishes(el));
  el.querySelectorAll("[data-heart]").forEach((b) => b.onclick = async () => {
    if (await tryAction(() => Repo.heartWish(Number(b.dataset.heart)))) wishes(el);
  });
  el.querySelectorAll("[data-like]").forEach((b) => b.onclick = async () => {
    if (await tryAction(() => Repo.likePhoto(Number(b.dataset.like)))) wishes(el);
  });
}

function wishDialog(onDone) {
  const grads = ["Tutti i Laureandi", ...GRADUATES];
  const emojis = ["🎓", "🧠", "🥂", "❤️", "⚡", "⭐", "🎉", "✨"];
  openModal(`<h3>✍️ Invia un Augurio ai Laureandi</h3>
    <div class="field"><label>Il tuo nome *</label><input id="w-author"></div>
    <div class="field"><label>Destinatario</label><div class="chips" id="w-targets">${grads.map((g, i) => `<button class="chip ${i === 0 ? "active" : ""}" data-tg="${esc(g)}">${esc(g)}</button>`).join("")}</div></div>
    <div class="field"><label>Scegli un'emoji celebrativa</label><div class="chips" id="w-emojis">${emojis.map((e, i) => `<button class="chip ${i === 0 ? "active" : ""}" data-em="${e}">${e}</button>`).join("")}</div></div>
    <div class="field"><label>Messaggio di congratulazioni *</label><textarea id="w-msg" rows="4" placeholder="Scrivi il tuo messaggio di auguri..."></textarea></div>
    <button class="btn btn-gold" id="w-send" style="margin-top:10px;">Pubblica Augurio</button>`,
    (bg, close) => {
      let target = "Tutti i Laureandi", emoji = "🎓";
      bg.querySelectorAll("#w-targets .chip").forEach((c) => c.onclick = () => {
        bg.querySelectorAll("#w-targets .chip").forEach((x) => x.classList.remove("active")); c.classList.add("active"); target = c.dataset.tg;
      });
      bg.querySelectorAll("#w-emojis .chip").forEach((c) => c.onclick = () => {
        bg.querySelectorAll("#w-emojis .chip").forEach((x) => x.classList.remove("active")); c.classList.add("active"); emoji = c.dataset.em;
      });
      bg.querySelector("#w-send").onclick = async () => {
        const author = bg.querySelector("#w-author").value.trim() || "Amico/a";
        const msg = bg.querySelector("#w-msg").value.trim();
        if (!msg) { toast("Scrivi un messaggio"); return; }
        const ok = await tryAction(() => Repo.addWish({ authorName: author, targetGraduate: target, message: msg, emojiBadge: emoji }), "Augurio pubblicato ✓");
        if (ok) { close(); onDone(); }
      };
    });
}

function photoDialog(onDone) {
  openModal(`<h3>📷 Condividi uno Scatto</h3>
    <div class="field"><label>Immagine *</label><input id="ph-file" type="file" accept="image/*"></div>
    <div id="ph-preview" style="margin-bottom:10px;"></div>
    <div class="field"><label>Nome di chi pubblica</label><input id="ph-author"></div>
    <div class="field"><label>Didascalia / Momento della festa</label><input id="ph-caption" placeholder="es. Ultimo turno insieme!"></div>
    <button class="btn btn-primary" id="ph-save" style="margin-top:10px;">Condividi in Galleria</button>`,
    (bg, close) => {
      const fileInput = bg.querySelector("#ph-file");
      fileInput.onchange = () => {
        const f = fileInput.files[0];
        const prev = bg.querySelector("#ph-preview");
        if (f) { const url = URL.createObjectURL(f); prev.innerHTML = `<img src="${url}" style="width:100%;max-height:200px;object-fit:cover;border-radius:12px;">`; }
        else prev.innerHTML = "";
      };
      bg.querySelector("#ph-save").onclick = async () => {
        const file = fileInput.files[0];
        if (!file) { toast("Scegli una foto"); return; }
        const btn = bg.querySelector("#ph-save"); btn.disabled = true; btn.textContent = "Caricamento…";
        const ok = await tryAction(() => Repo.addPhoto({
          file,
          authorName: bg.querySelector("#ph-author").value.trim() || "Invitato",
          caption: bg.querySelector("#ph-caption").value.trim()
        }), "Foto pubblicata ✓");
        btn.disabled = false; btn.textContent = "Condividi in Galleria";
        if (ok) { close(); onDone(); }
      };
    });
}

// ============================ REGALI ============================
// Nessuna cifra raccolta è visibile. Due modi per partecipare:
//  - quota unica al cassiere (registrata sul server con la ripartizione, visibile solo a lui);
//  - regalo diretto a un neo-specialista: copia IBAN / PayPal / Satispay, senza registrare nulla.
const POOL_STATUS = { PENDING: "In attesa", RECEIVED: "Ricevuta" };
const eur2 = (n) => `${Number(n || 0).toLocaleString("it-IT", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} €`;

async function copyText(text, okMsg) {
  try { await navigator.clipboard.writeText(text); toast(okMsg); }
  catch { toast("Copia non disponibile: seleziona il testo e copialo a mano"); }
}

export async function gifts(el) {
  const targets = await Repo.allGiftTargets();
  const collector = GIFT_COLLECTOR;
  const serverMode = Repo.mode === "server";
  const mine = await Repo.myPoolContributions();
  const showBoard = isTreasurer() && (!serverMode || Status.treasurerEnabled);
  el.innerHTML = `
    <h2 class="section">Regali di Specializzazione</h2>
    <div class="muted" style="margin-top:-4px;margin-bottom:12px;">Scegli come partecipare: una quota unica tramite il cassiere, oppure un regalo diretto a ciascun neo-specialista. Nessuna cifra raccolta viene mostrata.</div>

    <div class="card collector">
      <div class="gname">👛 Quota unica tramite ${esc(collector.name || "il cassiere")}</div>
      <div class="grole">${esc(collector.roleTitle)}</div>
      <div class="muted" style="margin-top:6px;">${esc(collector.description)}</div>
      <div class="muted" style="margin-top:6px;">Metodi: ${(collector.paymentMethods || []).map((m) => `<b>${esc(m)}</b>`).join(" · ")}</div>
      <button class="btn btn-gold" id="pool-open" style="margin-top:12px;">🤝 Partecipa con una quota unica</button>
      ${mine.length ? `<div class="mine"><b>Le tue quote registrate</b>${mine.map((c) => `
        <div class="row"><div class="grow"><div class="name">${eur2(c.totalAmount)} · ${esc(c.paymentMethod)} · <span class="pill ${c.status === "RECEIVED" ? "pill-conf" : "pill-pend"}">${POOL_STATUS[c.status] || c.status}</span></div>
          <div class="cat">${esc(c.donorName)} · ${c.allocations.length === targets.length ? "tutti i neo-specialisti" : c.allocations.map((a) => esc(shortName(a.graduateName))).join(", ")} · ${fmtTime(c.createdAt)}</div></div>
          ${c.status !== "RECEIVED" ? `<button class="icon-btn" data-pool-del="${c.id}" aria-label="Annulla quota">🗑️</button>` : ""}</div>`).join("")}</div>` : ""}
    </div>

    <div id="pool-board"></div>

    <h2 class="section">Regalo diretto a un neo-specialista</h2>
    <div class="muted" style="margin-top:-4px;margin-bottom:10px;">Copia l'IBAN o apri PayPal/Satispay: l'importo lo decidi tu e non serve registrarlo qui.</div>
    ${targets.map((t) => `<div class="card gift" data-target="${esc(t.id)}">
        <div class="gname">👤 ${esc(t.name)}</div>
        <div class="grole">${esc(t.specialization)} · ${esc(t.roleTitle)}</div>
        <div class="gtitle">🎁 ${esc(t.giftTitle)}</div>
        <div class="muted" style="margin-top:4px;">${esc(t.giftDescription)}</div>
        <div class="muted" style="margin-top:8px;"><b>Intestatario:</b> ${esc(t.ibanHolder)}</div>
        <div class="mono iban">${esc(t.iban)}</div>
        <div class="pay">
          <button class="btn btn-primary" data-copy-iban="${esc(t.id)}">📋 Copia IBAN</button>
          ${t.paypalMeUrl ? `<a class="btn paypal" href="${esc(t.paypalMeUrl)}" target="_blank" rel="noopener">PayPal</a>` : ""}
          ${t.satispayUrl ? `<a class="btn satispay" href="${esc(t.satispayUrl)}" target="_blank" rel="noopener">Satispay</a>` : ""}
        </div>
      </div>`).join("")}
  `;
  el.querySelector("#pool-open").onclick = () => poolDialog(targets, collector, () => gifts(el));
  el.querySelectorAll("[data-copy-iban]").forEach((b) => {
    const t = targets.find((x) => x.id === b.dataset.copyIban);
    b.onclick = () => copyText(t.iban, `IBAN di ${shortName(t.name)} copiato!`);
  });
  el.querySelectorAll("[data-pool-del]").forEach((b) => b.onclick = async () => {
    if (!confirm("Annullare questa quota? Il cassiere non la vedrà più.")) return;
    if (await tryAction(() => Repo.deletePoolContribution(Number(b.dataset.poolDel)), "Quota annullata")) gifts(el);
  });
  if (showBoard) await poolBoard(el.querySelector("#pool-board"), targets);
}

// "Dott. Fedele Luisi" -> "Fedele Luisi"
function shortName(n) { return String(n || "").replace(/^Dott\.(ssa)?\s*/i, ""); }

// Dialogo della quota unica: importo, ripartizione (parti uguali o personalizzata), metodo, dati per pagare.
function poolDialog(targets, collector, onDone) {
  const presets = [50, 100, 150, 200];
  const methods = (collector.paymentMethods || []).filter((m) => ["IBAN", "PayPal", "Contanti"].includes(m));
  openModal(`<h3>🤝 Quota unica • ${esc(collector.name || "cassiere")}</h3>
    <div class="muted" style="margin-bottom:10px;">Un solo versamento: ${esc(shortName(collector.name) || "il cassiere")} lo ripartisce fra i neo-specialisti secondo le tue indicazioni.</div>
    <div class="field"><label>Il tuo nome e cognome *</label><input id="q-donor" placeholder="Serve al cassiere per riconoscere il versamento"></div>
    <div class="field"><label>Contatto (facoltativo: telefono o e-mail)</label><input id="q-contact"></div>
    <div class="field"><label>Come vuoi ripartire la quota?</label>
      <div class="chips" id="q-modes"><button class="chip active" data-mode="EQUAL">In parti uguali</button><button class="chip" data-mode="CUSTOM">Personalizza</button></div></div>
    <div id="q-equal">
      <div class="field"><label>Importo totale</label>
        <div class="chips" id="q-presets">${presets.map((p) => `<button class="chip ${p === 100 ? "active" : ""}" data-amt="${p}">${p} €</button>`).join("")}</div>
        <input id="q-amount" type="number" min="1" step="0.01" inputmode="decimal" placeholder="Oppure un altro importo (€)"></div>
      <div class="field"><label>Per chi (togli la spunta per escludere qualcuno, es. te stesso)</label>
        <div class="split-list" id="q-who">${targets.map((t) => `<label class="split-row"><input type="checkbox" data-who="${esc(t.id)}" checked> <span class="grow">${esc(t.name)}</span><span class="muted" data-each="${esc(t.id)}"></span></label>`).join("")}</div></div>
    </div>
    <div id="q-custom" hidden>
      <div class="field"><label>Importo per ciascun neo-specialista (lascia vuoto chi non vuoi includere)</label>
        <div class="split-list">${targets.map((t) => `<label class="split-row"><span class="grow">${esc(t.name)}</span><input type="number" min="0" step="0.01" inputmode="decimal" placeholder="0" data-custom="${esc(t.id)}" style="width:96px;"></label>`).join("")}</div></div>
    </div>
    <div class="field"><label>Metodo di pagamento</label><div class="chips" id="q-methods">${methods.map((m, i) => `<button class="chip ${i === 0 ? "active" : ""}" data-m="${m}">${m}</button>`).join("")}</div></div>
    <div class="card" id="q-pay" style="background:var(--bg);"></div>
    <div class="field"><label>Nota per il cassiere (facoltativa)</label><textarea id="q-note" rows="2" placeholder="es. Consegno i contanti sabato"></textarea></div>
    <button class="btn btn-gold" id="q-save" style="margin-top:10px;">Registra quota (100,00 €)</button>`,
    (bg, close) => {
      let mode = "EQUAL", method = methods[0] || "IBAN", amount = 100;
      const $ = (sel) => bg.querySelector(sel);
      const $$ = (sel) => [...bg.querySelectorAll(sel)];
      const selectedIds = () => $$("[data-who]:checked").map((c) => c.dataset.who);
      const customRows = () => $$("[data-custom]").map((i) => ({ graduateId: i.dataset.custom, amount: Number(i.value) || 0 })).filter((a) => a.amount > 0);
      const totalNow = () => mode === "EQUAL" ? amount : customRows().reduce((s, a) => s + a.amount, 0);
      const refresh = () => {
        $("#q-save").textContent = `Registra quota (${eur2(totalNow())})`;
        const ids = selectedIds();
        const parts = ids.length && amount > 0 ? splitEqually(Math.round(amount * 100), ids.length) : [];
        $$("[data-each]").forEach((sp) => { const i = ids.indexOf(sp.dataset.each); sp.textContent = i >= 0 && parts.length ? eur2(parts[i] / 100) : "—"; });
      };
      $$("#q-modes .chip").forEach((c) => c.onclick = () => {
        $$("#q-modes .chip").forEach((x) => x.classList.remove("active")); c.classList.add("active");
        mode = c.dataset.mode; $("#q-equal").hidden = mode !== "EQUAL"; $("#q-custom").hidden = mode !== "CUSTOM"; refresh();
      });
      $$("#q-presets .chip").forEach((c) => c.onclick = () => {
        $$("#q-presets .chip").forEach((x) => x.classList.remove("active")); c.classList.add("active");
        amount = Number(c.dataset.amt); $("#q-amount").value = ""; refresh();
      });
      $("#q-amount").oninput = () => { $$("#q-presets .chip").forEach((x) => x.classList.remove("active")); amount = Number($("#q-amount").value) || 0; refresh(); };
      $$("[data-who]").forEach((c) => c.onchange = refresh);
      $$("[data-custom]").forEach((i) => i.oninput = refresh);
      const paintPay = () => {
        const reason = `${collector.transferReason || "Regalo specializzazione"} - ${$("#q-donor").value.trim() || "Nome Cognome"}`;
        const box = $("#q-pay");
        if (method === "IBAN") box.innerHTML = `<div class="muted"><b>Intestatario:</b> ${esc(collector.ibanHolder)}</div><div class="mono iban">${esc(collector.iban)}</div>
          <div class="muted"><b>Causale suggerita:</b> <span class="mono" id="q-reason">${esc(reason)}</span></div>
          <div class="btn-row" style="margin-top:8px;"><button class="btn btn-ghost" id="q-copy-iban">📋 Copia IBAN</button><button class="btn btn-ghost" id="q-copy-reason">📋 Copia causale</button></div>`;
        else if (method === "PayPal") box.innerHTML = `<div class="muted">Invia la quota con PayPal.me indicando nel messaggio il tuo nome:</div><a class="btn paypal" style="margin-top:8px;" href="${esc(collector.paypalMeUrl)}" target="_blank" rel="noopener">Apri PayPal.me</a>`;
        else box.innerHTML = `<div class="muted">Consegna i contanti a ${esc(shortName(collector.name) || "al cassiere")} (in reparto o alla festa): registra comunque la quota qui, così sa cosa aspettarsi.</div>`;
        const ci = $("#q-copy-iban"), cr = $("#q-copy-reason");
        if (ci) ci.onclick = () => copyText(collector.iban, "IBAN del cassiere copiato!");
        if (cr) cr.onclick = () => copyText(reason, "Causale copiata!");
      };
      $$("#q-methods .chip").forEach((c) => c.onclick = () => { $$("#q-methods .chip").forEach((x) => x.classList.remove("active")); c.classList.add("active"); method = c.dataset.m; paintPay(); });
      $("#q-donor").oninput = () => { if (method === "IBAN") paintPay(); };
      paintPay(); refresh();
      $("#q-save").onclick = async () => {
        const donorName = $("#q-donor").value.trim();
        if (!donorName) { toast("Inserisci nome e cognome: servono al cassiere"); return; }
        const body = { donorName, contact: $("#q-contact").value.trim(), paymentMethod: method, splitMode: mode, note: $("#q-note").value.trim() };
        if (mode === "EQUAL") {
          if (!amount || amount <= 0) { toast("Inserisci un importo valido"); return; }
          if (!selectedIds().length) { toast("Scegli almeno un neo-specialista"); return; }
          body.totalAmount = amount; body.graduateIds = selectedIds();
        } else {
          body.allocations = customRows();
          if (!body.allocations.length) { toast("Indica almeno un importo"); return; }
        }
        const ok = await tryAction(() => Repo.addPoolContribution(body), `Quota di ${eur2(totalNow())} registrata: grazie! 🎉`);
        if (ok) { close(); onDone(); }
      };
    });
}

// Cruscotto del cassiere (token cassiere o organizzatore): totali per neo-specialista, elenco quote, stato, CSV.
async function poolBoard(box, targets) {
  let board;
  try { board = await Repo.poolBoard(); }
  catch (e) { box.innerHTML = `<div class="card"><div class="muted">Cruscotto cassiere non disponibile: ${esc(e.message)}</div></div>`; return; }
  const s = board.summary, serverMode = Repo.mode === "server";
  box.innerHTML = `<details class="card board" open>
    <summary><b>📒 Cassa quote uniche</b> <span class="muted">${s.contributions} quote · ${eur2(s.totalAmount)} (ricevute ${eur2(s.receivedAmount)}, in attesa ${eur2(s.pendingAmount)})</span></summary>
    <div class="muted" style="margin:8px 0 4px;"><b>Quanto spetta a ciascuno</b> (tra parentesi la parte già ricevuta)</div>
    <table class="tbl"><tbody>${s.byGraduate.map((g) => `<tr><td>${esc(shortName(g.graduateName))}</td><td class="num"><b>${eur2(g.amount)}</b> <span class="muted">(${eur2(g.receivedAmount)})</span></td></tr>`).join("")}</tbody></table>
    ${serverMode ? `<button class="btn btn-primary" id="pool-csv" style="margin-top:10px;">📄 Scarica per Excel (CSV)</button>` : ""}
    <div class="muted" style="margin:12px 0 4px;"><b>Quote registrate</b> · tocca lo stato per segnarla ricevuta</div>
    ${board.contributions.length ? board.contributions.map((c) => `<div class="row">
      <div class="grow"><div class="name">${esc(c.donorName)} · ${eur2(c.totalAmount)} · ${esc(c.paymentMethod)}</div>
        <div class="cat">${c.allocations.length === targets.length && c.splitMode === "EQUAL" ? `parti uguali (${eur2(c.allocations[0]?.amount)} ciascuno)` : c.allocations.map((a) => `${esc(shortName(a.graduateName))} ${eur2(a.amount)}`).join(", ")}</div>
        <div class="cat">${fmtTime(c.createdAt)}${c.contact ? ` · ${esc(c.contact)}` : ""}${c.note ? ` · “${esc(c.note)}”` : ""}</div></div>
      <button class="pill ${c.status === "RECEIVED" ? "pill-conf" : "pill-pend"}" data-pool-status="${c.id}" data-next="${c.status === "RECEIVED" ? "PENDING" : "RECEIVED"}">${POOL_STATUS[c.status] || c.status}</button>
    </div>`).join("") : `<div class="muted">Nessuna quota registrata finora.</div>`}
  </details>`;
  const csv = box.querySelector("#pool-csv");
  if (csv) csv.onclick = () => tryAction(() => downloadCsv("gift-pool"), "quote-uniche.csv scaricato");
  box.querySelectorAll("[data-pool-status]").forEach((b) => b.onclick = async () => {
    if (await tryAction(() => Repo.setPoolStatus(Number(b.dataset.poolStatus), b.dataset.next), b.dataset.next === "RECEIVED" ? "Segnata come ricevuta ✓" : "Rimessa in attesa")) poolBoard(box, targets);
  });
}
