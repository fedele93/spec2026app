# Changelog

Tutti i cambiamenti notevoli del progetto NeuroParty saranno documentati in questo file.

Il formato è basato su [Keep a Changelog](https://keepachangelog.com/it/1.1.0/) e il
progetto adotta il [Semantic Versioning](https://semver.org/lang/it/spec/v2.0.0.html).

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
