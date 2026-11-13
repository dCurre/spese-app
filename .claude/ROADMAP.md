# Roadmap / attività da fare — spese-app

Elenco delle attività aperte (bug, miglioramenti, feature). Fonte di verità per il lavoro
da fare: aggiornare qui (spuntare / rimuovere quando completato).

## Feature

- [x] **Issue #21 — badge categoria checklist "spuntati/totale".** Il badge ora mostra `checked/total`
  (es. `2/3`) degli articoli ricorsivi quando la categoria ha articoli; conteggio diretto se ha solo
  sottocategorie vuote.
- [x] **Issue #20 — barre di ricerca più sottili.** `SpeseSearchField` riscritto con `BasicTextField`
  + `DecorationBox` e contentPadding compatto (`heightIn(min=44.dp)`): copre tutte le 5 schermate.
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
