// Schermate PWA - mirror delle 5 schermate Compose dell'app Android
import { Repo, RsvpStatus, MAX_BUS_SEATS, GRADUATES, MAP_POINTS } from "./data.js";
import { toast, openModal, fmtTime, goto } from "./app.js";
import { sendPush, ensurePermission } from "./notify.js";

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const euro = (n) => `€ ${Number(n || 0).toLocaleString("it-IT", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;

// ============================ PROGRAMMA ============================
export async function program(el, goto) {
  const wishes = await Repo.allWishes();
  const notifications = await Repo.allNotifications();
  const unread = notifications.filter((n) => !n.isRead).length;
  const ticker = wishes.length ? wishes[Math.floor(Math.random() * wishes.length)] : null;

  el.innerHTML = `
    <div class="hero">
      <div class="badge">🎓 SPECIALIZZAZIONE IN NEUROLOGIA</div>
      <h1>Seduta di Specializzazione & Festa</h1>
      <div class="sub">8 Neo-Specialisti in Neurologia</div>
      <div class="graduates">${GRADUATES.join(" • ")}</div>
      <div class="meta">📅 9 Novembre (seduta) • 13 Novembre (festa)</div>
      <div class="meta">📍 Aula Magna "G. De Benedictis" - Policlinico di Bari</div>
    </div>

    ${ticker ? `<div class="ticker"><span class="emoji">${ticker.emojiBadge || "🎓"}</span>
      <span class="txt"><b>${esc(ticker.authorName)}</b>: ${esc(ticker.message)}</span>
      <button class="heart" data-heart="${ticker.id}">❤️ ${ticker.heartCount}</button></div>` : ""}

    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <b style="font-size:14px;">🔔 Notifiche</b>
        ${unread > 0 ? `<span class="pill pill-pend">${unread} non lette</span>` : `<span class="pill pill-conf">tutte lette</span>`}
      </div>
      <div class="btn-row" style="margin-top:10px;">
        <button class="btn btn-ghost" id="ntf-show">Visualizza cronologia</button>
        <button class="btn btn-gold" id="ntf-send">Invia notifica push</button>
      </div>
    </div>

    <h2 class="section">Programma dell'Evento</h2>
    <div class="card">
      ${timelineItem("Ore 9/10", "🏫", "Seduta di Laurea & Proclamazione", 'Aula Magna "G. De Benedictis" - Policlinico di Bari', "Discussione delle tesi e proclamazione degli 8 neo-specialisti in Neurologia. L'ora esatta della seduta sarà comunicata a breve.", true)}
      ${timelineItem("Dopo", "🎉", "Brindisi Accademico & Foto di Rito", 'Aula Magna "G. De Benedictis" - Policlinico di Bari', "Consegna dei diplomi, corona d'alloro e foto di rito con colleghi, docenti e parenti al termine della seduta del 9 novembre.", true)}
      ${timelineItem("Ven 13", "🚌", "Ritrovo & Imbarco Autobus Navetta", "Piazzale Policlinico di Bari", "Venerdì 13 novembre: ritrovo dei partecipanti e transfer riservato 54 posti verso la location della festa (luogo da definire).", true)}
      ${timelineItem("Ven 13", "🌃", "Festa di Specializzazione", "Location da definire (Bari e dintorni)", "Venerdì 13 novembre: aperitivo, cena a buffet e brindisi tutti insieme per festeggiare i Neo-neurologi. Luogo e ora saranno comunicati a breve.", true)}
      ${timelineItem("Ven 13", "🎂", "Taglio della Torta & Dj Set", "Location da definire (Bari e dintorni)", "Taglio della torta di specializzazione, video celebrativo a sorpresa, musica e balli per chiudere in bellezza la serata.", false)}
    </div>

    <h2 class="section">Mappa Interattiva Punti di Ritrovo</h2>
    <div class="card">
      <div class="chips" id="mp-chips">
        ${MAP_POINTS.map((p, i) => `<button class="chip ${i === 0 ? "active" : ""}" data-mp="${p.id}">${p.title.split(" ")[0]}</button>`).join("")}
      </div>
      <div id="map"></div>
      <div id="mp-detail" style="margin-top:10px;"></div>
    </div>
  `;

  // ticker heart
  const hb = el.querySelector(".ticker .heart");
  if (hb) hb.onclick = async () => { await Repo.heartWish(Number(hb.dataset.heart)); toast("Auguri mandati ❤️"); };

  // notifiche
  el.querySelector("#ntf-show").onclick = () => showNotifications(notifications);
  el.querySelector("#ntf-send").onclick = () => sendPushDialog();
  el.querySelector("#ntf-show").onclick = async () => {
    await Repo.markAllRead();
    showNotifications(await Repo.allNotifications());
    program(el, goto);
  };

  // mappa
  initMap(el, 0);
  el.querySelectorAll("#mp-chips .chip").forEach((c) => {
    c.onclick = () => {
      el.querySelectorAll("#mp-chips .chip").forEach((x) => x.classList.remove("active"));
      c.classList.add("active");
      const idx = MAP_POINTS.findIndex((p) => p.id === c.dataset.mp);
      initMap(el, idx);
    };
  });
}

function timelineItem(time, icon, title, loc, det, more) {
  return `<div class="timeline">
    <div class="col-time"><div class="time">${time}</div><div class="dot">${icon}</div>${more ? '<div class="line"></div>' : ""}</div>
    <div class="body"><div class="t-title">${esc(title)}</div><div class="t-loc">📍 ${esc(loc)}</div><div class="t-det">${esc(det)}</div></div>
  </div>`;
}

let _map = null;
function initMap(el, idx) {
  const p = MAP_POINTS[idx];
  const det = el.querySelector("#mp-detail");
  det.innerHTML = `<b style="font-size:14px;">${esc(p.title)}</b><div class="muted">${esc(p.subtitle)} · ${esc(p.timeLabel)}</div><div class="muted" style="margin-top:4px;">📍 ${esc(p.address)}</div><div class="muted" style="margin-top:4px;">${esc(p.description)}</div>
    <div style="margin-top:8px;"><a class="btn btn-primary" style="display:inline-flex;width:auto;padding:8px 14px;" target="_blank" rel="noopener" href="https://maps.apple.com/?q=${encodeURIComponent(p.address)}&ll=${p.latitude},${p.longitude}">Apri in Mappe →</a></div>`;
  if (_map) { _map.remove(); _map = null; }
  if (typeof L === "undefined") { return; }
  _map = L.map(el.querySelector("#map")).setView([p.latitude, p.longitude], 15);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", { attribution: "© OpenStreetMap", maxZoom: 19 }).addTo(_map);
  const colorMap = { GRADUATION: "#005FB0", BUS: "#22D3EE", PARTY: "#8B5CF6" };
  const mark = L.circleMarker([p.latitude, p.longitude], { radius: 10, color: colorMap[p.iconType] || "#005FB0", fillColor: colorMap[p.iconType] || "#005FB0", fillOpacity: 0.9 }).addTo(_map);
  mark.bindPopup(`<div class="mp-title">${esc(p.title)}</div><div class="mp-sub">${esc(p.timeLabel)}</div>`);
}

async function showNotifications(notifications) {
  const sorted = [...notifications].sort((a, b) => b.timestamp - a.timestamp);
  openModal(`<h3>🔔 Cronologia Notifiche</h3>${sorted.length === 0 ? '<div class="empty">Nessuna notifica.</div>' : sorted.map((n) => `
    <div class="card" style="margin-bottom:10px;">
      <div style="display:flex;justify-content:space-between;"><b style="font-size:13px;">${esc(n.title)}</b><span class="pill ${n.category === "Seduta" ? "pill-conf" : n.category === "Festa" ? "pill-pend" : "pill-decl"}">${esc(n.category)}</span></div>
      <div class="muted" style="margin-top:6px;">${esc(n.message)}</div>
      <div class="muted" style="margin-top:6px;font-size:11px;">${fmtTime(n.timestamp)}</div>
    </div>`).join("")}`);
}

function sendPushDialog() {
  const templates = [
    ["Seduta del 9 Novembre!", 'La seduta di proclamazione è in corso all\'Aula Magna "G. De Benedictis" del Policlinico di Bari.'],
    ["Partenza Navetta Imminente", "L'autobus è in sosta al Piazzale Principale del Policlinico di Bari. Partenza tra 15 minuti!"],
    ["Benvenuti alla Festa!", "Venerdì 13 novembre: aperitivo di benvenuto aperto! Vi aspettiamo per il primo brindisi insieme."],
    ["Taglio della Torta & Dj Set", "Tutti attorno alla torta di specializzazione per il momento più atteso della serata!"]
  ];
  const cats = ["Seduta", "Festa", "Navetta", "Organizzazione"];
  openModal(`<h3>📡 Invia Notifica Push</h3>
    <div class="muted" style="margin-bottom:10px;">Invia un aggiornamento in tempo reale a tutti gli invitati tramite notifica di sistema.</div>
    <div class="field"><label>Titolo</label><input id="p-title" placeholder="Titolo notifica"></div>
    <div class="field"><label>Messaggio</label><textarea id="p-body" rows="3" placeholder="Corpo del messaggio"></textarea></div>
    <div class="field"><label>Categoria</label><select id="p-cat">${cats.map((c) => `<option>${c}</option>`).join("")}</select></div>
    <div class="muted" style="margin:6px 0;">Preset rapidi:</div>
    <div class="chips">${templates.map((t, i) => `<button class="chip" data-tpl="${i}">${t[0]}</button>`).join("")}</div>
    <button class="btn btn-primary" id="p-send" style="margin-top:12px;">Invia notifica</button>`,
    (bg, close) => {
      bg.querySelectorAll("[data-tpl]").forEach((c) => c.onclick = () => {
        const t = templates[Number(c.dataset.tpl)];
        bg.querySelector("#p-title").value = t[0];
        bg.querySelector("#p-body").value = t[1];
      });
      bg.querySelector("#p-send").onclick = async () => {
        const title = bg.querySelector("#p-title").value.trim();
        const body = bg.querySelector("#p-body").value.trim();
        const cat = bg.querySelector("#p-cat").value;
        if (!title || !body) { toast("Inserisci titolo e messaggio"); return; }
        await Repo.addNotification({ title, message: body, category: cat });
        const ok = await sendPush(title, body);
        close();
        toast(ok ? "Notifica inviata ✓" : "Notifica salvata (permesso negato)");
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
    const counts = {
      [RsvpStatus.CONFIRMED]: guests.filter((g) => g.rsvpStatus === RsvpStatus.CONFIRMED).length,
      [RsvpStatus.PENDING]: guests.filter((g) => g.rsvpStatus === RsvpStatus.PENDING).length,
      [RsvpStatus.DECLINED]: guests.filter((g) => g.rsvpStatus === RsvpStatus.DECLINED).length
    };
    el.innerHTML = `
      <h2 class="section">Invitati & RSVP</h2>
      <div class="card">
        <input id="g-search" placeholder="🔍 Cerca invitato o categoria" value="${esc(query)}">
        <div class="chips" style="margin-top:10px;">
          <button class="chip ${!filter ? "active" : ""}" data-f="">Tutti (${guests.length})</button>
          <button class="chip ${filter === RsvpStatus.CONFIRMED ? "active" : ""}" data-f="${RsvpStatus.CONFIRMED}">Confermati (${counts[RsvpStatus.CONFIRMED]})</button>
          <button class="chip ${filter === RsvpStatus.PENDING ? "active" : ""}" data-f="${RsvpStatus.PENDING}">In attesa (${counts[RsvpStatus.PENDING]})</button>
          <button class="chip ${filter === RsvpStatus.DECLINED ? "active" : ""}" data-f="${RsvpStatus.DECLINED}">Declinato (${counts[RsvpStatus.DECLINED]})</button>
        </div>
      </div>
      <div class="card">
        ${list.length === 0 ? '<div class="empty">Nessun invitato corrisponde ai filtri.</div>' : list.map((g) => `
          <div class="row">
            <div class="grow">
              <div class="name">${esc(g.fullName)}</div>
              <div class="cat">${esc(g.category)} · ${g.guestsCount} ${g.guestsCount === 1 ? "persona" : "persone"} ${g.dietaryNotes ? "· " + esc(g.dietaryNotes) : ""}</div>
            </div>
            <span class="pill ${g.rsvpStatus === RsvpStatus.CONFIRMED ? "pill-conf" : g.rsvpStatus === RsvpStatus.PENDING ? "pill-pend" : "pill-decl"}">${statusLabel(g.rsvpStatus)}</span>
            <div style="display:flex;gap:2px;">
              <button class="icon-btn" data-cycle="${g.id}" title="Cambia stato">🔄</button>
              <button class="icon-btn" data-del="${g.id}" title="Elimina">🗑️</button>
            </div>
          </div>`).join("")}
      </div>
      <button class="fab" id="add-guest">＋ Aggiungi</button>
    `;
    el.querySelector("#g-search").oninput = (e) => { query = e.target.value; paint(); };
    el.querySelectorAll("[data-f]").forEach((c) => c.onclick = () => { filter = c.dataset.f || null; paint(); });
    el.querySelectorAll("[data-cycle]").forEach((b) => b.onclick = async () => {
      const g = guests.find((x) => x.id === Number(b.dataset.cycle));
      const next = g.rsvpStatus === RsvpStatus.CONFIRMED ? RsvpStatus.PENDING : g.rsvpStatus === RsvpStatus.PENDING ? RsvpStatus.DECLINED : RsvpStatus.CONFIRMED;
      await Repo.updateGuest({ ...g, rsvpStatus: next });
      guests = await Repo.allGuests(); paint();
    });
    el.querySelectorAll("[data-del]").forEach((b) => b.onclick = async () => {
      await Repo.deleteGuest(Number(b.dataset.del));
      guests = await Repo.allGuests(); paint(); toast("Invitato rimosso");
    });
    el.querySelector("#add-guest").onclick = () => guestDialog(async () => { guests = await Repo.allGuests(); paint(); });
  }
  paint();
}

function statusLabel(s) { return s === RsvpStatus.CONFIRMED ? "Confermato" : s === RsvpStatus.PENDING ? "In attesa" : "Declinato"; }

function guestDialog(onDone) {
  const cats = ["Famigliari", "Amici Università", "Colleghi Reparto", "Docenti e Reparto", "Specializzandi 1° e 2° anno"];
  openModal(`<h3>＋ Aggiungi Invitato</h3>
    <div class="field"><label>Nome e cognome *</label><input id="ag-name"></div>
    <div class="field"><label>Categoria</label><input id="ag-cat" list="cat-list"><datalist id="cat-list">${cats.map((c) => `<option value="${c}">`).join("")}</datalist></div>
    <div class="field"><label>Numero persone (compreso l'invitato)</label><input id="ag-count" type="number" min="1" value="1"></div>
    <div class="field"><label>Note dietetiche</label><input id="ag-diet" placeholder="es. Celiaco, Vegetariano, Nessuna"></div>
    <div class="field"><label>Contatto (email o telefono)</label><input id="ag-contact"></div>
    <div class="field"><label>Stato RSVP</label><select id="ag-status"><option value="${RsvpStatus.CONFIRMED}">Confermato</option><option value="${RsvpStatus.PENDING}">In attesa</option><option value="${RsvpStatus.DECLINED}">Declinato</option></select></div>
    <button class="btn btn-primary" id="ag-save" style="margin-top:10px;">Salva invitato</button>`,
    (bg, close) => {
      bg.querySelector("#ag-save").onclick = async () => {
        const name = bg.querySelector("#ag-name").value.trim();
        if (!name) { toast("Inserisci il nome"); return; }
        await Repo.addGuest({
          fullName: name,
          category: bg.querySelector("#ag-cat").value.trim() || "Invitato",
          guestsCount: Math.max(1, Number(bg.querySelector("#ag-count").value) || 1),
          dietaryNotes: bg.querySelector("#ag-diet").value.trim(),
          contactInfo: bg.querySelector("#ag-contact").value.trim(),
          rsvpStatus: bg.querySelector("#ag-status").value
        });
        close(); toast("Invitato aggiunto ✓"); onDone();
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
    <div class="muted" style="margin-top:-4px;margin-bottom:12px;">Transfer andata e ritorno dal Policlinico di Bari alla sede della festa</div>
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <b>Posti totali</b><b>${booked} / ${MAX_BUS_SEATS}</b>
      </div>
      <div class="meter"><span style="width:${pct}%"></span></div>
      <div class="muted">${available} posti ancora disponibili</div>
    </div>
    <div class="card">
      <b style="font-size:14px;">Orari & Fermate Transfer</b>
      <div style="margin-top:12px;border-top:1px solid var(--outline);padding-top:12px;">
        <div style="display:flex;justify-content:space-between;"><b style="font-size:12px;">ANDATA</b><span class="pill pill-conf">Ven 13 - da definire</span></div>
        <div class="muted" style="margin-top:4px;">Policlinico di Bari (Piazzale Principale) → Location della festa (da definire)</div>
        <div class="muted" style="font-size:11px;">Orario di partenza in via di definizione, confermato appena nota la location</div>
      </div>
      <div style="margin-top:12px;border-top:1px solid var(--outline);padding-top:12px;">
        <div style="display:flex;justify-content:space-between;"><b style="font-size:12px;">RITORNO</b><span class="pill pill-pend">Ven 13 - notte</span></div>
        <div class="muted" style="margin-top:4px;">Location della festa → Policlinico di Bari / Stazione Centrale</div>
        <div class="muted" style="font-size:11px;">Rientro notturno garantito per tornare a casa in totale sicurezza</div>
      </div>
    </div>
    <h2 class="section">Elenco Passeggeri Prenotati (${bookings.length})</h2>
    <div class="card">
      ${bookings.length === 0 ? '<div class="empty">Nessun passeggero ha ancora prenotato la navetta.</div>' : bookings.map((b) => `
        <div class="row">
          <div class="grow">
            <div class="name">${esc(b.passengerName)}</div>
            <div class="cat">${b.seatsCount} ${b.seatsCount === 1 ? "posto" : "posti"} · ${esc(b.pickupStop)}${b.contactPhone ? " · " + esc(b.contactPhone) : ""}</div>
            ${b.notes ? `<div class="cat" style="font-style:italic;">${esc(b.notes)}</div>` : ""}
          </div>
          <button class="icon-btn" data-cancel="${b.id}" title="Annulla">🗑️</button>
        </div>`).join("")}
    </div>
    <button class="fab" id="book-bus">＋ Prenota posto</button>
  `;
  el.querySelectorAll("[data-cancel]").forEach((b) => b.onclick = async () => {
    await Repo.deleteBooking(Number(b.dataset.cancel)); bus(el); toast("Prenotazione annullata");
  });
  el.querySelector("#book-bus").onclick = () => busDialog(el, async (ok) => { if (ok) bus(el); });
}

