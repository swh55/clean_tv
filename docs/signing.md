# The release key

Scoop TV ships as one APK signed with **one permanent key**. This page is the key's whole story:
why it exists, how the build uses it, how the CI uses it, and what happens if it is ever lost.

## Why one key

Android refuses an update whose signature differs from the installed app — the installer answers
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` and nothing is written. Two history lessons from this very
repo:

1. The TV's original Afnon (`tv.afnon.home`) was signed by its own developer with his release key.
   No rebuild of that code — ours or anyone's — can ever install over it. That is not a bug to
   fix; it is the point of signatures. **Scoop TV does not try**: it is its own app
   (`pro.datascoop.tv`), installed *beside* the original, never over it, never touching its data.
2. v0.9's `scoop-tv.apk` was `assembleDebug` output renamed: signed by whatever throwaway debug
   keystore the CI runner had generated that hour (`CN=Android Debug`, a new one on every run).
   The package name was right, but the signature could never repeat — no future build could have
   updated it, on any TV.

From 0.9.1 on, every release APK — local or from CI — is signed with the same key, and an
update is a plain `adb install -r scoop-tv.apk`.

## The key itself

- File: `scoop-tv.keystore` (JKS, RSA 2048, alias `scoop-tv`, valid ~100 years).
- It is **never committed** — `.gitignore` also rejects any `*.keystore`/`*.jks` by name, so an
  accidental drop into the tree cannot be committed.
- It is not in the repo and not in the build logs. The hand-over copy ships with the build
  (`SCOOP-TV-KEYSTORE.md` beside it carries the passwords and the fingerprint).
- Back it up: a second copy somewhere safe (password manager, encrypted drive). If the key is
  lost, updates can no longer install over existing installs — users would have to remove
  Scoop TV first (its data goes with it) and install fresh. The key outlives any one machine.

## How the build reads it

`app/build.gradle` takes the four values from gradle properties or environment variables —
never from a file in the repo:

| value | gradle property | environment variable |
|---|---|---|
| keystore file | `scoopKeystore` | `SCOOP_KEYSTORE` |
| store password | `scoopStorePassword` | `SCOOP_STORE_PASSWORD` |
| key alias | `scoopKeyAlias` | `SCOOP_KEY_ALIAS` |
| key password | `scoopKeyPassword` | `SCOOP_KEY_PASSWORD` |

Local release build (relative paths resolve from the repo root):

```bash
./gradlew clean testDebugUnitTest assembleRelease --no-daemon \
  -PscoopKeystore=/safe/place/scoop-tv.keystore \
  -PscoopStorePassword=… -PscoopKeyAlias=scoop-tv -PscoopKeyPassword=…
```

Or with environment variables, which is what CI does:

```bash
export SCOOP_KEYSTORE=/safe/place/scoop-tv.keystore SCOOP_STORE_PASSWORD=… \
       SCOOP_KEY_ALIAS=scoop-tv SCOOP_KEY_PASSWORD=…
./gradlew clean testDebugUnitTest assembleRelease --no-daemon
```

A release build without the key **fails loudly** (a `GradleException` with this page's name) —
it never falls back to unsigned, and it never falls back to a debug key. `assembleDebug` needs
none of this: debug APKs stay run artifacts, never releases.

## How CI signs a tag

`.github/workflows/android.yml` reads four repository secrets (Settings → Secrets and
variables → Actions):

| secret | holds |
|---|---|
| `KEYSTORE_BASE64` | the keystore file, base64 (`base64 -w0 scoop-tv.keystore`) |
| `KEYSTORE_PASSWORD` | the store password |
| `KEY_ALIAS` | `scoop-tv` |
| `KEY_PASSWORD` | the key password |

Setting them from a terminal with the GitHub CLI:

```bash
gh secret set KEYSTORE_BASE64 --repo swh55/clean_tv < scoop-tv.keystore.base64
gh secret set KEYSTORE_PASSWORD --repo swh55/clean_tv
gh secret set KEY_ALIAS          --repo swh55/clean_tv
gh secret set KEY_PASSWORD       --repo swh55/clean_tv
```

On a `v*` tag the workflow decodes the keystore into the runner's temp directory, passes the
passwords as environment variables (GitHub masks secrets in the logs), runs
`assembleRelease`, then **verifies before publishing**: `apksigner verify --print-certs`, the
package name must read `pro.datascoop.tv`, the versionCode must have been raised — only then is
the APK attached to the tag's release as `scoop-tv.apk`. A tag without the secrets set fails
with an error pointing here; it never attaches an unsigned or debug APK.

Publishing a release is therefore:

```bash
# versionCode and versionName already raised in app/build.gradle, CHANGELOG entry written
git tag v0.9.1
git push origin v0.9.1
```

## Releasing beside the original Afnon — never over it

```bash
adb connect <tv-ip>:5555
adb install -r scoop-tv.apk          # installs/updates Scoop TV only
```

- `tv.afnon.home` (the original Afnon) is untouched: no uninstall, no `pm clear`, no data loss.
  Both apps stand side by side in the TV's app list.
- Updating Scoop TV later is the same `adb install -r scoop-tv.apk` with a newer, same-key,
  higher-versionCode APK. Nothing else is needed.
- If Android ever answers `INSTALL_FAILED_UPDATE_INCOMPATIBLE` for a Scoop TV update, the APK
  was not signed with this key — stop, find out who signed it, do not uninstall anything.
