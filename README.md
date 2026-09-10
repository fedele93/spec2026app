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

## Build

Il progetto usa Gradle (distribuzione 9.3.1). La build di debug non richiede
keystore; la build di release è configurata per usare un keystore firmato.

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