function busDialog(el, onDone) {
  const stops = ["Policlinico di Bari (Piazzale Principale)", "Policlinico di Bari (Fermata Metro/Navetta)", "Stazione Ferroviaria Centrale di Bari"];
  openModal(`<h3>＋ Prenota Posto Autobus Navetta</h3>
    <div class="field"><label>Nome passeggero *</label><input id="bk-name"></div>
    <div class="field"><label>Numero di posti</label><input id="bk-seats" type="number" min="1" value="1"></div>
    <div class="field"><label>Fermata di salita</label><select id="bk-stop">${stops.map((s) => `<option>${s}</option>`).join("")}</select></div>
    <div class="field"><label>Telefono (opzionale)</label><input id="bk-phone"></div>
    <div class="field"><label>Note</label><textarea id="bk-notes" rows="2" placeholder="es. Pronto subito dopo le proclamazioni"></textarea></div>
    <label style="display:flex;gap:8px;align-items:center;font-size:13px;"><input id="bk-return" type="checkbox" checked style="width:auto;"> Voglio il viaggio di ritorno</label>
    <button class="btn btn-primary" id="bk-save" style="margin-top:12px;">Conferma prenotazione</button>`,
    (bg, close) => {
      bg.querySelector("#bk-save").onclick = async () => {
        const name = bg.querySelector("#bk-name").value.trim();
        const seats = Math.max(1, Number(bg.querySelector("#bk-seats").value) || 1);
        const booked = (await Repo.allBookings()).reduce((s, b) => s + (b.seatsCount || 0), 0);
        if (booked + seats > MAX_BUS_SEATS) { toast("Posti insufficienti sulla navetta"); return; }
        await Repo.addBooking({
          passengerName: name || "Invitato",
          seatsCount: seats,
          pickupStop: bg.querySelector("#bk-stop").value,
          contactPhone: bg.querySelector("#bk-phone").value.trim(),
          notes: bg.querySelector("#bk-notes").value.trim(),
          returnTripWanted: bg.querySelector("#bk-return").checked
        });
        close(); toast("Posto prenotato ✓"); onDone(true);
      };
    });
}

