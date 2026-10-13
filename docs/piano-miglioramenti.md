# Piano di miglioramento NeuroParty

Data: 12 ottobre 2026. Riguarda i due repository `neuroparty-backend` (API) e `spec2026app`
(PWA e app Android). Stato di partenza: regali senza cifre con quote uniche a due cassieri,
assistente vocale con avatar nella PWA, notifiche push, export CSV, automazioni n8n.

Nota sulla richiesta: "un'urgenza per ogni invitato" è stata interpretata come **un'utenza per
ogni invitato**, cioè un profilo personale leggero (senza password) a cui agganciare RSVP,
menu e navetta e a cui inviare le email di conferma.

## 1. Obiettivi

1. Prenotazione della navetta affidabile, con **conferma via email** e promemoria.
2. Raccolta del **menu e delle preferenze alimentari** per ogni persona (invitato e
   accompagnatori), con conferma via email e riepilogo per il catering.
3. **Un profilo per ogni invitato**: una sola pagina "Il mio profilo" dove si trovano RSVP,
   persone al seguito, menu e navetta, raggiungibile da un link personale.
4. Più facilità d'uso per i tre ruoli: invitati, festeggiati, cassieri.

## 2. Dove siamo oggi (cosa limita)

| Area | Oggi | Problema |
|---|---|---|
| Identità | nessun account: ogni telefono ha un `X-Client-Id` casuale | chi cambia telefono o usa PWA e APK non ritrova i propri dati; nessun modo di contattare l'invitato |
| RSVP | record `guests` con nome, categoria, stato, numero persone, esigenze alimentari in testo libero | il catering deve interpretare testi liberi; gli accompagnatori non hanno nome né menu |
| Navetta | prenotazione separata dall'RSVP (nome ridigitato), nessuna conferma, nessun promemoria | doppie prenotazioni, nomi diversi fra liste, nessuna certezza che l'invitato si presenti |
| Email | nessuna; esistono push (solo chi le attiva) e webhook verso n8n | le push non arrivano a tutti e non lasciano traccia |
| Cassieri | cruscotto e CSV con token per cassiere | niente promemoria ai donatori in attesa, niente contatto rapido |
| Festeggiati | nessuna area dedicata | non vedono chi viene, gli auguri ricevuti, la propria voce dell'assistente |

## 3. Architettura proposta

### 3.1 Profilo invitato (utenza leggera, senza password)

- Tabella `guests` estesa: `email`, `phone`, `access_token` (casuale, 32 caratteri),
  `token_expires_at`, `consent_at` (privacy), `companions` (JSON: nome e menu per ogni
  accompagnatore), `menu` strutturato (`STANDARD`, `VEGETARIANO`, `VEGANO`, `SENZA_GLUTINE`,
  `ALTRO` + note allergie), `bus_seats_wanted`, `bus_pickup_stop`, `bus_return`.
- **Accesso con link personale** (magic link): l'invitato inserisce la propria email, riceve
  un link `https://.../#/profilo/<token>`; aprendolo la PWA salva il token sul dispositivo
  (`X-Guest-Token`) e da quel momento scrive sul proprio record. Il link vale anche per
  aprire il profilo su un altro telefono. Gli organizzatori possono generare il link dalla
  lista invitati e inviarlo (email, WhatsApp, QR stampato sull'invito).
- Un record = un invitato; gli accompagnatori stanno dentro il record. Le vecchie scritture
  basate su `X-Client-Id` restano valide per compatibilità con l'APK 1.6.

### 3.2 Email tramite n8n (già configurato)

L'invio delle email resta **in n8n**, che è già collegato al backend: nessun SMTP nel backend,
nessuna dipendenza nuova. Il backend prepara il messaggio completo e lo passa a n8n con un
evento dedicato; n8n lo spedisce con il nodo Send Email già configurato.

- Nuovo evento webhook `mail.send` (stesso `N8N_WEBHOOK_URL` e stesso segreto degli altri
  eventi) con il messaggio pronto: `to`, `subject`, `text`, `html`, `replyTo`, `kind`
  (`access_link`, `rsvp_confirmation`, `bus_confirmation`, `bus_reminder`, `pool_confirmation`,
  `pool_reminder`) e `attachments` (es. il file `.ics` della navetta, in base64). Così i
  modelli delle email vivono nel backend (testabili, in italiano, con i dati della festa) e
  n8n fa solo da postino.
- In n8n: nel workflow `avvisi-in-tempo-reale` un ramo `case 'mail.send'` (o un nodo IF
  sull'evento) che passa `to`, `subject`, `text`, `html` e gli allegati al nodo Send Email;
  il mittente e le credenziali restano quelli già impostati. Il repo del backend fornisce il
  workflow aggiornato da importare.
- Tracciamento: tabella `mail_outbox` nel backend (destinatario, tipo, stato, tentativi,
  esito della consegna a n8n, data) alimentata dal risultato del webhook; gli organizzatori
  vedono nel report e nella PWA le email non consegnate e possono rimandarle.
- Promemoria programmati (navetta 24 ore prima, menu mancante, quote in attesa): generati dal
  backend con lo scheduler già esistente, sempre come eventi `mail.send`; in alternativa un
  workflow n8n a orario che legge `GET /api/automation/report` e decide a chi scrivere.
- Email "di sistema" per gli organizzatori (nuovo RSVP, prenotazione, quota): restano gli
  eventi di oggi, nessuna modifica.

### 3.3 Navetta

- Prenotazione agganciata al profilo: posti proposti = persone dell'RSVP, fermata e ritorno
  nel profilo; una sola prenotazione per profilo, modificabile fino a una data limite.
- **Lista d'attesa** quando i 54 posti sono esauriti, con promozione automatica (ed email) se
  si libera un posto.
- Email di conferma con codice prenotazione e `.ics`; promemoria il giorno prima con orario
  definitivo; cancellazione dal link.
- Per l'autista e gli organizzatori: export CSV già esistente esteso con email e telefono,
  **check-in alla partenza** nella PWA (spunta per nome o scansione del QR del codice).

### 3.4 Menu e preferenze alimentari

- Menu scelto per ogni persona (invitato e accompagnatori) da un elenco chiuso più note
  libere per allergie; data limite per le modifiche, dopo la quale il profilo mostra il menu in
  sola lettura e rimanda agli organizzatori.
- Riepilogo per il catering (già presente) rifatto sulla struttura: conteggi per menu e per
  tavolo/categoria, elenco allergie con nomi, export CSV.
- Email di conferma del menu insieme all'RSVP; promemoria a chi non ha ancora scelto.

### 3.5 Facilità d'uso

**Invitati**
- Percorso in tre passi al primo accesso: chi sei e contatti → chi porti e menu → navetta.
- Pagina "Il mio profilo" con stato a colpo d'occhio (RSVP, menu, navetta, quota unica se
  registrata) e un solo pulsante "Modifica".
- L'assistente vocale legge e aggiorna il profilo ("ho prenotato 3 posti sulla navetta?",
  "cambia il mio menu in vegetariano") con gli strumenti già esistenti estesi al profilo.
