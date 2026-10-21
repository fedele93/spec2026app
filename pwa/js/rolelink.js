// Link personale e codice a 6 cifre per i ruoli (festeggiato, cassiere): la persona scrive la sua email, riceve
// il link (che apre l'area da solo) e il codice. Il codice serve nell'app aggiunta alla schermata Home dell'iPhone,
// dove il link dell'email si aprirebbe in Safari (memorie separate): email + codice → il server rende lo stesso
// token del link, che viene salvato su questo dispositivo. Vedi app/login_code.py nel backend.
import { api, setGraduateToken, setTreasurerToken } from "./api.js";
import { reconnect } from "./data.js";
import { toast, openModal, render, goto } from "./app.js";
import { isInstalledPwa } from "./notify.js";
import { maybeOpenRoleGuides } from "./guide.js";

const ROLES = {
  graduate: {
    title: "🎓 Sei un festeggiato?",
    intro: "Scrivi la tua email: se è quella registrata dagli organizzatori ti mandiamo il link che apre la tua area (chi viene, auguri per te, coordinate per i regali, il tuo avatar) e un codice.",
    prefix: "gl",
    request: (body) => api.graduateRequestLink(body),
    verify: (body) => api.graduateVerifyCode(body),
    save: setGraduateToken,
    after: async () => { const { openGraduateArea } = await import("./graduate.js"); openGraduateArea(); },
  },
  treasurer: {
    title: "👛 Sei un cassiere?",
    intro: "Scrivi la tua email: se è quella che gli organizzatori hanno registrato per te ti mandiamo il link che apre il cruscotto delle quote uniche e un codice.",
    prefix: "tl",
    request: (body) => api.treasurerRequestLink(body),
    verify: (body) => api.treasurerVerifyCode(body),
    save: setTreasurerToken,
    after: () => goto("gifts"),
  },
};

export function requestRoleLinkDialog(role) {
  const r = ROLES[role], p = r.prefix, installed = isInstalledPwa();
  openModal(`<h3>${r.title}</h3>
    <div class="muted" style="margin-bottom:10px;">${r.intro}</div>
    <div class="field"><label>La tua email *</label><input id="${p}-email" type="email" inputmode="email" autocomplete="email" placeholder="nome@esempio.it"></div>
    <button class="btn btn-primary" id="${p}-send">Inviami il link e il codice</button>
    <div class="muted as-hint" id="${p}-msg" style="margin-top:10px;"></div>
    <div class="muted as-hint">${installed ? "Dall'app installata entra con il codice: il link dell'email si aprirebbe nel browser, non qui." : "Apri il link dall'email su questo telefono: l'accesso si attiva da solo. Oppure usa il codice qui sotto."}</div>
    <div class="card" style="margin-top:12px;" id="${p}-code-card">
      <b style="font-size:13px;">🔢 Entra con il codice</b>
      <div class="muted" style="margin:4px 0 8px;">Il codice a 6 cifre è nell'email insieme al link (vale 15 minuti). Serve soprattutto a chi ha aggiunto l'app alla schermata Home dell'iPhone.</div>
      <div class="field"><label>Codice ricevuto</label><input id="${p}-code" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9 ]*" placeholder="123456"></div>
      <button class="btn btn-gold" id="${p}-verify">Entra</button>
    </div>`,
    (bg, close) => {
      const email = () => bg.querySelector(`#${p}-email`).value.trim();
      bg.querySelector(`#${p}-send`).onclick = async () => {
        if (!email().includes("@")) { toast("Inserisci un'email valida"); return; }
        const btn = bg.querySelector(`#${p}-send`); btn.disabled = true;
        try {
          const res = await r.request({ email: email() });
          bg.querySelector(`#${p}-msg`).textContent = res.message;
          if (res.queued) { toast("Controlla la posta"); bg.querySelector(`#${p}-code`).focus(); }
        } catch (e) { toast(e.message); }
        finally { btn.disabled = false; }
      };
      bg.querySelector(`#${p}-verify`).onclick = async () => {
        const code = bg.querySelector(`#${p}-code`).value.replace(/\D/g, "");
        if (!email().includes("@")) { toast("Scrivi prima la tua email"); return; }
        if (code.length !== 6) { toast("Il codice ha 6 cifre"); return; }
        const btn = bg.querySelector(`#${p}-verify`); btn.disabled = true;
        try {
          const res = await r.verify({ email: email(), code });
          r.save(res.token);
          close();
          await reconnect(); // il token cambia cosa manda il server: si ricarica tutto
          await render();
          toast("Accesso attivato su questo dispositivo ✓");
          await r.after();
          maybeOpenRoleGuides(); // guida del ruolo appena sbloccato (una volta sola)
        } catch (e) { toast(e.message); }
        finally { btn.disabled = false; }
      };
    });
}
