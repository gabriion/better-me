"""Generate the 6 muscle-group Lottie animations bundled with the app.

Each animation is a small, on-brand looping motion primitive that hints at the
exercise family. The output files are written to
`app/src/main/assets/ex/<group>.json` and are loaded by the Gym screen's
LottiePreview composable as a per-exercise fallback.

Run:
    py generate_lottie_exercises.py
"""
from __future__ import annotations

import json
from pathlib import Path

# ---- palette (Better Me brand) -----------------------------------------------
# Colours are Lottie RGBA in 0..1.
TEAL_DEEP = [0.058, 0.298, 0.361, 1]  # #0F4C5C
SAGE      = [0.490, 0.745, 0.604, 1]  # #7DBE9A
SKY_SOFT  = [0.659, 0.847, 0.910, 1]  # #A8D8E8
CREAM     = [0.957, 0.945, 0.871, 1]  # #F4F1DE

# ---- canvas ------------------------------------------------------------------
W = H = 200
CX = CY = 100
FR = 30
OP = 60  # ~2 seconds per loop


def _base(name: str, layers: list) -> dict:
    return {
        "v": "5.7.0",
        "fr": FR,
        "ip": 0,
        "op": OP,
        "w": W,
        "h": H,
        "nm": name,
        "ddd": 0,
        "assets": [],
        "layers": layers,
    }


def _static(val):
    return {"a": 0, "k": val}


def _kf(times_and_values, easing_in=(0.6, 1), easing_out=(0.4, 0)):
    """Build an animated keyframe list.

    times_and_values: list of (t, value) tuples. Each becomes a keyframe with
    smooth ease in/out unless it's the final frame.
    """
    kfs = []
    for i, (t, v) in enumerate(times_and_values):
        kf = {"t": t, "s": v}
        if i < len(times_and_values) - 1:
            kf["i"] = {"x": [easing_in[0]], "y": [easing_in[1]]}
            kf["o"] = {"x": [easing_out[0]], "y": [easing_out[1]]}
        kfs.append(kf)
    return {"a": 1, "k": kfs}


def _transform(pos=(0, 0), anchor=(0, 0), scale=(100, 100), rot=0, opacity=100):
    return {
        "ty": "tr",
        "p": _static(list(pos)),
        "a": _static(list(anchor)),
        "s": _static(list(scale)),
        "r": _static(rot),
        "o": _static(opacity),
    }


def _fill(color):
    return {"ty": "fl", "c": _static(color), "o": _static(100)}


def _stroke(color, width):
    return {
        "ty": "st",
        "c": _static(color),
        "o": _static(100),
        "w": _static(width),
        "lc": 2,  # round cap
        "lj": 2,  # round join
    }


def _ellipse(size, pos=(0, 0)):
    return {"ty": "el", "p": _static(list(pos)), "s": _static(list(size))}


def _rect(size, pos=(0, 0), r=0):
    return {"ty": "rc", "p": _static(list(pos)), "s": _static(list(size)), "r": _static(r)}


def _group(items):
    return {"ty": "gr", "it": items}


def _shape_layer(name, ind, shapes, ks=None):
    """Wrap shapes in a shape layer with default identity transform."""
    ks = ks or {
        "o": _static(100),
        "r": _static(0),
        "p": _static([CX, CY]),
        "a": _static([0, 0]),
        "s": _static([100, 100]),
    }
    return {
        "ty": 4,
        "nm": name,
        "ind": ind,
        "ao": 0,
        "ks": ks,
        "shapes": shapes,
        "ip": 0,
        "op": OP,
        "st": 0,
        "bm": 0,
    }


def _halo():
    """Soft sky-blue background halo used by every animation."""
    grp = _group([
        _ellipse(size=(160, 160)),
        _fill(SKY_SOFT),
        _transform(),
    ])
    ks = {
        "o": _static(60),
        "r": _static(0),
        "p": _static([CX, CY]),
        "a": _static([0, 0]),
        "s": _kf([(0, [95, 95]), (30, [105, 105]), (60, [95, 95])]),
    }
    return _shape_layer("halo", 100, [grp], ks)


