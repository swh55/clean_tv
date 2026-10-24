# Preloads commonly seen on Middle East / North Africa firmware

A starting checklist for people in the MENA region. This is **not** a tested list: tested lists
live in [`examples/`](../../examples/) only after somebody has lived with them for a few days
(the workflow is [docs/workflows/tv-list.md](../workflows/tv-list.md)). Right now the only Middle
East unit there is [TCL 55P7K](../../examples/tcl-55p7k/debloat.txt). Everything on this page is a
candidate to check, not a list to run, and **every package must be verified per device with
`dumpsys package <name>` before disabling**.

## Why regional firmware differs

Xiaomi, TCL, Hisense, Skyworth and the China-platform boards re-badged under many other names
build regional variants on top of their global set. A Middle East build usually adds local
streaming and channel apps, operator or retail builds add their own portals, and ad/ACR services
that ship disabled elsewhere can arrive enabled at the factory: on the 55P7K, Samba TV ACR and
TCL's logkit shipped enabled on the Middle East unit while the US unit of the same platform had
them off. So a list copied from a European or Indian set of the same model will not match, and a
list copied from this page will not match either until you have verified it on your own TV.

## The rule

- Verify first. Find candidates with `pm list packages | grep -i <keyword>`, then read what a
  package really is (activities, services, permissions) with `dumpsys package <name>` before it
  is ever disabled. Manufacturer package names often do not say what a package does.
- Disable, never uninstall: `pm disable-user --user 0`, one package per shell call, in small
  batches with a screenshot between them, then one reboot and a comparison of
  `pm list packages -d` against what you disabled. Regional streaming apps are among the things
  that come back: on one Gulf-region set, Yango Play, Jawwy TV and STARZ ON woke at every boot
  from their own WorkManager/recommendation jobs
  ([examples/tornado-65ua1400e](../../examples/tornado-65ua1400e/debloat.txt)).
- Keep what the TV needs: the TV/tuner app and the input services that play HDMI and the tuner,
  the input/source menu, remote and Bluetooth pairing services, the on-screen keyboard, the Play
  Store and Play Services, and the settings apps.
- `tools/disable.sh` refuses core Android packages, skips the app that owns Home, skips
  `com.google.android.tungsten.setupwraith` while it owns the saved Wi-Fi network, and skips
  packages that drive the HDMI/AV inputs or answer Settings; `tools/restore.sh` undoes a run.

## Commonly reported families

Every row below is **verify before disabling**. "Name varies by firmware" means the exact package
name has not been confirmed on a specific build in this repo - use the command in the last
column, then read the match with `dumpsys package` before deciding anything.

