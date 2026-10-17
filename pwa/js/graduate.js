// Area del festeggiato (token GRADUATE_TOKEN_<ID> in ⚙️ Impostazioni, oppure il link personale
// https://<app>/#festeggiato=<token> mandato alla sua email; header X-Graduate-Token):
// chi ha confermato, gli auguri per lui, le sue coordinate di pagamento come le vedono gli invitati,
// il suo avatar dell'assistente. Nessuna cifra delle quote uniche: quelle restano ai cassieri.
import { api, setGraduateToken } from "./api.js";
import { toast, openModal, fmtTime, render } from "./app.js";
import { openAssistantAdmin } from "./assistant.js";

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const shortName = (n) => String(n || "").replace(/^Dott\.(ssa)?\s*/i, "");
const stat = (title, value, sub, color) => `<div class="card" style="flex:1;padding:12px;margin:0;"><div class="muted" style="font-size:11px;">${title}</div><div style="font-size:24px;font-weight:800;color:${color};">${value}</div><div class="muted" style="font-size:11px;">${sub}</div></div>`;

// Link personale del festeggiato aperto dall'email: salva il token (come quello in ⚙️) e pulisce l'indirizzo.
export function initGraduateFromHash() {
  const m = /[#&]festeggiato=([A-Za-z0-9_-]{10,})/.exec(location.hash || "");
  if (!m) return false;
  setGraduateToken(m[1]);
  try { history.replaceState(null, "", location.pathname + location.search); } catch { /* ignora */ }
  return true;
}

// Il festeggiato scrive la sua email e riceve il link che apre la sua area (niente token da copiare).
export function requestGraduateLinkDialog() {
  openModal(`<h3>🎓 Sei un festeggiato?</h3>
    <div class="muted" style="margin-bottom:10px;">Scrivi la tua email: se è quella registrata dagli organizzatori ti mandiamo il link che apre la tua area (chi viene, auguri per te, coordinate per i regali, il tuo avatar).</div>
    <div class="field"><label>La tua email *</label><input id="gl-email" type="email" inputmode="email" autocomplete="email" placeholder="nome@esempio.it"></div>
    <button class="btn btn-primary" id="gl-send">Inviami il link</button>
    <div class="muted as-hint" id="gl-msg" style="margin-top:10px;"></div>
    <div class="muted as-hint">Apri il link dall'email su questo telefono: l'area si apre da sola.</div>`,
    (bg) => {
      bg.querySelector("#gl-send").onclick = async () => {
        const email = bg.querySelector("#gl-email").value.trim();
        if (!email.includes("@")) { toast("Inserisci un'email valida"); return; }
        const btn = bg.querySelector("#gl-send"); btn.disabled = true;
        try {
          const r = await api.graduateRequestLink({ email });
          bg.querySelector("#gl-msg").textContent = r.message;
          if (r.queued) toast("Controlla la posta");
        } catch (e) { toast(e.message); }
        finally { btn.disabled = false; }
      };
    });
}

export async function openGraduateArea() {
  let me;
  try { me = await api.graduateMe(); }
  catch (e) {
    if (e.status === 403) { setGraduateToken(""); toast("Token festeggiato non valido: rimosso"); render(); return; }
    toast(e.message); return;
  }
  const g = me.graduate, gu = me.guests, bus = me.bus;
  const forMe = me.wishes.filter((w) => shortName(w.targetGraduate).toLowerCase() === shortName(g.name).toLowerCase());
  const forAll = me.wishes.filter((w) => !forMe.includes(w));
  const wish = (w) => `<div class="grad-wish"><div>${esc(w.message)}</div><div class="who">— ${esc(w.authorName)} · ${fmtTime(w.createdAt)}${w.heartCount ? ` · ❤️ ${w.heartCount}` : ""}</div></div>`;
  const avatar = me.avatar;
  const avatarPill = avatar ? `<span class="pill ${avatar.ready ? "pill-conf" : avatar.enabled ? "pill-pend" : "pill-decl"}">${avatar.ready ? "pronto" : avatar.enabled ? "abilitato, senza voce" : "disabilitato"}</span> <span class="pill pill-pend">voce ${esc(avatar.voiceKind)}</span>` : "";
  openModal(`<h3>🎓 Ciao ${esc(shortName(g.name).split(" ")[0])}!</h3>
    <div class="muted" style="margin-bottom:8px;">La tua area personale: chi viene alla festa, gli auguri che ti hanno scritto, le coordinate che gli invitati vedono per il regalo diretto e il tuo avatar dell'assistente.</div>
    <div class="grad-stats" id="grad-stats">
      ${stat("Confermati", gu.confirmedGuests, `${gu.covers} persone`, "var(--success, #166534)")}
      ${stat("In attesa", gu.pendingGuests, `${gu.pendingCovers} persone`, "#854D0E")}
      ${stat("Navetta", bus.bookedSeats, `di ${bus.maxSeats} posti`, "var(--primary)")}
    </div>

    <details class="card" id="grad-wishes" open><summary><b>💌 Auguri per te</b> <span class="muted">· ${forMe.length} per te, ${forAll.length} per tutti</span></summary>
      ${forMe.length ? forMe.map(wish).join("") : `<div class="muted" style="margin-top:6px;">Nessun augurio dedicato a te per ora: arriveranno!</div>`}
      ${forAll.length ? `<div class="muted" style="margin-top:8px;"><b>Per tutti i neo-specialisti</b></div>${forAll.slice(0, 5).map(wish).join("")}` : ""}
    </details>

    <details class="card" id="grad-guests"><summary><b>👥 Chi viene</b> <span class="muted">· ${gu.confirmedGuests} confermati</span></summary>
      ${gu.confirmed.length ? gu.confirmed.map((x) => `<div class="row"><div class="grow"><div class="name">${esc(x.fullName)}${x.guestsCount > 1 ? ` <span class="muted">(${x.guestsCount} persone)</span>` : ""}</div><div class="cat">${esc(x.category)}${x.companions.filter(Boolean).length ? ` · con ${esc(x.companions.filter(Boolean).join(", "))}` : ""}</div></div></div>`).join("") : `<div class="muted" style="margin-top:6px;">Nessuna conferma ancora.</div>`}
      ${gu.pending.length ? `<div class="muted" style="margin-top:8px;"><b>Ancora in attesa:</b> ${esc(gu.pending.join(", "))}</div>` : ""}
    </details>

    <details class="card" id="grad-pay"><summary><b>💳 Le tue coordinate</b> <span class="muted">· come le vedono gli invitati</span></summary>
      <div class="muted" style="margin-top:6px;"><b>Intestatario:</b> ${esc(g.ibanHolder)}</div>
      <div class="mono iban">${esc(g.iban)}</div>
      <div class="muted">PayPal: ${g.paypalMeUrl ? `<a href="${esc(g.paypalMeUrl)}" target="_blank" rel="noopener">${esc(g.paypalMeUrl)}</a>` : g.paypalEmail ? `<span class="mono">${esc(g.paypalEmail)}</span>` : "—"} · Satispay: ${g.satispayUrl ? `<a href="${esc(g.satispayUrl)}" target="_blank" rel="noopener">${esc(g.satispayUrl)}</a>` : "—"} · BANCOMAT Pay: ${g.bancomatPay ? `<span class="mono">${esc(g.bancomatPay)}</span>` : "—"}</div>
      <div class="muted as-hint">Per cambiarle chiedi agli organizzatori (file giftTargets dell'evento). Le quote uniche ai cassieri non sono mostrate qui.</div>
      ${g.email ? `<div class="muted as-hint">Ricevi un'email a <b>${esc(g.email)}</b> per ogni augurio dedicato a te.</div>` : `<div class="muted as-hint">Se gli organizzatori aggiungono la tua email sul server, ricevi un'email per ogni augurio dedicato a te.</div>`}
    </details>

    <div class="card" id="grad-avatar"><b>🎙️ Il tuo avatar dell'assistente</b> ${avatarPill}
      <div class="muted" style="margin:6px 0 8px;">${avatar ? (me.assistantConfigured ? "Scegli come parla il tuo avatar e registra il campione di voce (10 secondi): gli invitati potranno chiederti informazioni sulla festa con la tua voce." : "L'assistente vocale non è ancora attivo sul server (MISTRAL_API_KEY): puoi preparare la persona del tuo avatar.") : "Nessun avatar collegato al tuo id."}</div>
      ${avatar ? `<button class="btn btn-primary" id="grad-avatar-open">Configura il mio avatar</button>` : ""}
    </div>`,
    (bg, close) => {
      const b = bg.querySelector("#grad-avatar-open");
      if (b) b.onclick = () => { close(); openAssistantAdmin(); };
    });
}
