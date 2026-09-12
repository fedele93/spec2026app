# NeuroParty — PWA (versione iOS / Web installabile)

Versione web installabile dell'app Android NeuroParty, pensata per girare anche su
**iPhone via Safari** ("Aggiungi a schermata Home") e su Android via Chrome, senza store
e senza account Apple. È una **PWA** (Progressive Web App): stessi dati, stessa UX
dell'app nativa (5 schermate), con persistenza locale (`IndexedDB`) e notifiche web.

## Cosa contiene

- 8 Neo-specialisti in Neurologia (Fedele Luisi, Sebastiano Carlone, Roberto
  Spiridione Prezioso, Dalila Totaro, Giorgia Ruta, Lorenzo Parrulli, Francesco
  Cusmai, Chiara Esposto)
- **Seduta**: 9 novembre, Aula Magna "G. De Benedictis" — AOUC Policlinico di Bari
- **Festa**: venerdì 13 novembre (luogo e ora da definire)
- 5 schermate a tab: Programma (timeline + mappa Leaflet + ticker auguri), Invitati
  & RSVP, Navetta bus, Auguri & Foto, Regali (quote IBAN/Satispay/PayPal)
- Notifiche push web locali (Notification API), offline via service worker

## Come provarla in locale

```bash
cd pwa
python3 -m http.server 8765
# apri http://localhost:8765
```

Poi "Aggiungi a schermata Home" dal browser per installarla come app.

## Pubblicazione su GitHub Pages (gratuita)

1. Vai su **Settings → Pages** del repo.
2. Sorgente: branch `main`, cartella `/pwa`.
3. L'app sarà disponibile a `https://fedele93.github.io/spec2026app/`.
4. Condividi quel link con gli invitati: su iOS Safari, "Aggiungi a schermata Home"
   installa la PWA; su Android Chrome, "Installa app".

## Note iOS

- Le **notifiche push** web funzionano su iOS 16.4+ con la PWA installata e permesso
  concesso (a differenza di Android dove sono più robuste). Per gli aggiornamenti
  in tempo reale più affidabili su iPhone si consiglia di affiancare un gruppo di
  messaggistica (es. WhatsApp/Signal).
- Lo storage iOS della PWA è limitato (~50 MB): le foto caricate vengono salvate come
  data-URL, perciò si consiglia di caricare immagini non troppo grandi (< 4.5 MB).

## Stack

- HTML/CSS/JS vanilla (niente build tooling), ES modules
- `IndexedDB` per la persistenza (mirror del layer Room dell'app Android)
- Leaflet + OpenStreetMap per la mappa (no SDK Google, coerente con la versione F-Droid)
- Manifest + service worker per l'installazione e l'offline
