# CLAUDE.md — convenzioni del progetto spese-app

App Android nativa (Kotlin + Jetpack Compose, MVVM + Hilt). Client del backend REST
`spese-ms`. Package applicazione: `com.dcapps.spese`.

## Regole non negoziabili

- **Backend modificabile se serve.** Puoi accedere a `spese-ms` e modificarlo (es. nuovi
  endpoint/campi) quando la funzione lo richiede. Verifica sempre gli endpoint esistenti in
  `app/api/*/routes.py` prima di scrivere il client. **Non creare un backend dentro l'app**: la
  logica server sta in `spese-ms`. Se tocchi il backend, pusha anche quel repo (su ok esplicito).
- **Push solo su ok esplicito** dell'utente. **Non bumpare `VERSION` a mano**: lo fa
  automaticamente l'hook `pre-push` (vedi sotto). Usa `git push` normale.
- **Mai committare segreti.** `local.properties`, `google-services.json`, keystore, file
  `.b64` e `.apk` sono gitignored: fai sempre un safety-check prima di `git add -A`.
- Identità git del repo: autore **dCurre**, nessuna attribuzione Claude nei commit.
- Comandi bash/build: eseguibili senza chiedere conferma.

## Build

```bash
./gradlew :app:assembleDebug --no-daemon      # verifica compilazione
./gradlew :app:assembleRelease --no-daemon    # applica R8 (più lento)
```
Leggi l'esito filtrando: `grep -iE "BUILD SUCCESSFUL|BUILD FAILED|^e: |^w: "`.
Per i soli errori Kotlin: `./gradlew :app:compileDebugKotlin` e filtra `^e: `.

## Compose — trappole ricorrenti (causano build falliti)

- **Icone Material**: importa SEMPRE esplicitamente
  `androidx.compose.material.icons.filled.X` (o `.automirrored.filled.X`) e usa `Icons.Default.X`
  / `Icons.AutoMirrored.Filled.X`. NON scrivere `androidx.compose.material.icons.Icons.Default.X`
  fully-qualified: non risolve ("Unresolved reference").
- Usa le varianti **AutoMirrored** per: `ArrowBack`, `Logout`, `TrendingUp`, `KeyboardArrowRight`,
  `ListAlt`, `Send` (quest'ultima non esiste in filled → usa altra icona).
- Dentro `ExposedDropdownMenuBox` usa **`DropdownMenu`** (importato), NON `ExposedDropdownMenu`
  (in material3 1.3.1 non è richiamabile come top-level).
- `var x by remember { mutableStateOf(...) }` richiede import di **`getValue` E `setValue`**
  (`androidx.compose.runtime.*`).
- `Card(onClick = ..., modifier = ...)`: l'onClick è il primo parametro; passa i nomi.
- Composable experimental (`FlowRow`, SegmentedButton, ExposedDropdown, DatePicker, Card cliccabile):
  serve `@OptIn(ExperimentalMaterial3Api::class)` / `ExperimentalLayoutApi`.

## Rete / serializzazione

- Il `Json` ha `explicitNulls = false`: i campi `null` NON vengono serializzati. Per
  **azzerare** un campo lato backend (es. `category_id` → null) serve inviarlo esplicitamente,
  non basta passarlo null.
- `@Body` deve essere una classe `@Serializable`, mai `Map` (rompe il converter Retrofit).
- R8/release richiede le keep rules in `proguard-rules.pro` per kotlinx.serialization + Retrofit
  (già presenti): senza, le modifiche falliscono solo in release ("Unable to create converter").
- Gli errori applicativi finiscono nelle snackbar via `ErrorSnackbarEffect` (azione "Segnala").

## Contratto dati del backend (quando aggiungi/leggi modelli)

- `amount` (e i Decimal in genere) arrivano come **stringa JSON** (es. `"50.00"`), non numero →
  usa `BigDecimalSerializer`. I purchase invece hanno price/quantity come **float**.
- Le date (`created_at`, `updated_at`, `joined_at`) sono **RFC 1123**, non ISO → usa
  `InstantSerializer`. `purchased_at`/`expense_date` sono date semplici (stringa `yyyy-MM-dd`).
- Le risposte wrappate: `{users:[...]}`, `{expenses:[...]}`, `{expenses_lists:[...]}`, ecc.
- Le mutation (POST) tornano `{id, message}`; PUT/DELETE tornano `{message}`.
- Nessun token di auth: l'utente è identificato per id/email nei path/body (`caller_email`
  serve al backend per autorizzare il cambio ruolo).

## Firma, release & build config

- **Ogni keystore** con cui firmi un APK deve avere il suo **SHA-1 registrato su Firebase**
  (progetto `spese-dc`, app `com.dcapps.spese`), altrimenti il **login Google fallisce**
  silenziosamente (Credential Manager non apre nemmeno il selettore). Vale sia per debug che
  per release.
