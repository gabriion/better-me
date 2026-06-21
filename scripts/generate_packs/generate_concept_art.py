#!/usr/bin/env python3
"""
generate_concept_art.py — render 5 minimalist hero images for Better Me.

Themes:
  1. Zen         — stacked stones at still water at dawn
  2. Mindfulness — silhouette under a leaf canopy with light beams
  3. Inner Child — paper boat on a stream with wildflowers
  4. Inner Peace — distant mountains with layered mist and lone tree
  5. Empowerment — sunrise over a ridgeline with a winding path

Outputs WebP (bundled in assets/concept_art/) and PNG previews (in docs/concept_art_preview/).

Palette (matches app/src/main/java/com/gabriion/betterme/ui/theme/Color.kt):
  Teal Deep #0F4C5C   Sage #7DBE9A   Sky Soft #A8D8E8   Cream #F4F1DE   Ink #0B1F26
"""

from __future__ import annotations
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import math, random

W, H = 1024, 1536

TEAL_DEEP = (15, 76, 92)
TEAL_MID  = (44, 122, 138)
SAGE      = (125, 190, 154)
SAGE_DEEP = (79, 140, 110)
SKY_SOFT  = (168, 216, 232)
CREAM     = (244, 241, 222)
INK       = (11, 31, 38)
SUNRISE   = (240, 200, 150)
DUSK_ROSE = (220, 160, 150)

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "concept_art"
PREVIEW = ROOT / "docs" / "concept_art_preview"


def vertical_gradient(top: tuple, bottom: tuple, w: int = W, h: int = H) -> Image.Image:
    img = Image.new("RGB", (w, h), top)
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        for x in range(w):
            px[x, y] = (r, g, b)
    return img


def add_grain(img: Image.Image, amount: int = 6) -> Image.Image:
    noise = Image.effect_noise((img.width, img.height), amount * 8).convert("L")
    noise = noise.filter(ImageFilter.GaussianBlur(0.6))
    overlay = Image.merge("RGB", (noise, noise, noise))
    return Image.blend(img, overlay, amount / 100.0)


def soft_disc(img: Image.Image, cx: int, cy: int, r: int, color: tuple, blur: int = 30, alpha: int = 200):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    img.paste(layer, (0, 0), layer)


