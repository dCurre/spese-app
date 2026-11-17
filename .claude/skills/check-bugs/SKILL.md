---
name: check-bugs
description: >-
  Inspect and triage the crash/error reports that the spese-app Android client files as GitHub Issues
  on dCurre/spese-app. Use this skill WHENEVER the user wants to check, review, read, triage, or close
  app bug reports / crash reports / GitHub issues for this project — e.g. "controlla i bug", "quali
  crash sono arrivati", "leggi la issue 12", "chiudi le issue risolte". It lists open issues via the
  public GitHub REST API (because `gh` is NOT installed here), reads an issue's obfuscated R8
  stacktrace, and closes issues with the token-safe local script. Do NOT print the bug-report token,
  and close issues ONLY after the user confirms the fix is verified.
---

# Check / triage app bug reports (spese-app)

The Android app sends crashes and errors as GitHub Issues to the **public** repo `dCurre/spese-app`.
There's no auth needed to READ a public repo's issues. `gh` is NOT installed on this machine — use the
REST API directly via PowerShell. All commands here use PowerShell.

## 1. List open issues (excludes PRs)

```powershell
$h = @{ "User-Agent"="spese-app-client"; "Accept"="application/vnd.github+json" }
(Invoke-RestMethod -Uri "https://api.github.com/repos/dCurre/spese-app/issues?state=open&per_page=50" -Headers $h) |
  Where-Object { -not $_.pull_request } | ForEach-Object { "#$($_.number) [$($_.created_at)] $($_.title)" }
```

The `Where-Object { -not $_.pull_request }` is required — the issues endpoint returns PRs too, and you
don't want those in a bug triage.

## 2. Read one issue's detail

```powershell
$h = @{ "User-Agent"="spese-app-client"; "Accept"="application/vnd.github+json" }
(Invoke-RestMethod -Uri "https://api.github.com/repos/dCurre/spese-app/issues/<N>" -Headers $h).body
```

The body has three sections: **Descrizione / Ambiente (app version, Android, device) / Stacktrace**.

**The stacktrace is R8-obfuscated** — class names look like `T6.p`, `v3.c`. Do NOT try to map those
names to source classes; they won't match. Diagnose from the *kind* of error instead: the exception
type, an `HTTP 500`, the endpoint or message text, the app version in the Ambiente section. Cross-
reference the app version with recent commits/CHANGELOG to see what shipped around when the crash
appeared.

## 3. Close resolved issues (token-safe)

Closing REQUIRES the fine-grained token `github.bugreport.token` from `local.properties` (permission
**Issues: Read and write**). Use ONLY the local script — it reads the token itself and NEVER prints it:

```powershell
# Closes one or more issues (state=closed, state_reason=completed), with an optional comment.
powershell -File scripts/close-issue.ps1 -Comment "Rilasciato in vX.Y.Z" 6 7 8
```

- Never read, echo, or inline the token yourself. The script exists precisely so the token never
  touches the transcript. Do not reconstruct the PATCH call by hand with the token.
- The comment convention is "Rilasciato in v<version>" when the fix shipped in a release.

## When to close — and when NOT to

- **Close only after the user confirms** the fix/feature works (normally verified on a device). A bug
  report is the user's to close — a green build is not confirmation.
- If the user is closing issues themselves from the GitHub UI, leave them alone — don't double-close.
- When you *think* an issue is fixed but it's not confirmed, say so and propose closing it; wait for
  the user's go-ahead before running the script.

## Typical flow

1. User: "controlla i bug" → run step 1, present the open issues compactly (`#N [date] title`).
2. User asks about one → run step 2, read the body, diagnose from the error kind + app version.
3. Once the user confirms a fix is live and verified → run step 3 to close, with the release comment.
