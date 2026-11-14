# 08 — Audio: reciters, the player, focus, downloads

**Report status:** PASS (URLs verified by HTTP, logic reviewed; actual playback on a
TV NOT TESTED).

## Reciters

Twelve reciters, each a card with a drawn monogram plate (the first letter of his
name in Amiri on lapis — no photographs ship, keeping the launcher offline-first and
every card equal), his name, «حفص عن عاصم · مرتّل», the count of his downloaded
chapters, and a preview that plays Al-Fatihah from his server for twenty seconds
and stops on its own. OK makes him the default — saved for the player and Continue
listening; the chosen card carries the gold outline. The servers are mp3quran.net's
own (its public reciter list, the same servers its site and apps stream), one file
per chapter: `{server}{NNN}.mp3`. **Every one of the twelve servers answered
HTTP 200 for 001.mp3 at build time** (and the first for 002.mp3, 51.8 MB — the
shape the download manager stores).

## The player (Tilawa)

One static player for the whole app, modelled on the launcher's radio:

- **Controls:** play/pause, ±30 s, previous/next surah, and the repeat mode chip —
  تسلسل تلقائي (auto-continue, the default), تكرار السورة (repeat one), وقف بعد
  السورة (stop after). The position bar drags by focus (◀▶ seek; ±10 s) with the
  chapter's time line under it. Back leaves the screen with the recitation carrying
  on — across screens, exactly like the radio.
- **Sources:** the downloaded file when the registry says it is complete, else the
  stream; the card's state line says نسخة محفوظة when it plays the local file.
- **Position persistence:** every pause, stop, error, and each ~5 s of play write
  the (reciter, chapter, position) row; Continue listening returns to that spot; a
  finished chapter resets its saved position so it starts over next time.
- **Audio focus, the launcher's own rules:** GAIN while playing; the adhan or
  another transient loss **pauses** it and it resumes afterwards; a permanent loss
  stops it and saves the position. It and the radio are **one sound source**:
  whichever starts first stops the other (`Tilawa.start` stops the radio;
  `Radio.start` now stops a playing recitation) — so neither ever talks over the
  adhan, the radio, or another app. The radio's own sleep-timer arrangement is
  shared the same way: only one of them can ever be managing it at a time.
- **Errors:** up to three retries with the house backoff, then a quiet «تعذّر
  التشغيل» line — nothing loops forever, nothing sounds on its own.

## Downloads

The system's DownloadManager fetches a chapter only when asked for — the player's
تنزيل button, or the Downloads screen's digit pad — into the app's own external
files. The Downloads screen lists every downloaded chapter (reciter, surah, size
from the registry, progress while running), plays one with OK, deletes it with its
pill, and settles the registry on every visit: a chapter that finished while the
TV was off is marked DONE by the screen's own query or by the manifest-registered
finisher broadcast. Nothing ever downloads on its own.
