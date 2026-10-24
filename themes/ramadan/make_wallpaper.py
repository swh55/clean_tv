#!/usr/bin/env python3
"""Draws themes/ramadan/wallpaper.jpg from nothing but code: a Ramadan night, deep indigo fading
to near-black, a thin gold crescent, a scatter of dim stars and two fanoos (Ramadan lanterns)
hanging at the bottom corners over a dark ground. No photos, artwork or fonts are used, so the
picture is original and can be redrawn by anyone.

    python3 make_wallpaper.py            # needs Pillow and NumPy; seed 1448 (the Hijri year) gives
                                         # the same picture every time

Kept dark on purpose (see themes/README.md, Wallpapers): the clock, the greeting and the dock band
are designed for black, so the sky goes almost black below the middle and the lantern glow is kept
low - the picture only warms the corners it stands in.
"""
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

W, H = 1920, 1080
S = 2                                   # draw at 2x, scale down for clean edges
w, h = W * S, H * S
rng = np.random.default_rng(1448)       # seed 1448, the Hijri year the theme's festival_until points at
MOON = (0.78, 0.20)                     # moon centre, as fractions of width and height
GOLD = (232, 195, 106)                  # the theme's accent, #E8C36A


def gradient(stops, n):
    """Rows of colour from (position, (r, g, b)) stops."""
    ys = np.linspace(0, 1, n)
    out = np.zeros((n, 3))
    for c in range(3):
        out[:, c] = np.interp(ys, [p for p, _ in stops], [col[c] for _, col in stops])
    return out


# ---- the sky: deep indigo up top, near-black by the dock --------------------------------------
img = np.zeros((h, w, 3))
img[:] = gradient([(0.0, (17, 23, 54)), (0.35, (11, 15, 36)), (0.70, (6, 8, 19)), (1.0, (3, 4, 10))],
                  h)[:, None, :]
yy, xx = np.mgrid[0:h, 0:w]

# a faint warm halo round the moon
mx, my = w * MOON[0], h * MOON[1]
halo = np.exp(-(((xx - mx) ** 2 + (yy - my) ** 2) / (h * 0.10) ** 2))
img += halo[:, :, None] * np.array([16, 12, 5])

# ---- a scattering of small dim stars, seeded ----------------------------------------------------
def clear_of_moon(x, y):
    """False too close to the crescent, so stars never sit on its edge."""
    return (x - mx) ** 2 + (y - my) ** 2 >= (64 * S) ** 2

for _ in range(190):
    x, y = int(rng.integers(0, w)), int(rng.integers(0, h * 0.55))
    if not clear_of_moon(x, y):
        continue
    img[y:y + S, x:x + S] += rng.uniform(14, 55)
for _ in range(7):                                          # a few brighter ones
    x, y = int(rng.integers(0, w)), int(rng.integers(0, h * 0.45))
    if not clear_of_moon(x, y):
        continue
    img[y:y + S, x:x + S] += rng.uniform(55, 85)

# ---- the thin gold crescent ----------------------------------------------------------------------
mr = 56 * S
dx, dy = -0.34 * mr, -0.52 * mr              # the shadowed disc is cut away up-left of the moon
crescent = ((xx - mx) ** 2 + (yy - my) ** 2 <= mr ** 2) & \
           (((xx - (mx + dx)) ** 2 + (yy - (my + dy)) ** 2) > mr ** 2)
img[crescent] = np.array(GOLD) * np.array([1.0, 0.98, 0.88])   # a hair cooler than pure gold

# ---- keep the text areas dark: the top band and the left side -------------------------------------
v = np.clip(1 - np.exp(-((yy / h) / 0.15) ** 2) * 0.32, 0, 1) * \
    np.clip(1 - np.exp(-((xx / w) / 0.24) ** 2) * 0.26, 0, 1)
img *= v[:, :, None]

scene = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
d = ImageDraw.Draw(scene)

# ---- a subtle dark ground across the bottom -------------------------------------------------------
gy = 0.935
pts = [(x, h * gy - 8 * S * math.sin(x / w * math.pi * 2.2 + 0.6)) for x in range(0, w + 40, 40)]
d.polygon(pts + [(w, h), (0, h)], fill=(2, 3, 7))

