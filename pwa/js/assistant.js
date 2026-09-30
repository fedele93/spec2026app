// Assistente vocale con gli avatar dei neo-specialisti (backend: /api/assistant, Mistral).
// - pulsante rotondo in basso a sinistra su tutte le schermate (solo in modalità server e se il
//   server ha la chiave Mistral e almeno un avatar pronto);
// - pannello: scelta dell'avatar, "tieni premuto per parlare" (MediaRecorder), oppure testo;
//   la risposta arriva scritta e letta ad alta voce; le azioni (RSVP, navetta, auguri,
//   apertura sezione) le esegue il server e qui vengono solo mostrate/applicate;
// - pannello organizzatori: persona, abilitazione, voce preimpostata, campione vocale da
//   clonare (file o registrazione), riascolto, prova della voce.
import { api, fetchBlobUrl, isAdmin } from "./api.js";
import { Repo } from "./data.js";
import { toast, openModal, goto } from "./app.js";

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const LS_STATE = "np.assistant";
const SECTION_LABELS = { program: "Programma", rsvp: "Invitati", bus: "Navetta", wishes: "Bacheca auguri", gifts: "Regali" };

// Stato della conversazione (sopravvive alla chiusura del pannello e al ricaricamento della pagina)
const state = loadState();
let status = null; // ultimo /api/assistant/status
let avatars = [];
let player = null; // <audio> unico, fuori dal modale: la voce continua anche se il pannello si chiude

function loadState() {
  try { return { avatarId: null, history: [], messages: [], ...(JSON.parse(sessionStorage.getItem(LS_STATE) || "{}")) }; }
  catch { return { avatarId: null, history: [], messages: [] }; }
}
function saveState() { try { sessionStorage.setItem(LS_STATE, JSON.stringify({ avatarId: state.avatarId, history: state.history.slice(-12), messages: state.messages.slice(-30) })); } catch { /* ignora */ } }

// ---------------------------------------------------------------- audio helpers

export function pickRecorderMime() {
  if (typeof MediaRecorder === "undefined") return null;
  for (const m of ["audio/webm;codecs=opus", "audio/webm", "audio/mp4", "audio/ogg;codecs=opus", "audio/mpeg"]) {
    if (MediaRecorder.isTypeSupported(m)) return m;
  }
  return "";
}
const extForMime = (mime) => (mime || "").includes("mp4") ? "m4a" : (mime || "").includes("ogg") ? "ogg" : (mime || "").includes("mpeg") ? "mp3" : "webm";

// Registra dal microfono finché non viene chiamato stop(); risolve con {blob, mime, ms}.
async function startRecording() {
  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") throw new Error("Il microfono non è disponibile su questo browser: scrivi il messaggio");
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  const mime = pickRecorderMime();
  const rec = mime ? new MediaRecorder(stream, { mimeType: mime }) : new MediaRecorder(stream);
  const chunks = [];
  const started = Date.now();
  rec.ondataavailable = (e) => { if (e.data && e.data.size) chunks.push(e.data); };
  const done = new Promise((resolve) => {
    rec.onstop = () => {
      stream.getTracks().forEach((t) => t.stop());
      resolve({ blob: new Blob(chunks, { type: rec.mimeType || mime || "audio/webm" }), mime: rec.mimeType || mime, ms: Date.now() - started });
    };
  });
  rec.start(250);
  return { stop: () => { if (rec.state !== "inactive") rec.stop(); }, done };
}

function ensurePlayer() {
  if (!player) { player = document.createElement("audio"); player.id = "assistant-audio"; player.setAttribute("playsinline", ""); document.body.appendChild(player); }
  return player;
}
function play(base64, mime) {
  if (!base64) return;
  ensurePlayer();
  player.src = `data:${mime || "audio/mpeg"};base64,${base64}`;
  player.play().catch(() => { /* autoplay bloccato: resta la risposta scritta */ });
}
export function stopSpeaking() { if (player) { player.pause(); player.removeAttribute("src"); } }

// ---------------------------------------------------------------- pulsante flottante

export async function initAssistant() {
  const fab = document.getElementById("assistant-fab");
  if (Repo.mode !== "server") { if (fab) fab.remove(); return; }
  try { status = await api.assistantStatus(); } catch { status = null; }
  const on = !!(status && status.configured && status.avatars > 0);
  if (!on) { if (fab) fab.remove(); return; }
  if (fab) return;
  const b = document.createElement("button");
  b.id = "assistant-fab"; b.className = "as-fab"; b.title = "Parla con un festeggiato"; b.setAttribute("aria-label", "Assistente vocale");
  b.innerHTML = `<span class="as-fab-ico">🎙️</span><span class="as-fab-lbl">Chiedi</span>`;
  b.onclick = () => openAssistant();
  document.getElementById("app").appendChild(b);
}

