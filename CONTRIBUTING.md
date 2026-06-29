# Contributing

## Commit messages - Conventional Commits

This repo uses [Conventional Commits](https://www.conventionalcommits.org). The commit **type**
drives both the changelog and the next version number, so it matters:

```
type(scope)!: subject

body (what & why)

footer (BREAKING CHANGE: ... / Refs #123)
```

| type | when | version effect |
|------|------|----------------|
| `feat` | a new capability | **minor** bump (1.2.0 → 1.3.0) |
| `fix` | a bug fix | **patch** bump (1.2.0 → 1.2.1) |
| `docs` `style` `refactor` `perf` `test` `build` `ci` `chore` | everything else | no bump (still listed in the changelog) |
| any with `!` or a `BREAKING CHANGE:` footer | incompatible change | **major** bump (1.2.0 → 2.0.0) |

Examples:
- `fix(audio): stop restarting audioserver on every engine toggle`
- `feat(panel): sync engine state with JamesDSP's own switch`
- `refactor!: drop the Magisk activation path`

### Enable the local checks (once per clone)

```sh
git config core.hooksPath .githooks      # validate commit messages
git config commit.template .gitmessage   # prefill the template when committing
```

The `commit-msg` hook is dependency-free POSIX sh - it only validates the header grammar.

## Releases - automated

You don't tag or write changelogs by hand. The
[`release-please`](.github/workflows/release-please.yml) workflow runs on every push to `main`:

1. **release-please** accumulates releasable commits into a **release PR** that bumps
   `versionName` in [`app/build.gradle.kts`](app/build.gradle.kts) (the `versionCode` is derived
   from it automatically). Review and merge that PR when you want to ship.
2. Merging tags the release (`vX.Y.Z`) and creates a **draft** GitHub Release. The publish job
   then builds the release APK, uses **git-cliff** to render the release notes and regenerate
   [`CHANGELOG.md`](CHANGELOG.md), attaches the APK, and publishes the release.

So: land conventional commits → merge the release PR → the release builds and publishes itself.
No release PR appears until there's at least one `feat`/`fix` (or breaking) commit since the last
release.

Preview the changelog locally any time with `git cliff` (or `git cliff -o CHANGELOG.md`).

### Release signing (one-time setup)

The publish job signs the APK from environment variables backed by GitHub secrets. The keystore
is your permanent upgrade identity, so generate it once, **back it up somewhere safe, and never
commit it** (`*.jks` is gitignored). A local `assembleRelease` without these set still works — it
just produces an unsigned APK.

1. Generate a keystore (pick your own passwords; keep the alias):

   ```sh
   keytool -genkeypair -v -keystore release.jks -alias thortune \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=ThorTune, O=androosio, C=GB"
   ```

2. Add the four secrets to the repo (the keystore goes in base64-encoded):

   ```sh
   gh secret set KEYSTORE_BASE64   < <(base64 -w0 release.jks)
   gh secret set KEYSTORE_PASSWORD                 # the store password from step 1
   gh secret set KEY_ALIAS         --body thortune # the alias from step 1
   gh secret set KEY_PASSWORD                      # the key password from step 1
   ```

That's it — the next release builds a signed APK. To rotate the key later, regenerate and re-set
the secrets, but note: a release signed with a different key can't upgrade installs of the old one,
so users would have to uninstall/reinstall.
