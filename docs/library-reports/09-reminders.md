# 09 — Daily reminders: five, off by default, never in the way

**Report status:** PASS (logic reviewed; the on-TV banner and Home card NOT TESTED).

## The five kinds

Morning adhkar, evening adhkar, after-prayer adhkar, the daily reading, the khatm
day. Each is its own switch in Library settings (a ninth section also opens them
from the launcher's own Settings), **every one off by default**, each with its own
time (stepped in half hours; the after-prayer one has no time — it rides ten
minutes behind each prayer). Changing a setting clears the day's shown-stamps so
the new choice takes effect at once; restarts need nothing — see below.

## How they show

- **On Home:** the launcher's own quiet card shape — the cream card with Open,
  Later (back in half an hour, once) and Close; Back closes it. Nothing opens by
  itself, nothing plays a sound, the TV's programme is never covered by anything
  that demands an answer.
- **Over another app:** the same non-touchable, non-focusable accessibility banner
  the prayer reminder uses — it leaves on its own after twelve seconds, and nothing
  sounds. It never takes focus and never needs one.

## How they never clash

- They ride the existing fifteen-second watch in `PrayerWatch` — the same loop that
  already shows the prayer reminder and the call screen — so they survive service
  restarts and reboots with nothing to re-schedule (the "already shown" stamps live
  in SharedPreferences, one per kind per day).
- The call guard already returns before them: **nothing of the Library shows while
  a prayer's call is due**, and nothing shows over the screensaver. The adhan never
  competes with them, and they never compete with it.
- Each kind shows at most once a day inside its thirty-minute window; Later pushes
  one away for half an hour and lets it return once.

## What the card says

The Library's own words — «حان وقت أذكار الصباح», «وقت ورد القرآن اليومي», «ورد
الختمة اليومي», and the after-prayer one names the prayer it follows — under the
caps label «المكتبة الإسلامية». Open goes where the kind belongs (the adhkar
section, the Qur'an index, or the khatm screen).
