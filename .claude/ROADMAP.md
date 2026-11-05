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

## Nuove richieste (da pianificare)

- [ ] **Toolbar laterale a scomparsa (navigation drawer).** Spostare i bottoni oggi nel profilo
  utente — "Informazioni" (about), "Amministrazione" (admin), "Controlla aggiornamenti" — in un
  drawer laterale a scomparsa.

- [ ] **Cancellazione checklist.** Al momento non è possibile eliminare una checklist dalla UI.
  Verificare endpoint backend disponibile e aggiungere l'azione (coerente con elimina lista spese).

- [ ] **Acquisto prodotto: data non modificabile + unità nella card.** Nel dettaglio prodotto
  (acquisti): (a) non si può modificare la **data** dell'acquisto (il `NewPurchaseDialog` non ha un
  date picker, usa `purchasedAt` esistente o oggi); aggiungere selezione data. (b) Nella card
  acquisto il prezzo unitario è mostrato come `€/u` fisso (`PriceDetailScreen` ~riga "%.2f €/u") →
  usare il **simbolo dell'unità** scelta per il prodotto invece di "u".

- [ ] **FAB "+" meno invadente.** Il FloatingActionButton viola (presente ovunque) copre parte delle
  informazioni a schermo. Valutare: FAB più piccolo, posizione diversa, auto-hide allo scroll, o
  spostamento dell'azione.

- [ ] **Restyling estetico dell'app (da fare più avanti).** Miglioria generale dell'aspetto:
  coerenza visiva, spaziature, tipografia, palette/tema, card e liste. Da pianificare in dettaglio
  quando ci si arriva.

## Fuori scope / escluse

- Notifiche push → richiederebbero modifiche al backend (intoccabile).
- Escluse dall'utente: condivisione liste spese con bottoni WhatsApp/Telegram dedicati; pagina
  "Partecipa" dedicata.
- Placeholder anche nella webapp (non mancanti): login email/password, registrazione, archivio
  liste, cambio password, export dati, elimina account, drag&drop riordino.