# ---------- Theme 1: Zen ----------
def render_zen() -> Image.Image:
    img = vertical_gradient(SKY_SOFT, CREAM).convert("RGBA")

    # rising sun glow
    soft_disc(img, W // 2, int(H * 0.32), 260, SUNRISE, blur=80, alpha=180)
    soft_disc(img, W // 2, int(H * 0.32), 90, CREAM, blur=20, alpha=255)

    # water (lower half)
    water = Image.new("RGBA", (W, H // 2), TEAL_MID + (255,))
    grad = vertical_gradient(TEAL_MID, TEAL_DEEP, W, H // 2).convert("RGBA")
    img.paste(grad, (0, H // 2), grad)

    d = ImageDraw.Draw(img)
    # horizon line
    d.rectangle([0, H // 2 - 2, W, H // 2 + 2], fill=TEAL_DEEP + (200,))

    # sun reflection on water
    refl = Image.new("RGBA", img.size, (0, 0, 0, 0))
    rd = ImageDraw.Draw(refl)
    for i in range(8):
        y = H // 2 + 40 + i * 30
        w_ = 200 - i * 18
        rd.ellipse([W // 2 - w_, y - 6, W // 2 + w_, y + 6], fill=SUNRISE + (max(160 - i * 18, 30),))
    refl = refl.filter(ImageFilter.GaussianBlur(8))
    img.paste(refl, (0, 0), refl)

    # stone stack on right
    sx = int(W * 0.66)
    sy_base = int(H * 0.66)
    stones = [
        (140, 36, INK),
        (110, 32, TEAL_DEEP),
        (88, 28, SAGE_DEEP),
        (62, 24, INK),
        (40, 20, SAGE_DEEP),
    ]
    cur_y = sy_base
    for w_, h_, color in stones:
        d.ellipse([sx - w_, cur_y - h_, sx + w_, cur_y + h_], fill=color + (255,))
        cur_y -= int(h_ * 1.6)
    # stone reflection (faded)
    refl2 = Image.new("RGBA", img.size, (0, 0, 0, 0))
    rd2 = ImageDraw.Draw(refl2)
    cur_y = sy_base + 30
    for w_, h_, color in stones:
        rd2.ellipse([sx - w_, cur_y - h_, sx + w_, cur_y + h_], fill=color + (60,))
        cur_y += int(h_ * 1.6)
    refl2 = refl2.filter(ImageFilter.GaussianBlur(6))
    img.paste(refl2, (0, 0), refl2)

    return img.convert("RGB")


# ---------- Theme 2: Mindfulness ----------
def render_mindfulness() -> Image.Image:
    img = vertical_gradient((30, 70, 80), TEAL_DEEP).convert("RGBA")

    # light beams through canopy from top
    beams = Image.new("RGBA", img.size, (0, 0, 0, 0))
    bd = ImageDraw.Draw(beams)
    for i, x in enumerate([W * 0.25, W * 0.42, W * 0.55, W * 0.7, W * 0.82]):
        bd.polygon([
            (x, 0),
            (x + 60 + i * 8, H),
            (x + 180 + i * 12, H),
            (x + 30 + i * 4, 0),
        ], fill=CREAM + (60,))
    beams = beams.filter(ImageFilter.GaussianBlur(40))
    img = Image.alpha_composite(img, beams)

    d = ImageDraw.Draw(img)

    # ground line
    ground_y = int(H * 0.78)
    d.rectangle([0, ground_y, W, H], fill=INK + (255,))

    # canopy of leaves (top portion - layered circles)
    canopy = Image.new("RGBA", img.size, (0, 0, 0, 0))
    cd = ImageDraw.Draw(canopy)
    random.seed(42)
    for _ in range(80):
        x = random.randint(-50, W + 50)
        y = random.randint(-50, int(H * 0.35))
        r = random.randint(40, 120)
        shade = random.choice([SAGE_DEEP, (60, 100, 75), (40, 80, 60), SAGE_DEEP])
        cd.ellipse([x - r, y - r, x + r, y + r], fill=shade + (240,))
    canopy = canopy.filter(ImageFilter.GaussianBlur(1.5))
    img = Image.alpha_composite(img, canopy)

    # trunk
    d = ImageDraw.Draw(img)
    trunk_x = int(W * 0.38)
    d.polygon([
        (trunk_x - 30, ground_y),
        (trunk_x + 30, ground_y),
        (trunk_x + 18, int(H * 0.2)),
        (trunk_x - 18, int(H * 0.2)),
    ], fill=INK + (255,))

    # seated silhouette under canopy
    sx, sy = int(W * 0.55), ground_y
    # body
    d.ellipse([sx - 90, sy - 180, sx + 90, sy - 20], fill=INK + (255,))
    # head
    d.ellipse([sx - 36, sy - 230, sx + 36, sy - 160], fill=INK + (255,))
    # legs (crossed)
    d.ellipse([sx - 130, sy - 60, sx + 130, sy + 10], fill=INK + (255,))

    return img.convert("RGB")


# ---------- Theme 3: Inner Child ----------
def render_inner_child() -> Image.Image:
    img = vertical_gradient(SKY_SOFT, CREAM).convert("RGBA")

    # soft sun
    soft_disc(img, int(W * 0.78), int(H * 0.22), 180, SUNRISE, blur=60, alpha=200)
    soft_disc(img, int(W * 0.78), int(H * 0.22), 60, CREAM, blur=15, alpha=255)

    d = ImageDraw.Draw(img)

    # stream — wavy band
    stream_top = int(H * 0.55)
    stream_bot = int(H * 0.82)
    stream = Image.new("RGBA", img.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(stream)
    sd.rectangle([0, stream_top, W, stream_bot], fill=TEAL_MID + (255,))
    # ripples
    for i in range(12):
        y = stream_top + 20 + i * 22
        sd.ellipse([100 + i * 20, y - 4, 220 + i * 30, y + 4], fill=SKY_SOFT + (180,))
        sd.ellipse([W - 320 + i * 5, y + 8, W - 180 + i * 12, y + 16], fill=SKY_SOFT + (160,))
    stream = stream.filter(ImageFilter.GaussianBlur(0.8))
    img = Image.alpha_composite(img, stream)

    # grass banks
    d = ImageDraw.Draw(img)
    d.rectangle([0, stream_bot, W, H], fill=SAGE_DEEP + (255,))
    # rolling grass on top bank
    grass = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(grass)
    gd.ellipse([-200, stream_top - 80, 600, stream_top + 80], fill=SAGE + (255,))
    gd.ellipse([W - 700, stream_top - 60, W + 200, stream_top + 80], fill=SAGE + (255,))
    img = Image.alpha_composite(img, grass)

    # wildflowers — small dots
    d = ImageDraw.Draw(img)
    random.seed(7)
    for _ in range(45):
        x = random.randint(40, W - 40)
        y_band = random.choice([
            (stream_top - 90, stream_top - 10),
            (stream_bot + 30, H - 60),
        ])
        y = random.randint(*y_band)
        color = random.choice([CREAM, SUNRISE, DUSK_ROSE, (250, 230, 130)])
        r = random.randint(6, 14)
        d.ellipse([x - r, y - r, x + r, y + r], fill=color + (255,))

    # paper boat in the middle of stream
    bx, by = int(W * 0.5), int((stream_top + stream_bot) / 2)
    # hull
    d.polygon([
        (bx - 110, by + 30),
        (bx + 110, by + 30),
        (bx + 70, by + 70),
        (bx - 70, by + 70),
    ], fill=CREAM + (255,))
    # sail
    d.polygon([
        (bx, by - 70),
        (bx - 80, by + 30),
        (bx + 5, by + 30),
    ], fill=CREAM + (255,))
    d.polygon([
        (bx + 5, by - 70),
        (bx + 5, by + 30),
        (bx + 80, by + 30),
    ], fill=(230, 225, 200) + (255,))
    # outline
    d.line([(bx - 110, by + 30), (bx + 110, by + 30), (bx + 70, by + 70), (bx - 70, by + 70), (bx - 110, by + 30)], fill=INK + (255,), width=2)

    return img.convert("RGB")


# ---------- Theme 4: Inner Peace ----------
def render_inner_peace() -> Image.Image:
    img = vertical_gradient((180, 210, 220), CREAM).convert("RGBA")
    d = ImageDraw.Draw(img)

    # far mountain layer 3 (lightest)
    for layer_idx, (base_y, color, alpha) in enumerate([
        (int(H * 0.55), TEAL_DEEP, 80),
        (int(H * 0.62), TEAL_MID, 130),
        (int(H * 0.7), SAGE_DEEP, 200),
    ]):
        layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        # zig-zag mountain ridge
        random.seed(layer_idx + 11)
        points = [(0, H)]
        x = 0
        while x < W:
            x += random.randint(80, 180)
            peak_offset = random.randint(-80, 80) - layer_idx * 30
            points.append((x, base_y + peak_offset))
        points.append((W, H))
        ld.polygon(points, fill=color + (alpha,))
        # mist on top of each layer
        layer = layer.filter(ImageFilter.GaussianBlur(1.2))
        img = Image.alpha_composite(img, layer)
        # mist band above the ridge
        mist = Image.new("RGBA", img.size, (0, 0, 0, 0))
        md = ImageDraw.Draw(mist)
        md.rectangle([0, base_y - 60, W, base_y + 20], fill=CREAM + (90,))
        mist = mist.filter(ImageFilter.GaussianBlur(30))
        img = Image.alpha_composite(img, mist)

    # foreground meadow
    d = ImageDraw.Draw(img)
    d.rectangle([0, int(H * 0.78), W, H], fill=SAGE + (255,))

    # lone tree on a small rise
    tx = int(W * 0.7)
    ty = int(H * 0.78)
    # trunk
    d.polygon([(tx - 8, ty), (tx + 8, ty), (tx + 4, ty - 110), (tx - 4, ty - 110)], fill=INK + (255,))
    # canopy
    d.ellipse([tx - 70, ty - 220, tx + 70, ty - 100], fill=SAGE_DEEP + (255,))
    d.ellipse([tx - 50, ty - 250, tx + 90, ty - 130], fill=(70, 130, 100) + (255,))

    return img.convert("RGB")


# ---------- Theme 5: Empowerment ----------
def render_empowerment() -> Image.Image:
    img = vertical_gradient(SUNRISE, (250, 220, 180)).convert("RGBA")

    # big sun
    soft_disc(img, W // 2, int(H * 0.42), 380, (255, 230, 180), blur=100, alpha=210)
    soft_disc(img, W // 2, int(H * 0.42), 140, CREAM, blur=20, alpha=255)

    d = ImageDraw.Draw(img)

    # ridge silhouettes - dramatic
    # far ridge
    far = [(0, int(H * 0.65))]
    random.seed(99)
    x = 0
    while x < W:
        x += random.randint(120, 220)
        far.append((x, int(H * 0.55) + random.randint(-40, 40)))
    far.extend([(W, int(H * 0.65)), (W, H), (0, H)])
    d.polygon(far, fill=TEAL_DEEP + (200,))

    # near ridge
    near = [(0, int(H * 0.78))]
    x = 0
    while x < W:
        x += random.randint(80, 160)
        near.append((x, int(H * 0.68) + random.randint(-30, 50)))
    near.extend([(W, int(H * 0.78)), (W, H), (0, H)])
    d.polygon(near, fill=INK + (255,))

    # winding path — curve from bottom center up to peak
    path = Image.new("RGBA", img.size, (0, 0, 0, 0))
    pd = ImageDraw.Draw(path)
    # construct path as a series of widening triangles going up
    bottom_w = 220
    peak_w = 8
    bottom_y = H - 20
    peak_y = int(H * 0.7)
    steps = 30
    for i in range(steps):
        t1 = i / steps
        t2 = (i + 1) / steps
        # sinusoidal sway
        sway1 = math.sin(t1 * math.pi * 1.5) * 120
        sway2 = math.sin(t2 * math.pi * 1.5) * 120
        w1 = bottom_w + (peak_w - bottom_w) * t1
        w2 = bottom_w + (peak_w - bottom_w) * t2
        y1 = bottom_y + (peak_y - bottom_y) * t1
        y2 = bottom_y + (peak_y - bottom_y) * t2
        cx1 = W // 2 + sway1
        cx2 = W // 2 + sway2
        pd.polygon([
            (cx1 - w1 / 2, y1),
            (cx1 + w1 / 2, y1),
            (cx2 + w2 / 2, y2),
            (cx2 - w2 / 2, y2),
        ], fill=SUNRISE + (220,))
    img = Image.alpha_composite(img, path)

    return img.convert("RGB")


RENDERERS = {
    "zen": render_zen,
    "mindfulness": render_mindfulness,
    "inner_child": render_inner_child,
    "inner_peace": render_inner_peace,
    "empowerment": render_empowerment,
}


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    for name, fn in RENDERERS.items():
        print(f"rendering {name}...")
        img = fn()
        img = add_grain(img, amount=4)
        webp_path = ASSETS / f"{name}.webp"
        png_path = PREVIEW / f"{name}.png"
        img.save(webp_path, "WEBP", quality=88, method=6)
        # smaller PNG preview for README
        preview = img.copy()
        preview.thumbnail((512, 768))
        preview.save(png_path, "PNG", optimize=True)
        print(f"  wrote {webp_path.relative_to(ROOT)} ({webp_path.stat().st_size // 1024} KB)")
        print(f"  wrote {png_path.relative_to(ROOT)}  ({png_path.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
