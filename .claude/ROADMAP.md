# Roadmap / attività da fare — spese-app

Elenco delle attività aperte (bug, miglioramenti, feature). Fonte di verità per il lavoro
da fare: aggiornare qui (spuntare / rimuovere quando completato).

## Bug

_(nessun bug aperto)_

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
- [ ] **Test automatici (unit) — coda.** Fatti: `SemVer`, serializer, **diff batch-save checklist
  (`ChecklistEditState`), parsing import (`ChecklistImport`), changelog (`Changelog`)**; lo step
  unit test è **agganciato al CI** (blocca la release se falliscono). Resta da coprire, se si vuole:
  statistiche prodotto (prezzo min/ultimo/variazione) — andrebbe prima estratta in funzione pura.

## Fuori scope / escluse

- Notifiche push "vere" (lato server, es. nuova spesa su lista condivisa) → richiederebbero FCM +
  modifiche al backend. Fatte invece le notifiche **locali di nuova versione** (issue #22): worker
  periodico che interroga GitHub Releases e notifica, senza backend.
- Escluse dall'utente: condivisione liste spese con bottoni WhatsApp/Telegram dedicati; pagina
  "Partecipa" dedicata.
- Placeholder anche nella webapp (non mancanti): login email/password, registrazione, archivio
  liste, cambio password, export dati, elimina account, drag&drop riordino.