- I **secret GitHub** non possono iniziare con `GITHUB_` (prefisso riservato): il token bug
  report è `BUGREPORT_TOKEN`, mappato nel workflow su env `GITHUB_BUGREPORT_TOKEN`.
- In `build.gradle.kts`: il codice che legge `local.properties` (es. `Properties()`) va a
  **livello top-level dello script**, NON dentro `android{}`/`defaultConfig`; serve
  `import java.util.Properties`.

## Versionamento (bump automatico)

- La patch di `VERSION` è bumpata **automaticamente a ogni `git push`** dall'hook
  `scripts/hooks/pre-push` (versionato; attivo via `core.hooksPath = scripts/hooks`).
  L'hook incrementa `VERSION`, la include **nello stesso commit** del codice (amend),
  poi fa partire il push buono e annulla quello originale → a GitHub arriva **un solo
  push = una sola build**. L'`error: failed to push` finale è **atteso** (è l'hook che
  blocca il push vecchio dopo aver spinto quello aggiornato).
- **Non bumpare `VERSION` a mano** e non aggiungere step di bump al CI: il workflow
  `release.yml` ora **legge soltanto** `VERSION` e pubblica (se il tag esiste già,
  fallisce di proposito per segnalare un bump mancato).
- Dopo un **clone nuovo** l'hook va riattivato una volta: `git config core.hooksPath
  scripts/hooks` (la config locale non si committa; lo script sì). Dettagli in
  `scripts/README.md`.

## Attività da fare

Le attività aperte (bug, miglioramenti, feature) sono in **[`ROADMAP.md`](ROADMAP.md)** (stessa
cartella `.claude/`): è la fonte di verità. LEGGILA all'inizio di un lavoro di sviluppo e
aggiornala quando completi un punto (marca `[x]` mentre lavori, così resta traccia nella sessione).

- **Prima di ogni push**: RIMUOVI dalla ROADMAP le voci completate (`[x]`), non lasciarle barrate.
  La roadmap committata deve contenere solo attività ancora aperte.

## Controllo bug su GitHub (si fa spesso)

L'app invia i crash/errori come Issue su GitHub (`dCurre/spese-app`, repo pubblico). **`gh` NON è
installato**: usa l'API REST pubblica via PowerShell (no auth per le issue di un repo pubblico).

```powershell
# Elenco issue aperte (esclude le PR)
$h = @{ "User-Agent"="spese-app-client"; "Accept"="application/vnd.github+json" }
(Invoke-RestMethod -Uri "https://api.github.com/repos/dCurre/spese-app/issues?state=open&per_page=50" -Headers $h) |
  Where-Object { -not $_.pull_request } | ForEach-Object { "#$($_.number) [$($_.created_at)] $($_.title)" }

# Dettaglio di una issue (corpo = descrizione + ambiente + stacktrace)
(Invoke-RestMethod -Uri "https://api.github.com/repos/dCurre/spese-app/issues/<N>" -Headers $h).body
```
Il corpo ha sezioni **Descrizione / Ambiente (versione app, Android, dispositivo) / Stacktrace**.
Lo stacktrace è **offuscato da R8** (classi tipo `T6.p`, `v3.c`) → identifica il bug dal tipo di
errore (es. `HTTP 500`, endpoint) più che dai nomi di classe.

### Chiudere le issue

Il token `github.bugreport.token` in `local.properties` (fine-grained, permesso **Issues: Read
and write**) **chiude** le issue, non solo le apre. Usa lo script locale (gitignored):

```powershell
# Chiude una o più issue con commento opzionale; NON stampa mai il token.
powershell -File scripts/close-issue.ps1 -Comment "Rilasciato in vX.Y.Z" 6 7 8
```

Lo script legge il token da `local.properties` e fa la PATCH `state=closed` (state_reason
`completed`). **Chiudi solo dopo che l'utente ha confermato** che la fix/feature è ok (di norma
verificata su device); se è l'utente a chiudere le issue a mano dal frontend, non toccarle.

## Scelta del modello (Opus vs Sonnet)

Default **Sonnet**; passa a **Opus** per i punti difficili.
- **Sonnet** (meccanico, ben definito): conversione i18n stringa per stringa, aggiungere voci a
  menu/liste, cambiare colori/padding/testi, edit ripetitivi su molti file, feature la cui logica
  esiste già (serve solo la UI).
- **Opus** (ragionamento/rischio): debug non ovvio, refactor ampi, modifiche a navigazione o
  architettura, build/release e R8, decisioni di design, tutto ciò dove sbagliare costa.
- Si cambia al volo con `/model`; `/fast` = Opus con output più rapido.

## Dove sta il contesto

Decisioni e fatti stabili del progetto stanno nella memoria di Claude; le cose DA FARE stanno in
`ROADMAP.md`. Questo file = solo convenzioni operative.