// ---------------------------------------------------------------- pannello conversazione

export async function openAssistant() {
  try { avatars = await api.assistantAvatars(); } catch (e) { toast(e.message); return; }
  if (!avatars.length) { toast("Nessun avatar disponibile al momento"); return; }
  if (!avatars.some((a) => a.id === state.avatarId)) { state.avatarId = avatars[0].id; state.history = []; state.messages = []; }
  const current = () => avatars.find((a) => a.id === state.avatarId) || avatars[0];
  const canRecord = !!pickRecorderMime() && !!navigator.mediaDevices?.getUserMedia;

  openModal(`<h3>🎙️ Parla con un festeggiato</h3>
    <div class="chips" id="as-avatars">${avatars.map((a) => `<button class="chip ${a.id === state.avatarId ? "active" : ""}" data-avatar="${esc(a.id)}">${esc(a.shortName)}</button>`).join("")}</div>
    <div class="muted as-persona" id="as-persona"></div>
    <div class="as-chat" id="as-chat"></div>
    <div class="as-controls">
      ${canRecord ? `<button class="btn btn-primary as-mic" id="as-mic">🎤 Tieni premuto e parla</button>` : `<div class="muted">Su questo browser il microfono non è disponibile: scrivi qui sotto.</div>`}
      <div class="as-row"><input id="as-text" placeholder="…oppure scrivi qui" autocomplete="off"><button class="btn btn-gold" id="as-send">Invia</button></div>
      <div class="as-row-small"><button class="btn btn-ghost" id="as-stop">🔇 Silenzio</button><button class="btn btn-ghost" id="as-clear">🧹 Nuova conversazione</button></div>
    </div>
    <div class="muted as-hint">${status?.fake ? "Modalità simulata: le risposte sono finte (nessuna chiamata a Mistral)." : "Le registrazioni vengono trascritte da Mistral e non vengono conservate."}</div>`,
    (bg, close) => {
      const $ = (sel) => bg.querySelector(sel);
      const chat = $("#as-chat");
      let busy = false;

      const paintPersona = () => { const a = current(); $("#as-persona").textContent = `${a.name} · voce ${a.voiceKind}`; };
      const paint = () => {
        chat.innerHTML = state.messages.length ? state.messages.map((m) => `<div class="as-msg ${m.role}">
            <div class="as-bubble">${esc(m.text)}</div>
            ${m.actions?.length ? `<div class="as-actions">${m.actions.map((x) => `<span class="pill ${x.ok ? "pill-conf" : "pill-decl"}">${x.ok ? "✓" : "✗"} ${esc(x.summary || x.tool)}</span>`).join(" ")}</div>` : ""}
          </div>`).join("")
          : `<div class="muted as-empty">Ciao! Sono ${esc(current().shortName)}. Chiedimi del programma, degli orari, della navetta o dei regali; posso anche prenotarti la navetta o confermare la tua presenza.</div>`;
        chat.scrollTop = chat.scrollHeight;
      };
      const setBusy = (v, label) => {
        busy = v;
        const mic = $("#as-mic"); if (mic) { mic.disabled = v; mic.textContent = v ? (label || "⏳ Un attimo…") : "🎤 Tieni premuto e parla"; }
        $("#as-send").disabled = v; $("#as-text").disabled = v;
      };

      const send = async ({ text, audio, mime }) => {
        if (busy) return;
        setBusy(true, audio ? "⏳ Trascrivo e rispondo…" : "⏳ Rispondo…");
        if (text) { state.messages.push({ role: "user", text }); paint(); }
        try {
          const res = await api.assistantTalk({ avatarId: state.avatarId, text, audio, audioExt: extForMime(mime), history: state.history });
          if (audio) state.messages.push({ role: "user", text: res.userText || res.transcript || "(audio)" });
          state.messages.push({ role: "assistant", text: res.reply, actions: res.actions || [] });
          state.history = res.history || state.history;
          saveState(); paint();
          play(res.audio, res.audioMime);
          if (res.navigate) { goto(res.navigate); toast(`Sezione ${SECTION_LABELS[res.navigate] || res.navigate} aperta`); close(); }
        } catch (e) {
          toast(e.message || "Assistente non disponibile");
        } finally { setBusy(false); }
      };

      bg.querySelectorAll("[data-avatar]").forEach((c) => c.onclick = () => {
        if (busy || c.dataset.avatar === state.avatarId) return;
        bg.querySelectorAll("[data-avatar]").forEach((x) => x.classList.remove("active")); c.classList.add("active");
        state.avatarId = c.dataset.avatar; state.history = []; state.messages = []; saveState(); stopSpeaking(); paintPersona(); paint();
      });
      $("#as-send").onclick = () => { const t = $("#as-text").value.trim(); if (!t) return; $("#as-text").value = ""; send({ text: t }); };
      $("#as-text").onkeydown = (e) => { if (e.key === "Enter") { e.preventDefault(); $("#as-send").click(); } };
      $("#as-stop").onclick = stopSpeaking;
      $("#as-clear").onclick = () => { state.history = []; state.messages = []; saveState(); stopSpeaking(); paint(); };

      const mic = $("#as-mic");
      if (mic) {
        let rec = null;
        const start = async (e) => {
          e.preventDefault();
          if (busy || rec) return;
          stopSpeaking();
          try { rec = await startRecording(); }
          catch (err) { toast(err.name === "NotAllowedError" ? "Permesso microfono negato: consentilo dalle impostazioni del browser" : err.message); rec = null; return; }
          mic.classList.add("recording"); mic.textContent = "🔴 Sto ascoltando… rilascia per inviare";
        };
        const stop = async (e) => {
          if (!rec) return;
          e.preventDefault();
          const r = rec; rec = null;
          r.stop();
          mic.classList.remove("recording"); mic.textContent = "🎤 Tieni premuto e parla";
          const { blob, mime, ms } = await r.done;
          if (ms < 400 || blob.size < 100) { toast("Registrazione troppo corta: tieni premuto mentre parli"); return; }
          send({ audio: blob, mime });
        };
        mic.addEventListener("pointerdown", start);
        mic.addEventListener("pointerup", stop);
        mic.addEventListener("pointercancel", stop);
        mic.addEventListener("pointerleave", stop);
        mic.addEventListener("contextmenu", (e) => e.preventDefault());
      }
      paintPersona(); paint();
    });
}

