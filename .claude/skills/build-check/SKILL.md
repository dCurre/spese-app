---
name: build-check
description: >-
  Compile/build the spese-app Android project and report the result cleanly. Use this skill WHENEVER
  you need to verify the project compiles after code changes — "compila", "builda", "verifica che
  compili", "fai la build", "controlla gli errori di compilazione", or before a push of non-trivial
  changes. It runs the right Gradle task for the goal (fast Kotlin-only check vs full debug/release
  build) and filters the noisy output down to the lines that matter (BUILD SUCCESSFUL/FAILED and the
  `e:` / `w:` lines). Prefer the fast compile for a quick correctness check; use assembleRelease only
  when R8 behavior must be verified.
---

# Build / compile check (spese-app)

Gradle output is very noisy. Always filter it to the signal. Commands run from the project root.

## Pick the right task

- **Fastest correctness check (default)** — Kotlin compile only, no packaging:
  ```bash
  ./gradlew :app:compileDebugKotlin --no-daemon 2>&1 | grep -iE "BUILD SUCCESSFUL|BUILD FAILED|^e: "
  ```
  Use this after ordinary code edits. `^e: ` lines are the actual Kotlin errors.

- **Full debug build** — compiles + packages the APK (catches resource/manifest/R issues the Kotlin
  task alone won't):
  ```bash
  ./gradlew :app:assembleDebug --no-daemon 2>&1 | grep -iE "BUILD SUCCESSFUL|BUILD FAILED|^e: |^w: "
  ```

- **Release build (R8)** — slower; only when you must verify R8/minification (e.g. after touching
  serialization, Retrofit, or `proguard-rules.pro`), since some failures appear ONLY in release:
  ```bash
  ./gradlew :app:assembleRelease --no-daemon 2>&1 | grep -iE "BUILD SUCCESSFUL|BUILD FAILED|^e: |^w: "
  ```

## Reading the result

- `BUILD SUCCESSFUL` → report success plainly. Don't re-read files to "double-check" — if it compiled,
  the edits are consistent.
- `BUILD FAILED` with `e:` lines → each `e:` is a Kotlin error with file:line. Fix the root cause;
  don't suppress. Common spese-app traps live in the project CLAUDE.md (Material icons must be
  explicitly imported; experimental Compose APIs need `@OptIn`; `by remember { mutableStateOf }` needs
  `getValue`/`setValue` imports).
- Only `grep` showing nothing but `BUILD FAILED` usually means the failure is non-Kotlin (resources,
  manifest, Gradle config). Re-run without the filter, or widen it, to see the real error.

## Notes

- `--no-daemon` keeps runs isolated and avoids stale-daemon surprises.
- A full build takes a couple of minutes; the Kotlin-only task is much faster — prefer it unless you
  specifically need packaging or R8.
- Reading build output is pre-authorized in this project; just run and report.
