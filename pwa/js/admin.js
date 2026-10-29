// Organizzatori (token X-Admin-Token): cruscotto con i numeri e le cose da fare, import della lista invitati.
import { api, downloadCsv } from "./api.js";
import { Repo } from "./data.js";
import { toast, openModal, goto } from "./app.js";
import { openMailAdmin, MENU_LABELS } from "./profile.js";
import { openAssistantAdmin } from "./assistant.js";

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const stat = (title, value, sub, color) => `<div class="card" style="flex:1;min-width:92px;padding:12px;margin:0;"><div class="muted" style="font-size:11px;">${title}</div><div style="font-size:22px;font-weight:800;color:${color};">${value}</div><div class="muted" style="font-size:11px;">${sub}</div></div>`;
const fmtDeadline = (iso) => { try { return new Date(iso + "T12:00:00").toLocaleDateString("it-IT", { day: "numeric", month: "long" }); } catch { return iso; } };

// ------------------------------------------------------------------ cruscotto
export async function openDashboard() {
  let d;
  try { d = await api.adminDashboard(); } catch (e) { toast(e.message); return; }
  const g = d.guests, b = d.bus, m = d.mail, p = d.pool;
  const mailTotal = Object.values(m.counts || {}).reduce((s, n) => s + n, 0);
  const todo = (t) => `<div class="row dash-todo"><div class="grow"><div class="name" style="font-size:13px;">${esc(t.text)}</div></div>
      ${t.action || t.section ? `<button class="btn btn-ghost" style="width:auto;padding:6px 10px;font-size:12px;" data-todo="${esc(t.key)}" data-action="${esc(t.action || "")}" data-section="${esc(t.section || "")}">Vai →</button>` : ""}</div>`;
  openModal(`<h3>📊 Cruscotto organizzatori</h3>
    <div class="muted" style="margin-bottom:8px;">Aggiornato ${new Date(d.generatedAt).toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" })}. Date limite: menu ${fmtDeadline(d.deadlines.menuDeadline)}${d.deadlines.menuLocked ? " (chiuso)" : ""}, navetta ${fmtDeadline(d.deadlines.busDeadline)}${d.deadlines.busLocked ? " (chiusa)" : ""}.</div>
    <div class="grad-stats" id="dash-stats" style="flex-wrap:wrap;">
      ${stat("Confermati", g.confirmed, `${g.covers} coperti`, "#166534")}
      ${stat("In attesa", g.pending, `${g.pendingCovers} persone · ${g.declined} no`, "#854D0E")}
      ${stat("Senza menu", g.missingMenu, `${g.allergies} allergie`, g.missingMenu ? "#991B1B" : "#166534")}
      ${stat("Navetta", b.bookedSeats, `di ${b.maxSeats} · ${b.waitlistSeats} in attesa · ${b.checkedInSeats} saliti${b.withoutGuest ? ` · <span style="color:#991B1B;font-weight:700;">${b.withoutGuest} senza invitato</span>` : ""}`, b.withoutGuest ? "#991B1B" : "var(--primary)")}
      ${stat("Profili", g.withProfile, `${g.withEmail} con email · ${g.consented} consensi`, "var(--primary)")}
      ${stat("Email", mailTotal, m.enabled ? `${m.counts.DELIVERED || 0} consegnate · ${m.counts.FAILED || 0} fallite` : "n8n non attivo", m.enabled ? "#166534" : "#991B1B")}
      ${stat("Quote uniche", p.contributions, `${p.pending} in attesa · ${p.received} ricevute (cifre solo ai cassieri)`, "var(--gold-dark)")}
      ${stat("Bacheca", d.wishes, `auguri · ${d.photos} foto · ${d.pushSubscriptions} push`, "var(--primary)")}
    </div>
    <details class="card" id="dash-todo" open><summary><b>✅ Cose da fare</b> <span class="muted">· ${d.todo.length}</span></summary>
      ${d.todo.length ? d.todo.map(todo).join("") : `<div class="muted" style="margin-top:6px;">Niente in sospeso: tutto a posto!</div>`}
    </details>
    <details class="card" id="dash-menu"><summary><b>🍽️ Menu scelti</b> <span class="muted">· ${g.byMenu.reduce((s, x) => s + x.covers, 0)} coperti</span></summary>
      <table class="tbl"><tbody>${g.byMenu.map((x) => `<tr><td>${esc(MENU_LABELS[x.menu] || x.menu || "Da scegliere")}</td><td class="num"><b>${x.covers}</b></td></tr>`).join("")}</tbody></table>
      <div class="muted as-hint">Assistente: ${d.assistant.configured ? `${d.assistant.ready} avatar pronti su ${d.assistant.avatars}` : "non configurato"} · invitati aggiornati negli ultimi 7 giorni: ${g.updatedLastWeek}</div>
    </details>
    <div class="btn-row" style="flex-wrap:wrap;margin-top:10px;">
      <button class="btn btn-ghost" id="dash-import" style="width:auto;">📥 Importa invitati</button>
      <button class="btn btn-ghost" id="dash-mail" style="width:auto;">✉️ Email</button>
      <button class="btn btn-ghost" id="dash-exp-guests" style="width:auto;">📄 CSV invitati</button>
      <button class="btn btn-ghost" id="dash-exp-bus" style="width:auto;">📄 CSV navetta</button>
      <button class="btn ${b.withoutGuest ? "btn-gold" : "btn-ghost"}" id="dash-orphans" style="width:auto;">🚌 Senza invitato (${b.withoutGuest || 0})</button>
      <button class="btn btn-ghost" id="dash-exp-pool" style="width:auto;">📄 CSV quote</button>
    </div>
    <details class="card" id="dash-invites"><summary><b>🎟️ Inviti a festeggiati e cassieri</b></summary>
      <div class="muted" style="margin:6px 0;">Manda a ciascuno il link personale (festeggiati: area 🎓; cassieri: cruscotto delle quote) con il codice a 6 cifre e le istruzioni dettagliate per usare l'app. Le email vengono dal <span class="mono">.env</span> del server (<span class="mono">GRADUATE_EMAIL_&lt;ID&gt;</span>, <span class="mono">TREASURER_EMAIL_&lt;ID&gt;</span>) o dal campo email dei festeggiati; i link restano gli stessi a ogni invio, il codice è nuovo ogni volta (vale 15 minuti). Il pulsante accanto a un nome rimanda l'email solo a quella persona.</div>
      <div id="inv-list" class="muted">Caricamento…</div>
      <label class="as-check" style="margin-top:8px;"><input type="checkbox" id="inv-test" checked> Aggiungi le istruzioni per la fase di prova (segnalare i bug; i dati di prova verranno cancellati)</label>
      <label class="as-check"><input type="checkbox" id="inv-rotate"> Rigenera i link prima di inviarli (quelli già in giro smettono di valere: usalo se un'email è finita alla persona sbagliata)</label>
      <button class="btn btn-primary" id="inv-send" style="margin-top:8px;" disabled>Invia gli inviti</button>
    </details>
    <details class="card" id="dash-reset"><summary><b>🧹 Fine della fase di prova</b></summary>
      <div class="muted" style="margin:6px 0;">Cancella invitati e profili, navetta, auguri, foto, quote uniche, notifiche ed email in coda. Restano la configurazione dell'evento, IBAN e link dei regali, gli accessi dei festeggiati, gli avatar e le iscrizioni alle notifiche. Non si può annullare: fai prima un backup (<span class="mono">scripts/backup.sh</span>).</div>
      <button class="btn btn-ghost" id="reset-go" style="color:#991B1B;">Azzera i dati di prova…</button>
    </details>`,
    (bg, close) => {
      const act = (action, section) => {
        close();
        if (action === "mail" || action === "remind_menu") openMailAdmin();
        else if (action === "bus_orphans") openBusOrphans();
        else if (action === "import") openImportDialog();
        else if (action === "assistant") openAssistantAdmin();
        else if (section) goto(section);
      };
      bg.querySelectorAll("[data-todo]").forEach((b) => b.onclick = () => act(b.dataset.action, b.dataset.section));
      bg.querySelector("#dash-import").onclick = () => { close(); openImportDialog(); };
      bg.querySelector("#dash-mail").onclick = () => { close(); openMailAdmin(); };
      bg.querySelector("#dash-exp-guests").onclick = () => downloadCsv("guests").then(() => toast("invitati.csv scaricato")).catch((e) => toast(e.message));
      bg.querySelector("#dash-exp-bus").onclick = () => downloadCsv("bus").then(() => toast("navetta.csv scaricato")).catch((e) => toast(e.message));
      bg.querySelector("#dash-orphans").onclick = () => { close(); openBusOrphans(); };
      bg.querySelector("#dash-exp-pool").onclick = () => downloadCsv("gift-pool").then(() => toast("quote-uniche.csv scaricato")).catch((e) => toast(e.message));
      initInvites(bg);
      bg.querySelector("#reset-go").onclick = async () => {
        const word = prompt("Questo cancella TUTTI i dati raccolti (invitati, navetta, auguri, foto, quote). Per confermare scrivi AZZERA");
        if (!word) return;
        try {
          const r = await api.adminResetTestData(word);
          const n = r.deleted;
          toast(`Dati di prova cancellati: ${n.guests} invitati, ${n.busBookings} prenotazioni, ${n.wishes} auguri, ${n.photos} foto, ${n.poolContributions} quote`);
          close(); await Repo.pollState();
        } catch (e) { toast(e.message); }
      };
    });
}