# ---- per-muscle-group builders ----------------------------------------------

def build_chest() -> dict:
    """Two teal circles expanding outward horizontally then back — chest press."""
    def circle(ind, sign, name):
        grp = _group([
            _ellipse(size=(56, 56)),
            _fill(TEAL_DEEP),
            _transform(),
        ])
        ks = {
            "o": _static(100),
            "r": _static(0),
            "p": _kf([
                (0,  [CX + sign * 15, CY]),
                (30, [CX + sign * 45, CY]),
                (60, [CX + sign * 15, CY]),
            ]),
            "a": _static([0, 0]),
            "s": _kf([
                (0,  [100, 100]),
                (30, [115, 115]),
                (60, [100, 100]),
            ]),
        }
        return _shape_layer(name, ind, [grp], ks)

    return _base("chest", [_halo(), circle(1, -1, "left"), circle(2, +1, "right")])


def build_back() -> dict:
    """Two sage circles converging vertically then spreading — pulldown."""
    def circle(ind, sign, name):
        grp = _group([
            _ellipse(size=(50, 50)),
            _fill(SAGE),
            _transform(),
        ])
        ks = {
            "o": _static(100),
            "r": _static(0),
            "p": _kf([
                (0,  [CX, CY + sign * 55]),
                (30, [CX, CY + sign * 15]),
                (60, [CX, CY + sign * 55]),
            ]),
            "a": _static([0, 0]),
            "s": _static([100, 100]),
        }
        return _shape_layer(name, ind, [grp], ks)

    return _base("back", [_halo(), circle(1, -1, "top"), circle(2, +1, "bottom")])


def build_shoulders() -> dict:
    """Two circles rotating around a central pivot — press arc."""
    def circle(ind, phase_offset, color, name):
        # Ride a wrapper layer whose rotation is animated; the shape is offset
        # from the anchor so rotating the layer sweeps it around the pivot.
        grp = _group([
            _ellipse(size=(46, 46), pos=(0, -45)),
            _fill(color),
            _transform(),
        ])
        ks = {
            "o": _static(100),
            "r": _kf([
                (0,  phase_offset + 0),
                (30, phase_offset + 180),
                (60, phase_offset + 360),
            ]),
            "p": _static([CX, CY]),
            "a": _static([0, 0]),
            "s": _static([100, 100]),
        }
        return _shape_layer(name, ind, [grp], ks)

    # Center pivot dot for visual anchor.
    pivot = _shape_layer(
        "pivot",
        3,
        [_group([_ellipse(size=(18, 18)), _fill(CREAM), _transform()])],
    )
    return _base(
        "shoulders",
        [_halo(), circle(1, 0, TEAL_DEEP, "arm_a"), circle(2, 180, SAGE, "arm_b"), pivot],
    )


def build_legs() -> dict:
    """A single teal circle bouncing vertically — squat rhythm."""
    grp = _group([
        _ellipse(size=(70, 70)),
        _fill(TEAL_DEEP),
        _transform(),
    ])
    ks = {
        "o": _static(100),
        "r": _static(0),
        "p": _kf([
            (0,  [CX, CY - 30]),
            (30, [CX, CY + 30]),
            (60, [CX, CY - 30]),
        ]),
        "a": _static([0, 0]),
        "s": _kf([
            (0,  [100, 100]),
            (30, [115, 85]),   # squish at the bottom
            (60, [100, 100]),
        ]),
    }
    ball = _shape_layer("ball", 1, [grp], ks)

    # A sage "floor" bar at the bottom.
    floor = _shape_layer(
        "floor",
        2,
        [_group([_rect(size=(140, 10), r=5), _fill(SAGE), _transform()])],
        ks={
            "o": _static(80),
            "r": _static(0),
            "p": _static([CX, CY + 70]),
            "a": _static([0, 0]),
            "s": _static([100, 100]),
        },
    )
    return _base("legs", [_halo(), floor, ball])


