// Guide dell'app.
// 1) Guida alla prima apertura: cosa fa ogni scheda e da dove cominciare. Si apre da sola la prima
//    volta (localStorage np.guideSeen), poi dal pulsante "?" nell'intestazione e da ⚙️ Impostazioni.
//    I passi dipendono da ciò che il server offre (profilo personale, assistente).
// 2) Guide dei ruoli: quando in ⚙️ Impostazioni si salva un token che il server riconosce
//    (organizzatore, cassiere, festeggiato) si apre una volta sola la guida di quel ruolo, con le
//    funzioni appena sbloccate e dove trovarle (localStorage np.roleGuideSeen.<ruolo>). Si riapre da
//    ⚙️ Impostazioni. I ruoli attivi arrivano dal server (campo "roles" di GET /api/state).
import { Repo, Status, GIFT_COLLECTORS } from "./data.js";
import { openModal } from "./app.js";

const LS_SEEN = "np.guideSeen";
const LS_ROLE = "np.roleGuideSeen."; // + ruolo
const GUIDE_VERSION = "3"; // alza il numero per rimostrare la guida a tutti dopo novità importanti

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const shortName = (n) => String(n || "").replace(/^Dott\.(ssa)?\s*/i, "");

export function guideSeen() { try { return localStorage.getItem(LS_SEEN) === GUIDE_VERSION; } catch { return true; } }
export function markGuideSeen() { try { localStorage.setItem(LS_SEEN, GUIDE_VERSION); } catch { /* ignora */ } }
export function roleGuideSeen(role) { try { return localStorage.getItem(LS_ROLE + role) === "1"; } catch { return true; } }
export function markRoleGuideSeen(role) { try { localStorage.setItem(LS_ROLE + role, "1"); } catch { /* ignora */ } }

// ------------------------------------------------------------------ guida generale
function steps({ serverMode, assistant }) {
  const s = [
    { icon: "🎓", title: "Benvenuto a NeuroParty", text: "Questa è l'app della festa di specializzazione in Neurologia: programma, conferma della presenza, navetta, auguri, foto e regali, tutto in un posto. Non serve un account: l'app riconosce questo telefono e ti chiede solo il nome.", tip: "Aggiungila alla schermata Home dal menu del browser (\"Installa app\" o \"Aggiungi a Home\"): si apre come un'app normale e funziona anche offline con gli ultimi dati." },
    { icon: "🧭", title: "Come è fatta", text: "Le cinque schede in basso sono tutto quello che c'è: Programma, Invitati, Navetta, Auguri e Regali. In alto a destra trovi la campanella con gli avvisi degli organizzatori, il pulsante ? per riaprire questa guida e ⚙️ per le impostazioni.", tip: "La barra sotto l'intestazione compare solo se sei offline o in modalità demo." },
    { icon: "📅", title: "Programma e mappa", text: "In Programma vedi la seduta di specializzazione e la festa con orari e luoghi. Ogni punto sulla mappa ha il pulsante Naviga, che apre Google Maps o Apple Maps, e c'è il pulsante per aggiungere gli eventi al calendario del telefono.", tip: "Attiva le notifiche dalla campanella: così ricevi i cambi di orario e l'avviso di partenza della navetta." },
  ];
  if (serverMode) {
    s.push({ icon: "👤", title: "Conferma la presenza", text: "In Invitati crei il tuo profilo con nome ed email: ricevi un link personale (niente password) e da lì confermi la partecipazione, indichi chi viene con te e il menu di ciascuno, con allergie e intolleranze. A ogni modifica arriva un'email di conferma.", tip: "Il link è personale: non inoltrarlo. Se lo perdi, dalla scheda Invitati te ne fai mandare uno nuovo." });
    s.push({ icon: "🚌", title: "Navetta gratuita", text: "Dal tuo profilo prenoti i posti sulla navetta e scegli la fermata: ricevi la conferma con il codice e l'evento per il calendario. Se i posti sono finiti entri in lista d'attesa e ti avvisiamo appena se ne libera uno.", tip: "Alla partenza basta dire il tuo nome o mostrare il codice della prenotazione." });
  } else {
    s.push({ icon: "👥", title: "Invitati e navetta", text: "In Invitati confermi la presenza e il numero di persone; in Navetta prenoti i posti sul pullman scegliendo la fermata.", tip: "In questa modalità i dati restano solo su questo dispositivo." });
  }
  s.push({ icon: "🎉", title: "Auguri e foto", text: serverMode
    ? "In Auguri lasci un messaggio ai neo-specialisti, a tutti o a uno in particolare, e condividi le foto della giornata. Auguri e foto li vede solo chi è registrato: apri il link personale ricevuto via email (vale anche se non vieni) e firmi col tuo nome."
    : "In Auguri lasci un messaggio ai neo-specialisti, a tutti o a uno in particolare, e condividi le foto della giornata.", tip: "Le foto vengono ridimensionate in automatico: puoi caricarle direttamente dal telefono." });
  s.push({ icon: "🎁", title: "Regali", text: "In Regali ci sono due strade. Regalo diretto: ogni neo-specialista ha IBAN, PayPal e, dove c'è, Satispay; copi e versi, senza registrare nulla. Quota unica: un solo versamento a uno dei cassieri, che lo ripartisce fra i neo-specialisti come indichi tu (in parti uguali o personalizzato).", tip: "Nessuna cifra raccolta viene mostrata: ognuno decide liberamente. Se lasci l'email, il cassiere ti conferma quando la quota è arrivata." });
  if (assistant) {
    s.push({ icon: "🎙️", title: "Chiedi a un festeggiato", text: "Il pulsante Chiedi in basso a sinistra apre l'assistente vocale: scegli un neo-specialista, tieni premuto per parlare (o scrivi) e ti risponde con la sua voce. Sa orari e luoghi, i posti sulla navetta, può confermare la tua presenza o prenotarti la navetta; con il profilo aperto cambia a voce anche menu e accompagnatori.", tip: "Prima di ogni azione ti chiede conferma." });
  }
  s.push({ icon: "🔑", title: "Hai un ruolo nella festa?", text: "Organizzatori, cassieri delle quote uniche e neo-specialisti ricevono un token personale. Inseriscilo in ⚙️ Impostazioni: si sbloccano le funzioni del tuo ruolo (cruscotto, quote, area festeggiato) e l'app ti spiega come usarle.", tip: "Se sei un invitato non ti serve nessun token: puoi ignorare quei campi. I neo-specialisti possono anche farsi mandare il link della loro area via email (⚙️ → Sono un festeggiato)." });
  s.push({ icon: "✅", title: "Da dove cominciare", text: serverMode
    ? "1. In Invitati crea il tuo profilo e conferma la presenza. 2. Dal profilo prenota la navetta, se ti serve. 3. Attiva le notifiche dalla campanella. Il resto, auguri, foto e regali, quando vuoi."
    : "1. In Invitati conferma la presenza. 2. In Navetta prenota i posti, se ti servono. 3. Lascia un augurio ai neo-specialisti.", tip: "Puoi riaprire questa guida quando vuoi dal pulsante ? in alto." });
  return s;
}

