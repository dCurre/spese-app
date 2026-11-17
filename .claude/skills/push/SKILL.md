---
name: push
description: >-
  Commit and push changes for the spese-app project the project-correct way. Use this skill
  WHENEVER the user asks to commit, push, ship, release, or publish changes in this repo — even if
  they just say "pusha", "committa", "fai il push", or "rilascia". It runs the full project workflow:
  secret safety-check, CHANGELOG section for the bumped version (the pre-push hook REJECTS a push
  without it), the `[version] - description` commit format, and it correctly treats the hook's final
  "failed to push" as the expected success signal. Do NOT push with a plain `git push` without this
  skill, and do NOT bump VERSION by hand.
---

# Push workflow (spese-app)

This repo has a non-obvious push pipeline. A naive `git commit` + `git push` will either be rejected
by the pre-push hook (missing CHANGELOG), leak a secret, or confuse you with an expected error. This
skill encapsulates the whole thing so a push is correct and safe every time.

## Sentinel for the commit/push guard hook

A project `PreToolUse` hook BLOCKS plain `git commit` and `git push` and redirects to this skill. When
THIS skill runs those commands, prefix them with `SPESE_SKILL_PUSH=1` so the hook lets them through:
```bash
SPESE_SKILL_PUSH=1 git commit -m "..."
SPESE_SKILL_PUSH=1 git push
```
Only use this prefix from inside this skill's workflow — it's what tells the guard "this is the vetted
path, not a hand-rolled commit." Every `git commit`/`git push` example below already includes it.

## Non-negotiable rules

- **Push only on the user's explicit OK.** Never push just because changes are ready. If the user
  hasn't clearly said to push/ship/release in this turn, stop and ask.
- **Never bump `VERSION` by hand.** The `pre-push` hook bumps the patch automatically and amends it
  into the same commit. Touching VERSION yourself causes a double bump.
- **Never commit secrets.** `local.properties`, `google-services.json`, keystores, `.b64` and `.apk`
  files are gitignored — but always verify before `git add -A`.
- **Git identity is dCurre, with NO Claude attribution** in commit messages. Do not add
  "Co-Authored-By: Claude" or "Generated with Claude Code" lines.
- Commit message format: `[version] - description` (see below).

## How the pre-push hook works (so its output doesn't confuse you)

On every `git push`, `scripts/hooks/pre-push`:
1. Bumps `VERSION`'s patch (e.g. 0.2.11 → 0.2.12) and `--amend`s it into the last commit, so the
   version travels in the SAME commit as the code (no extra commit).
2. Pushes the updated commit itself, then ABORTS the original push.

So the very last line, `error: failed to push some refs`, is **EXPECTED and means success** — it's
the hook blocking the stale (pre-bump) push after it already pushed the good one. GitHub receives a
single push → a single release build. Do not retry on seeing this error; verify instead (see step 6).

The hook also REJECTS the push if `CHANGELOG.md` lacks a `## <new-version>` section for the bumped
version. That's why step 3 is mandatory.

## Steps

Run these in order. Stop and surface the problem to the user if any check fails.

### 1. Confirm the push is authorized
The user must have explicitly asked to push/ship/release in this turn. If not, ask first — do not
proceed.

### 2. Safety-check for secrets
Before staging, make sure no sensitive file is about to be committed:
```bash
git status --short | grep -iE "local.properties|google-services|keystore|\.b64$|\.apk$|\.jks$" \
  && echo "STOP: sensitive file staged — do not push" || echo "no sensitive files"
```
If anything matches, STOP and tell the user. Do not push.

### 3. Add the CHANGELOG section for the NEXT version (mandatory)
The hook will bump `VERSION`'s patch. Compute the next version and add its section, or the push is
rejected.
```bash
cat VERSION   # current, e.g. 0.2.11 → next will be 0.2.12
```
Add a `## <next-version>` section at the TOP of the version list in `CHANGELOG.md` (right under the
header, above the previous version). Write **user-facing** release notes in Italian — plain language,
one bullet per change, describing the user benefit, NOT implementation details. These notes are shown
in the app's update screen, so they must read like product notes, not a git log.

Example section:
```markdown
## 0.2.12
- Breve descrizione in italiano di cosa cambia PER L'UTENTE, orientata al beneficio.
- Un punto per ogni cambiamento rilevante.
```

### 4. Update ROADMAP if needed
If any item in `.claude/ROADMAP.md` was completed by these changes, REMOVE it (the committed roadmap
must contain only still-open work — don't leave `[x]` lines). If nothing matches, leave it as is.

### 5. Stage, commit, push
```bash
git add -A
git status --short   # review one last time
```
Commit with the `[version] - description` format, where `version` is the NEXT version from step 3.
Write a thorough, factual description (what changed and why). Prefix with the sentinel so the guard
hook allows it:
```bash
SPESE_SKILL_PUSH=1 git commit -m "[0.2.12] - descrizione fattuale dei cambiamenti: cosa e perché."
```
(Keep the `[version] - description` shape. For a long multi-line message, write it to a temp file and
use `SPESE_SKILL_PUSH=1 git commit -F <file>`.)

Then push and let the pre-push hook do its work:
```bash
SPESE_SKILL_PUSH=1 git push
```
Remember: the final `error: failed to push` is EXPECTED success (see above). Do not retry.

### 6. Verify the push landed
Confirm the remote advanced and VERSION was bumped into the pushed commit:
```bash
git log --oneline -1 origin/main
cat VERSION
```
`origin/main` should show your `[next-version]` commit, and `VERSION` should equal the next version.
If so, report success: the release workflow (`release.yml`) will build and publish the GitHub Release
for that version, reading the notes from the CHANGELOG section you wrote.

## Commit message format

**Example:**
`[0.2.12] - fix ordinamento prodotti: il criterio marca ora usa anche il nome come tie-break; aggiunto indice su listId per velocizzare la lista`

The leading `[version]` must be the version the hook will produce (current VERSION + 1 patch). The
description is factual and specific — it's the developer-facing record, distinct from the user-facing
CHANGELOG notes.

## When NOT to push

- If the user only asked to commit (not push), do steps 1–5 but stop before `git push`.
- If on a branch other than `main` and the user expects a release, note that `release.yml` triggers on
  `main` — confirm the target branch with the user.
- If the build hasn't been verified and the changes are non-trivial, suggest compiling first
  (`./gradlew :app:compileDebugKotlin --no-daemon`) before pushing, since a broken push still triggers
  a release build.
