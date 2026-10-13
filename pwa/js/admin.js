// Organizzatori (token X-Admin-Token): cruscotto con i numeri e le cose da fare, import della lista invitati.
import { api, downloadCsv } from "./api.js";
import { Repo } from "./data.js";
import { toast, openModal, goto } from "./app.js";
import { openMailAdmin, MENU_LABELS } from "./profile.js";
import { openAssistantAdmin } from "./assistant.js";

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const eur = (v) => `${Number(v || 0).toFixed(2).replace(".", ",")} €`;
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
      ${stat("Navetta", b.bookedSeats, `di ${b.maxSeats} · ${b.waitlistSeats} in attesa · ${b.checkedInSeats} saliti`, "var(--primary)")}
      ${stat("Profili", g.withProfile, `${g.withEmail} con email · ${g.consented} consensi`, "var(--primary)")}
      ${stat("Email", mailTotal, m.enabled ? `${m.counts.DELIVERED || 0} consegnate · ${m.counts.FAILED || 0} fallite` : "n8n non attivo", m.enabled ? "#166534" : "#991B1B")}
      ${stat("Quote uniche", p.contributions, `${p.pending} in attesa · ${eur(p.receivedAmount)} ricevuti`, "var(--gold-dark)")}
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
      <button class="btn btn-ghost" id="dash-exp-pool" style="width:auto;">📄 CSV quote</button>
    </div>`,
    (bg, close) => {
      const act = (action, section) => {
        close();
        if (action === "mail" || action === "remind_menu") openMailAdmin();
        else if (action === "import") openImportDialog();
        else if (action === "assistant") openAssistantAdmin();
        else if (section) goto(section);
      };
      bg.querySelectorAll("[data-todo]").forEach((b) => b.onclick = () => act(b.dataset.action, b.dataset.section));
      bg.querySelector("#dash-import").onclick = () => { close(); openImportDialog(); };
      bg.querySelector("#dash-mail").onclick = () => { close(); openMailAdmin(); };
      bg.querySelector("#dash-exp-guests").onclick = () => downloadCsv("guests").then(() => toast("invitati.csv scaricato")).catch((e) => toast(e.message));
      bg.querySelector("#dash-exp-bus").onclick = () => downloadCsv("bus").then(() => toast("navetta.csv scaricato")).catch((e) => toast(e.message));
      bg.querySelector("#dash-exp-pool").onclick = () => downloadCsv("gift-pool").then(() => toast("quote-uniche.csv scaricato")).catch((e) => toast(e.message));
    });
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
    <textarea id="im-text" rows="6" placeholder="Nome;Categoria;Email;Telefono;Persone&#10;Maria Rossi;Famigliari;maria@esempio.it;333 1234567;2" style="width:100%;"></textarea>
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