| Family | Likely package name(s) | What it does | Verify before disabling: find the exact name |
|---|---|---|---|
| Shahid by MBC | `net.mbc.shahid` family; name varies by firmware | MBC's streaming service, one of the most common regional preloads on Middle East builds; kept by choice on the Panasonic unit in [examples/panasonic-4ktv-0su](../../examples/panasonic-4ktv-0su/debloat.txt) | `pm list packages \| grep -i shahid` |
| OSN+ | `com.osn.go` (seen on the Middle East TCL G10 unit) | Pay-TV streaming app, disabled by choice on that set | `pm list packages \| grep -i osn` |
| STARZ ON / STARZPLAY | Name varies by firmware | STARZ's regional streaming service; the free STARZ ON app re-woke at every boot on one Gulf-region set | `pm list packages \| grep -i starz` |
| TOD (beIN) | Name varies by firmware | Sports and entertainment streaming, seen installed on some Middle East Google TV sets ([tcl-q7c-google-tv](../../examples/tcl-q7c-google-tv/README.md)) | `pm list packages \| grep -i tod` |
| iflix MENA | Name varies by firmware | Regional streaming service; less common on recent builds | `pm list packages \| grep -i iflix` |
| Jawwy TV / Yango Play | Name varies by firmware | Regional streaming and recommendation apps reported on Gulf-market sets | `pm list packages \| grep -iE 'jawwy\|yango'` |
| Weyyak | Name varies by firmware | Free Arabic streaming service | `pm list packages \| grep -i weyyak` |
| Disney+ MEA, iQIYI international | `com.disneyplus.mea`, `com.iqiyi.i18n.tv` (on the 55P7K) | Regional builds of global services, preloaded alongside the ones above | `pm list packages \| grep -iE 'disney\|iqiyi'` |
| Local TV-channel apps | Family; name varies by firmware | National and private broadcaster apps, sometimes operator-branded; nothing in this repo names one | `pm list packages \| grep -i <channel or operator name>` |
| Anoki ACR / FAST channels | `tv.anoki.acr.controller`, `tv.anoki.acr.anokiacroptin`, `com.cltv.fast` | Viewing recognition and free ad-supported channels on Changhong/Expressluck "AI PONT" boards; the same families other lists disable | `pm list packages \| grep -iE 'anoki\|cltv'` |
| Samba TV ACR | `tv.samba.ssm.ui` | Viewing tracking on TCL; shipped enabled at the factory on the Middle East G10 unit while US units had it off | `pm list packages \| grep -i samba` |
| Alphonso ACR | `tv.alphonso.alphonso_eula` | Viewing tracking that reads app usage stats, seen on Xiaomi boxes | `pm list packages \| grep -i alphonso` |
| Hisense/VIDAA ACR | `com.hisense.evservice`, `com.vt.tv.evservice` | Viewing recognition, watching HDMI inputs too | `pm list packages \| grep -i evservice` |
| Operator portals | Family; name varies by firmware | Operator- and retailer-branded portals, billing or promo screens and updater agents on subsidised builds; no confirmed names here | `pm list packages \| grep -i <operator brand>` |

Do not take the absence of a family from this table as evidence a package is safe to disable, and
do not launch a package to see what it does: some vendor factory/production apps
(`com.cvte.tv.hkcfac` and friends) break the Input key and CEC when opened. `dumpsys package`
answers without starting anything, and [TROUBLESHOOTING.md](../TROUBLESHOOTING.md) has the way
out if one has been opened.

## Building your own list instead of trusting any table

The flow in [PROMPT.md](../../PROMPT.md): connect over ADB, back up the full package list, the
disabled-package list, `getprop` and settings before changing anything, then sort the packages
into manufacturer telemetry/analytics, content and ad services, preloads and streaming apps.
Checklist question before streaming apps and assistants: disable only the ones you do not use.
Disable in small batches with `pm disable-user --user 0`, screenshot between batches, reboot once
at the end and compare `pm list packages -d` with what you disabled - some firmware re-enables
packages at every boot from its own list, and MENA streaming apps have done exactly this on sets
reported here. Note those in the list header instead of disabling them again after each boot.

When your list has lived a few days, add it to `examples/` as
[docs/workflows/tv-list.md](../workflows/tv-list.md) describes; a report without a list belongs
in `docs/devices/` with one line in the notes index at the end of `examples/README.md`. A MENA
unit of a model that already has a list is an update to that list's header (same product code
and firmware family), not a competing folder.

## For Arabic-speaking users

- The [prayer-times add-on](../../addons/prayer-times/README.md) shows today's five prayer times
  and a countdown, as a launcher module card; the city and calculation method are set before
  building.
- The launcher's `hijri_date` key (in `launcher/app/src/main/res/values/config.xml` or a theme's
  `config.json`, documented in the [themes key table](../../themes/README.md)) shows the Islamic
  Hijri date next to the Gregorian one; `shamsi_date` is the Solar Hijri used in Iran and
  Afghanistan.

## Validation scope

Nothing on this page was measured on a TV by this note's author. Package names in the confirmed
column come from this repo's own tested lists and READMEs, cited in the rows; every other name is
a family description or a grep keyword. No IP addresses, no accounts, no screenshots - a page
like this can only become an `examples/` list after somebody verifies each package with
`dumpsys package` on a real regional firmware and lives with the result.