// ------------------------------------------------------------------ navetta: prenotazioni senza invitato
// Nate dal vecchio modulo libero o inserite a mano: il server propone gli invitati dal nome simile, ma
// il collegamento lo decide sempre l'organizzatore con un tocco (mai automatico).
const RSVP_SHORT = { CONFIRMED: "confermato", PENDING: "in attesa", DECLINED: "non viene" };

export async function openBusOrphans() {
  let rows;
  try { rows = await api.adminBusOrphans(); } catch (e) { toast(e.message); return; }
  const sugg = (o, s) => `<div class="row" style="padding:6px 0;"><div class="grow"><div class="name" style="font-size:13px;">${esc(s.fullName)} ${s.score === 2 ? '<span class="pill pill-conf">stesso nome</span>' : '<span class="pill pill-pend">simile</span>'}</div>
      <div class="cat">${esc(s.category)} · ${RSVP_SHORT[s.rsvpStatus] || esc(s.rsvpStatus)} · ${s.guestsCount} ${s.guestsCount === 1 ? "persona" : "persone"}${s.hasEmail ? " · ✉️" : ""}${s.hasBooking ? " · ha già una prenotazione" : ""}</div></div>
      <button class="btn btn-ghost" style="width:auto;padding:6px 10px;font-size:12px;" data-link="${o.id}" data-guest="${s.id}" data-name="${esc(s.fullName)}" ${s.hasBooking ? "disabled" : ""}>Collega</button></div>`;
  const row = (o) => `<div class="card orphan" data-orphan="${o.id}">
      <div class="name">🚌 ${esc(o.passengerName)} <span class="muted">· ${o.seatsCount} ${o.seatsCount === 1 ? "posto" : "posti"} · ${esc(o.code || "")}${o.status === "WAITLIST" ? " · lista d'attesa" : ""}</span></div>
      <div class="cat">${o.pickupStop ? `Fermata: ${esc(o.pickupStop)}` : ""}${o.contactPhone ? ` · ☎ <a href="tel:${esc(o.contactPhone.replace(/\s+/g, ""))}">${esc(o.contactPhone)}</a>` : ""}${o.notes ? ` · ${esc(o.notes)}` : ""}</div>
      ${o.suggestions.length ? `<div class="muted" style="margin-top:6px;">Invitati con un nome simile:</div>${o.suggestions.map((s) => sugg(o, s)).join("")}` : `<div class="muted" style="margin-top:6px;">Nessun invitato con un nome simile.</div>`}
      <button class="btn btn-ghost" style="margin-top:6px;" data-create="${o.id}" data-name="${esc(o.passengerName)}">➕ Crea l'invitato "${esc(o.passengerName)}" (in attesa) e collega</button>
    </div>`;
  openModal(`<h3>🚌 Prenotazioni senza invitato</h3>
    <div class="muted" style="margin-bottom:10px;">Passeggeri prenotati che non risultano fra gli invitati (il catering non li conta). Per ciascuno: collegalo a un invitato esistente o crea l'invitato dalla prenotazione. Chi ha l'email riceve la conferma con il codice. Da ora la navetta si prenota solo dal profilo, quindi di nuove non ne nascono (salvo quelle inserite a mano da voi).</div>
    <div id="orphans">${rows.length ? rows.map(row).join("") : `<div class="muted">Nessuna: ogni passeggero è anche un invitato ✓</div>`}</div>`,
    (bg) => {
      const done = (id, msg) => { toast(msg); const card = bg.querySelector(`[data-orphan="${id}"]`); if (card) card.remove(); if (!bg.querySelector(".orphan")) bg.querySelector("#orphans").innerHTML = `<div class="muted">Tutto collegato ✓</div>`; Repo.pollState(); };
      bg.querySelectorAll("[data-link]").forEach((b) => b.onclick = async () => {
        if (!confirm(`Collegare questa prenotazione a ${b.dataset.name}?`)) return;
        b.disabled = true;
        try { const r = await api.adminBusLink(Number(b.dataset.link), Number(b.dataset.guest)); done(b.dataset.link, r.mail ? `Collegata a ${b.dataset.name}: conferma inviata ✓` : `Collegata a ${b.dataset.name} ✓`); }
        catch (e) { toast(e.message); b.disabled = false; }
      });
      bg.querySelectorAll("[data-create]").forEach((b) => b.onclick = async () => {
        if (!confirm(`Creare l'invitato "${b.dataset.name}" (in attesa di conferma) e collegargli la prenotazione?`)) return;
        b.disabled = true;
        try { await api.adminBusCreateGuest(Number(b.dataset.create)); done(b.dataset.create, `Invitato "${b.dataset.name}" creato e collegato ✓`); }
        catch (e) { toast(e.message); b.disabled = false; }
      });
    });
}