// ---------------------------------------------------------------- pannello organizzatori

export async function openAssistantAdmin() {
  if (!isAdmin()) { toast("Serve il token organizzatore"); return; }
  let board, voices;
  try { [board, voices] = await Promise.all([api.assistantAdminAvatars(), api.assistantAdminVoices()]); }
  catch (e) { toast(e.message); return; }
  const voiceOptions = (sel) => `<option value="">Voce di riserva${board.fallbackVoiceId ? ` (${esc((voices.voices.find((v) => v.id === board.fallbackVoiceId) || {}).name || board.fallbackVoiceId)})` : ""}</option>`
    + voices.voices.map((v) => `<option value="${esc(v.id)}" ${v.id === sel ? "selected" : ""}>${esc(v.name)}${v.gender ? ` · ${esc(v.gender)}` : ""}${v.languages?.length ? ` · ${esc(v.languages.join(","))}` : ""}</option>`).join("");
  const kindPill = (a) => `<span class="pill ${a.ready ? "pill-conf" : a.enabled ? "pill-pend" : "pill-decl"}">${a.ready ? "pronto" : a.enabled ? "abilitato, senza voce" : "disabilitato"}</span> <span class="pill pill-pend">voce ${esc(a.voiceKind)}</span>`;
  const canRecord = !!pickRecorderMime() && !!navigator.mediaDevices?.getUserMedia;

  openModal(`<h3>🎙️ Assistente vocale · avatar</h3>
    <div class="muted" style="margin-bottom:10px;">${board.configured ? (board.fake ? "Server in modalità simulata (ASSISTANT_FAKE): niente chiamate a Mistral." : "Collegato a Mistral.") : "MISTRAL_API_KEY non configurata sul server: puoi preparare persona e abilitazioni, ma le voci e le conversazioni non funzionano."}
      Un avatar è selezionabile dagli invitati solo se abilitato e con una voce (clonata dal campione, preimpostata scelta o di riserva). Carica campioni solo con il consenso della persona: 5-15 secondi di parlato pulito, senza rumore.</div>
    ${board.avatars.map((a) => `<div class="card as-admin" data-id="${esc(a.id)}">
      <div class="as-admin-head"><b>${esc(a.name)}</b><span class="as-admin-status">${kindPill(a)}</span></div>
      <label class="as-check"><input type="checkbox" data-enabled ${a.enabled ? "checked" : ""}> Abilitato (selezionabile dagli invitati)</label>
      <div class="field"><label>Persona (come parla, cosa gli piace)</label><textarea data-persona rows="3">${esc(a.persona)}</textarea></div>
      <div class="field"><label>Voce preimpostata (se non c'è un campione)</label><select data-preset>${voiceOptions(a.presetVoiceId)}</select></div>
      <div class="btn-row"><button class="btn btn-primary" data-save>Salva</button><button class="btn btn-ghost" data-preview>🔊 Prova voce</button></div>
      <div class="field" style="margin-top:10px;"><label>Campione vocale ${a.sampleFilename ? `· <b>${esc(a.sampleFilename)}</b> (${a.sampleUploadedAt ? new Date(a.sampleUploadedAt).toLocaleDateString("it-IT") : ""})` : "· nessuno"}</label>
        <div class="btn-row" style="flex-wrap:wrap;">
          <label class="btn btn-ghost" style="width:auto;">📁 Carica file<input type="file" accept="audio/*" data-file hidden></label>
          ${canRecord ? `<button class="btn btn-ghost" data-record style="width:auto;">🎤 Registra 10 s</button>` : ""}
          ${a.sampleFilename ? `<button class="btn btn-ghost" data-listen style="width:auto;">▶ Riascolta</button><button class="btn btn-ghost" data-delete style="width:auto;">🗑 Elimina</button>` : ""}
        </div></div>
    </div>`).join("")}`,
    (bg, close) => {
      const refresh = () => { close(); openAssistantAdmin(); };
      bg.querySelectorAll(".as-admin").forEach((card) => {
        const id = card.dataset.id;
        const $ = (sel) => card.querySelector(sel);
        $("[data-save]").onclick = async () => {
          const patch = { enabled: $("[data-enabled]").checked, persona: $("[data-persona]").value.trim(), presetVoiceId: $("[data-preset]").value };
          try { const a = await api.assistantAdminUpdate(id, patch); card.querySelector(".as-admin-status").innerHTML = kindPill(a); toast(`${a.shortName} salvato${a.ready ? ": pronto" : ""}`); }
          catch (e) { toast(e.message); }
        };
        $("[data-preview]").onclick = async () => {
          try { const r = await api.assistantAdminPreview(id); play(r.audio, r.audioMime); toast(`Voce ${r.voiceKind}: "${r.text.slice(0, 40)}…"`); }
          catch (e) { toast(e.message); }
        };
        const upload = async (blob, filename) => {
          toast("Carico il campione e clono la voce…");
          try { const a = await api.assistantAdminUploadSample(id, blob, filename); toast(`Voce di ${a.shortName} clonata ✓`); refresh(); }
          catch (e) { toast(e.message); }
        };
        $("[data-file]").onchange = (e) => { const f = e.target.files[0]; if (f) upload(f, f.name); };
        const rb = $("[data-record]");
        if (rb) rb.onclick = async () => {
          let rec;
          try { rec = await startRecording(); } catch (e) { toast(e.message); return; }
          let left = 10;
          rb.disabled = true; rb.textContent = `🔴 ${left} s…`;
          const timer = setInterval(() => { left -= 1; rb.textContent = `🔴 ${left} s…`; if (left <= 0) { clearInterval(timer); rec.stop(); } }, 1000);
          const { blob, mime } = await rec.done;
          clearInterval(timer); rb.disabled = false; rb.textContent = "🎤 Registra 10 s";
          upload(blob, `campione.${extForMime(mime)}`);
        };
        const lb = $("[data-listen]");
        if (lb) lb.onclick = async () => {
          try { const url = await fetchBlobUrl(`/api/assistant/admin/avatars/${id}/sample`); const p = ensurePlayer(); p.src = url; p.play(); }
          catch (e) { toast(e.message); }
        };
        const db = $("[data-delete]");
        if (db) db.onclick = async () => {
          if (!confirm("Eliminare il campione e la voce clonata? L'avatar userà la voce preimpostata o di riserva.")) return;
          try { await api.assistantAdminDeleteSample(id); toast("Campione eliminato"); refresh(); } catch (e) { toast(e.message); }
        };
      });
    });
}