// ------------------------------------------------------------------ guide dei ruoli
export const ROLE_LABELS = { admin: "organizzatore", treasurer: "cassiere", graduate: "festeggiato" };

function roleSteps(role, ctx) {
  const { assistant } = ctx;
  const roles = Status.roles || {};
  if (role === "admin") {
    return [
      { icon: "🗂️", title: "Sei un organizzatore", text: "Il token organizzatore è attivo su questo dispositivo. In ⚙️ Impostazioni ora trovi il cruscotto, l'import degli invitati, le esportazioni, le email e gli avatar dell'assistente; nelle schede compaiono i comandi riservati (invio notifiche, cancellazioni).", tip: "Il token vale solo su questo telefono: su un altro dispositivo va reinserito. Non condividerlo con gli invitati." },
      { icon: "📊", title: "Cruscotto e cose da fare", text: "Il cruscotto riassume in tempo reale confermati, menu ancora da scegliere, posti sulla navetta e lista d'attesa, email non consegnate e il numero di quote uniche registrate. Sotto, la lista delle cose da fare con le scadenze.", tip: "Apri il cruscotto da ⚙️ Impostazioni → Apri il cruscotto." },
      { icon: "📥", title: "Importa gli invitati", text: "Incolla la lista (nome, email, telefono) copiata da Excel, Google Fogli o WhatsApp: l'app crea i profili e, se vuoi, manda a ciascuno l'email con il link personale. Chi esiste già viene aggiornato, non duplicato.", tip: "Prima usa \"Solo anteprima\": vedi cosa verrebbe creato senza salvare né inviare nulla." },
      { icon: "🔔", title: "Notifiche e pulizia", text: "Dalla campanella invii una notifica push a tutti (cambi di orario, partenza della navetta, foto). In Auguri puoi eliminare foto e messaggi inopportuni e svuotare la cronologia delle notifiche; in Invitati e Navetta puoi cancellare qualsiasi record.", tip: "Le cancellazioni sono definitive: l'app chiede conferma ogni volta." },
      { icon: "📄", title: "Esportazioni ed email", text: "In ⚙️ Impostazioni scarichi i CSV di invitati (con menu e allergie, per il ristorante) e delle prenotazioni navetta (per l'autista). Da \"Apri le email\" controlli le email inviate, reinvii quelle non consegnate e mandi i promemoria per menu e navetta.", tip: "I CSV si aprono con Excel o Google Fogli." },
      { icon: "🔒", title: "Cosa non vedi", text: "Le cifre delle quote uniche restano ai cassieri: tu vedi solo quante quote sono state registrate. Gli elenchi completi di invitati e navetta sono visibili a organizzatori, cassieri e neo-specialisti; gli invitati vedono solo i propri dati.", tip: "Riapri questa guida da ⚙️ Impostazioni → Guida del tuo ruolo." },
    ];
  }
  if (role === "treasurer") {
    const ids = roles.treasurerIds || [];
    const names = ids.map((id) => GIFT_COLLECTORS.find((c) => c.id === id)).filter(Boolean).map((c) => shortName(c.name));
    const who = names.length ? `le quote affidate a ${names.join(" e ")}` : "le quote uniche di tutti i cassieri";
    return [
      { icon: "👛", title: "Sei un cassiere", text: `Il token cassiere è attivo: in Regali, sotto il blocco della quota unica, compare il cruscotto con ${who}: chi ha registrato una quota, quanto, con quale metodo e per quali neo-specialisti.`, tip: "Il token vale solo su questo telefono. Ogni cassiere vede solo le proprie quote: il token organizzatore non basta." },
      { icon: "✅", title: "Segna le quote ricevute", text: "Quando un versamento arriva (bonifico, PayPal o contanti) segnalo come ricevuto: chi ha lasciato l'email riceve la conferma in automatico. Puoi selezionarne più d'una e segnarle in blocco, e aggiungere un appunto a ciascuna (es. \"contanti consegnati in reparto\").", tip: "Accanto a ogni quota ci sono telefono ed email del donatore: un tocco per chiamare o scrivere." },
      { icon: "⏰", title: "Promemoria", text: "Il pulsante Promemoria manda un'email a chi ha registrato una quota ma non l'ha ancora versata, con IBAN e causale. Puoi mandarlo quando vuoi, ad esempio una settimana prima della festa.", tip: "Riceve il promemoria solo chi ha lasciato l'email." },
      { icon: "📒", title: "Ripartizione ed Excel", text: "Per ogni neo-specialista il cruscotto mostra il totale da girargli, calcolato dalle quote ricevute. In ⚙️ Impostazioni scarichi il CSV con una riga per quota e una colonna per neo-specialista: si apre con Excel.", tip: "Il CSV ti arriva anche ogni settimana per email, se gli organizzatori hanno attivato l'automazione." },
      { icon: "👥", title: "Elenchi completi", text: "Con il token vedi anche l'elenco completo degli invitati e delle prenotazioni della navetta, utile per riconoscere chi ti consegna i contanti. Le cifre delle quote invece le vedi solo tu: non organizzatori, non neo-specialisti, non invitati.", tip: "Riapri questa guida da ⚙️ Impostazioni → Guida del tuo ruolo." },
    ];
  }
  // festeggiato
  const name = (ctx.name || "").split(" ")[0];
  const s = [
    { icon: "🎓", title: name ? `Ciao ${name}, questa è la tua area` : "Sei un neo-specialista", text: "Il token festeggiato è attivo: in alto compare il pulsante 🎓 con la tua area personale, aggiornata in tempo reale. Qui sotto cosa ci trovi.", tip: "Il token vale solo su questo telefono. Non condividerlo." },
    { icon: "👥", title: "Chi viene", text: "Quanti hanno confermato, gli accompagnatori, i menu scelti e chi ha prenotato la navetta. In Invitati e Navetta vedi anche gli elenchi completi, che agli invitati non sono visibili.", tip: "Chi manca all'appello? Puoi chiedere agli organizzatori di mandargli il link personale." },
    { icon: "💌", title: "Gli auguri per te", text: "I messaggi indirizzati a te, con i cuori ricevuti; quelli per tutti restano in Auguri. Puoi rispondere a voce o di persona alla festa.", tip: "I messaggi e le foto li possono eliminare solo gli organizzatori." },
    { icon: "💳", title: "Le tue coordinate", text: "IBAN, PayPal e Satispay come li vedono gli invitati per il regalo diretto. Controlla che siano giusti: se c'è da correggere qualcosa, avvisa gli organizzatori (si cambiano nel file dell'evento).", tip: "Le quote uniche sono riservate ai cassieri: niente cifre né elenco dei donatori, nemmeno per te." },
  ];
  if (assistant) s.push({ icon: "🎙️", title: "Il tuo avatar", text: "Dalla tua area configuri il tuo avatar dell'assistente vocale: la persona (come si presenta), la voce preimpostata o un campione della tua voce da clonare, e puoi provarlo.", tip: "Riapri questa guida da ⚙️ Impostazioni → Guida del tuo ruolo." });
  else s[s.length - 1].tip += " Riapri questa guida da ⚙️ Impostazioni → Guida del tuo ruolo.";
  return s;
}