const INVITE_STATUS = { READY: "pronto", QUEUED: "in invio", DELIVERED: "inviato", FAILED: "non inviato", NO_EMAIL: "manca l'email", MAIL_DISABLED: "email non attive (n8n)" };

// Elenco di chi riceverebbe l'invito (anteprima, nessuna email) e invio con conferma.
async function initInvites(bg) {
  const list = bg.querySelector("#inv-list"), send = bg.querySelector("#inv-send");
  let preview;
  // una riga per persona; il pulsante "Invia" rimanda link e codice solo a lei (es. codice scaduto o email persa)
  const row = (r) => `<div class="row"><div class="grow"><div class="name" style="font-size:13px;">${r.role === "graduate" ? "🎓" : "👛"} ${esc(r.name)}</div>
      <div class="cat">${esc(r.email || "—")}</div></div><span class="pill ${["READY", "QUEUED", "DELIVERED"].includes(r.status) ? "pill-conf" : "pill-pend"}">${esc(INVITE_STATUS[r.status] || r.status)}</span>
      <button class="btn btn-ghost" style="width:auto;padding:6px 10px;font-size:12px;margin-left:6px;" data-inv-one="${esc(r.id)}" data-inv-role="${esc(r.role)}" data-inv-name="${esc(r.name)}" ${preview && preview.mailEnabled && r.email ? "" : "disabled"} title="Rimanda link e codice solo a questa persona">✉️ Invia</button></div>`;
  const options = () => ({ testPhase: bg.querySelector("#inv-test").checked, rotate: bg.querySelector("#inv-rotate").checked });
  const confirmRotate = () => !bg.querySelector("#inv-rotate").checked || confirm("Rigenerare i link? Chi ha già aperto il vecchio link dovrà aprire quello nuovo.");
  const wireRows = () => bg.querySelectorAll("[data-inv-one]").forEach((b) => b.onclick = async () => {
    if (!confirm(`Mandare link e codice a ${b.dataset.invName}?`) || !confirmRotate()) return;
    b.disabled = true;
    try {
      const r = await api.adminInvites({ dryRun: false, roles: [b.dataset.invRole + "s"], ids: [b.dataset.invOne], ...options() });
      const sent = r.invites.find((x) => x.id === b.dataset.invOne && x.role === b.dataset.invRole);
      toast(sent && ["QUEUED", "DELIVERED"].includes(sent.status) ? `Email mandata a ${b.dataset.invName} ✓` : `Non inviata: ${INVITE_STATUS[sent?.status] || sent?.status || "errore"}`);
    } catch (e) { toast(e.message); }
    finally { b.disabled = false; }
  });
  try { preview = await api.adminInvites({ dryRun: true }); }
  catch (e) { list.textContent = e.message; return; }
  list.innerHTML = preview.invites.map(row).join("") + (preview.mailEnabled ? "" : `<div class="muted as-hint">Le email non sono attive (N8N_WEBHOOK_URL): gli inviti non possono partire.</div>`);
  wireRows();
  send.disabled = !preview.mailEnabled || !preview.ready;
  send.textContent = `Invia a tutti (${preview.ready})`;
  send.onclick = async () => {
    if (!confirm(`Mandare l'invito con il link personale e il codice a ${preview.ready} persone?`) || !confirmRotate()) return;
    send.disabled = true;
    try {
      const r = await api.adminInvites({ dryRun: false, ...options() });
      list.innerHTML = r.invites.map(row).join("");
      wireRows();
      toast(`Inviti mandati: ${r.ready}`);
    } catch (e) { toast(e.message); send.disabled = false; }
  };
}

