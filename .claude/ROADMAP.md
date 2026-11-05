# Roadmap / attività da fare — spese-app

Elenco delle attività aperte (bug, miglioramenti, feature). Fonte di verità per il lavoro
da fare: aggiornare qui (spuntare / rimuovere quando completato).

## Feature

- [ ] **Offline-first / sincronizzazione.** L'app deve funzionare offline / su connessione lenta e
  sincronizzare al ritorno della rete. Approccio a fasi: (1) cache read-only con Room
  (cache-first + refresh), (2) coda di scritture con WorkManager, last-write-wins. Vincoli backend:
  ID DB-autoincrement (serve temp-id→id come nel batch-save), nessun endpoint di sync.
  Domande aperte: modificare offline o solo consultare? quanto è reale l'editing concorrente?

- [ ] **Test automatici (unit).** Nessuno al momento. Target di valore: diff del batch-save
  checklist, serializer (BigDecimal/Instant), `SemVer`, parsing import (Samsung Notes), statistiche
  prodotto. Agganciarli al CI così un push che rompe la logica non diventa release.

- [ ] **Restyling estetico dell'app — rifiniture rimaste.** Base fatta (stile "Ledger verde":
  temi light/dark/legacy, liste a righe, colori semantici consolidati in token). Restano eventuali
  rifiniture viste sull'APK e l'estensione dello stile a schermate minori/dialog se necessario.
  Fatto: sistema colori a TOKEN (`ThemeTokens` in `Color.kt`) + builder unico token→ColorScheme
  (`Theme.kt`) + 3 colonne di valori (light/dark/legacy), come la lingua.

## Fuori scope / escluse

- Notifiche push → richiederebbero modifiche al backend (intoccabile).
- Escluse dall'utente: condivisione liste spese con bottoni WhatsApp/Telegram dedicati; pagina
  "Partecipa" dedicata.
- Placeholder anche nella webapp (non mancanti): login email/password, registrazione, archivio
  liste, cambio password, export dati, elimina account, drag&drop riordino.
