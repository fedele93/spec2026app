# NeuroParty — PWA (iPhone, iPad, PC e web)

Versione web installabile dell'app NeuroParty: gira in **Safari su iPhone** ("Aggiungi alla
schermata Home"), in Chrome su Android ("Installa app") e in qualsiasi browser desktop. Stesse
5 schermate dell'app Android (Programma, Invitati, Navetta, Auguri & Foto, Regali).

## Due modalità

| Modalità | Quando | Dati |
|---|---|---|
| **Server** (consigliata) | la PWA è servita dal backend ([neuroparty-backend](https://github.com/fedele93/neuroparty-backend)) allo stesso dominio, oppure l'URL del server è impostato in ⚙️ Impostazioni | condivisi fra tutti gli invitati; copia offline dell'ultimo snapshot |
| **Demo locale** | nessun server raggiungibile al primo avvio (es. GitHub Pages senza backend) | solo su questo dispositivo (IndexedDB) |

In modalità server la PWA interroga `GET /api/state` ogni 20 secondi e ridisegna la schermata
quando i dati cambiano; le scritture vanno al backend e, in caso di errore (posti esauriti,
permessi), mostrano il messaggio restituito dal server.

## Notifiche push

- Pulsante **"Attiva notifiche push"** nella schermata Programma: il browser si iscrive con la
  chiave VAPID del server (`/api/push/vapid-public-key`) e riceve gli avvisi degli
  organizzatori anche a browser chiuso (Chrome, Edge, Firefox; **iOS 16.4+ solo con la PWA
  aggiunta alla Home**).
- Gli organizzatori inseriscono il token in ⚙️ Impostazioni: compare il pulsante **"Invia
  notifica"** che pubblica l'avviso a tutti (app Android inclusa).

## Struttura

```
index.html, manifest.webmanifest, sw.js   shell, manifest, service worker (cache + push)
css/app.css
js/app.js        routing tab, polling, toast/modali
js/api.js        client HTTP (X-Client-Id, X-Admin-Token)
js/data.js       Repo: modalità server (snapshot + cache) o locale (IndexedDB)
js/db.js         wrapper IndexedDB (modalità locale)
js/notify.js     permesso notifiche, Web Push subscribe
js/schedule.js   orari (blocco "schedule"), segnaposto nei testi, calendario .ics locale
js/screens.js    le 5 schermate
shared/event-data.json   dati evento (fallback offline; sorgente per SeedData.kt Android)
vendor/leaflet/  mappa (OpenStreetMap, nessun SDK Google)
tests/e2e.mjs    test end-to-end Playwright
```

## Provare in locale

```bash
# 1) backend con dati demo che serve anche questa cartella
git clone https://github.com/fedele93/neuroparty-backend ../neuroparty-backend
cd ../neuroparty-backend && python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
ADMIN_TOKEN=test SEED_DEMO_DATA=true PWA_DIR=../spec2026app/pwa uvicorn app.main:app --port 8765
# 2) apri http://127.0.0.1:8765
```

Solo la PWA senza backend (modalità demo): `cd pwa && python3 -m http.server 8765`.

## Test end-to-end

```bash
npm i playwright && npx playwright install chromium
BASE_URL=http://127.0.0.1:8765 ADMIN_TOKEN=test node pwa/tests/e2e.mjs
```

Il test copre: caricamento dati dal server, notifiche, invitati (ricerca, aggiunta, stato,
permessi di cancellazione), navetta (overbooking rifiutato), auguri, upload foto, quote regalo,
invio notifica da organizzatore, persistenza dopo reload, polling, service worker e manifest.
Gli screenshot finiscono in `pwa/tests/screenshots/`.

## Aggiornare i dati dell'evento

`shared/event-data.json` è la sorgente per l'app Android (`python3 tools/gen-event-data.py`
rigenera `SeedData.kt`) e il fallback offline della PWA. In modalità server la PWA legge la
stessa struttura da `GET /api/event`: aggiorna anche `seed/event-data.json` nel repo del backend.

### Orari da definire e calendario

Il blocco `schedule` del JSON contiene date e orari (`ceremonyTime`, `partyTime`,
`busDepartureTime`, `busReturnTime`; vuoto = da definire). Nei testi si usano segnaposto del
tipo `{partyTime|Inizio ore $.|Orario da confermare.}` (con orario: 2° pezzo, `$` = orario;
senza: 3° pezzo) risolti dal server, dalla PWA in locale e dal codegen Android. Quando decidi
l'orario compila `schedule` e tutte le frasi si aggiornano. Il pulsante **📅 Aggiungi al
calendario** nell'intestazione apre `GET /api/event/calendar.ics` (seduta + festa) o, in
modalità demo, genera il file nel browser.

### Per gli organizzatori

Con il token inserito in ⚙️ Impostazioni:

- **Notifiche programmate**: nel dialogo "Invia notifica" il campo *Programma l'invio* fa
  partire la notifica da sola all'ora scelta (partenza navetta, taglio della torta...). Le
  notifiche in attesa sono elencate nello stesso dialogo e si possono annullare.
- **Export CSV**: pulsanti *Esporta invitati* e *Esporta navetta* in Impostazioni (file per
  il ristorante e per l'autista, apribili con Excel).
- Nella tab Invitati il riquadro **Riepilogo per il catering** mostra coperti per categoria ed
  esigenze alimentari con i nomi (visibile a tutti, anche offline).

## Pubblicazione

- **Consigliata**: servita dal backend (Caddy) su `https://neurospec.peukeia.eu/` — nessuna
  configurazione aggiuntiva, notifiche push funzionanti.
- **Alternativa**: GitHub Pages (Settings → Pages → branch `main`, cartella `/pwa`): la PWA parte
  in modalità demo; per usare il server inserisci l'URL in ⚙️ Impostazioni (il backend ha CORS
  aperto).