// ------------------------------------------------------------------ import invitati
// Colonne riconosciute dall'intestazione (maiuscole e accenti non contano); senza intestazione: Nome;Categoria;Email;Telefono;Persone;Stato
const COLUMNS = {
  fullName: ["nome", "nome e cognome", "nominativo", "invitato", "name"],
  category: ["categoria", "gruppo", "category"],
  email: ["email", "e-mail", "mail", "posta"],
  phone: ["telefono", "cellulare", "tel", "phone"],
  guestsCount: ["persone", "posti", "coperti", "n", "numero", "guests"],
  rsvpStatus: ["stato", "rsvp", "status"],
};
const STATUS_WORDS = { confermato: "CONFIRMED", confirmed: "CONFIRMED", si: "CONFIRMED", sì: "CONFIRMED", "in attesa": "PENDING", attesa: "PENDING", pending: "PENDING", declinato: "DECLINED", declined: "DECLINED", no: "DECLINED" };
const norm = (s) => String(s || "").trim().toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "");

export function parseGuestRows(text) {
  const lines = String(text || "").split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  if (!lines.length) return { rows: [], header: null };
  const sep = [";", "\t", ","].map((s) => [s, (lines[0].match(new RegExp(s === "\t" ? "\t" : `\\${s}`, "g")) || []).length]).sort((a, b) => b[1] - a[1])[0][0];
  const split = (l) => l.split(sep).map((c) => c.trim().replace(/^"(.*)"$/, "$1"));
  const first = split(lines[0]).map(norm);
  const known = Object.entries(COLUMNS);
  const map = first.map((h) => (known.find(([, names]) => names.includes(h)) || [null])[0]);
  const hasHeader = map.some(Boolean) && !first.some((h) => h.includes("@"));
  const order = hasHeader ? map : ["fullName", "category", "email", "phone", "guestsCount", "rsvpStatus"];
  const rows = lines.slice(hasHeader ? 1 : 0).map((l) => {
    const cells = split(l), row = {};
    order.forEach((key, i) => { if (!key || cells[i] === undefined || cells[i] === "") return;
      if (key === "guestsCount") { const n = parseInt(cells[i], 10); if (!Number.isNaN(n)) row.guestsCount = n; }
      else if (key === "rsvpStatus") { const st = STATUS_WORDS[norm(cells[i])]; if (st) row.rsvpStatus = st; }
      else row[key] = cells[i];
    });
    if (!row.fullName && !row.email) return null;
    return { fullName: row.fullName || "", ...row };
  }).filter(Boolean);
  return { rows, header: hasHeader ? order : null };
}