# ---- fanoos: silhouettes with a warm glow, hanging at the bottom corners --------------------------
overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
od = ImageDraw.Draw(overlay)
FRAME = (26, 20, 13, 255)
PANE = (196, 148, 66)


def fanoos(cx, top, hgt, cord_from, pane_alpha):
    """One lantern: a cord fading out of the dark, a pointed cap, a tapering ribbed body and a base."""
    bw = hgt * 0.60
    # cord, invisible where it leaves the dark, firmer near the lantern
    steps = 60
    for i in range(steps):
        t1 = (i + 1) / steps
        y0 = cord_from + (top - cord_from) * (i / steps)
        y1 = cord_from + (top - cord_from) * t1
        od.line([(cx, y0), (cx, y1)], fill=(34, 28, 18, int(150 * t1 ** 2)), width=S)
    r = hgt * 0.045
    od.ellipse([cx - r, top, cx + r, top + 2 * r], outline=FRAME, width=max(S, 1))  # the hanging ring
    cap = top + 2 * r
    od.pieslice([cx - bw * 0.30, cap, cx + bw * 0.30, cap + hgt * 0.18], 180, 360, fill=FRAME)
    od.polygon([(cx, cap - hgt * 0.05), (cx - bw * 0.10, cap + hgt * 0.06),
                (cx + bw * 0.10, cap + hgt * 0.06)], fill=FRAME)                     # the cap's point
    y0 = cap + hgt * 0.17
    od.rectangle([cx - bw * 0.34, y0, cx + bw * 0.34, y0 + hgt * 0.03], fill=FRAME)  # collar
    y1 = top + hgt * 0.88
    od.polygon([(cx - bw * 0.50, y0 + hgt * 0.08), (cx - bw * 0.42, y0 + hgt * 0.03),
                (cx + bw * 0.42, y0 + hgt * 0.03), (cx + bw * 0.50, y0 + hgt * 0.08),
                (cx + bw * 0.34, y1), (cx - bw * 0.34, y1)], fill=FRAME)             # the body
    py0, py1 = y0 + hgt * 0.06, y1 - hgt * 0.05
    od.polygon([(cx - bw * 0.36, py0 + hgt * 0.06), (cx - bw * 0.30, py0),
                (cx + bw * 0.30, py0), (cx + bw * 0.36, py0 + hgt * 0.06),
                (cx + bw * 0.24, py1), (cx - bw * 0.24, py1)],
               fill=PANE + (pane_alpha,))                                            # the lit glass
    for k in (-1, 0, 1):                                                             # ribs over the glass
        od.line([(cx + k * bw * 0.28, py0), (cx + k * bw * 0.17, py1)],
                fill=FRAME, width=max(S, 1))
    od.rectangle([cx - bw * 0.38, y1, cx + bw * 0.38, y1 + hgt * 0.035], fill=FRAME)  # the base
    od.polygon([(cx, y1 + hgt * 0.11), (cx - bw * 0.08, y1 + hgt * 0.035),
                (cx + bw * 0.08, y1 + hgt * 0.035)], fill=FRAME)                      # the drop finial
    return (cx, (y0 + y1) / 2, hgt)                                                   # where the glow goes


lanterns = [fanoos(w * 0.085, h * 0.635, h * 0.150, h * 0.28, 165),
            fanoos(w * 0.925, h * 0.685, h * 0.118, h * 0.40, 140)]

# the glow, drawn first and blurred wide so each lantern warms only its corner
glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
gd = ImageDraw.Draw(glow)
for gx, gy2, hgt in lanterns:
    gr = hgt * 1.15
    gd.ellipse([gx - gr, gy2 - gr, gx + gr, gy2 + gr], fill=GOLD + (58,))
overlay = Image.alpha_composite(glow.filter(ImageFilter.GaussianBlur(26 * S)), overlay)
scene = Image.alpha_composite(scene.convert("RGBA"), overlay).convert("RGB")

# ---- grain against banding, down to 1080p, out -----------------------------------------------------
out = np.asarray(scene).astype(np.float64)
out += rng.normal(0, 1.3, out.shape)
final = Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).resize((W, H), Image.LANCZOS)
path = Path(__file__).with_name("wallpaper.jpg")
final.save(path, "JPEG", quality=84, optimize=True, progressive=True)
print(f"{path} {path.stat().st_size // 1024} KB")