// ============================ AUGURI & FOTO ============================
export async function wishes(el) {
  const wishes = await Repo.allWishes();
  const photos = await Repo.allPhotos();
  const wSorted = [...wishes].sort((a, b) => b.heartCount - a.heartCount);
  el.innerHTML = `
    <h2 class="section">Bacheca Auguri</h2>
    <div class="card">
      <button class="btn btn-gold" id="add-wish">✍️ Invia un augurio</button>
    </div>
    ${wSorted.length === 0 ? '<div class="empty">Nessun augurio ancora. Scrivi il primo!</div>' : wSorted.map((w) => `
      <div class="wish">
        <div class="head"><span class="author">${w.emojiBadge || "🎓"} ${esc(w.authorName)}</span><span class="target">a ${esc(w.targetGraduate)}</span></div>
        <div class="msg">${esc(w.message)}</div>
        <div class="foot"><span class="muted" style="font-size:11px;">${fmtTime(w.createdAt)}</span>
          <button class="btn btn-ghost" style="width:auto;padding:6px 12px;" data-heart="${w.id}">❤️ ${w.heartCount}</button></div>
      </div>`).join("")}

    <h2 class="section">Galleria Foto</h2>
    <div class="card">
      <button class="btn btn-primary" id="add-photo">📷 Aggiungi una foto</button>
    </div>
    ${photos.length === 0 ? '<div class="empty">Nessuna foto condivisa ancora.</div>' : photos.map((p) => `
      <div class="photo">
        ${p.imageUri ? `<img class="thumb" src="${esc(p.imageUri)}" alt="">` : `<div class="placeholder">🖼️</div>`}
        <div class="name" style="font-weight:700;font-size:13px;">${esc(p.caption)}</div>
        <div class="cat">di ${esc(p.authorName)} · ${fmtTime(p.createdAt)}</div>
        <div style="margin-top:6px;"><button class="btn btn-ghost" style="width:auto;padding:6px 12px;" data-like="${p.id}">👍 ${p.likesCount}</button></div>
      </div>`).join("")}
  `;
  el.querySelector("#add-wish").onclick = () => wishDialog(async () => wishes(el, goto));
  el.querySelector("#add-photo").onclick = () => photoDialog(async () => wishes(el, goto));
  el.querySelectorAll("[data-heart]").forEach((b) => b.onclick = async () => {
    await Repo.heartWish(Number(b.dataset.heart)); wishes(el, goto);
  });
  el.querySelectorAll("[data-like]").forEach((b) => b.onclick = async () => {
    await Repo.likePhoto(Number(b.dataset.like)); wishes(el, goto);
  });
}

