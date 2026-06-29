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