def build_arms() -> dict:
    """A bar rotating around a pivot — bicep curl arc."""
    bar_grp = _group([
        # A rounded bar. The rect is positioned above the anchor so rotating
        # the wrapper layer swings it around the pivot at the elbow.
        _rect(size=(14, 90), pos=(0, -45), r=7),
        _fill(TEAL_DEEP),
        _transform(),
    ])
    bar_ks = {
        "o": _static(100),
        "r": _kf([
            (0,  90),    # extended (bar pointing right)
            (30, -20),   # curled up
            (60, 90),    # back down
        ]),
        "p": _static([CX, CY + 40]),
        "a": _static([0, 0]),
        "s": _static([100, 100]),
    }
    bar = _shape_layer("bar", 1, [bar_grp], bar_ks)

    weight_grp = _group([
        _ellipse(size=(32, 32)),
        _fill(SAGE),
        _transform(),
    ])
    # Match the end of the bar (length 90 from pivot). We animate position on a
    # circle so the "weight" tracks the bar tip.
    import math
    def tip(deg):
        rad = math.radians(deg - 90)  # convert Lottie rotation to xy
        r = 90
        return [CX + r * math.cos(rad), (CY + 40) + r * math.sin(rad)]

    weight_ks = {
        "o": _static(100),
        "r": _static(0),
        "p": _kf([(0, tip(90)), (30, tip(-20)), (60, tip(90))]),
        "a": _static([0, 0]),
        "s": _static([100, 100]),
    }
    weight = _shape_layer("weight", 2, [weight_grp], weight_ks)

    pivot = _shape_layer(
        "pivot",
        3,
        [_group([_ellipse(size=(20, 20)), _fill(CREAM), _transform()])],
        ks={
            "o": _static(100),
            "r": _static(0),
            "p": _static([CX, CY + 40]),
            "a": _static([0, 0]),
            "s": _static([100, 100]),
        },
    )
    return _base("arms", [_halo(), bar, weight, pivot])


def build_core() -> dict:
    """A curved line flexing back and forth — crunch motion.

    Implemented as a tall rounded rectangle whose vertical scale shrinks
    (crunch) and horizontal scale slightly bulges, evoking a torso curl.
    """
    torso_grp = _group([
        _rect(size=(40, 130), r=20),
        _fill(TEAL_DEEP),
        _transform(),
    ])
    torso_ks = {
        "o": _static(100),
        "r": _kf([(0, -8), (30, 18), (60, -8)]),
        "p": _static([CX, CY + 10]),
        "a": _static([0, 40]),  # anchor near the hips so rotation crunches forward
        "s": _kf([
            (0,  [100, 100]),
            (30, [115, 75]),
            (60, [100, 100]),
        ]),
    }
    torso = _shape_layer("torso", 1, [torso_grp], torso_ks)

    head_grp = _group([
        _ellipse(size=(34, 34)),
        _fill(SAGE),
        _transform(),
    ])
    head_ks = {
        "o": _static(100),
        "r": _static(0),
        "p": _kf([
            (0,  [CX, CY - 55]),
            (30, [CX + 20, CY - 20]),
            (60, [CX, CY - 55]),
        ]),
        "a": _static([0, 0]),
        "s": _static([100, 100]),
    }
    head = _shape_layer("head", 2, [head_grp], head_ks)

    hips = _shape_layer(
        "hips",
        3,
        [_group([_ellipse(size=(24, 24)), _fill(CREAM), _transform()])],
        ks={
            "o": _static(100),
            "r": _static(0),
            "p": _static([CX, CY + 60]),
            "a": _static([0, 0]),
            "s": _static([100, 100]),
        },
    )
    return _base("core", [_halo(), hips, torso, head])


BUILDERS = {
    "chest": build_chest,
    "back": build_back,
    "shoulders": build_shoulders,
    "legs": build_legs,
    "arms": build_arms,
    "core": build_core,
}


def main() -> None:
    here = Path(__file__).resolve()
    repo_root = here.parents[2]
    out_dir = repo_root / "app" / "src" / "main" / "assets" / "ex"
    out_dir.mkdir(parents=True, exist_ok=True)

    for group, builder in BUILDERS.items():
        data = builder()
        path = out_dir / f"{group}.json"
        path.write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
        print(f"wrote {path} ({path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