// ------------------------------------------------------------------ rendering
function showSteps(list, { id = "guide", role = "", onFinish }) {
  let i = 0;
  openModal(`<div class="guide" id="${id}" ${role ? `data-role="${esc(role)}"` : ""}>
      <div class="guide-body" id="guide-body"></div>
      <div class="guide-dots" id="guide-dots"></div>
      <div class="btn-row">
        <button class="btn btn-ghost" id="guide-prev">← Indietro</button>
        <button class="btn btn-primary" id="guide-next">Avanti →</button>
      </div>
      <button class="guide-skip" id="guide-skip">Salta la guida</button>
    </div>`,
    (bg, close) => {
      const paint = () => {
        const st = list[i];
        bg.querySelector("#guide-body").innerHTML = `<div class="guide-icon">${st.icon}</div><h3 class="guide-title">${esc(st.title)}</h3><p class="guide-text">${esc(st.text)}</p>${st.tip ? `<div class="guide-tip">💡 ${esc(st.tip)}</div>` : ""}`;
        bg.querySelector("#guide-dots").innerHTML = list.map((_, k) => `<span class="${k === i ? "on" : ""}"></span>`).join("");
        bg.querySelector("#guide-prev").style.visibility = i === 0 ? "hidden" : "visible";
        bg.querySelector("#guide-next").textContent = i === list.length - 1 ? (role ? "Ho capito ✓" : "Inizia! 🎉") : "Avanti →";
        bg.querySelector("#guide-skip").style.visibility = i === list.length - 1 ? "hidden" : "visible";
      };
      let done = false;
      const finish = () => { if (done) return; done = true; close(); if (onFinish) onFinish(); };
      bg.querySelector("#guide-prev").onclick = () => { if (i > 0) { i -= 1; paint(); } };
      bg.querySelector("#guide-next").onclick = () => { if (i < list.length - 1) { i += 1; paint(); } else finish(); };
      bg.querySelector("#guide-skip").onclick = finish;
      bg.querySelector(".close").addEventListener("click", finish);
      bg.addEventListener("click", (e) => { if (e.target === bg) finish(); });
      paint();
    });
}

