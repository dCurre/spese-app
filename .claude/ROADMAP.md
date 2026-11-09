# Roadmap / attività da fare — spese-app

Elenco delle attività aperte (bug, miglioramenti, feature). Fonte di verità per il lavoro
da fare: aggiornare qui (spuntare / rimuovere quando completato).

## Feature

- [ ] **Offline-first / sincronizzazione.** L'app deve funzionare offline / su connessione lenta e
  sincronizzare al ritorno della rete. Approccio a fasi: (1) cache read-only con Room
  (cache-first + refresh), (2) coda di scritture con WorkManager, last-write-wins. Vincoli backend:
  ID DB-autoincrement (serve temp-id→id come nel batch-save), nessun endpoint di sync.
  Domande aperte: modificare offline o solo consultare? quanto è reale l'editing concorrente?
  - **Login offline (parte da fare per prima, blocca l'uso offline):** oggi
    `AuthManager.restoreSession()` riconosce l'utente da Firebase (che funziona offline) ma poi
    chiama `userRepository.getByEmail()` sul backend; se la rete manca la chiamata fallisce e
    l'utente viene rimandato al login. Fix mirato: persistere l'ultimo `User` loggato
    (DataStore/Prefs) e, quando Firebase ha già la sessione e il backend è irraggiungibile *per
    rete* (non per errore applicativo), entrare con l'utente cachato invece di bloccare.

- [ ] **Test automatici (unit) — coda.** Fatti: `SemVer`, serializer, **diff batch-save checklist
  (`ChecklistEditState`), parsing import (`ChecklistImport`), changelog (`Changelog`)**; lo step
  unit test è **agganciato al CI** (blocca la release se falliscono). Resta da coprire, se si vuole:
  statistiche prodotto (prezzo min/ultimo/variazione) — andrebbe prima estratta in funzione pura.

## Fuori scope / escluse

- Notifiche push → richiederebbero modifiche al backend (intoccabile).
- Escluse dall'utente: condivisione liste spese con bottoni WhatsApp/Telegram dedicati; pagina
  "Partecipa" dedicata.
- Placeholder anche nella webapp (non mancanti): login email/password, registrazione, archivio
  liste, cambio password, export dati, elimina account, drag&drop riordino.
