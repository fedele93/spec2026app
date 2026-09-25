# Changelog

Tutti i cambiamenti notevoli del progetto NeuroParty saranno documentati in questo file.

Il formato è basato su [Keep a Changelog](https://keepachangelog.com/it/1.1.0/) e il
progetto adotta il [Semantic Versioning](https://semver.org/lang/it/spec/v2.0.0.html).

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
