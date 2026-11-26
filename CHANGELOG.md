# Novità di spese-app

Note di rilascio in linguaggio comprensibile, mostrate nella schermata di aggiornamento dell'app.
Ogni versione ha una sezione `## X.Y.Z` con un elenco puntato delle novità per l'utente.

## 0.2.16
- **Aggiornamento di sicurezza**: l'app ora si identifica con il server in modo verificato. Installa questa versione: quelle precedenti presto non potranno più sincronizzare i dati.

## 0.2.15
- Corretto un errore per cui cambiare una lista spese da condivisa a personale (o viceversa) poteva fallire con un errore di rete: ora l'operazione va a buon fine. Lo stesso errore poteva colpire anche l'aggiunta/rimozione di partecipanti e il passaggio di proprietà: ora funzionano in modo affidabile.
- Se provi un'operazione di condivisione su una lista appena creata e non ancora sincronizzata, l'app ora lo spiega con un messaggio chiaro invece di dare un errore generico.

## 0.2.14
- Nelle **liste spese** di cui sei proprietario è di nuovo disponibile l'opzione per renderle condivise o personali (dal menu in alto a destra nel dettaglio della lista): prima non compariva. Per le liste già esistenti l'opzione appare dopo la prima sincronizzazione.

## 0.2.13
- Completata la correzione del tipo delle **liste spese**: ora una lista spese creata come personale resta personale e una condivisa resta condivisa, in modo affidabile (il problema residuo riguardava solo le liste spese, le checklist erano già a posto).

## 0.2.12
- Corretto un problema per cui una lista o checklist creata come **personale** poteva comparire come **condivisa**: ora il tipo scelto viene rispettato correttamente (di default le nuove liste sono personali).
- Aggiornando l'app i tuoi dati locali **non vengono più azzerati**: prima un aggiornamento interno poteva svuotare liste, spese e prezzi salvati sul dispositivo; ora vengono conservati.
- Pulizia interna dei dati cancellati: le liste/elementi eliminati prima di essere sincronizzati non lasciano più residui inutili sul dispositivo.

## 0.2.11
- Ora puoi creare davvero liste spese e checklist **condivise**: scegliendo "condivisa" (da connesso) la lista viene creata come condivisa e resta tale; prima restava sempre personale. Offline la condivisione resta non disponibile, come previsto.
- L'app si accorge subito quando **vai offline o torni online** mentre la stai usando (prima a volte non se ne accorgeva finché non la riavviavi).
- Cambiando l'**unità di misura** di un prodotto, ora l'unità si aggiorna ovunque: grafico andamento, statistiche, elenco prezzi e righe dei singoli acquisti. Nello storico non resta più "€/u".
- La sezione **andamento prezzi** mostra solo i periodi (giorno/mese/anno) che hanno abbastanza dati per un andamento; se nessun periodo ne ha, l'intera sezione resta nascosta.
- L'**ordinamento** ora è a più campi in cascata: tocca i criteri nell'ordine che vuoi (il numero indica la priorità), ognuno con la sua direzione crescente/decrescente; toccando ancora un criterio lo inverti e poi lo togli. Vale in tutte le schermate con ordinamento, e la scelta viene ricordata.

## 0.2.10
- Nelle liste spese condivise ora vedi i saldi "chi deve a chi": un riepilogo di quanto ha pagato ciascun partecipante e di chi deve rimborsare chi, con la spesa divisa in parti uguali. Compare automaticamente nelle liste condivise quando sei online.

## 0.2.9
- L'app ora ti avvisa con una notifica quando è disponibile una nuova versione, anche se non la stai usando: così non ti perdi gli aggiornamenti. Il controllo avviene in background un paio di volte al giorno (solo quando c'è rete); toccando la notifica si apre direttamente la schermata di aggiornamento.
- Dal Profilo trovi la nuova voce "Notifiche di aggiornamento", che mostra se le notifiche sono attive e permette di attivarle o gestirle con un tocco.

## 0.2.8
- Correzione importante: al logout i dati locali (checklist, spese, prezzi) vengono ora rimossi dal dispositivo, così accedendo con un altro account non si vedono più i dati dell'account precedente. Prima di uscire l'app prova a sincronizzare; se qualcosa non è ancora stato salvato sul server (offline), chiede conferma per non perdere dati.

