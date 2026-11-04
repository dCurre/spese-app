# Spese — App Android

App Android nativa (Kotlin + Jetpack Compose) per la gestione di spese condivise, liste
della spesa e storico prezzi.

## Funzionalità

- **Spese condivise**: liste condivise/personali, registrazione spese, calcolo saldi
  ("chi deve a chi") con grafico per persona o tipologia, partecipanti e ospiti,
  condivisione via link, trasferimento proprietà.
- **Checklist**: liste della spesa collaborative con categorie e sottocategorie, spunta,
  quantità, modifica in blocco, import/export (incl. formato Samsung Notes), condivisione.
- **Storico prezzi**: prodotti, registro acquisti, grafico andamento prezzo e statistiche.
- **Profilo**: avatar, tema chiaro/scuro/auto/legacy, statistiche, modifica dati.
- **Amministrazione** (ruoli admin): tipi spesa, unità di misura, ruoli, utenti.
- **Realtime** (Supabase), **aggiornamento in-app** via GitHub Releases, **segnalazione bug**.

## Stack

| Layer | Tecnologia |
|-------|-----------|
| Linguaggio / UI | Kotlin + Jetpack Compose (Material 3) |
| Architettura | MVVM (ui → viewmodel → repository → api/realtime) |
| DI | Hilt |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| Auth | Firebase Auth + Google Sign-In (Credential Manager) |
| Realtime | Supabase Realtime (client Kotlin) |
| Immagini | Coil |

Package applicazione: `com.dcapps.spese`.

## Autenticazione

Il backend non verifica token: gli endpoint sono pubblici. Il login consiste nel login
Google (Firebase, per ottenere l'email) seguito da `POST /api/users/upsert-by-email`; lo
`User.id` restituito viene usato nelle chiamate successive.

## Requisiti di sviluppo

- **JDK 17+**
- **Android SDK** in `%LOCALAPPDATA%\Android\Sdk` (platform-tools, platform android-35,
  build-tools 35.0.0). Il percorso va in `local.properties` (non versionato).
- Build via Gradle wrapper: `./gradlew` (scarica Gradle al primo avvio).

### File non versionati da predisporre

Questi file/valori sono in `.gitignore` e vanno creati in locale:

1. **`app/google-services.json`** — dal progetto Firebase `spese-dc`, app Android
   `com.dcapps.spese`. Registra anche l'impronta **SHA-1** della keystore con cui firmi
   (debug e/o release) e abilita **Google** come metodo di accesso. Senza questo file
   l'app va in crash all'avvio.
2. **`local.properties`** — oltre a `sdk.dir`, può contenere il token per le
   segnalazioni bug:
   ```
   github.bugreport.token=<token GitHub fine-grained con permesso Issues: write>
   ```

I valori di rete (`API_BASE_URL`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`,
`GOOGLE_WEB_CLIENT_ID`) sono già impostati come `BuildConfig` in
[`app/build.gradle.kts`](app/build.gradle.kts). La anon key Supabase è pubblica per natura.

## Build

```bash
./gradlew :app:assembleDebug     # APK di debug in app/build/outputs/apk/debug/
./gradlew :app:installDebug      # installa su device/emulatore collegato
./gradlew :app:assembleRelease   # APK release (firma da secret/keystore, vedi CI)
```

> Il backend è su Render: il primo accesso dopo inattività può essere lento (cold start).

## Release automatiche (CI)

Ad ogni push su `main`, GitHub Actions ([.github/workflows/release.yml](.github/workflows/release.yml))
determina la versione dal file `VERSION` (incrementa la patch se la versione corrente è già
taggata, altrimenti pubblica quella indicata), compila l'APK release firmato e crea una
GitHub Release `vX.Y.Z` con l'APK allegato. Secret richiesti sul repo:
`RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD`,
`RELEASE_KEY_ALIAS`, `GOOGLE_SERVICES_JSON_BASE64`, `BUGREPORT_TOKEN`.
