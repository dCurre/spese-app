# Roadmap / attività da fare — spese-app

Elenco delle attività aperte (bug, miglioramenti, feature). Fonte di verità per il lavoro
da fare: aggiornare qui (spuntare / rimuovere quando completato).

## Feature

- [ ] **Offline-first / sincronizzazione.** L'app deve funzionare offline / su connessione lenta e
  sincronizzare al ritorno della rete. Approccio a fasi: (1) cache read-only con Room
  (cache-first + refresh), (2) coda di scritture con WorkManager, last-write-wins. Vincoli backend:
  ID DB-autoincrement (serve temp-id→id come nel batch-save), nessun endpoint di sync.
  Domande aperte: modificare offline o solo consultare? quanto è reale l'editing concorrente?

- [ ] **Test automatici (unit) — ampliare.** Fatti: `SemVer`, `BigDecimalSerializer`,
  `InstantSerializer` (in `app/src/test`). Restano da coprire: diff del batch-save checklist,
  parsing import (Samsung Notes), statistiche prodotto. Agganciare l'esecuzione al CI così un push
  che rompe la logica non diventa release.

- [ ] **Refactor boilerplate ViewModel (mutazioni).** Il pattern `viewModelScope.launch { when
  (repo.xxx()) { Success -> load(); Error -> message } }` è ripetuto 15+ volte in 8 VM. Estrarre
  un helper/BaseViewModel, ma prima standardizzare i campi `message`/`error`/`isLoading` degli
  UiState (oggi ogni VM ha il proprio data class). Rischio medio: da fare in sessione dedicata.
- [ ] **Row-elenco a slot (ListRow condiviso).** Lo scaffold riga-elenco (Row clickable + Column
  nome/sottotitolo + trailing) ricorre in ChecklistScreen/ExpensesListScreen/PriceListScreen con
  trailing diversi (star, badge, chevron). Estraibile come componente a slot. Rischio medio.

- [ ] **Restyling estetico dell'app — rifiniture rimaste.** Base fatta (stile "Ledger verde":
  temi light/dark/legacy, liste a righe, colori semantici consolidati in token). Restano eventuali
  rifiniture viste sull'APK e l'estensione dello stile a schermate minori/dialog se necessario.
  Fatto: sistema colori a TOKEN (`ThemeTokens` in `Color.kt`) + builder unico token→ColorScheme
  (`Theme.kt`) + 3 colonne di valori (light/dark/legacy), come la lingua.

## UI / UX — rifiniture

- [ ] **Contrasto dei bordi** delle label nelle modali — da verificare sull'APK dopo
  l'introduzione di `SpeseDialog` (shape/elevation/surface); se serve, usare `dialogBorder()`
  già predisposto in `SpeseDialog.kt`.
- [ ] **Sezione partecipanti** — oggi è una modale, ripensarla.
- [ ] **Azione "Elimina" nei dialog di modifica** — da gestire meglio. Ora in ItemEditDialog /
  ExpenseDialog / CategoryEditDialog è un cestino (danger) accanto al titolo; decidere un pattern
  definitivo e coerente per l'elimina dentro tutte le modali di modifica.

## Fuori scope / escluse

- Notifiche push → richiederebbero modifiche al backend (intoccabile).
- Escluse dall'utente: condivisione liste spese con bottoni WhatsApp/Telegram dedicati; pagina
  "Partecipa" dedicata.
- Placeholder anche nella webapp (non mancanti): login email/password, registrazione, archivio
  liste, cambio password, export dati, elimina account, drag&drop riordino.