## 0.2.7
- Nelle categorie delle checklist il contatore ora mostra quanti articoli sono spuntati sul totale (es. "2/3"), così vedi a colpo d'occhio quanto manca.
- Le barre di ricerca sono più sottili e compatte in tutte le schermate.

## 0.2.6
- Corretto un errore che impediva di sincronizzare nuove categorie e articoli aggiunti a checklist già esistenti: ora si salvano correttamente sul server.
- Nella selezione multipla degli articoli di una checklist puoi spostarli tutti insieme in una categoria (o toglierli da ogni categoria con "Senza categoria").

## 0.2.5
- Nuova sezione "Statistiche spese" (dal Profilo): mostra quanto hai speso nel periodo scelto (questo mese, 6 mesi, anno), l'andamento nel tempo, la ripartizione per tipologia e il confronto con il periodo precedente. Conteggia solo le spese che hai pagato tu.

## 0.2.4
- La sincronizzazione non viene più interrotta a metà quando modifichi qualcosa mentre è in corso: le nuove modifiche vengono messe in coda e sincronizzate subito dopo, senza falsi errori.

## 0.2.3
- Corretto un errore che impediva di sincronizzare le liste spese e le checklist create offline (il salvataggio sul server falliva): ora le nuove liste e spese si sincronizzano correttamente al ritorno online.

## 0.2.2
- Quando la sincronizzazione non riesce per un errore reale, l'app ora invia da sola una segnalazione con i dettagli tecnici (una sola volta per tipo di errore), così il problema può essere corretto prima.

## 0.2.1
- L'indicatore di sincronizzazione in alto è più chiaro: mostra "Offline" quando non c'è rete e "Sync non riuscita" (toccabile) in caso di errore, sempre alla stessa altezza.
- Le righe con modifiche non ancora sincronizzate mostrano una piccola nuvoletta "in attesa"; puoi disattivarla da Profilo › Impostazioni.
- Tolto il "tira per aggiornare": con l'app offline-first le pagine si aggiornano già da sole.
- Quando sei offline l'opzione "lista condivisa" è disattivata con una spiegazione: la condivisione richiede la connessione.

## 0.2.0
- Ora puoi usare l'app subito, senza account: entri come ospite e crei liste spese, checklist e prezzi direttamente sul tuo telefono.
- Tutto funziona anche offline: le tue modifiche sono immediate e vengono sincronizzate da sole quando torni online.
- Quando accedi con Google puoi scegliere se unire i dati creati come ospite al tuo account oppure usare solo quelli già online.
- Profilo ospite con nome e foto personalizzabili, salvati sul dispositivo finché non crei un account.
- Le funzioni di condivisione (partecipanti, inviti, liste condivise) mostrano un lucchetto per gli ospiti: basta accedere con un account per usarle.
- Nuovo indicatore di sincronizzazione e segnalazione automatica degli errori anche per le operazioni offline.

## 0.1.8
- Miglioramenti interni, pulizia del codice e test automatici per maggiore stabilità.

## 0.1.7
- Ora puoi eliminare più elementi insieme in tutte le liste: tieni premuto su una riga per attivare la selezione multipla.
- Aggiunta la ✕ per svuotare rapidamente la barra di ricerca.
- La gestione dei partecipanti si apre ora in un pannello dal basso, più comodo.
- Rivisto il pulsante "Elimina" nelle schede di modifica, ora più chiaro e separato.
- Contorni dei campi di testo più visibili.

## 0.1.6
- Selezione multipla ed eliminazione in blocco in liste spese, checklist, prezzi e relativi dettagli.
- Avviso quando aggiungi un articolo già presente nella checklist.
- Nella lista spese il numero di partecipanti appare solo per le liste condivise.

## 0.1.5
- Puoi passare una lista da personale a condivisa e viceversa.
- La checklist suggerisce di creare l'articolo cercato se non esiste.
- Le barre di ricerca ricordano l'ordinamento che preferisci.
- Nei prezzi: numero di prezzi registrati e prezzo minimo storico per ogni prodotto.
