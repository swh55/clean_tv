# Changelog

All notable changes to Afnon. Versions match `versionName` in `app/build.gradle` and the `vX.Y` git tags.

## [Unreleased]

## [0.9.1] - 2026-11-14

### Fixed
- **The release APK is signed with Scoop TV's own permanent key now — not the build machine's debug key.**
  v0.9's `scoop-tv.apk` was `assembleDebug` output renamed: signed with whatever throwaway debug
  keystore the CI runner generated that hour (`CN=Android Debug`), so no two builds could ever
  agree on a signature — no future version could have updated it on any TV. The tag build is now
  `assembleRelease` signed with the one key held in the repository secrets (`KEYSTORE_BASE64`,
  `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), and CI verifies before publishing
  (`apksigner verify`, the `pro.datascoop.tv` package stamp, a raised versionCode). A release
  build without the key fails loudly instead of falling back — unsigned or debug-signed APKs can
  no longer reach a release by mistake. See `docs/signing.md`.
- The signing story is written down (`docs/signing.md`): the key's rules, the four CI secrets,
  local signed builds, backup advice, and the never-over-the-original-Afnon install contract.
- `.gitignore` now rejects `*.keystore` and `*.jks` anywhere in the tree, so the key cannot be
  committed by accident.
- versionCode 13.

## [0.9] - 2026-11-14

### Added
- **The Islamic Library** (المكتبة الإسلامية), a whole bento section off a new gold pill beside the radio
  on Home — twelve screens and three data files, no new libraries, no new permissions:
  - **Adhkar with their counters** (أذكار الصباح والمساء وأذكار بعد الصلاة): the three sections of Hisn
    al-Muslim, verbatim, every zikr carrying the repetition the book itself prints (never an invented
    count), its own source line and its note; one OK counts a repetition, the day's session is saved,
    a one-by-one mode shows a large counter, and a reset chip zeroes the section.
  - **The Qur'an** (المصحف): the Uthmani text of Tanzil — 114 surahs, 6236 verses, 604 pages, 30 juz —
    built by `tools/quran-data` with integrity gates and the Tanzil notice shipped in the APK; search by
    surah name and number with an on-screen letter/digit pad; verse search through two letters-only
    columns so a query matches with or without the diacritics (and with today's ا-spelling where the
    mushaf writes the dagger alif); a reader in Amiri with the mushaf's ornate verse numbers, the
    Bismillah as its own header row (never part of verse 1), five text sizes, jump-to-verse, the focused
    verse's own juz and page, and optional bookmarks. Reading positions are kept (text separate from
    audio) and «أكمل القراءة» returns to the exact verse.
  - **Reciters and a chapter player** (القرّاء): twelve reciters from mp3quran.net's own servers, each a
    card with a preview of Al-Fatihah and a monogram plate (no photos ship); the player plays a whole
    chapter, seeks by the bar, steps ±30 s, repeats or follows on, and carries on across screens like
    the radio — pausing for the adhan and resuming after it, never talking over the radio (one starts,
    the other stops) — and remembers the listening position per reciter and chapter.
  - **Downloads, only on demand**: any chapter of any reciter through the TV's own DownloadManager into
    the app's own files, with a manager screen (progress, sizes, delete), a finisher broadcast, and the
    player preferring the completed copy.
  - **Khatm plans** (ختمة القرآن): the 30/60/90-day presets or a custom pace (pages or juz a day, a start
    delay); a progress card with the percentage, pages and juz left, days left, the expected finish and
    the day's pages with the verse they start at; «أنجزت ورد اليوم» marks the day — **nothing is ever
    marked automatically**; falling behind shows the days behind and «إعادة توزيع الأيام» spreads the
    remaining days at the same pace without touching the history; several plans live side by side.
  - **Daily reminders, all off by default** (التذكير اليومي): morning and evening adhkar, the adhkar
    ten minutes after each prayer, the daily reading and the khatm day — each its own switch and time in
    Library settings (also a ninth section in Scoop TV Settings), a quiet Home card with Open / Later /
    Close, a banner that leaves on its own over other apps, never a sound, never over the adhan, the call
    screen or the screensaver; riding the existing fifteen-second watch, so restarts schedule nothing.
  - **Library settings** (إعدادات المكتبة): the reminder switches and times, the reader's text size,
    the adhkar counters' reset, and the download manager.
  - **Unit tests** for the pure layers: the letters-only folding (with the two-column search contract),
    khatm day arithmetic across months and years, the adhkar count words and the reminder words in all
    five languages — 14 new tests, 37 in all, green.
  - **Twelve technical reports** under `docs/library-reports/` (source and licence verification for the
    Qur'an text, the adhkar curation audit with its printed table, storage, the bento layout, each
    feature, performance, and the QA report with its honest PASS / NOT TESTED ledger) beside the data
    pipeline `tools/quran-data/` that rebuilds and re-verifies every asset from its source.

## [0.8] - 2026-10-25

### Changed
- **The app is Scoop TV now, package `pro.datascoop.tv`** (كان `pro.datascoop.afnon`): still its own app beside
  the original Afnon — every tool, doc, ADB command and download link uses the new name, and the release asset
  is `scoop-tv.apk`. The older packages (`pro.datascoop.afnon`, `tv.afnon.home`) stay hidden from the app list
  even when several are installed side by side. versionCode 10 → 11.

### Added
- **A tone with the reminder** (نغمة مع التنبيه): Settings → Reminders; two soft beeps with the reminder card
  or banner, off by default.
- **An iqamah countdown** (عدّاد الإقامة): Settings → Reminders; on the call screen, from the prayer's time —
  10/15/20/30 minutes or off, "الإقامة بعد mm:ss" once a second, «حان وقت الإقامة» at the end.
- **The Qibla on the call screen**: a small dial and "القبلة · من دمشق" under the prayer's time.
- **A 3-day forecast** (توقعات 3 أيام): from the same Open-Meteo response, today's low/high and the two days
  after it, one quiet line on Home (today by name, then tomorrow and the weekday), with its own display row.
- **Wind instead of humidity** (تفصيل الطقس): the detail after the temperature switches to wind speed and
  direction ("الرياح ١٣ كم/س شمال غرب"), from the same request.
- **A temperature unit row** (وحدة الحرارة): Celsius or Fahrenheit, applied to the current reading, the
  forecast and the Asr preview's clock alike.
- **A rain alert** (احتمال مطر اليوم): when today's chance of rain tops 50%, a gold line under the weather.
- **Weather glyphs** (رموز الطقس): sun and moon by day and night, clouds, fog, drizzle, rain, snow and a storm,
  drawn by hand on the 24px grid — no images, they follow the gold of the line.
- **The arch follows the day** (لون القوس مع الوقت): its fill shifts through deep night, a warm dawn, the day
  lapis, a warm sunset and night again; a display row turns it off.
- **A fuller Ramadan** (وضع رمضان موسّع): the imsakiya — this month's Fajr (imsak) and Maghrib (iftar) day by
  day, scrollable, with ﴿شهر رمضان الذي أنزل فيه القرآن﴾ over it; the button lives on Prayer day only in
  Ramadan.
- **Countdowns before the season** (عدادات رمضان/عرفة): "باقي على رمضان ٢٣ يومًا" under the arch's countdown
  through the last month before the first of Ramadan and the day of Arafah, with the Arabic count's proper
  forms (يوم، يومان، أيام، يوماً).
- **French, Turkish and Urdu** (Français · Türkçe · اردو): every interface string — over a hundred of them —
  now exists in all five languages, each picked by one table; Urdu runs right to left with Eastern digits by
  default, and «TV» still follows the TV's own language.
- **A kids watch timer** (مؤقت مشاهدة الأطفال): Settings → Family & startup; after 15/30/45/60 minutes in any
  other app the TV comes back to Home with «انتهى وقت المشاهدة» — watched by the remote-buttons service, paused
  whenever Home or the screensaver is in front.
- **An app at startup** (تطبيق يفتح عند الإقلاع): Settings → Family & startup; a chosen app opens itself
  10/20/30 seconds after the TV boots, once per boot, with Home underneath so Back returns.
- **The Qibla at a long press**: holding the remote's Settings button opens a dedicated Qibla screen (the dial,
  the bearing and the direction in words); a tap still opens the settings. Wherever the service filters keys.
- **Unit tests** (اختبارات وحدة): 23 of them, plain JUnit on the pure layers — prayer times (sane hours in
  Damascus, whole minutes, the Hanafi Asr, the Qibla bearings), the Open-Meteo parser against a real response
  shape, the five-language string table, and the design tokens' package and invariants. CI runs them before
  the build.

## [0.7] - 2026-10-24

### Changed
- **Afnon's package name is now `pro.datascoop.afnon`** (كان `tv.afnon.home`): the app installs as its own
  app beside the original Afnon launcher instead of updating it — install this build and the TV treats it
  as a new launcher while the original stays untouched. Every setup path uses the new name already: HOME
  and the screensaver (`settings put secure screensaver_components pro.datascoop.afnon/.AfnonDream`), the
  remote's buttons (`settings put secure enabled_accessibility_services pro.datascoop.afnon/.RemoteKeys`),
  the `tools/install.sh` helper, and the `adb` commands in the README and the Arabic install guide. The
  original Afnon (`tv.afnon.home`) stays hidden from the app list even when both launchers are installed
  side by side. versionCode 9 → 10.
- Download, clone and release links in the READMEs and the Arabic install guide now point at this repo
  (github.com/swh55/clean_tv), so `afnon.apk` always downloads this fork's build — and the CI release step
  names the APK `afnon.apk` on every `v*` tag. The original project stays credited in the README (MIT).

## [0.6] - 2026-10-24

### Added
- **The weather on Home** (الطقس): one quiet gold line under the dates — the sky, the temperature and the
  humidity for the saved city, from [Open-Meteo](https://open-meteo.com) (no key, no account; the request
  carries only the city's coordinates). Home shows the saved reading at once and refreshes at most every
  half hour; a TV that lost its network keeps its last reading. Settings → Display & language → Weather
  turns it off; it is on by default.
- **Eid and Arafah on the arch**: on 1 Shawwal the arch greets عيد مبارك · عيد الفطر المبارك, on 10 Dhu
  al-Hijjah عيد الأضحى المبارك, with «تقبل الله طاعتكم وكل عام وأنتم بخير» under the countdown; the day
  of Arafah (9 Dhu al-Hijjah) shows «اليوم يوم عرفة». Both follow the Umm al-Qura calendar and the Hijri
  date offset, like Ramadan and Friday.
- **A lock for Afnon Settings** (قفل الإعدادات): Settings → Security (الأمان), a new section. Turning the
  lock on asks for a four-digit PIN twice; after that every way into Afnon Settings (the gear on Home, the
  remote's Settings button from any app) opens the PIN pad first. The pad works with the D-pad (a grid like
  a phone keypad, which keeps its order in Arabic too) and with the remote's number keys, ⌫ deletes, Back
  backs out — and a locked Settings never opens without the right PIN. The PIN is saved only as a SHA-256
  hash, never as digits. Change PIN replaces it; turning the lock off clears it. Forgot the PIN? Clearing
  Afnon's data wipes every setting along with it (`adb shell pm clear tv.afnon.home`).
- **Damascus is Afnon's own city**: the launcher starts on دمشق (Muslim World League, Shafi'i), not Cairo.
  Every other city is still one pick away.
- A GitHub Actions workflow (`.github/workflows/android.yml`): builds the debug APK on every push to main
  and every pull request (the adhans fetched by `tools/fetch-adhans.sh`), and attaches the APK to the
  release when a `v*` tag lands.

### Changed
- versionCode 9 (was 8), versionName 0.6.

## [0.5] - 2026-10-22

### Added
- Settings → Reminders → Close the call screen: when the adhan ends, or 1, 5 or 10 minutes after it appears (10,
  as before, by default). Then it's back to what was playing. With no adhan playing (turned off, or stopped with
  the button), "when the adhan ends" closes it after a minute. The call screen's hint says which.
- Spoken reminder: «اقترب موعد صلاة …» with the reminder card or banner (Settings → Reminders, on by default).
- Announce the time: «حان الآن موعد صلاة …» when a prayer's time begins with its adhan off, either for all prayers
  or by that prayer's bell (on by default). Friday says الجمعة. Both lower whatever is playing for a moment
  instead of pausing it, and each is said once per prayer, also across a restart of the remote buttons service.
  The 12 clips are not in the repo: `tools/fetch-announce.sh` (run by `install.sh` the first time) records
  them with a Microsoft neural Arabic voice through edge-tts into `app/src/main/assets/announce/`; without them
  both stay silent. Release APKs include them. `--es test announce` tries it; `--es test banner` and
  `--es test reminder` now say the reminder too.

### Changed
- The reminder can come 5, 10, 15, 20 or 30 minutes before each prayer (or not at all). Settings → Reminders →
  Remind me before prayer is now a list, like the adhan voices; 15 is still the default.
- The Hijri date comes first: on Home it is the line under the clock (with the city) and the Gregorian date is
  the smaller line below it; on Prayer day the date line starts with it.
- The verse reference under the arch («النساء 103») follows the digits setting like every other number; it was
  always Eastern Arabic (١٠٣), even with Western digits chosen. It is the only surah:ayah reference on screen:
  Friday, Ramadan, the call screen and the screensaver show none.

## [0.4] - 2026-10-22

### Added
- 59 more cities, now 136 in 43 countries: India, Pakistan, Bangladesh, Sri Lanka, Maldives, Indonesia, Malaysia,
  Singapore, Brunei, Thailand, Japan, South Korea, Turkey, the United Kingdom, France, Germany, the Netherlands,
  Belgium, Austria, Switzerland, Italy, Spain, Bosnia and Herzegovina, Canada, Kenya, Nigeria, South Africa,
  Australia and New Zealand, plus Houston and Los Angeles. Each starts on its mosques' calculation method. Thanks,
  @mohammed-althaf-h (#4).

### Changed
- Location is picked by region first (Arab world, Asia, Europe, Americas, Africa, Oceania), then country (A to Z),
  then city, so no list is longer than 16.
- Picking a city also sets its usual Asr school: Hanafi for Pakistan, Bangladesh and most Indian cities (Kerala and
  Chennai stay Shafi'i). The Asr row can still change it.

### Fixed
- In Settings, list chooser items no longer draw over the chooser's title when scrolled (#8, thanks
  @mohammed-althaf-h in #9).

## [0.3.1] - 2026-10-22

### Changed
- Settings keys are the same on every row: the arrows only move (toward the start side goes back to the
  sections), and OK is the only key that changes anything. OK on a choice or the Hijri offset starts editing it
  (a ring shows round the control): ◀▶ move the choice, OK saves, Back cancels. OK flips a switch and opens a
  list as before. Every save shows "✓ Saved" at the end of the hint line for 2 s. Reported in #5 (thanks,
  @mohammed-althaf-h).

## [0.3] - 2026-10-22

### Added
- United States in the location list, with New York, Chicago and Dallas (ISNA method).
  The Hijri date and Ramadan mode still use Umm al-Qura, so US users who follow local moon sighting set the Hijri
  offset.
- A web preview of the launcher in `site/` (Astro, deployed to GitHub Pages by `.github/workflows/site.yml`): every
  screen in Arabic and English, driven by the arrow keys like the remote, with live prayer times for the visitor's
  chosen city. It reads the app's own strings (`Text.java`), cities, methods, stations and fonts at build time, so
  it never keeps a second copy. One page per screen and mode, with titles, descriptions, `hreflang`, a sitemap and
  structured data for search engines.
- `CODE_OF_CONDUCT.md`, `SECURITY.md`, issue templates (bug, prayer times or city, TV report, idea) and a pull
  request template. `THIRD_PARTY.md` now lists every radio station and the reciter channels.

### Changed
- `afnon-mihrab/tokens.json` is the single source of the design tokens. `Tokens.java` and `colors.xml` are now
  generated from it (`npm run tokens` in `site/`, Style Dictionary), as is the site's CSS; CI fails if they drift.
  The 15 colours that were hard-coded in the app (screensaver, menus, the reminder card, Qibla rings, the banner
  track, the Settings sidebar) are tokens now. Every value is unchanged.

## [0.2.1] - 2026-10-22

### Fixed
- A prayer's reminder and the call to prayer no longer come back after you close them. Each check computed the
  prayer's time with a few different milliseconds, so it never matched the one already shown; the reminder
  returned within 15 s and the call screen during its first two minutes. What was shown is also saved, so a
  restart of the remote buttons service (low memory, TCL's background manager) doesn't show it again.

## [0.2] - 2026-10-21

### Added
- Radio stations: Settings → Radio → Station picks broadcast Quran radios (Cairo, Nablus, Shabab FM Quran, Qatar 1
  and 2, Jordan) or one of 10 reciters' 24/7 channels from mp3quran.net. Each URL is the station's own stream;
  the same single player plays them. Home's card, the screensaver and the screen-off view show the station.
  `--es station <id>` picks one from adb.
- 57 cities, for 74 in 13 countries. Coordinates and Arabic names from Wikipedia/Wikidata.
  - Palestine, the seat of every governorate: Jerusalem, Ramallah, Nablus, Hebron, Bethlehem, Jenin, Tulkarm,
    Qalqilya, Salfit, Tubas, Jericho, Gaza, Jabalia, Deir al-Balah, Khan Yunis and Rafah.
  - Lebanon (Beirut, Tripoli, Sidon, Tyre, Baalbek), Syria (Damascus, Aleppo, Homs, Hama, Latakia, Deir ez-Zor),
    Iraq (Baghdad, Basra, Mosul, Erbil, Najaf, Karbala, Kirkuk, Sulaymaniyah), Bahrain (Manama, Muharraq),
    Oman (Muscat, Salalah, Sohar, Nizwa) and Yemen (Sanaa, Aden, Taiz, Hodeidah, Mukalla).
  - More cities where there was one: Saudi Arabia (Jeddah, Dammam, Taif, Tabuk, Abha) with Umm al-Qura,
    UAE (Abu Dhabi, Sharjah, Al Ain) with the Dubai method, Jordan (Irbid, Zarqa, Aqaba).
  - The new countries use the Muslim World League method, like Amman; the method row can still change it.
- A `redirects` preference (`from=to` package pairs) that opens another app whenever one app's screen comes up,
  for remote buttons the firmware hard-wires to an app that can't be replaced (`Redirects.java`). Empty by default.
- A `key_filter` preference (`auto`, `on`, `off`) to force key filtering on or off on a TV the guess gets wrong.

### Changed
- Location picks a country first, then one of its cities, so the remote never scrolls through all 74. A country
  with a single city is picked at once.

### Fixed
- On TVs whose firmware answers the remote's buttons itself (CVTE boards, e.g. Super General), holding Volume
  works continuously and the Source button opens the TV's own source menu again. The remote buttons service no
  longer filters keys there (`FirmwareKeys.java`, detected by CVTE's `android.intent.action.HOTKEYSERVICE`).
  CVTE's firmware leaves Source to the app in front, so Home now opens the TV's source menu itself.
- `tools/install.sh` grants location, so Home shows the Wi-Fi name on a fresh install (Android 9+ hides it otherwise).
- Where the service still filters keys, a button whose target is missing (an app that isn't installed, or no
  Android TV inputs screen) now passes through to the TV instead of doing nothing.
- Settings no longer stops answering OK after you leave it with a list open (city, method, adhan) and change
  something elsewhere, such as the room direction: coming back rebuilt the screen without the list but kept it
  marked as open.
- In the settings lists (cities, methods, adhan voices), items scrolled up no longer draw over the list's title.
- Settings → Room direction stays open. Settings acts on OK's press and the picker on its release, so the same
  press closed the picker at once and saved 0°.
- Screen off while listening uses TCL's Audio Only on Android TV 11 TCL sets too (tested on a C06_2K_GB set),
  where TCL Settings hosts it in `com.tcl.settings` instead of `com.android.tv.settings`.

## [0.1.1] - 2026-10-21

### Fixed
- Holding Volume Up or Down on the remote changes the volume continuously again, instead of one step per hold.
  The remote buttons service no longer filters keys on TCL TVs, which had stopped held keys from repeating.

### Changed
- On TCL TVs, the remote's app, Settings, Input and All apps buttons are now handed to TCL's own button
  remapping (`TclKeys.java`). Other TVs keep the previous behaviour.
- `tools/install.sh` and the other tools find adb by themselves (PATH, the Android SDK in `local.properties`,
  `ANDROID_HOME`, or the default SDK folder). `ADB_BIN` still overrides.
- `tools/restore.sh` also removes the TCL button mappings.

## [0.1] - 2026-10-21

First public release.

### Added
- Home with the next prayer and a live countdown in a mihrab arch, the day's six times, favourites and more apps.
- Today's prayers with an adhan on/off bell per prayer, the Qibla dial and room direction.
- Reminders before each prayer, and the call screen with the adhan at prayer time.
- 14 adhan voices plus a separate Fajr adhan.
- Friday (Jumu'ah, Surat al-Kahf) and Ramadan (iftar and suhoor) modes.
- Quran Radio Cairo 98.2 FM with live level bars; pauses for the adhan; screen off while listening (TCL Audio Only).
- Screensaver, on-TV settings and remote button mapping.
- Arabic (RTL) first, with English.

[Unreleased]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.6...HEAD
[0.6]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.5...v0.6
[0.5]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.4...v0.5
[0.4]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.3.1...v0.4
[0.3.1]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.3...v0.3.1
[0.3]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.2.1...v0.3
[0.2.1]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.2...v0.2.1
[0.2]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.1.1...v0.2
[0.1.1]: https://github.com/egyjs/afnon-android-tv-launcher/compare/v0.1...v0.1.1
[0.1]: https://github.com/egyjs/afnon-android-tv-launcher/releases/tag/v0.1