function wishDialog(onDone) {
  const grads = ["Tutti i Laureandi", ...GRADUATES];
  const emojis = ["🎓", "🧠", "🥂", "❤️", "⚡", "⭐", "🎉", "✨"];
  openModal(`<h3>✍️ Invia un Augurio</h3>
    <div class="field"><label>Il tuo nome *</label><input id="w-author"></div>
    <div class="field"><label>Destinatario</label><div class="chips" id="w-targets">${grads.map((g, i) => `<button class="chip ${i === 0 ? "active" : ""}" data-tg="${esc(g)}">${esc(g)}</button>`).join("")}</div></div>
    <div class="field"><label>Augurio *</label><textarea id="w-msg" rows="4" placeholder="Scrivi il tuo messaggio di auguri..."></textarea></div>
    <div class="field"><label>Emoji</label><div class="chips" id="w-emojis">${emojis.map((e, i) => `<button class="chip ${i === 0 ? "active" : ""}" data-em="${e}">${e}</button>`).join("")}</div></div>
    <button class="btn btn-gold" id="w-send" style="margin-top:10px;">Pubblica augurio</button>`,
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
        await Repo.addWish({ authorName: author, targetGraduate: target, message: msg, emojiBadge: emoji });
        close(); toast("Augurio pubblicato ✓"); onDone();
      };
    });
}

