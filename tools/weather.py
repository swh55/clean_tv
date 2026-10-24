#!/usr/bin/env python3
"""Fetch the weather from Open-Meteo on the computer and push it to the TV for the launcher's weather
line (config.xml show_weather_line, or a theme's "show_weather_line"). The launcher itself never goes
online, so it only reads a small weather.json pushed next to the theme's config.json; this tool writes
that file (into the theme folder, so the next theme push carries it too) and pushes it right away.

    tools/venv/bin/python -I tools/weather.py themes/<your theme> 33.5138 36.2765
    TV_HOST=<tv ip> tools/venv/bin/python -I tools/weather.py themes/<your theme> 33.5138 36.2765

Without TV_HOST the tool only writes themes/<your theme>/weather.json (a later tools/theme.sh push
carries it). With TV_HOST it also pushes the file now and restarts Home. Open-Meteo needs no key;
what is sent is just the coordinates. Keep the theme folder out of any commit you share: the file
holds your place's weather, and every other key of a personal theme stays local anyway.

The launcher rereads the file once a minute, so re-running the tool refreshes the line in place.
"""
import json
import subprocess
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
URL = ("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lng}"
       "&current=temperature_2m,weather_code&timezone=auto&forecast_days=1")


def fetch(lat, lng):
    """The {temperature_2m, weather_code} of Open-Meteo's current block, or None."""
    try:
        with urllib.request.urlopen(URL.format(lat=lat, lng=lng), timeout=15) as r:
            data = json.load(r)
        current = data.get("current", {})
        temp = current.get("temperature_2m")
        code = current.get("weather_code")
        if not isinstance(temp, (int, float)) or not isinstance(code, int):
            return None
        return {"temp_c": round(float(temp), 1), "code": code}
    except Exception:
        return None


def tv_host():
    for arg in sys.argv[1:]:
        if arg.startswith("TV_HOST="):
            return arg.split("=", 1)[1]
    import os
    return os.environ.get("TV_HOST")


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("TV_HOST=")]
    if len(args) != 3:
        print(__doc__)
        return 1
    theme, lat, lng = args[0], float(args[1]), float(args[2])
    folder = (ROOT / theme).resolve() if not Path(theme).is_absolute() else Path(theme)
    if not (folder / "config.json").is_file():
        print(f"no config.json in {theme}: point weather.py at a theme folder", file=sys.stderr)
        return 1
    data = fetch(lat, lng)
    if data is None:
        print("Open-Meteo did not answer (check the coordinates and the network)", file=sys.stderr)
        return 1
    out = folder / "weather.json"
    out.write_text(json.dumps(data), encoding="utf-8")
    print(f"wrote {out.relative_to(ROOT) if out.is_relative_to(ROOT) else out}: {data}")

    host = tv_host()
    if not host:
        print("no TV_HOST: the file rides the next tools/theme.sh push")
        return 0
    env_py = sys.executable
    tmp = "/data/local/tmp/cyt-weather.json"
    for cmd in (
        [env_py, "-I", str(ROOT / "tools/tv.py"), "--push", str(out), tmp],
        # The same write tools/theme.sh makes, so locked-down firmware is covered too.
        [env_py, "-I", str(ROOT / "tools/tv.py"),
         f"mkdir -p /sdcard/Android/data/com.gathin.tvhome/files 2>/dev/null; "
         f"content write --uri content://com.gathin.tvhome.theme/weather.json < {tmp} 2>/dev/null || "
         f"cp {tmp} /sdcard/Android/data/com.gathin.tvhome/files/weather.json; rm -f {tmp}"],
        [env_py, "-I", str(ROOT / "tools/tv.py"), "am force-stop com.gathin.tvhome"],
    ):
        r = subprocess.run(cmd, capture_output=True, text=True)
        if r.returncode != 0:
            print(r.stdout + r.stderr, file=sys.stderr)
            print("a push step failed; the file is still in the theme folder for the next theme push",
                  file=sys.stderr)
            return 1
    print(f"weather pushed to {host}; Home restarts to show it")
    return 0


if __name__ == "__main__":
    sys.exit(main())
