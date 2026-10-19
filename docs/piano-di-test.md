# NeuroParty — piano di test prima dell'apertura

Obiettivo: provare l'app con festeggiati, cassieri e qualche amico **prima** di raccogliere dati veri,
trovare i bug e poi azzerare tutto.

## 0. Preparazione (organizzatore, sul server)
1. Nel `.env` del backend:
   - `ADMIN_TOKEN` (genera con `openssl rand -hex 24`); i cassieri non hanno bisogno di token nel `.env`: il loro link lo genera il cruscotto;
   - `GRADUATE_EMAIL_<ID>` per i nove festeggiati e `TREASURER_EMAIL_ROBERTO`, `TREASURER_EMAIL_AKI`;
   - `N8N_WEBHOOK_URL` attivo (senza n8n le email non partono);
   - `GIFT_SYNC_UPDATE_TEXTS=true` per allineare IBAN e link al JSON, `SEED_DEMO_DATA=false`.
2. Riavvia il backend, apri la PWA, inserisci il token organizzatore in ⚙️.
3. Cruscotto → **Inviti a festeggiati e cassieri**: controlla l'anteprima (tutti *pronto*), lascia
   spuntato *fase di prova* e invia.

## 1. Cosa provare
Segna ✅ / ❌ e, per ogni ❌, manda una segnalazione (vedi in fondo).

### Tutti (anche da invitato, in una finestra anonima o un altro telefono)
- [ ] Installazione sulla Home (iPhone Safari, Android Chrome) e apertura offline con gli ultimi dati.
- [ ] Guida alla prima apertura: si legge bene, nessun passo sbagliato.
- [ ] Programma: orari, mappa, pulsante *Naviga*, *Aggiungi al calendario*.
- [ ] Invitati: richiesta del link con email (e nome se non in lista) → arriva l'email → il link apre il profilo.
- [ ] Profilo: conferma, accompagnatori con menu, allergie, consenso → email di conferma.
- [ ] Navetta dal profilo: prenotazione, codice, modifica, annullamento; lista d'attesa se pieno.
- [ ] Auguri: senza profilo la bacheca è bloccata; con il profilo si vede, si pubblica e si firma col proprio nome.
- [ ] Foto: caricamento da galleria e fotocamera, ordine dalla più recente, ingrandimento al tocco e
      chiusura al secondo tocco, *Mostra foto precedenti* quando sono più di 24.
- [ ] Regali: copia IBAN, apertura PayPal/Satispay, copia email PayPal (Cusmai) e BANCOMAT Pay (Luisi).
      **Controlla che ogni IBAN e link corrisponda alla persona giusta.**
- [ ] Quota unica a ciascun cassiere: parti uguali e personalizzata, metodi ammessi,
      email di riepilogo, annullamento finché non è ricevuta.
- [ ] Notifiche push: attivazione, ricezione di una notifica di prova degli organizzatori.
- [ ] Assistente (*Chiedi*): orari, posti in navetta, «come faccio un regalo a Dalila?», «leggimi gli ultimi
      auguri», «mandami il link» (deve chiedere conferma prima di scrivere o mandare email).

### Festeggiati
- [ ] Il link dell'email apre l'area 🎓; su un secondo dispositivo funziona riaprendo il link.
- [ ] *Sono un festeggiato: mandami il link* (⚙️) rimanda lo stesso link.
- [ ] Area: numeri giusti, auguri per te, **coordinate giuste**, email ricevuta a ogni augurio dedicato.
- [ ] Avatar: persona, voce, prova dell'assistente con la propria voce.
- [ ] App Android: incollare il link in ⚙️ → card Area festeggiato; Bacheca sbloccata.

### Cassieri
- [ ] Il link dell'email apre Regali con il cruscotto e la guida del ruolo.
- [ ] Si vedono solo le proprie quote; ricevuta singola e in blocco; appunti; contatti; promemoria; CSV.
- [ ] Ripartizione per neo-specialista coerente con le quote ricevute.

### Organizzatori
- [ ] Cruscotto: contatori, cose da fare, import invitati (anteprima e import con invio link).
- [ ] Email: stato, reinvio delle non consegnate, promemoria menu/navetta.
- [ ] Notifica programmata e immediata; eliminazione di un augurio e di una foto; CSV invitati e navetta.

## 2. Come segnalare un bug
Scrivi a Fedele (WhatsApp o email) con:
1. **Cosa stavi facendo** (passo per passo) e **cosa ti aspettavi**;
2. **Cosa è successo** (messaggio di errore preciso);
3. uno **screenshot** o una registrazione dello schermo;
4. **telefono e browser** (es. iPhone 13, Safari; Pixel 7, app Android) e l'ora.

## 3. Fine della fase di prova (organizzatore)
1. Corretti i bug e aggiornata l'app, fai un **backup**: `scripts/backup.sh`.
2. Cruscotto → **Fine della fase di prova** → scrivi `AZZERA`: cancella invitati, navetta, auguri, foto,
   quote, notifiche ed email in coda. Restano configurazione, coordinate dei regali, accessi dei
   festeggiati, avatar e iscrizioni push.
3. Controlla `SEED_DEMO_DATA=false`, poi importa la lista vera degli invitati e manda i link.