function photoDialog(onDone) {
  openModal(`<h3>📷 Aggiungi una Foto</h3>
    <div class="field"><label>Autore</label><input id="ph-author"></div>
    <div class="field"><label>Didascalia *</label><input id="ph-caption" placeholder="es. Ultimo turno insieme!"></div>
    <div class="field"><label>Immagine (opzionale)</label><input id="ph-file" type="file" accept="image/*"></div>
    <button class="btn btn-primary" id="ph-save" style="margin-top:10px;">Pubblica foto</button>`,
    (bg, close) => {
      bg.querySelector("#ph-save").onclick = async () => {
        const author = bg.querySelector("#ph-author").value.trim() || "Invitato";
        const caption = bg.querySelector("#ph-caption").value.trim();
        if (!caption) { toast("Inserisci una didascalia"); return; }
        const file = bg.querySelector("#ph-file").files[0];
        let imageUri = "";
        if (file) {
          if (file.size > 4_500_000) { toast("Immagine troppo grande (max ~4.5MB)"); return; }
          imageUri = await new Promise((res) => { const r = new FileReader(); r.onload = () => res(r.result); r.readAsDataURL(file); });
        }
        await Repo.addPhoto({ authorName: author, caption, imageUri });
        close(); toast("Foto pubblicata ✓"); onDone();
      };
    });
}

