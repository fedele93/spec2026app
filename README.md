# NeuroParty

App Android (installabile via `.apk`) per la festa di specializzazione e laurea in
Neurologia. Gestisce invitati e RSVP, notifiche push in tempo reale, mappa dei
ritrovi, prenotazione della navetta bus, bacheca degli auguri, galleria foto e
quote regali differenziate.

L'app è nativa in Kotlin + Jetpack Compose, senza dipendenze non-libere
(Firebase/Google Play Services sono state rimosse per compatibilità F-Droid):
le notifiche usano `NotificationCompat` locale e la mappa apre l'app di mappe
di sistema tramite `Intent`. Le note di posizionamento geografico usano un URI
di sistema, niente SDK Google.

## Le tre componenti

| Componente | Dove | Per chi |
|---|---|---|
| **App Android** (questo repo, cartella `app/`) | APK dalle GitHub Release | invitati con Android |
| **PWA / webapp** (cartella `pwa/`) | `https://<sottodominio>/` servita dal backend | iPhone, iPad, PC e chiunque non voglia installare l'APK |
| **Backend** ([fedele93/neuroparty-backend](https://github.com/fedele93/neuroparty-backend)) | Docker su Ubuntu, sottodominio con HTTPS automatico | condivide i dati fra tutti e invia le notifiche push |

Con il backend configurato (variabile `API_BASE_URL`, vedi sotto) l'app Android e la PWA
leggono e scrivono gli stessi dati: RSVP, prenotazioni navetta, auguri, foto, quote regalo e
notifiche. Senza backend entrambe funzionano in **modalità locale/demo** con i dati di esempio.

## Funzionalità

- **Programma & evento**: programma della giornata e dettagli della location.
- **Invitati & RSVP**: gestione degli invitati e conferme di partecipazione.
- **Mappa ritrovi**: canvas interattivo per i punti di ritrovo.
- **Navetta bus**: prenotazione e gestione dei posti sulla navetta.
- **Bacheca auguri**: ticker/banner con gli auguri degli ospiti.
- **Galleria foto**: visualizzazione foto (caricamento tramite Coil).
- **Regali**: gestione delle quote regali differenziate.

## Stack tecnico

- Kotlin 2.2.10, Jetpack Compose (BOM), Material 3
- AndroidX Lifecycle (ViewModel + runtime.compose), Activity Compose
- Room (database locale con DAO), Moshi (serializzazione), Retrofit + OkHttp
- Coil per il caricamento immagini
- Roborazzi/Robolectric per i test

Requisiti: `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`.

## Collegamento al backend

1. Metti online il backend seguendo il README di
   [neuroparty-backend](https://github.com/fedele93/neuroparty-backend) (Docker + Caddy su un
   sottodominio, es. `https://neurospec.peukeia.eu`).
2. Nella root di questo repo crea un file `.env` (è ignorato da git):

   ```bash
   API_BASE_URL=https://neurospec.peukeia.eu
   # solo nella build degli organizzatori:
   ADMIN_TOKEN=lo-stesso-token-del-server
   ```

   `.env.example` contiene già `API_BASE_URL=https://neurospec.peukeia.eu`, quindi anche senza
   `.env` (per esempio nella CI) l'APK punta al server. Precedenza: variabile d'ambiente,
   poi `.env`, poi `.env.example`. `ADMIN_TOKEN` va messo solo in `.env` o in un secret di CI.
3. Compila l'APK. In alternativa l'URL e il token si possono inserire dall'app: schermata
   **Programma → ⚙️** (icona ingranaggio nell'intestazione).

Come funziona la sincronizzazione: l'app interroga `GET /api/state` ogni 20 secondi mentre è
aperta e, se la versione dei dati è cambiata, scarica `GET /api/snapshot` e aggiorna il database
Room locale (che resta la cache da cui legge l'interfaccia). Ogni scrittura va prima al server.
Le notifiche pubblicate dagli organizzatori vengono mostrate come notifiche di sistema: subito se
l'app è aperta, altrimenti da un controllo periodico in background (WorkManager, ogni 15 minuti,
perché l'app non usa Firebase per restare compatibile con F-Droid). La PWA riceve invece vere
notifiche Web Push anche a browser chiuso.

### Strumenti per gli organizzatori

- **Notifiche programmate**: nel dialogo "Invia notifica" (app e PWA) si può indicare data e
  ora; il server pubblica e invia in push da solo al momento giusto (partenza navetta,
  taglio della torta). Nella PWA le notifiche in attesa si possono annullare.
- **Riepilogo per il catering** nella tab Invitati: coperti per categoria ed esigenze
  alimentari con i nomi; `GET /api/guests/summary` dà gli stessi numeri in JSON.
- **Export CSV** di invitati e navetta (PWA → ⚙️ Impostazioni, con il token organizzatore).

## PWA (iPhone e web)

La cartella `pwa/` contiene la versione web installabile: vedi [pwa/README.md](pwa/README.md).
Viene servita direttamente dal backend (Caddy) allo stesso sottodominio dell'API.

## Test

- **Android** (unit test JVM + Robolectric): `./gradlew testDebugUnitTest`
  - `SnapshotParsingTest`: i modelli Kotlin leggono una risposta reale di `/api/snapshot`
  - `EventRepositoryRoomTest`: Room in memoria, dati demo, contributi, sincronizzazione snapshot
  - `BusCapacityTest`: regola dei 54 posti
  - `NotificationScheduleTest`: parsing della data "Programma invio" delle notifiche
- **PWA** (Playwright end-to-end contro il backend): `node pwa/tests/e2e.mjs` (vedi `pwa/README.md`)
- **Dati**: `python3 tools/check-event-data.py` controlla `event-data.json` (id, segnaposto orari,
  coordinate, IBAN; con `--strict` gli IBAN segnaposto bloccano la release);
  `python3 tools/gen-snapshot-fixture.py` rigenera il fixture `snapshot.json` dal backend
  (`--check` in CI verifica che sia allineato)
- La CI (`.github/workflows/ci.yml`) esegue tutto ad ogni push/PR e pubblica l'APK di debug e
  gli screenshot della PWA come artifact.

## Build

Il progetto usa Gradle (distribuzione 9.3.1). La build di debug non richiede
keystore; la build di release è configurata per usare un keystore firmato.

Il repo non contiene ancora lo script `gradlew`: la prima volta crealo con
`tools/bootstrap-gradlew.sh` (usa Gradle se installato, altrimenti lo scarica) e committa
`gradlew`, `gradlew.bat` e `gradle/wrapper/gradle-wrapper.jar`. Da quel momento chiunque
cloni il repo compila senza installare Gradle; la CI usa il wrapper committato e lo genera
solo se manca.

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (richiede keystore e relative variabili d'ambiente)
KEYSTORE_PATH=/percorso/my-upload-key.jks \
STORE_PASSWORD=... \
KEY_PASSWORD=... \
./gradlew assembleRelease
```

L'APK viene generato in `app/build/outputs/apk/`.

## Release

Le release vengono prodotte automaticamente dalla GitHub Action
`.github/workflows/release.yml`: al push di un tag `v*` viene compilato l'APK
release e pubblicato come asset di una GitHub Release.

### Firma dell'APK in CI (per installazione su cellulare)

Per produrre un APK firmato e installabile sul telefono, la CI usa un keystore
salvato come secret. Configura una sola volta questi 4 secret nel repo
(**Settings → Secrets and variables → Actions → New repository secret**):

| Secret | Valore |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | il keystore `.jks` codificato in base64 (vedi sotto) |
| `SIGNING_KEY_ALIAS` | alias della chiave nel keystore (es. `upload`) |
| `SIGNING_STORE_PASSWORD` | password del keystore |
| `SIGNING_KEY_PASSWORD` | password della chiave |

Genera il keystore in locale (una sola volta) e codificalo in base64:

```bash
# 1. Crea il keystore (JDK richiesto)
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload

# 2. Codificalo in base64 e copia l'output nel secret SIGNING_KEYSTORE_BASE64
base64 -w0 my-upload-key.jks
```

Conserva `my-upload-key.jks` in luogo sicuro: è necessario per firmare ogni
futura release con la stessa identità, altrimenti gli aggiornamenti non si
installano sopra la versione precedente. Una volta configurati i secret, ogni
tag `v*` produce un APK firmato pronto da installare sul cellulare.

## Licenza

MIT — Fedele Luisi, 2026. Vedi [LICENSE](LICENSE).
