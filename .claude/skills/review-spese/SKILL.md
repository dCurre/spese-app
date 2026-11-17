---
name: review-spese
description: >-
  THE code review for the spese-app project — always use this instead of the generic /code-review in
  this repo, because it runs /code-review AND adds the spese-app-specific pass on top. Use this skill
  WHENEVER the user wants ANY kind of code review, check, or sanity-pass over their changes here, even
  with generic wording and even if they don't mention spese-app: "rivedi il codice", "controlla le
  modifiche", "fai una review", "dai un'occhiata", "è a posto questo?", "review this", "check my
  code", or before shipping a non-trivial change. If you're about to run /code-review in this repo,
  run this skill instead — it's a superset. It layers domain knowledge (serialization/explicitNulls
  traps, the local-first propagation chain, Room migrations, Compose pitfalls, the backend data
  contract) on top of the built-in review, catching the silent, project-specific bugs that compile
  fine and only break at runtime or in release.
---

# Review changed code (spese-app)

A generic reviewer reads Kotlin well but can't know THIS project's traps — the ones that compile
cleanly and fail silently at runtime, in release, or on a second device. This skill runs the generic
review, then adds a spese-app pass for exactly those.

## Step 1 — Generic review first

Run the built-in review on the current diff (or the target the user named):
```
/code-review
```
Let it surface the language-level and logic issues (correctness, null, reuse, simplification,
efficiency). Use the effort level the user asked for; default to its own default otherwise.

## Step 2 — spese-app specific pass

Then, over the same diff, check the project's recurring traps below. For the full rationale of each,
the project `CLAUDE.md` is the source of truth — this is the actionable checklist. Only flag a
category when the diff actually touches it.

### A. Serialization / networking (silent or release-only bugs)
- **`explicitNulls = false` AND defaults-not-encoded**: a field equal to its default is NOT sent, so
  the backend applies ITS default. If a value must reach the backend even when it equals the client
  default, it needs `@EncodeDefault(EncodeDefault.Mode.ALWAYS)`. (This is exactly how new lists were
  silently created as `shared`: `list_type="personal"` was omitted and the backend defaulted to
  `shared`.) Flag any new Sync*Create / request field whose default matters.
- `@Body` must be a `@Serializable` class, never a `Map` (breaks the Retrofit converter).
- New `@Serializable` models used in release: confirm the R8 keep rules cover them (kotlinx
  serialization / Retrofit) — failures appear ONLY in release ("Unable to create converter").
- Decimals arrive as JSON **strings** (`BigDecimalSerializer`); purchase price/quantity are **floats**.
  Dates: `created_at`/`updated_at`/`joined_at` are RFC 1123 (`InstantSerializer`); `expense_date` /
  `purchased_at` are plain `yyyy-MM-dd`. A wrong serializer parses to garbage without a compile error.
- App errors should surface via `ErrorSnackbarEffect`, not be swallowed.

### B. Local-first propagation chain (the #1 place fields get lost)
When a change adds or touches a field on a list/expense/checklist/product that must round-trip, verify
it's wired through EVERY link — a missing link compiles but drops the field silently:
1. Room **entity** has the field.
2. **createList/insert** persists it (not just `name`).
3. **toModel** reads it (not hardcoded — `list_type` was hardcoded to `"personal"`).
4. **Sync\*Create** (PUSH) sends it.
5. **applyServerList** (PULL merge) writes it back from the server.
Also: batch-sync `*_create` blocks require the mandatory owner fields, or the backend 500s.

### C. Room schema changes
- A schema change needs a REAL `Migration` (ALTER TABLE …), NOT `fallbackToDestructiveMigration` —
  the latter wipes the user's local data on update (it did, once). Bump the DB version AND add the
  migration in `DatabaseModule`.

### D. Compose pitfalls (these break the build, so usually caught by compile — but review intent)
- **Prefer an explicit import over an inline fully-qualified reference, especially where the
  fully-qualified form doesn't even resolve.** The canonical trap is Material icons: import
  `androidx.compose.material.icons.filled.X` and use `Icons.Default.X` (or the AutoMirrored variants
  for `ArrowBack`, `Logout`, `TrendingUp`, etc.). The inline
  `androidx.compose.material.icons.Icons.Default.X` form does NOT resolve ("Unresolved reference"). As
  a general rule, reach for a top-of-file `import` instead of writing the package path inline — it's
  more readable and sidesteps resolution traps.
  (Exceptions that are fine and already used in this codebase: convention-driven fully-qualified
  annotations like `@kotlinx.serialization.Transient`, and one-off JDK util calls like
  `java.util.UUID.randomUUID()`. Don't flag those.)
- Experimental composables (FlowRow, SegmentedButton, ExposedDropdown, DatePicker, clickable Card)
  need `@OptIn(...)`.
- `by remember { mutableStateOf(...) }` needs `getValue` AND `setValue` imports.
- Inside `ExposedDropdownMenuBox` use `DropdownMenu`, not `ExposedDropdownMenu`.

### E. Tombstones / deletion
- A delete of a row that was never synced (`remoteId == null`) should hard-delete immediately (no
  useless tombstone), and should also remove its orphan children (expenses of a list, items/categories
  of a checklist, purchases of a product). A tombstone is only for rows the server knows about.

## Step 3 — Report

Merge the two passes into one review. Clearly separate the generic findings from the spese-app ones so
the user sees what the project-specific pass added. For each spese-app finding, point at the exact link
in the chain or the exact trap, and give the concrete fix (e.g. "add `@EncodeDefault(ALWAYS)` to
`SyncExpensesListCreate.listType`").