// ============================ REGALI ============================
export async function gifts(el) {
  const targets = await Repo.allGiftTargets();
  const contributions = await Repo.allGiftContributions();
  const totalCollected = targets.reduce((s, t) => s + (t.collectedAmount || 0), 0);
  const totalGoal = targets.reduce((s, t) => s + (t.targetAmount || 0), 0);
  const overallPct = totalGoal > 0 ? Math.min(100, (totalCollected / totalGoal) * 100) : 0;
  el.innerHTML = `
    <h2 class="section">Regali di Specializzazione</h2>
    <div class="muted" style="margin-top:-4px;margin-bottom:12px;">Partecipa ai regali con quote differenziali verso i diversi laureandi</div>
    <div class="card">
      <div style="display:flex;justify-content:space-between;"><b>Raccolto complessivo</b><b>${euro(totalCollected)} / ${euro(totalGoal)}</b></div>
      <div class="gbar"><span style="width:${overallPct}%"></span></div>
      <div class="muted">${contributions.length} contributi ricevuti</div>
    </div>
    ${targets.map((t) => {
      const pct = t.targetAmount > 0 ? Math.min(100, (t.collectedAmount / t.targetAmount) * 100) : 0;
      return `<div class="card gift">
        <div class="gname">${esc(t.name)}</div>
        <div class="grole">${esc(t.specialization)} · ${esc(t.roleTitle)}</div>
        <div class="gtitle">🎁 ${esc(t.giftTitle)}</div>
        <div class="muted" style="margin-top:4px;">${esc(t.giftDescription)}</div>
        <div class="gbar"><span style="width:${pct}%"></span></div>
        <div style="display:flex;justify-content:space-between;"><span class="muted">${euro(t.collectedAmount)} di ${euro(t.targetAmount)}</span><span class="muted">${pct.toFixed(0)}%</span></div>
        <div class="pay">
          <a class="btn btn-ghost" style="color:var(--on-surface)" data-iban="${esc(t.iban)}" data-holder="${esc(t.ibanHolder)}">IBAN</a>
          <a class="btn btn-ghost" style="color:#DC2626" href="${esc(t.satispayUrl)}" target="_blank" rel="noopener">Satispay</a>
          <a class="btn btn-ghost" style="color:#005FB0" href="${esc(t.paypalMeUrl)}" target="_blank" rel="noopener">PayPal</a>
          <button class="btn btn-gold" style="width:auto;padding:6px 12px;" data-cont="${esc(t.id)}">＋ Contribuisci</button>
        </div>
      </div>`;
    }).join("")}
  `;
  el.querySelectorAll("[data-iban]").forEach((a) => a.onclick = async (e) => {
    e.preventDefault();
    const iban = a.dataset.iban, holder = a.dataset.holder;
    try { await navigator.clipboard.writeText(iban); toast(`IBAN copiato (${esc(holder)})`); }
    catch { openModal(`<h3>IBAN</h3><div class="muted">Intestatario: ${esc(holder)}</div><div class="card" style="margin-top:10px;font-family:monospace;font-size:13px;word-break:break-all;">${esc(iban)}</div>`); }
  });
  el.querySelectorAll("[data-cont]").forEach((b) => b.onclick = () => contributionDialog(b.dataset.cont, targets, async () => gifts(el, goto)));
}