- Link personale nell'invito cartaceo (QR) e nelle email: niente da cercare.

**Festeggiati**
- Area "Festeggiato" con token personale (`GRADUATE_TOKEN_<ID>`, stesso schema dei cassieri):
  chi ha confermato, auguri ricevuti, le proprie coordinate di pagamento come le vedono gli
  invitati, configurazione del proprio avatar (persona e campione vocale) senza passare dal
  token organizzatore.
- Notifica push/email quando arriva un augurio o una foto che li riguarda.

**Cassieri**
- Promemoria automatico ai donatori con quota "in attesa" dopo N giorni (email dal profilo).
- Pulsanti "contatta" (telefono o email) e nota libera per ogni quota; segnare più quote come
  ricevute in una volta.
- Invio settimanale automatico del CSV al cassiere via n8n (report già esistente esteso).
- App Android: scelta del cassiere nel dialogo della quota (oggi usa solo il primo).

**Organizzatori**
- Import della lista invitati da CSV con invio dei link personali in blocco; cruscotto con
  contatori (confermati, menu mancanti, navetta, email non consegnate).

## 4. Fasi e ordine di lavoro

Stato al 13 ottobre 2026: fasi 1, 2 e 3 realizzate (PWA e backend); in più la guida alla prima
apertura. Restano la fase 4 (area festeggiati, promemoria ai donatori, scelta del cassiere su
Android) e la fase 5 (assistente sul profilo, import invitati, cruscotto organizzatori).

| Fase | Contenuto | Stima | Dipendenze |
|---|---|---|---|
| 1. Email via n8n e profilo | evento `mail.send` + `mail_outbox`, modelli delle email, workflow n8n aggiornato, campi profilo, magic link, pagina "Il mio profilo" nella PWA, email di accesso e conferma RSVP | 3-4 giorni | n8n già attivo con Send Email |
| 2. Menu | menu strutturato per persona, riepilogo catering, data limite, email di conferma | 1-2 giorni | fase 1 |
| 3. Navetta | prenotazione dal profilo, lista d'attesa, conferma con `.ics`, promemoria, check-in | 2-3 giorni | fase 1 |
| 4. Festeggiati e cassieri | token per festeggiato, area festeggiato, promemoria ai donatori, azioni rapide nel cruscotto, scelta cassiere su Android | 2-3 giorni | fase 1 |
| 5. Assistente e rifiniture | strumenti dell'assistente sul profilo, import invitati, cruscotto organizzatori, test e2e | 2 giorni | fasi 1-4 |

Ogni fase è una PR per repository, con test backend e controlli e2e nella PWA; l'app Android
viene aggiornata alla fine delle fasi 1 e 3 (profilo e navetta) e 4 (cassiere).

## 5. Rischi e scelte da confermare

- **Consegna delle email**: il mittente configurato in n8n deve avere SPF/DKIM validi sul
  dominio, altrimenti le email agli invitati finiscono nello spam; conviene un test con
  Gmail e Outlook prima dell'invio in blocco.
- **Privacy**: raccogliere email, telefono e allergie richiede un'informativa breve e un
  consenso esplicito nel profilo; i dati vanno cancellati dopo la festa (comando di pulizia).
- **Link personale** = accesso senza password: chi inoltra il link può modificare il profilo.
  Mitigazioni: scadenza, possibilità di rigenerarlo, email di avviso a ogni modifica.
- **Date limite** (menu, navetta): da fissare con il catering e l'autista.
- **Compatibilità APK**: l'app 1.6 continua a funzionare con `X-Client-Id`; le novità arrivano
  prima nella PWA, come già fatto per l'assistente.
