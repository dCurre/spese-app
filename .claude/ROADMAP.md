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

- [ ] **Restyling estetico dell'app (grosso, da pianificare).** Miglioria generale dell'aspetto:
  coerenza visiva, spaziature, tipografia, palette/tema, card e liste. Da pianificare in dettaglio
  quando ci si arriva. **Include**: sistema di colori semantici ("colori specifici per cose
  specifiche" — es. spesa/saldo/entrata, accento primario, stati successo/errore) definito dentro i
  temi esistenti (chiaro/scuro/legacy/auto), usato in modo consistente in tutta l'app invece del solo
  colore primario Material. NB: il selettore temi e la persistenza (come la lingua) restano; qui si
  definisce la palette *dentro* ogni tema.
  - **Consolidamento colori semantici (a fine restyling):** stesso schema della lingua — un set
    unico di "nomi semantici" (uno per ogni cosa a schermo: credit/debit/accent/divider/badge/chip…)
    valorizzato per i 3 temi (light/dark/legacy), niente colori hardcoded nelle schermate. Già
    impostato il meccanismo (`SpeseSemanticColors` + `SpeseTheme.colors` in `ui/theme/Theme.kt`):
    resta da RACCOGLIERE tutti i colori usati nelle schermate ridisegnate in questo set completo.
    **Architettura voluta** (idea dell'utente, come la lingua): definire i TOKEN semantici
    (`bgPage, bgCard, bgInput, textPrimary, textSecondary, border, brand, credit, debit, accent`),
    poi UN SOLO builder che mappa token→ColorScheme (sempre `surface=bgCard`, `onSurface=textPrimary`,
    `outline=border`, ecc.) e TRE set di VALORI (light/dark/legacy = le "colonne"). Così la mappatura
    cosa→token è una sola e un nuovo tema = riempire una colonna. Oggi invece ci sono 3 ColorScheme
    completi scritti a mano: va rifattorizzato in token + builder.

## Fuori scope / escluse

- Notifiche push → richiederebbero modifiche al backend (intoccabile).
- Escluse dall'utente: condivisione liste spese con bottoni WhatsApp/Telegram dedicati; pagina
  "Partecipa" dedicata.
- Placeholder anche nella webapp (non mancanti): login email/password, registrazione, archivio
  liste, cambio password, export dati, elimina account, drag&drop riordino.
