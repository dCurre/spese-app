# scripts/

## Bump automatico della versione (`scripts/hooks/pre-push`)

A **ogni `git push`** la patch di `VERSION` viene incrementata automaticamente e inclusa
**nello stesso commit** del codice. Non serve ricordarsi nulla: usi `git push` come sempre.

### Attivazione (una tantum, dopo un clone)

Gli hook versionati vanno abilitati con `core.hooksPath`:

```bash
git config core.hooksPath scripts/hooks
```

Su Windows/Git-Bash l'hook è già eseguibile (bit registrato in git).

### Come funziona

1. Dai `git push`.
2. L'hook `pre-push` capisce quali commit stai pushando.
3. Se VERSION **non** è già cambiata in quei commit, incrementa la patch, la aggiunge e
   fa `git commit --amend` sull'ultimo commit (versione + codice nello stesso commit).
4. git aveva già "congelato" il vecchio HEAD per quel push: l'hook quindi **annulla** il
   push corrente e rilancia lui il push del commit aggiornato.
5. A GitHub arriva **un solo push** → **una sola build/release**. Nessun doppio bump,
   nessun commit "chore: bump" separato.

Se VERSION è già stata bumpata a mano in un commit che stai pushando, l'hook la rispetta
e non ribumpa.

### Interazione col CI (`.github/workflows/release.yml`)

Il workflow **legge soltanto** `VERSION` e pubblica la release con quel numero (non
incrementa più nulla). Se il tag `vX.Y.Z` esistesse già, fallisce di proposito per
segnalare un bump mancato.
