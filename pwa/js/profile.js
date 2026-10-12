// Profilo personale dell'invitato (backend: /api/profile, link via email).
// - il link https://<app>/#profilo=<token> salva il token sul dispositivo e apre il profilo;
// - senza token: modulo "inserisci la tua email" (e il nome se non sei in lista);
// - con token: RSVP, contatti, accompagnatori con menu, consenso; salvando arriva l'email di conferma.
import { api, getGuestToken, setGuestToken } from "./api.js";
import { Repo } from "./data.js";
import { toast, openModal, render } from "./app.js";

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
export const MENU_LABELS = { "": "Da scegliere", STANDARD: "Menu completo", VEGETARIANO: "Vegetariano", VEGANO: "Vegano", SENZA_GLUTINE: "Senza glutine", ALTRO: "Altro (vedi note)" };
const CATEGORIES = ["Famigliari", "Colleghi Reparto", "Docenti e Reparto", "Specializzandi 1° e 2° anno", "Amici Università", "Invitato"];
const PRIVACY = "I dati (nome, contatti, menu e allergie, persone al seguito) servono solo a organizzare la festa, sono visibili agli organizzatori e al catering e vengono cancellati dopo l'evento. Puoi modificarli o chiederne la cancellazione in qualsiasi momento.";

export const hasProfile = () => !!getGuestToken();