function contributionDialog(targetId, targets, onDone) {
  const t = targets.find((x) => x.id === targetId);
  const methods = ["IBAN", "Satispay", "PayPal", "Contanti"];
  openModal(`<h3>🎁 Contribuisci a ${esc(t.name)}</h3>
    <div class="muted" style="margin-bottom:10px;">${esc(t.giftTitle)}</div>
    <div class="field"><label>Tuo nome (opzionale)</label><input id="c-donor" placeholder="Lascia vuoto per anonimo"></div>
    <div class="field"><label>Importo (€) *</label><input id="c-amount" type="number" min="1" step="0.01" value="50"></div>
    <div class="field"><label>Metodo di pagamento</label><select id="c-method">${methods.map((m) => `<option>${m}</option>`).join("")}</select></div>
    <div class="field"><label>Messaggio di auguri</label><textarea id="c-note" rows="2" placeholder="es. Brindiamo a te stasera!"></textarea></div>
    <button class="btn btn-gold" id="c-save" style="margin-top:10px;">Registra contributo</button>`,
    (bg, close) => {
      bg.querySelector("#c-save").onclick = async () => {
        const amount = Number(bg.querySelector("#c-amount").value);
        if (!amount || amount <= 0) { toast("Inserisci un importo valido"); return; }
        const donor = bg.querySelector("#c-donor").value.trim();
        await Repo.addContribution({
          donorName: donor || "Un invitato generoso",
          targetGraduateId: t.id,
          targetGraduateName: t.name,
          amount,
          paymentMethod: bg.querySelector("#c-method").value,
          note: bg.querySelector("#c-note").value.trim(),
          isAnonymous: !donor
        });
        close(); toast("Contributo registrato ✓"); onDone();
      };
    });
}
