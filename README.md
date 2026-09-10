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

## Licenza

MIT — Fedele Luisi, 2026. Vedi [LICENSE](LICENSE).
