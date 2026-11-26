# Roadmap / attività da fare — spese-app

Elenco delle attività aperte (bug, miglioramenti, feature). Fonte di verità per il lavoro
da fare: aggiornare qui (spuntare / rimuovere quando completato).

## Bug

_(nessun bug aperto)_

## Sicurezza (audit 2026-11-26) — in quest'ordine

- [ ] **Rilascio token Firebase.** App (0.2.16) e webapp lo inviano già. Resta: provare login e
  sincronizzazione, poi pushare `spese-ms` (blocca di default le chiamate senza token).
- [ ] **Autorizzazione nel backend:** utente ricavato dal token (non da id/email inviati dal client),
  accesso solo a liste proprie/condivise e propri acquisti, categorie vincolate alla lista in
  batch-save; utenti, ruoli e cataloghi di base solo admin. Validazione input e upload immagini.
- [ ] **Inviti a token per le liste spese** (oggi `/join?list=<id>`) e rimozione dell'elenco di tutti
  gli utenti per aggiungere partecipanti.
- [ ] **Chiudere l'accesso `anon` di Supabase realtime** (app + webapp): sostituire con aggiornamento
  all'apertura e periodico nelle liste condivise, poi revocare i permessi nel database.
- [ ] CORS ristretto alla webapp, rate limit, limite dimensione richieste; tipi MIME sul bucket
  `profile-images`.
- [ ] **Audit di app e webapp** (incluso il token GitHub dei bug report estraibile dall'APK).

## Feature

- [ ] **Pubblicazione su Google Play (prerequisito per la voce via Assistant).** Passi lato utente
  (admin, non codice): account Play Developer (25$), creazione app `com.dcapps.spese`, privacy policy
  pubblica (URL), Data safety, content rating, asset grafici (icona 512, feature 1024×500, screenshot).
  Nuovi account personali: richiesto test chiuso ~12 tester/~14gg prima della produzione. Lato repo
  (quando si procede): `bundleRelease` (AAB) nel `release.yml`, upload keystore come secret CI,
  Play App Signing → **prendere lo SHA della chiave di firma Play e registrarlo su Firebase `spese-dc`**
  (altrimenti il login Google si rompe in prod). La pubblicazione è un canale aggiuntivo, non sostituisce
  la GitHub Release attuale.
- [ ] **Azioni vocali "Ok Google …su Spese" (App Actions).** Richiede l'app su Play (anche solo canale
  interno: serve all'App Actions Test Tool). Scope realistico: aprire sezioni/liste nominando l'app
  ("apri le checklist / la lista X su Spese"), NON comprensione conversazionale libera (Conversational
  Actions dismesse da Google). Lavoro client: deep link/App Links sulle rotte esistenti (`Routes`),
  `assetlinks.json` con SHA-256 della firma Play, `shortcuts.xml` con capability su built-in intent
  (es. `GET_ITEM_LIST`, `OPEN_APP_FEATURE`) + eventuale custom intent, gestione URI → navigazione.
  Base riutilizzabile anche senza store: deep link + App Shortcuts (long-press icona). Il fulfillment
  di *scrittura* vocale (es. "aggiungi latte") è molto più oneroso e spesso richiede l'app in foreground.
- [ ] **Saldi condivisi — miglioramenti (opzionale).** Fatto: sezione "chi deve a chi" nelle liste
  SHARED, cablando `getBalance` sull'endpoint backend `/balance` (divisione equa). Limiti noti se si
  vuole evolvere: (a) solo divisione EQUA, niente split per quote/percentuali/sottoinsiemi (servirebbe
  nuova entità share su DTO+Room+backend); (b) i saldi usano come chiave "Nome Cognome" (fragile con
  omonimi, contratto attuale del backend); (c) niente "segna rimborso come pagato".
- [ ] **Registrazione e login con email/password (credenziali classiche).** Oggi l'auth è solo Google
  (Credential Manager) + guest. Aggiungere registrazione e accesso con email + password come
  alternativa. Da definire/verificare: endpoint backend `spese-ms` (registrazione, login, hash
  password, eventuale verifica email / reset password) — probabilmente da creare/estendere lato
  server; schermate client (form registrazione, form login, validazione, gestione errori); come
  convive col flusso Google e col guest-first/upgrade; sicurezza (niente password in chiaro nei log,
  storage token/sessione lato app). È una feature cross client+backend, non solo UI.

## Fuori scope / escluse

- Notifiche push "vere" (lato server, es. nuova spesa su lista condivisa) → richiederebbero FCM +
  modifiche al backend. Fatte invece le notifiche **locali di nuova versione** (issue #22): worker
  periodico che interroga GitHub Releases e notifica, senza backend.
- Escluse dall'utente: condivisione liste spese con bottoni WhatsApp/Telegram dedicati; pagina
  "Partecipa" dedicata.
- Placeholder anche nella webapp (non mancanti): archivio liste, cambio password, export dati,
  elimina account, drag&drop riordino. (Login email/password + registrazione: ora pianificati come
  feature sopra, non più solo placeholder.)