const hasAssistant = () => !!document.getElementById("assistant-fab");

export function openGuide(opts = {}) {
  const list = steps({ serverMode: Repo.mode === "server", assistant: hasAssistant(), ...opts });
  showSteps(list, { onFinish: () => { markGuideSeen(); maybeOpenRoleGuides(); } });
}

// Guida di un ruolo (organizzatore, cassiere, festeggiato); al termine apre quelle degli altri ruoli
// attivi non ancora viste, se chainNext è true.
export async function openRoleGuide(role, { chainNext = false } = {}) {
  if (!ROLE_LABELS[role]) return;
  const name = role === "graduate" ? await graduateName((Status.roles || {}).graduate) : "";
  if (chainNext && document.querySelector(".modal-bg")) return; // nel frattempo l'utente ha aperto altro: riproposta al prossimo avvio
  showSteps(roleSteps(role, { assistant: hasAssistant(), name }), { role, onFinish: () => { markRoleGuideSeen(role); if (chainNext) maybeOpenRoleGuides(); } });
}

// Nome del festeggiato dal suo id (roles.graduate è l'id, es. "luisi"): dai regali, che hanno id e nome.
async function graduateName(id) {
  if (typeof id !== "string" || !id) return "";
  try { const t = (await Repo.allGiftTargets()).find((x) => x.id === id); if (t) return shortName(t.name); } catch { /* offline */ }
  return "";
}

// Ruoli attivi su questo dispositivo, solo se confermati dal server (campo roles di GET /api/state:
// admin e treasurer booleani, graduate = id del festeggiato o null). Senza conferma (server vecchio,
// offline, modalità demo) nessuna guida di ruolo: un token sbagliato non deve "consumarla".
export function activeRoles() {
  const r = Status.roles;
  if (!r || Repo.mode !== "server" || !Status.online) return [];
  return ["admin", "treasurer", "graduate"].filter((k) => !!r[k]);
}

// Apre la guida del primo ruolo attivo non ancora spiegato (una alla volta, in catena).
export function maybeOpenRoleGuides() {
  if (document.querySelector(".modal-bg")) return false;
  const role = activeRoles().find((r) => !roleGuideSeen(r));
  if (!role) return false;
  openRoleGuide(role, { chainNext: true });
  return true;
}

// Alla prima apertura (non quando si arriva da un link personale: prima il profilo).
// Se la guida è già stata vista, apre eventuali guide dei ruoli attivi non ancora viste
// (es. token inserito prima di questo aggiornamento).
export function maybeOpenGuide({ fromLink = false } = {}) {
  if (fromLink) return false;
  if (guideSeen()) return maybeOpenRoleGuides();
  openGuide();
  return true;
}
