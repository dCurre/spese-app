---
name: develop-spese
description: >-
  The clean-development playbook for the spese-app Android project — the project's traps and working
  principles, so code is written correctly from the start instead of fixed afterwards. Use this skill
  WHENEVER you are about to write or change code in this repo: implementing a feature, fixing a bug,
  refactoring, adding a field/endpoint/screen, wiring sync, touching Room, Compose, or serialization —
  even when the user just says "sviluppa X", "aggiungi Y", "fai una modifica", "implementa", "correggi
  questo", without naming any rule. Read it BEFORE writing the first edit of a development task. It
  encodes the serialization/explicitNulls traps, the local-first propagation chain, Room migration
  safety, Compose pitfalls, and the import/no-fully-qualified rule — the things that compile fine but
  break silently at runtime or in release. For the review AFTER coding, use review-spese.
---

# Clean development playbook (spese-app)

Read this before starting a development task here. It's the "write it right the first time" companion
to `review-spese` (which checks it afterwards). The `CLAUDE.md` holds the full rationale; this is the
working playbook.

## Working principles (how to develop here)

- **Propagate a new field through the WHOLE local-first chain.** A field added to a list/expense/
  checklist/product that must round-trip has to be wired through all of: Room **entity** → **insert/
  create** → **toModel** → **Sync\*Create** (push) → **applyServerList** (pull merge). Miss one link
  and it compiles but drops the value silently. (This is how `list_type` was lost.)
- **Room schema change ⇒ a real `Migration`**, never `fallbackToDestructiveMigration` — that wipes the
  user's local data on update. Bump the version AND add an `ALTER TABLE` migration in `DatabaseModule`.
- **Compile after non-trivial edits** and read the filtered result (see the `build-check` skill). Don't
  assume it builds; don't re-read files to "verify" an edit that the tool already confirmed.
- **No shortcuts that create silent debt**: don't hardcode a value that should come from data (a
  `toModel` returning a constant), don't swallow errors (route them through `ErrorSnackbarEffect`),
  don't leave orphan rows when hard-deleting a parent.
- **Backend lives in `spese-ms`** — don't build server logic inside the app. Check existing endpoints
  in `app/api/*/routes.py` before writing the client. Touch the backend only when the feature needs
  it, and push it only on explicit OK.
- **When the design is genuinely ambiguous, ask** rather than guessing — a wrong assumption baked into
  a feature is costlier than a question.

## The traps (compile ≠ correct)

### Serialization / networking
- **`explicitNulls = false` + defaults-not-encoded**: a field equal to its default is NOT serialized,
  so the backend applies ITS default. If a value must reach the backend even when it equals the client
  default, annotate it `@EncodeDefault(EncodeDefault.Mode.ALWAYS)`. (Omitting `list_type="personal"`
  made the backend default it to `shared` — personal lists became shared.)
- `@Body` must be a `@Serializable` class, never a `Map`.
- New `@Serializable` models: the R8 keep rules must cover them, or release fails only at runtime
  ("Unable to create converter").
- Decimals are JSON **strings** (`BigDecimalSerializer`); purchase price/quantity are **floats**. Dates
  `created_at`/`updated_at`/`joined_at` are RFC 1123 (`InstantSerializer`); `expense_date`/
  `purchased_at` are plain `yyyy-MM-dd`. Wrong serializer = silent bad parse, no compile error.
- Responses are wrapped (`{expenses:[...]}`); POST returns `{id, message}`, PUT/DELETE `{message}`.

### Compose
- **Prefer an explicit `import` over an inline fully-qualified reference**, especially where the
  fully-qualified form doesn't resolve. Material icons: import
  `androidx.compose.material.icons.filled.X`, use `Icons.Default.X` (AutoMirrored for `ArrowBack`,
  `Logout`, `TrendingUp`, …); the inline `androidx.compose.material.icons.Icons.Default.X` does NOT
  resolve. (Fine as-is, don't change: convention annotations like `@kotlinx.serialization.Transient`
  and one-off JDK calls like `java.util.UUID.randomUUID()`.)
- Experimental composables (FlowRow, SegmentedButton, ExposedDropdown, DatePicker, clickable Card)
  need `@OptIn(...)`.
- `by remember { mutableStateOf(...) }` needs `getValue` AND `setValue` imports.
- Inside `ExposedDropdownMenuBox` use `DropdownMenu`, not `ExposedDropdownMenu`.
- `Card(onClick = ..., modifier = ...)`: onClick is the first param — pass names.

### Deletion / tombstones
- Deleting a row never synced (`remoteId == null`) hard-deletes immediately (no useless tombstone) and
  also removes its orphan children. A tombstone is only for rows the server already knows.

## After coding

Build (`build-check`), and for a thorough pass run `review-spese` (which also runs the generic review).
