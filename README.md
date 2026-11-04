# Spese — App Android

App Android nativa (Kotlin + Jetpack Compose) per la gestione di spese condivise e liste
della spesa.

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

## Come funziona l'autenticazione

Il backend **non verifica alcun token**: tutti gli endpoint sono pubblici. Il "login" consiste in:

1. Login Google tramite Firebase Auth (serve solo a ottenere l'email dell'utente).
2. `POST /api/users/upsert-by-email` con i dati dell'account → ritorna il record utente.
3. Si usa lo `User.id` restituito in tutte le chiamate successive.

## Requisiti di sviluppo

- **JDK 17+** (presente: Temurin 21).
- **Android SDK** installato in `%LOCALAPPDATA%\Android\Sdk` (platform-tools, platform
  android-35, build-tools 35.0.0). Il percorso è in `local.properties` (non versionato).
- Build via Gradle wrapper: `./gradlew` (scarica Gradle 8.11.1 al primo avvio).

### Prerequisito Firebase (necessario per il login)

L'app usa il progetto Firebase **`spese-dc`**; serve registrare l'**app Android**:

1. Firebase Console → progetto `spese-dc` → *Aggiungi app* → **Android**.
2. Package name: **`app.spese`**.
3. Aggiungi l'impronta **SHA-1** della keystore di debug:
   ```bash
   keytool -list -v -alias androiddebugkey \
     -keystore "$HOME/.android/debug.keystore" \
     -storepass android -keypass android
   ```
4. Scarica **`google-services.json`** e mettilo in **`app/google-services.json`**.
5. Nel file [`app/build.gradle.kts`](app/build.gradle.kts), **de-commenta** la riga del plugin
   `alias(libs.plugins.google.services)`.
6. In *Authentication → Sign-in method*, abilita **Google**.
7. Copia il **Web client ID** OAuth (Firebase Console → impostazioni progetto, oppure GCP →
   Credenziali → *OAuth 2.0 Client IDs → Web client*) e sostituiscilo al placeholder
   `GOOGLE_WEB_CLIENT_ID` in [`app/build.gradle.kts`](app/build.gradle.kts).

> Nota: il `google-services.json` è in `.gitignore` e non va committato.

> ⚠️ **L'app si avvia correttamente solo dopo** aver aggiunto `google-services.json` e
> de-commentato il plugin: senza, Firebase non viene inizializzato e l'app va in crash
> all'avvio. Il codice compila comunque anche senza configurazione.

### Configurazione Supabase (per il realtime)

`SUPABASE_URL` e `SUPABASE_ANON_KEY` sono in `app/build.gradle.kts` come `BuildConfig`.
La URL è già impostata; **sostituisci il placeholder di `SUPABASE_ANON_KEY`** con la chiave
anon pubblica reale. La chiave *anon* è pubblica per definizione, non è un segreto.

## Build & run

```bash
# APK di debug
./gradlew :app:assembleDebug

# Installa su un dispositivo/emulatore collegato
./gradlew :app:installDebug
```

L'APK viene prodotto in `app/build/outputs/apk/debug/`.

## Configurazione di rete

Definita in `app/build.gradle.kts` (`BuildConfig`):

- `API_BASE_URL` = `https://spese-ms.onrender.com/` (backend di produzione)
- `SUPABASE_URL` / `SUPABASE_ANON_KEY`
- `GOOGLE_WEB_CLIENT_ID`

> Il backend è su Render: il primo accesso dopo inattività può essere lento (cold start).

## Stato

In sviluppo incrementale, feature per feature:

- [x] **Step 1** — Setup progetto (Gradle, Compose, Hilt, dipendenze)
- [x] **Step 2** — Layer di rete (Retrofit, modelli, API)
- [x] **Step 3** — Auth (Google Sign-In → upsert-by-email) *(richiede config Firebase per l'esecuzione)*
- [x] **Step 4** — Liste spese + saldi (elenco, dettaglio, CRUD spese, saldo con grafico)
- [x] **Step 5** — Realtime (Supabase) su liste, spese, partecipanti
- [ ] Step 6 — Checklist
- [ ] Step 7 — Profilo + tema
- [ ] Step 8 — Statistiche
