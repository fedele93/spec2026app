// Guida alla prima apertura: pochi passi con le funzioni dell'app e come usarle.
// Si apre da sola la prima volta (localStorage np.guideSeen), poi dal pulsante "?" nell'intestazione
// e da ⚙️ Impostazioni. I passi dipendono da ciò che il server offre (assistente, profilo).
import { Repo } from "./data.js";
import { openModal } from "./app.js";

const LS_SEEN = "np.guideSeen";
const GUIDE_VERSION = "2"; // alza il numero per rimostrare la guida a tutti dopo novità importanti

export function guideSeen() { try { return localStorage.getItem(LS_SEEN) === GUIDE_VERSION; } catch { return true; } }
export function markGuideSeen() { try { localStorage.setItem(LS_SEEN, GUIDE_VERSION); } catch { /* ignora */ } }

function steps({ serverMode, assistant }) {
  const s = [
    { icon: "🎓", title: "Benvenuto a NeuroParty", text: "Questa è l'app della festa di specializzazione in Neurologia. In pochi passi vedi cosa puoi fare: tutto è nelle cinque schede in basso.", tip: "Puoi riaprire questa guida quando vuoi dal pulsante ? in alto." },
    { icon: "📅", title: "Programma e mappa", text: "Nella scheda Programma trovi seduta, festa, orari e i punti di ritrovo sulla mappa, con il pulsante per aprire il navigatore e quello per aggiungere gli eventi al calendario del telefono.", tip: "Attiva le notifiche per ricevere gli aggiornamenti degli organizzatori (orari, partenza navetta)." },
  ];
  if (serverMode) {
    s.push({ icon: "👤", title: "Il tuo profilo", text: "Nella scheda Invitati crea il tuo profilo con la tua email: ricevi un link personale (niente password) e da lì confermi la partecipazione, indichi chi viene con te, il menu di ciascuno e le allergie. A ogni modifica arriva un'email di conferma.", tip: "Il link è personale: non inoltrarlo. Puoi uscire dal profilo quando vuoi." });
    s.push({ icon: "🚌", title: "Navetta gratuita", text: "Dal tuo profilo prenoti i posti sulla navetta scegliendo la fermata: ricevi la conferma con il codice e l'evento per il calendario. Se i posti sono finiti entri in lista d'attesa e vieni avvisato appena se ne libera uno.", tip: "Alla partenza basta dire il tuo nome o mostrare il codice della prenotazione." });
  } else {
    s.push({ icon: "👥", title: "Invitati e navetta", text: "Nella scheda Invitati confermi la presenza e il numero di persone; nella scheda Navetta prenoti i posti sul pullman scegliendo la fermata.", tip: "In questa modalità i dati restano solo su questo dispositivo." });
  }
  s.push({ icon: "🎉", title: "Auguri e foto", text: "Nella scheda Auguri lasci un messaggio ai neo-specialisti (anche a uno in particolare) e condividi le foto della giornata; i cuori e i like sono per tutti.", tip: "Le foto vengono ridimensionate automaticamente: puoi caricarle anche dal telefono." });
  s.push({ icon: "🎁", title: "Regali", text: "Nella scheda Regali ogni neo-specialista ha IBAN, PayPal e Satispay per un regalo diretto, senza registrare nulla. Chi preferisce un solo versamento per tutti registra una quota unica a uno dei cassieri, scegliendo come ripartirla.", tip: "Non vengono mostrate cifre raccolte: ognuno decide liberamente." });
  if (assistant) {
    s.push({ icon: "🎙️", title: "Chiedi a un festeggiato", text: "Il pulsante Chiedi in basso a sinistra apre l'assistente vocale: scegli un neo-specialista, tieni premuto per parlare (o scrivi) e lui ti risponde a voce. Può dirti orari e luoghi, i posti sulla navetta, confermare la tua presenza o prenotarti la navetta.", tip: "Prima di ogni azione ti chiede conferma." });
  }
  return s;
}

export function openGuide(opts = {}) {
  const list = steps({ serverMode: Repo.mode === "server", assistant: !!document.getElementById("assistant-fab"), ...opts });
  let i = 0;
  openModal(`<div class="guide" id="guide">
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
        bg.querySelector("#guide-body").innerHTML = `<div class="guide-icon">${st.icon}</div><h3 class="guide-title">${st.title}</h3><p class="guide-text">${st.text}</p>${st.tip ? `<div class="guide-tip">💡 ${st.tip}</div>` : ""}`;
        bg.querySelector("#guide-dots").innerHTML = list.map((_, k) => `<span class="${k === i ? "on" : ""}"></span>`).join("");
        bg.querySelector("#guide-prev").style.visibility = i === 0 ? "hidden" : "visible";
        bg.querySelector("#guide-next").textContent = i === list.length - 1 ? "Inizia! 🎉" : "Avanti →";
        bg.querySelector("#guide-skip").style.visibility = i === list.length - 1 ? "hidden" : "visible";
      };
      const finish = () => { markGuideSeen(); close(); };
      bg.querySelector("#guide-prev").onclick = () => { if (i > 0) { i -= 1; paint(); } };
      bg.querySelector("#guide-next").onclick = () => { if (i < list.length - 1) { i += 1; paint(); } else finish(); };
      bg.querySelector("#guide-skip").onclick = finish;
      bg.querySelector(".close").addEventListener("click", markGuideSeen);
      paint();
    });
}

// Alla prima apertura (non quando si arriva da un link personale: prima il profilo).
export function maybeOpenGuide({ fromLink = false } = {}) {
  if (fromLink || guideSeen()) return false;
  openGuide();
  return true;
}