export function openImportDialog() {
  if (Repo.mode !== "server") { toast("L'import funziona solo con il server della festa"); return; }
  openModal(`<h3>📥 Importa la lista invitati</h3>
    <div class="muted" style="margin-bottom:8px;">Incolla qui le righe da Excel o da un CSV (una persona per riga, colonne Nome, Categoria, Email, Telefono, Persone, Stato: l'intestazione viene riconosciuta), oppure carica il file. Chi c'è già viene aggiornato (abbinato per email o per nome), gli altri vengono creati in attesa di risposta.</div>
    <textarea id="im-text" rows="6" placeholder="Nome;Categoria;Email;Telefono;Persone&#10;Maria Rossi;Familiari;maria@esempio.it;333 1234567;2" style="width:100%;"></textarea>
    <div class="btn-row" style="margin-top:8px;"><label class="btn btn-ghost" style="width:auto;">📁 Carica CSV<input type="file" accept=".csv,.txt,.tsv,text/csv,text/plain" id="im-file" hidden></label><button class="btn btn-primary" id="im-preview" style="width:auto;">Anteprima</button></div>
    <label class="as-check" style="margin-top:8px;"><input type="checkbox" id="im-links"> Invia il link personale via email a chi ha l'indirizzo</label>
    <div id="im-result" class="muted as-hint" style="margin-top:8px;"></div>
    <button class="btn btn-gold" id="im-go" style="margin-top:8px;" disabled>Importa</button>`,
    (bg, close) => {
      const $ = (sel) => bg.querySelector(sel);
      let rows = [];
      $("#im-file").onchange = async (e) => { const f = e.target.files[0]; if (f) { $("#im-text").value = await f.text(); $("#im-preview").click(); } };
      $("#im-preview").onclick = async () => {
        rows = parseGuestRows($("#im-text").value).rows;
        if (!rows.length) { $("#im-result").textContent = "Nessuna riga riconosciuta: controlla separatori e intestazione."; $("#im-go").disabled = true; return; }
        try {
          const d = await api.importGuests(rows, { sendLinks: $("#im-links").checked, dryRun: true });
          const invites = d.rows.filter((r) => r.willInvite).length;
          $("#im-result").innerHTML = `<b>Anteprima:</b> ${d.created} da creare, ${d.updated} da aggiornare, ${d.unchanged} già uguali, ${d.skipped} senza nome${$("#im-links").checked ? `, ${invites} email con il link` : ""}.<br>` +
            d.rows.slice(0, 8).map((r) => `${esc(r.fullName || "(senza nome)")}: ${r.action === "created" ? "nuovo" : r.action === "updated" ? "aggiorna " + r.changes.join(", ") : r.action === "skipped" ? "saltato" : "uguale"}`).join(" · ") + (d.rows.length > 8 ? ` · … (${d.rows.length} righe)` : "");
          $("#im-go").disabled = !(d.created || d.updated || invites);
          $("#im-go").textContent = `Importa ${rows.length} righe`;
        } catch (e) { toast(e.message); }
      };
      $("#im-go").onclick = async () => {
        $("#im-go").disabled = true;
        try {
          const d = await api.importGuests(rows, { sendLinks: $("#im-links").checked });
          toast(`Import: ${d.created} creati, ${d.updated} aggiornati${d.invited ? `, ${d.invited} link inviati` : ""}${d.inviteFailed ? `, ${d.inviteFailed} email non inviate` : ""}`);
          await Repo.pollState();
          close();
          goto("rsvp");
        } catch (e) { toast(e.message); $("#im-go").disabled = false; }
      };
    });
}
