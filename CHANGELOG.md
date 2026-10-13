# Changelog

Tutti i cambiamenti notevoli del progetto NeuroParty saranno documentati in questo file.

Il formato è basato su [Keep a Changelog](https://keepachangelog.com/it/1.1.0/) e il
progetto adotta il [Semantic Versioning](https://semver.org/lang/it/spec/v2.0.0.html).

## [1.10.0] - 2026-10-13

Fase 5 del piano: assistente sul profilo, cruscotto organizzatori, import invitati.

### Aggiunto

- **Assistente sul profilo** (PWA): chi ha aperto il proprio profilo può chiedere all'avatar del
  suo menu, dei suoi accompagnatori e della sua navetta, e cambiarli a voce ("cambia il mio menu
  in vegetariano", "prenota la mia navetta"): le modifiche passano dalle stesse regole del
  profilo (consenso, date limite, lista d'attesa, email di conferma). Backend: strumenti
  `get_my_profile`, `update_my_profile`, `book_my_bus`, `cancel_my_bus` con `X-Guest-Token`.
- **Cruscotto organizzatori** (⚙️ Impostazioni): confermati e coperti, in attesa, senza menu,
  navetta (prenotati, lista d'attesa, saliti), profili ed email, quote uniche, bacheca, menu
  scelti, e l'elenco delle **cose da fare** con il pulsante che porta alla sezione o al pannello
  giusto; scorciatoie per CSV, email e import. Backend: `GET /api/admin/dashboard`.
- **Import della lista invitati**: incolla da Excel o carica un CSV (intestazione riconosciuta:
  Nome, Categoria, Email, Telefono, Persone, Stato), anteprima con creati/aggiornati/uguali,
  abbinamento per email o per nome, invio dei link personali a chi ha l'email. Backend:
  `POST /api/guests/import` (`rows`, `sendLinks`, `dryRun`), evento webhook `guest.imported`.
- Service worker v2.9.0; 10 nuovi controlli e2e (145). Piano di miglioramento completato.

## [1.9.0] - 2026-10-13

Fase 4 del piano: area festeggiato, strumenti dei cassieri, scelta del cassiere su Android.

### Aggiunto

- **Area festeggiato** (PWA): con il token personale `GRADUATE_TOKEN_<ID>` in ⚙️ Impostazioni
  compare il pulsante 🎓: chi ha confermato (con accompagnatori) e chi è in attesa, posti
  navetta, gli auguri dedicati e quelli per tutti, le proprie coordinate di pagamento come le
  vedono gli invitati, stato e configurazione del **proprio avatar** dell'assistente (persona,
  voce, campione) senza passare dal token organizzatore. Nessuna cifra delle quote uniche.
- **Email ai donatori e ai festeggiati**: nel dialogo della quota unica il campo email
  (precompilato dal profilo, con nome e telefono) fa arrivare la conferma con IBAN e causale del
  cassiere; se la quota resta in attesa parte un promemoria (pulsante nel cruscotto o workflow
  n8n settimanale). Ogni neo-specialista con un'`email` privata in `giftTargets` riceve un'email
  per ogni augurio dedicato a lui.
- **Cruscotto del cassiere**: pulsanti ✉️ Email e 📞 Chiama per ogni quota, appunti del
  cassiere (anche nel CSV, colonne "Email" e "Appunti cassiere"), selezione multipla con "Segna
  ricevute le selezionate", "Promemoria ai donatori in attesa". Backend: `PATCH
  /api/gifts/pool/{id}`, `POST /api/gifts/pool/status`, `POST /api/mail/reminders/pool`,
  `GET /api/graduate/me`, `?collector=` sul CSV; workflow n8n `csv-cassieri-settimanale.json`.
- **App Android 1.7.0**: scelta del cassiere (una card per cassiere, dialogo intestato al
  cassiere scelto, "a chi" nelle proprie quote), campo email nella quota unica; `giftCollectors`
  dallo snapshot (tabella `gift_collectors`, database v3 ricreato), `SeedData.giftCollectors`.
- Service worker v2.8.0; 21 nuovi controlli e2e (135).

## [1.8.0] - 2026-10-13

Fasi 2 e 3 del piano: menu con data limite, navetta dal profilo, guida alla prima apertura.

### Aggiunto

- **Guida alla prima apertura** (PWA): popup a passi con cosa fa ogni scheda e come usarla
  (programma, profilo, navetta, auguri e foto, regali, assistente se attivo); si riapre dal
  pulsante ? nell'intestazione e da ⚙️ Impostazioni. Non compare quando si arriva da un link
  personale.
- **Navetta dal profilo**: sezione Navetta nel profilo (posti = persone, fermata, ritorno),
  conferma via email con codice e `navetta.ics`, lista d'attesa a posti esauriti con promozione
  automatica (email "posto confermato"), annullamento; card "Prenota dal tuo profilo" nella
  scheda Navetta; per gli organizzatori check-in ✅ di chi sale e contatori (confermati, in
  attesa, saliti); etichette lista d'attesa/salito e codice nelle righe.
- **Menu con data limite**: `meta.menuDeadline` e `meta.busDeadline` in `event-data.json`;
  il profilo avvisa della scadenza e dopo blocca menu, accompagnatori, partecipazione e navetta;
  riepilogo catering con menu scelti, persone senza menu e allergie degli accompagnatori;
  `invitati.csv` con menu, accompagnatori, email e telefono; `navetta.csv` con stato, codice,
  email e check-in.
- Pannello Email: pulsanti "Promemoria menu mancante" e "Promemoria navetta" (anche da n8n a
  orario con il token automazioni). Workflow n8n con allegati. Service worker v2.7.0; 16 nuovi
  controlli e2e (114).

## [1.7.0] - 2026-10-12

Fase 1 del piano (`docs/piano-miglioramenti.md`): profilo dell'invitato ed email tramite n8n.

### Aggiunto

- **Profilo personale** (PWA): card "Il mio profilo" nella tab Invitati e icona 👤 nell'intestazione.
  L'invitato inserisce la propria email (e il nome se non è in lista) e riceve un link personale
  `#profilo=<token>` che apre il profilo: partecipazione, categoria, email e telefono, menu
  (completo, vegetariano, vegano, senza glutine, altro) con note allergie, accompagnatori con
  nome e menu, consenso al trattamento dei dati; salvando arriva l'email di conferma. "Esci"
  invalida il link. Il link funziona anche incollato con l'app già aperta.
- Lista invitati con menu, accompagnatori ed etichetta "profilo"; per gli organizzatori il
  pulsante 🔗 con il link da copiare o l'invio dell'email con il link.
- Pannello organizzatori **Email (tramite n8n)** in ⚙️ Impostazioni: stato del collegamento,
  ultime email con esito, reinvio, email di prova.
- Backend: `/api/profile`, `/api/mail`, evento webhook `mail.send`, outbox, riepilogo catering
  per menu, workflow n8n aggiornato. Service worker v2.6.0; 13 nuovi controlli e2e (98).

## [1.6.1] - 2026-10-12

### Aggiunto

- Secondo cassiere per le quote uniche, **Dott. Aki**: `giftCollectors` (lista con id) in
  `event-data.json`; nella PWA una card per cassiere, il dialogo della quota è intestato al
  cassiere scelto, "Le tue quote registrate" indica a chi è stata versata, il cruscotto ha una
  sezione per cassiere e il CSV la colonna "Cassiere" con i totali per cassiere. Backend:
  `collectorId` nelle quote, `TREASURER_TOKEN_<ID>` per far vedere a ogni cassiere solo le sue
  quote. L'app Android 1.6 continua a usare il primo cassiere (`giftCollector` nello snapshot).

## [1.6.0] - 2026-09-30

Sezione Regali rifatta: niente cifre raccolte, quota unica al cassiere e cruscotto.

### Modificato

- Regali (Android e PWA): rimossi obiettivi, importi raccolti, barre di avanzamento e l'elenco
  pubblico delle quote. La card di ogni neo-specialista mostra solo regalo, intestatario e IBAN con
  i pulsanti **Copia IBAN**, **PayPal** e **Satispay**: la donazione diretta non viene registrata
  (niente importo, niente nome, niente contanti).
- Il "Regalo Comune Specializzandi" è sostituito dal **cassiere delle quote uniche**
  (`giftCollector` in `event-data.json`: Dott. Paolo Roberto, IBAN, PayPal, causale suggerita):
  chi vuole fare un solo versamento registra la quota indicando nome, importo, metodo
  (IBAN, PayPal o contanti) e la ripartizione **in parti uguali** (con possibilità di escludere
  qualcuno) o **personalizzata**; "Le tue quote registrate" con annullamento finché in attesa.
- Room: database v2 (regali senza importi, tabella `gift_collector`, via le quote per singolo
  regalo), ricreato in automatico; `SnapshotDto` legge `event.giftCollector`.
- Backend (`neuroparty-backend`): `POST/GET /api/gifts/pool...`, token `TREASURER_TOKEN`,
  `GET /api/export/gift-pool.csv`, webhook `gift.pooled`; i vecchi endpoint delle quote e gli
  importi spariscono dallo snapshot. Gli APK precedenti devono essere aggiornati.

### Aggiunto

- PWA: **cruscotto del cassiere** nella tab Regali (token cassiere o organizzatore in
  ⚙️ Impostazioni): quanto spetta a ciascun neo-specialista, elenco delle quote con stato
  "ricevuta" e download di `quote-uniche.csv` (una colonna per neo-specialista, righe dei
  totali, virgola decimale) pronto per Excel.
- `GiftSplit`: stessa ripartizione ai centesimi del server, usata per l'anteprima nel dialogo e
  in modalità locale; test Room e di parsing dello snapshot aggiornati; 13 nuovi controlli e2e.
- `tools/check-event-data.py` verifica anche il cassiere (IBAN, metodi ammessi) e segnala i campi
  degli importi non più supportati.
- **Assistente vocale con avatar** (PWA): pulsante "Chiedi" su tutte le schermate; pannello con
  scelta del neo-specialista, "tieni premuto e parla" (MediaRecorder) o testo, risposta scritta
  e letta ad alta voce, azioni eseguite dal server mostrate come etichette (posti navetta,
  RSVP, prenotazione, augurio, apertura sezione). Pannello organizzatori per persona,
  abilitazione, voce preimpostata, campione vocale da clonare (file o registrazione), riascolto
  e prova della voce. Backend `neuroparty-backend` con API Mistral (Voxtral, chat con function
  calling, `voxtral-mini-tts-2603`); `assistant.avatars` in `event-data.json` (solo Fedele
  attivo all'inizio). La CI avvia il backend con `ASSISTANT_FAKE=true`; 13 nuovi controlli e2e.

## [1.5.0] - 2026-09-25

Strumenti per gli organizzatori: notifiche programmate, riepilogo catering, export CSV,
controllo dei dati, backup e wrapper Gradle.

### Aggiunto

- Notifiche programmate: campo "Programma invio" nel dialogo notifiche (Android: `gg/mm hh:mm`,
  PWA: selettore data/ora); il backend pubblica e invia in push all'ora indicata
  (`sendAt` nel POST, `GET /api/notifications/scheduled`, `DELETE /api/notifications/{id}`).
  Nella PWA le notifiche in attesa sono elencate nel dialogo e annullabili.
- Riepilogo per il catering nella tab Invitati (Android `CateringSummaryCard`, PWA riquadro
  espandibile): coperti per categoria ed esigenze alimentari con i nomi; backend
  `GET /api/guests/summary`.
- Export CSV di invitati e navetta dalla PWA (⚙️ Impostazioni, solo organizzatori) e
  `GET /api/export/{guests,bus}.csv`.
- `tools/check-event-data.py`: controllo di `event-data.json` (id, segnaposto orari, coordinate,
  destinatari degli auguri, IBAN). In CI avvisa; nel workflow di release blocca la pubblicazione
  con IBAN segnaposto solo se la variabile di repository `STRICT_EVENT_DATA` vale `true`.
  Il backend scrive lo stesso avviso nel log.
- `tools/gen-snapshot-fixture.py`: rigenera `snapshot.json` dal backend; la CI verifica con
  `--check` che il fixture sia allineato a JSON e backend.
- `tools/bootstrap-gradlew.sh` per creare e committare il Gradle wrapper; la CI genera il
  wrapper solo se manca.
- Backend: `scripts/backup.sh` (copia consistente del DB, rotazione 7 giorni) e
  `scripts/install-backup-cron.sh` (backup notturno).
- Test: `NotificationScheduleTest` (Android), 5 test backend, 11 controlli e2e.

### Modificato

- Orario della festa confermato: **dalle 21:30 alle 03:00** (`schedule.partyTime` e nuovo
  `schedule.partyEndTime`, usato anche dal calendario .ics e dal chip calendario Android).
- Fixture `snapshot.json` rigenerato (campo `scheduledAt` nelle notifiche).
- Cache del service worker `v2.3.0`; `versionName` 1.4.0 → 1.5.0, `versionCode` 6 → 7.

## [1.4.0] - 2026-09-25

Orari strutturati, calendario e aggiornamento dei regali dal JSON.

### Aggiunto

- Blocco `schedule` in `event-data.json` (date e orari di seduta, festa, navetta; vuoto = da
  definire) e segnaposto nei testi, es. `{partyTime|Inizio ore $.|Orario da confermare.}`:
  risolti dal backend (`app/schedule.py`), dalla PWA in locale (`pwa/js/schedule.js`) e dal
  codegen Android. Quando l'orario sarà deciso basterà compilare `schedule`.
- Calendario: `GET /api/event/calendar.ics` (seduta + festa, tutto il giorno finché manca
  l'orario); pulsante **📅 Aggiungi al calendario** nell'intestazione della PWA (in modalità
  demo il file è generato nel browser); su Android due chip "Seduta 9 nov" / "Festa 13 nov"
  che aprono l'app Calendario con l'evento precompilato (`util/CalendarHelper.kt`).
- Backend: variabile `GIFT_SYNC_UPDATE_TEXTS=true` per aggiornare al riavvio testi, IBAN,
  link e obiettivo dei regali già in database a partire dal JSON (quote raccolte intatte).
- Test: 3 nuovi test backend (segnaposto, calendario, aggiornamento regali) e 4 controlli e2e.

### Modificato

- `SeedData.kt` espone `schedule` (`EventSchedule`); cache del service worker `v2.2.0`.
- Versione app: `versionName` 1.3.0 → 1.4.0, `versionCode` 5 → 6.

## [1.3.0] - 2026-09-25

Nono neo-specialista e sede della festa confermata.

### Aggiunto

- Donato Regina è il nono neo-specialista: compare fra i laureandi (hero, selettore degli
  auguri), con il proprio regalo personale (`giftTargets`, id `regina`) e un augurio demo.
- Backend: i regali di `event-data.json` mancanti nel database vengono aggiunti anche su un
  server già avviato (senza toccare le quote raccolte) e la versione dati viene incrementata,
  così app e PWA ricaricano lo snapshot e mostrano subito il nuovo regalo.
- Backend: test `test_new_gift_target_added_to_existing_database`.

### Modificato

- Festa di specializzazione: venerdì 13 novembre a **Il Giardino dei Tempi - Orto Botanico**,
  Via Giovanni Amendola 247, Bari (orario ancora da definire). Aggiornati timeline, punto
  sulla mappa (coordinate indicative, da verificare), navetta, notifiche iniziali e preset
  delle notifiche push su Android e PWA.
- Sottotitolo, testi e regalo comune parlano di 9 neo-specialisti.
- Etichetta della mappa Android "Villa Festa" → "Giardino dei Tempi"; descrizione della PWA
  con le due date e la sede.
- Fixture `snapshot.json` dei test Android rigenerata dal backend con i nuovi dati (10 regali).
- Versione app: `versionName` 1.2.0 → 1.3.0, `versionCode` 4 → 5; cache del service worker
  `neuroparty-v2.0.0` → `neuroparty-v2.1.0` (invalida il vecchio `event-data.json` offline).

## [1.2.0] - 2026-09-13

Backend condiviso, PWA collegata al server e sincronizzazione dell'app Android.

### Aggiunto

- Nuovo repository [neuroparty-backend](https://github.com/fedele93/neuroparty-backend):
  API FastAPI + SQLite, upload foto, Web Push (VAPID), token organizzatore, Docker Compose
  con Caddy (HTTPS automatico su sottodominio), test pytest e CI.
- Android: livello di rete Retrofit/Moshi (`data/remote/`), `AppPrefs`, sincronizzazione di
  Room con `GET /api/snapshot`, polling di `GET /api/state` ogni 20 s, notifiche di sistema
  per gli avvisi del server (in foreground e via `NotificationCheckWorker` ogni 15 min),
  dialog ⚙️ per URL server e token organizzatore, Toast per gli errori del server
  (es. posti navetta esauriti, permessi).
- Android: `API_BASE_URL` e `ADMIN_TOKEN` letti da `.env` in `BuildConfig`.
- Android: test `SnapshotParsingTest`, `EventRepositoryRoomTest`, `BusCapacityTest`.
- PWA: client API (`js/api.js`), modalità server con cache offline e ripiego automatico alla
  modalità demo locale (IndexedDB), sottoscrizione Web Push, impostazioni organizzatore,
  pulsanti di stato RSVP a tre vie, upload foto multipart, polling degli aggiornamenti.
- PWA: Leaflet incluso localmente (`pwa/vendor/leaflet`) invece che da unpkg (funziona
  offline e senza CDN).
- PWA: test end-to-end Playwright (`pwa/tests/e2e.mjs`, 39 controlli) eseguiti in CI contro
  il backend.
- CI: job `pwa`, esecuzione sui branch `claude/**`, `workflow_dispatch`, upload dell'APK di
  debug e dei report di test come artifact.

### Corretto

- PWA: la funzione `timelineIcon` era dichiarata dentro `timelineItem` (dopo il `return`),
  quindi la timeline del programma andava in errore a runtime; il gestore del pulsante
  notifiche era assegnato due volte.
- Android: `versionName` 1.1.0 → 1.2.0, `versionCode` 3 → 4.

## [Unreleased-precedente] Unificazione sorgente dati

Unificazione della sorgente dati tra app Android e PWA (Opzione A: singola sorgente).

### Aggiunto

- `pwa/shared/event-data.json` diventa l'unica sorgente dati per entrambe le
  piattaforme: modificarlo aggiorna PWA e Android insieme.
- `tools/gen-event-data.py`: codegen che trasforma il JSON in
  `app/src/main/java/com/example/data/SeedData.kt` (Kotlin).
- Task Gradle `genEventData` che rigenera `SeedData.kt` prima della compilazione.
- Workflow CI `.github/workflows/ci.yml` che, ad ogni PR, rigenera il codegen,
  verifica che `SeedData.kt` sia sincronizzato col JSON, compila l'APK di debug
  ed esegue i test.

### Modificato

- PWA: `pwa/js/data.js` carica seed e costanti dal JSON via `fetch` invece di
  avere i dati hardcoded; esporta anche `PROGRAM` e `BUS_SCHEDULE`.
- PWA: `pwa/js/screens.js` usa `PROGRAM`/`BUS_SCHEDULE` dal JSON per hero,
  timeline e orari navetta (rimossi i testi hardcoded).
- Android: `EventRepository.kt` usa `SeedData` invece di dati inline (-385 righe).
- Android: `EventViewModel`, schermate Programma/Navetta/Auguri leggono i dati
  testuali (badge, titolo, timeline, orari, fermate, laureandi) da `SeedData`.
- Service worker: cache bumped a `v1.2.0` e include `shared/event-data.json`.

## [1.1.0] - 2026-09-11

Aggiornamento con i dati reali dell'evento di specializzazione in Neurologia a Bari.

### Aggiunto

- 8 neo-specialisti reali: Fedele Luisi, Sebastiano Carlone, Roberto
  Spiridione Prezioso, Dalila Totaro, Giorgia Ruta, Lorenzo Parrulli,
  Francesco Cusmai, Chiara Esposto (in auguri, regali, foto e selettori UI).
- Sede della seduta: Aula Magna "G. De Benedictis" - AOUC Policlinico di
  Bari, 9 novembre (ora da definire).
- Festa di specializzazione: venerdì 13 novembre (luogo e ora da definire).
- Regalo comune + 8 regali personali con quote IBAN/Satispay/PayPal.
- Notifiche iniziali e preset push aggiornati per Bari e le nuove date.

### Modificato

- Versione app: `versionName` 1.0.0 → 1.1.0, `versionCode` 2 → 3.
- Timeline del programma riorganizzata su seduta (9 nov) e festa (13 nov).
- Mappa punti di ritrovo con coordinate del Policlinico di Bari.
- Orari/fermate della navetta da/per il Policlinico di Bari.

## [1.0.0] - 2026-09-10

Prima release pubblica dell'app, dedicata alla festa di specializzazione e laurea
in Neurologia.

### Aggiunto

- Gestione invitati e RSVP con persistenza locale (Room).
- Programma dell'evento e dettagli della location.
- Mappa interattiva dei punti di ritrovo (canvas Compose, senza SDK Google).
- Prenotazione della navetta bus con gestione dei posti.
- Bacheca auguri con ticker/banner.
- Galleria foto (caricamento immagini tramite Coil).
- Gestione delle quote regali differenziate.
- Notifiche locali via `NotificationCompat`.
- Documentazione: `README.md` e `CHANGELOG.md`.
- Workflow GitHub Actions per compilare l'APK release e pubblicarlo come asset
  di una GitHub Release al push di un tag `v*`.

### Modificato

- Versione app: `versionName` 1.0 → 1.0.0, `versionCode` 1 → 2.

### Rimosso

- Dipendenze non-libere per la compatibilità F-Droid: plugin `google-services`,
  Firebase BOM, Firebase AI e Firebase App Check (vedi PR #1).