// Link personale aperto dall'email: salva il token, pulisce l'indirizzo e apre il profilo.
export async function initProfileFromHash() {
  const m = /[#&]profilo=([A-Za-z0-9_-]{10,})/.exec(location.hash || "");
  if (!m) return false;
  setGuestToken(m[1]);
  try { history.replaceState(null, "", location.pathname + location.search); } catch { /* ignora */ }
  return true;
}

export async function openProfile() {
  if (Repo.mode !== "server") { toast("Il profilo personale funziona solo con il server della festa"); return; }
  if (!getGuestToken()) { requestLinkDialog(); return; }
  let data;
  try { data = await api.profileGet(); }
  catch (e) {
    if (e.status === 401) { setGuestToken(""); toast(e.message); requestLinkDialog(); return; }
    toast(e.message); return;
  }
  profileDialog(data.guest);
}

function requestLinkDialog() {
  openModal(`<h3>👤 Il mio profilo</h3>
    <div class="muted" style="margin-bottom:10px;">Con il profilo confermi la partecipazione, indichi chi viene con te e il menu di ciascuno, e ricevi le conferme via email. Niente password: ti mandiamo un link personale.</div>
    <div class="field"><label>La tua email *</label><input id="pl-email" type="email" inputmode="email" autocomplete="email" placeholder="nome@esempio.it"></div>
    <div class="field"><label>Nome e cognome (solo se non sei ancora in lista)</label><input id="pl-name" autocomplete="name" placeholder="es. Maria Rossi"></div>
    <button class="btn btn-primary" id="pl-send">Inviami il link</button>
    <div class="muted as-hint" id="pl-msg" style="margin-top:10px;"></div>
    <div class="muted as-hint">Hai già ricevuto il link? Aprilo dall'email: il profilo si apre da solo.</div>`,
    (bg) => {
      bg.querySelector("#pl-send").onclick = async () => {
        const email = bg.querySelector("#pl-email").value.trim(), fullName = bg.querySelector("#pl-name").value.trim();
        if (!email.includes("@")) { toast("Inserisci un'email valida"); return; }
        const btn = bg.querySelector("#pl-send"); btn.disabled = true;
        try {
          const r = await api.profileRequestLink({ email, fullName });
          bg.querySelector("#pl-msg").textContent = r.message;
          if (r.queued) toast("Email inviata: controlla la posta");
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

function profileDialog(g) {
  const firstTime = !g.consentAt;
  openModal(`<h3>👤 Il mio profilo</h3>
    <div class="muted" style="margin-bottom:10px;">Ciao ${esc(g.fullName)}! Qui trovi la tua partecipazione, chi viene con te e il menu. Salvando ricevi un'email di conferma.</div>
    <div class="field"><label>Partecipazione</label>
      <div class="chips" id="pf-status">${[["CONFIRMED", "✅ Ci sarò"], ["PENDING", "🤔 Non so ancora"], ["DECLINED", "❌ Non posso"]].map(([k, v]) => `<button class="chip ${g.rsvpStatus === k ? "active" : ""}" data-st="${k}">${v}</button>`).join("")}</div></div>
    <div class="field"><label>Nome e cognome *</label><input id="pf-name" value="${esc(g.fullName)}"></div>
    <div class="field"><label>Chi sei per i festeggiati</label><select id="pf-cat">${CATEGORIES.map((c) => `<option ${g.category === c ? "selected" : ""}>${c}</option>`).join("")}${CATEGORIES.includes(g.category) ? "" : `<option selected>${esc(g.category)}</option>`}</select></div>
    <div class="as-row"><div class="field" style="flex:1;"><label>Email</label><input id="pf-email" type="email" value="${esc(g.email || "")}"></div><div class="field" style="flex:1;"><label>Telefono</label><input id="pf-phone" type="tel" value="${esc(g.phone || "")}"></div></div>
    <div id="pf-food">
      <div class="field"><label>Il tuo menu</label><select id="pf-menu">${Object.entries(MENU_LABELS).map(([k, v]) => `<option value="${k}" ${k === (g.menu || "") ? "selected" : ""}>${v}</option>`).join("")}</select></div>
      <div class="field"><label>Allergie o note per la cucina</label><input id="pf-diet" value="${esc(g.dietaryNotes || "")}" placeholder="es. allergia alle noci"></div>
      <div class="field"><label>Chi viene con te</label><div id="pf-comps">${(g.companions || []).map(companionRow).join("")}</div>
        <button class="btn btn-ghost" id="pf-add" style="margin-top:6px;">＋ Aggiungi una persona</button></div>
      <div class="muted" id="pf-count" style="margin-bottom:8px;"></div>
    </div>
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
      $("#pf-logout").onclick = async () => {
        if (!confirm("Uscire dal profilo su questo dispositivo? Il link ricevuto non varrà più: potrai chiederne uno nuovo con la tua email.")) return;
        try { await api.profileLogout(); } catch { /* token già non valido */ }
        setGuestToken(""); close(); render(); toast("Sei uscito dal profilo");
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
      <div class="as-row" style="margin-top:8px;"><input id="ml-to" type="email" placeholder="Email di prova a…"><button class="btn btn-ghost" id="ml-test">Invia prova</button></div></div>
    <b style="font-size:13px;">Ultime email</b>
    ${box.length ? box.map((m) => `<div class="row"><div class="grow"><div class="name" style="font-size:13px;">${esc(m.to)} ${pill(m.status)}</div><div class="cat">${esc(st.kinds[m.kind] || m.kind)} · ${esc(m.subject)} · ${new Date(m.createdAt).toLocaleString("it-IT", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })}${m.error ? ` · <span style="color:var(--error)">${esc(m.error)}</span>` : ""}</div></div>${m.status !== "DELIVERED" ? `<button class="btn btn-ghost" style="width:auto;padding:6px 10px;font-size:12px;" data-resend="${m.id}">↻</button>` : ""}</div>`).join("") : `<div class="muted">Nessuna email.</div>`}`,
    (bg, close) => {
      bg.querySelector("#ml-test").onclick = async () => {
        const to = bg.querySelector("#ml-to").value.trim(); if (!to.includes("@")) { toast("Inserisci un'email"); return; }
        try { const r = await api.mailTest(to); toast(r.status === "DELIVERED" || r.status === "QUEUED" ? "Email di prova inviata a n8n" : `Non inviata: ${r.error}`); close(); openMailAdmin(); } catch (e) { toast(e.message); }
      };
      bg.querySelectorAll("[data-resend]").forEach((b) => b.onclick = async () => {
        try { const r = await api.mailResend(Number(b.dataset.resend)); toast(r.status === "DELIVERED" ? "Consegnata ✓" : `Stato: ${r.status}`); close(); openMailAdmin(); } catch (e) { toast(e.message); }
      });
    });
}
