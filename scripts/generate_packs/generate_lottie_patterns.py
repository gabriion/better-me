"""Generate 20 stick-figure Lottie animations for exercise movement patterns.

Each animation is a 2-second looping stick figure on a 200x200 canvas using
the Better Me brand palette:
  Teal Deep #0F4C5C  primary limb/joint colour
  Sage     #7DBE9A   equipment (barbell, dumbbell)
  SkySoft  #A8D8E8   background halo
  Cream    #F4F1DE   face/head fill

Bones are drawn as animated 2-vertex Lottie shape paths, which lets us move
either endpoint freely per keyframe using trig computed in Python. Equipment
(dumbbells, barbells) is drawn as an animated ellipse (follows a wrist) or a
rectangle positioned at the wrist midpoint.

Files land in ``app/src/main/assets/ex/patterns/<pattern>.json`` and are loaded
by ``LottiePreview`` when an exercise has a ``pattern`` field.

Run:
    py generate_lottie_patterns.py
"""
from __future__ import annotations

import json
import math
from pathlib import Path

# ---- palette (Better Me brand) ----------------------------------------------
TEAL_DEEP = [0.058, 0.298, 0.361, 1]
SAGE      = [0.490, 0.745, 0.604, 1]
SKY_SOFT  = [0.659, 0.847, 0.910, 1]
CREAM     = [0.957, 0.945, 0.871, 1]

# ---- canvas -----------------------------------------------------------------
W = H = 200
CX = CY = 100
FR = 30
OP = 60  # 2 seconds at 30fps

# Standard loop times (start, mid, end). end==start ensures a seamless loop.
T0, T1, T2 = 0, 30, 60


# ---- primitive helpers ------------------------------------------------------

def _base(name: str, layers: list) -> dict:
    return {
        "v": "5.7.0", "fr": FR, "ip": 0, "op": OP, "w": W, "h": H,
        "nm": name, "ddd": 0, "assets": [], "layers": layers,
    }


def _static(val):
    return {"a": 0, "k": val}


def _kf_scalar(times_and_values):
    kfs = []
    for i, (t, v) in enumerate(times_and_values):
        kf = {"t": t, "s": [v] if not isinstance(v, list) else v}
        if i < len(times_and_values) - 1:
            kf["i"] = {"x": [0.6], "y": [1]}
            kf["o"] = {"x": [0.4], "y": [0]}
        kfs.append(kf)
    return {"a": 1, "k": kfs}


def _kf_pos(times_and_points):
    kfs = []
    for i, (t, p) in enumerate(times_and_points):
        kf = {"t": t, "s": list(p)}
        if i < len(times_and_points) - 1:
            kf["i"] = {"x": [0.6], "y": [1]}
            kf["o"] = {"x": [0.4], "y": [0]}
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
    return {"ty": "st", "c": _static(color), "o": _static(100),
            "w": _static(width), "lc": 2, "lj": 2}


def _ellipse(size, pos=(0, 0)):
    return {"ty": "el", "p": _static(list(pos)), "s": _static(list(size))}


def _rect(size, pos=(0, 0), r=0):
    return {"ty": "rc", "p": _static(list(pos)), "s": _static(list(size)),
            "r": _static(r)}


def _group(items):
    return {"ty": "gr", "it": items}


def _shape_layer(name, ind, shapes, ks=None):
    ks = ks or {
        "o": _static(100), "r": _static(0),
        "p": _static([0, 0]), "a": _static([0, 0]),
        "s": _static([100, 100]),
    }
    return {
        "ty": 4, "nm": name, "ind": ind, "ao": 0, "ks": ks,
        "shapes": shapes, "ip": 0, "op": OP, "st": 0, "bm": 0,
    }


# ---- animated primitives ----------------------------------------------------

def _animated_path(times, p1s, p2s):
    """A 2-vertex open path whose endpoints animate through the given frames."""
    kfs = []
    for i, t in enumerate(times):
        s = [{
            "i": [[0, 0], [0, 0]],
            "o": [[0, 0], [0, 0]],
            "v": [list(p1s[i]), list(p2s[i])],
            "c": False,
        }]
        kf = {"t": t, "s": s}
        if i < len(times) - 1:
            kf["i"] = {"x": [0.6], "y": [1]}
            kf["o"] = {"x": [0.4], "y": [0]}
        kfs.append(kf)
    return {"ty": "sh", "ks": {"a": 1, "k": kfs}}


def bone(name, ind, times, p1s, p2s, color=TEAL_DEEP, width=5):
    grp = _group([
        _animated_path(times, p1s, p2s),
        _stroke(color, width),
        _transform(),
    ])
    return _shape_layer(name, ind, [grp])


def joint(name, ind, times, centers, r=4, color=TEAL_DEEP):
    grp = _group([
        _ellipse(size=(r * 2, r * 2)),
        _fill(color),
        _transform(),
    ])
    ks = {
        "o": _static(100), "r": _static(0),
        "p": _kf_pos([(t, c) for t, c in zip(times, centers)]),
        "a": _static([0, 0]), "s": _static([100, 100]),
    }
    return _shape_layer(name, ind, [grp], ks)


def head(name, ind, times, centers, r=12):
    grp = _group([
        _ellipse(size=(r * 2, r * 2)),
        _stroke(TEAL_DEEP, 3),
        _fill(CREAM),
        _transform(),
    ])
    ks = {
        "o": _static(100), "r": _static(0),
        "p": _kf_pos([(t, c) for t, c in zip(times, centers)]),
        "a": _static([0, 0]), "s": _static([100, 100]),
    }
    return _shape_layer(name, ind, [grp], ks)


def dumbbell(name, ind, times, centers, r=7):
    grp = _group([
        _ellipse(size=(r * 2, r * 2)),
        _fill(SAGE),
        _transform(),
    ])
    ks = {
        "o": _static(100), "r": _static(0),
        "p": _kf_pos([(t, c) for t, c in zip(times, centers)]),
        "a": _static([0, 0]), "s": _static([100, 100]),
    }
    return _shape_layer(name, ind, [grp], ks)


def barbell(name, ind, times, midpoints, length=90, thickness=6):
    """Horizontal sage bar centered at animated midpoints. Fixed length."""
    grp = _group([
        _rect(size=(length, thickness), r=thickness / 2),
        _fill(SAGE),
        _transform(),
    ])
    ks = {
        "o": _static(100), "r": _static(0),
        "p": _kf_pos([(t, c) for t, c in zip(times, midpoints)]),
        "a": _static([0, 0]), "s": _static([100, 100]),
    }
    return _shape_layer(name, ind, [grp], ks)


def prop_rect(name, ind, pos, size, color=SAGE, r=4):
    """Static prop rectangle (bench, bar, floor)."""
    grp = _group([
        _rect(size=list(size), r=r),
        _fill(color),
        _transform(),
    ])
    ks = {
        "o": _static(100), "r": _static(0),
        "p": _static(list(pos)),
        "a": _static([0, 0]), "s": _static([100, 100]),
    }
    return _shape_layer(name, ind, [grp], ks)


def halo():
    grp = _group([_ellipse(size=(160, 160)), _fill(SKY_SOFT), _transform()])
    ks = {
        "o": _static(55), "r": _static(0),
        "p": _static([CX, CY]), "a": _static([0, 0]),
        "s": _kf_pos([(0, [95, 95]), (30, [105, 105]), (60, [95, 95])]),
    }
    return _shape_layer("halo", 200, [grp], ks)


# ---- pose builder ------------------------------------------------------------

# A "pose" is a dict of joint name -> (x, y). Bones are drawn between named
# joints. Every pattern supplies 2-5 poses and this helper emits the layers.

BONES = [
    # (name, from_joint, to_joint, stroke_width)
    ("torso",       "shoulder_c", "hip_c",     6),
    ("upper_arm_L", "shoulder_L", "elbow_L",   4),
    ("lower_arm_L", "elbow_L",    "wrist_L",   4),
    ("upper_arm_R", "shoulder_R", "elbow_R",   4),
    ("lower_arm_R", "elbow_R",    "wrist_R",   4),
    ("upper_leg_L", "hip_L",      "knee_L",    5),
    ("lower_leg_L", "knee_L",     "ankle_L",   5),
    ("upper_leg_R", "hip_R",      "knee_R",    5),
    ("lower_leg_R", "knee_R",     "ankle_R",   5),
]

JOINT_DOTS = ["shoulder_L", "shoulder_R", "elbow_L", "elbow_R",
              "hip_L", "hip_R", "knee_L", "knee_R"]


def build_figure(name, times, poses, extras=None, props=None):
    """Emit a full Lottie animation.

    Parameters
    ----------
    name    : animation name.
    times   : list of frame times (same length as poses).
    poses   : list of dicts joint_name -> (x, y).
    extras  : optional list of extra layers appended AFTER bones (e.g. weights).
    props   : optional list of static prop layers rendered UNDER the figure
              (e.g. bench, bar, floor).
    """
    layers = [halo()]
    ind = 1
    if props:
        for p in props:
            p["ind"] = ind
            ind += 1
            layers.append(p)

    # bones
    for bname, a, b, w in BONES:
        p1s = [pose[a] for pose in poses]
        p2s = [pose[b] for pose in poses]
        layers.append(bone(bname, ind, times, p1s, p2s, width=w))
        ind += 1

    # joint dots for a cleaner look
    for jname in JOINT_DOTS:
        centers = [pose[jname] for pose in poses]
        layers.append(joint(jname, ind, times, centers, r=3))
        ind += 1

    # head last so it sits on top
    head_centers = [pose["head_c"] for pose in poses]
    layers.append(head("head", ind, times, head_centers))
    ind += 1

    if extras:
        for e in extras:
            e["ind"] = ind
            ind += 1
            layers.append(e)

    return _base(name, layers)


# ---- pose helpers -----------------------------------------------------------

def standing_pose(overrides=None):
    """A neutral upright standing pose (arms at sides, feet on ground)."""
    p = {
        "head_c":     (100, 42),
        "shoulder_c": (100, 68),
        "hip_c":      (100, 118),
        "shoulder_L": (86, 70), "shoulder_R": (114, 70),
        "elbow_L":    (80, 100), "elbow_R":    (120, 100),
        "wrist_L":    (78, 128), "wrist_R":    (122, 128),
        "hip_L":      (92, 120), "hip_R":      (108, 120),
        "knee_L":     (90, 150), "knee_R":     (110, 150),
        "ankle_L":    (90, 180), "ankle_R":    (110, 180),
    }
    if overrides:
        p.update(overrides)
    return p


def supine_pose(y_body=110, overrides=None):
    """Supine (face-up) pose lying horizontally. Head to the LEFT.

    Body runs along the horizontal midline; arms fold up above chest by
    default; legs extend to the right (feet)."""
    yb = y_body
    p = {
        "head_c":     (40, yb),
        "shoulder_c": (66, yb),
        "hip_c":      (116, yb),
        "shoulder_L": (66, yb - 12), "shoulder_R": (66, yb + 12),
        "elbow_L":    (86, yb - 22), "elbow_R":    (86, yb + 22),
        "wrist_L":    (100, yb - 30), "wrist_R":   (100, yb + 30),
        "hip_L":      (116, yb - 8),  "hip_R":     (116, yb + 8),
        "knee_L":     (146, yb - 8),  "knee_R":    (146, yb + 8),
        "ankle_L":    (176, yb - 8),  "ankle_R":   (176, yb + 8),
    }
    if overrides:
        p.update(overrides)
    return p


def prone_pose(y_body=120, overrides=None):
    """Prone (face-down) pose lying horizontally. Head to the LEFT.

    Used for push-up / plank / leg curl (prone). Body runs along horizontal
    midline; hands/feet on ground below body line."""
    yb = y_body
    p = {
        "head_c":     (40, yb - 6),
        "shoulder_c": (66, yb),
        "hip_c":      (116, yb),
        "shoulder_L": (66, yb - 8),  "shoulder_R": (66, yb + 8),
        "elbow_L":    (66, yb - 22), "elbow_R":    (66, yb + 22),
        "wrist_L":    (66, yb - 40), "wrist_R":    (66, yb + 40),
        "hip_L":      (116, yb - 8), "hip_R":      (116, yb + 8),
        "knee_L":     (146, yb - 8), "knee_R":     (146, yb + 8),
        "ankle_L":    (176, yb - 8), "ankle_R":    (176, yb + 8),
    }
    if overrides:
        p.update(overrides)
    return p


# ---- pattern builders --------------------------------------------------------

def build_push_up():
    """Prone figure, arms extend and bend, chest rises/falls."""
    # High position: elbows straight (shoulder_y == hip_y line; wrists below).
    # Low position: elbows bent, body lower.
    hi = prone_pose(120, {
        "elbow_L": (66, 145), "wrist_L": (66, 170),
        "elbow_R": (66, 145), "wrist_R": (66, 170),
        # feet on ground
        "ankle_L": (176, 170), "ankle_R": (176, 170),
        "knee_L":  (146, 165), "knee_R":  (146, 165),
        "hip_L":   (116, 122), "hip_R":   (116, 122),
        "hip_c":   (116, 122),
        "shoulder_c": (66, 122), "head_c": (40, 118),
        "shoulder_L": (66, 122), "shoulder_R": (66, 122),
    })
    lo = dict(hi)
    # body pushed down: shoulders/head drop toward ground; elbows bend outward
    for k in ("head_c", "shoulder_c", "shoulder_L", "shoulder_R",
              "hip_c", "hip_L", "hip_R"):
        x, y = lo[k]
        lo[k] = (x, y + 22)
    lo["elbow_L"] = (50, 152); lo["elbow_R"] = (50, 152)
    lo["wrist_L"] = (66, 170); lo["wrist_R"] = (66, 170)
    lo["head_c"]  = (40, 140)
    poses = [hi, lo, hi]
    floor = prop_rect("floor", 0, (100, 178), (170, 4))
    return build_figure("push_up", [T0, T1, T2], poses, props=[floor])


def build_bench_press(name="bench_press", incline=False):
    """Supine on bench. Barbell moves up (away from body) and back down."""
    poses = []
    y_body = 100 if not incline else 108
    # elbows out; wrists together over chest
    dx_out, dy_out = 0, 30  # wrist offset from shoulders (up = -y_screen)
    # In our supine layout head is LEFT; "up" from chest = -y screen (toward top).
    # Bar at wrists, above chest.
    for wrist_y in [65, 45, 65]:
        p = supine_pose(y_body, {
            "elbow_L": (86, wrist_y + 12), "wrist_L": (100, wrist_y),
            "elbow_R": (86, wrist_y + 12), "wrist_R": (100, wrist_y),
        })
        # actually elbow_R symmetrical below wrist? For a top-down bench view
        # we're rendering side-on; both hands share the bar. Make L/R identical.
        p["elbow_R"] = (86, wrist_y + 20)
        p["wrist_R"] = (100, wrist_y + 8)
        poses.append(p)
    # bar rides with wrist_L (top hand)
    bar_pts = [pose["wrist_L"] for pose in poses]
    # rotate whole figure if incline: shift ys along a slope
    if incline:
        # tilt bench: rear (head end) higher
        def tilt(p):
            out = {}
            for k, (x, y) in p.items():
                dy = -(x - 100) * math.tan(math.radians(20))
                out[k] = (x, y + dy)
            return out
        poses = [tilt(p) for p in poses]
        bar_pts = [tilt({"w": pose["wrist_L"]})["w"] for pose in poses]

    bar = barbell("bar", 99, [T0, T1, T2], bar_pts, length=70, thickness=6)
    # bench
    if incline:
        bench = prop_rect("bench", 0, (100, 130), (160, 10), r=3)
    else:
        bench = prop_rect("bench", 0, (100, 128), (160, 10), r=3)
    floor = prop_rect("floor", 0, (100, 182), (180, 4))
    return build_figure(name, [T0, T1, T2], poses,
                        extras=[bar], props=[bench, floor])


def build_incline_bench():
    return build_bench_press("incline_bench", incline=True)


def build_chest_fly():
    """Supine, dumbbells wide-open then close over chest."""
    poses = []
    for wrist_offset in [40, 15, 40]:
        y_body = 110
        # 'up' = -y (toward top of canvas)
        wl = (100, y_body - wrist_offset)
        wr = (100, y_body + wrist_offset)
        el = (85, y_body - wrist_offset * 0.6)
        er = (85, y_body + wrist_offset * 0.6)
        p = supine_pose(y_body, {
            "wrist_L": wl, "wrist_R": wr,
            "elbow_L": el, "elbow_R": er,
        })
        poses.append(p)
    dbL = dumbbell("db_L", 98, [T0, T1, T2], [pose["wrist_L"] for pose in poses])
    dbR = dumbbell("db_R", 99, [T0, T1, T2], [pose["wrist_R"] for pose in poses])
    bench = prop_rect("bench", 0, (100, 128), (160, 10), r=3)
    return build_figure("chest_fly", [T0, T1, T2], poses,
                        extras=[dbL, dbR], props=[bench])


def build_overhead_press():
    """Standing figure presses barbell from shoulders to overhead."""
    poses = []
    for wrist_y in [72, 20, 72]:
        elbow_y = (wrist_y + 70) / 2 + 5  # bend under bar
        p = standing_pose({
            "elbow_L": (80, elbow_y), "elbow_R": (120, elbow_y),
            "wrist_L": (86, wrist_y), "wrist_R": (114, wrist_y),
        })
        poses.append(p)
    bar_pts = [((pose["wrist_L"][0] + pose["wrist_R"][0]) / 2,
                (pose["wrist_L"][1] + pose["wrist_R"][1]) / 2) for pose in poses]
    bar = barbell("bar", 99, [T0, T1, T2], bar_pts, length=80)
    return build_figure("overhead_press", [T0, T1, T2], poses, extras=[bar])


def build_lateral_raise():
    """Arms raise sideways to shoulder height, dumbbells in each hand."""
    poses = []
    # elbow angles: at rest arms down (elbow at 100, wrist 128).
    # raised: arms out at shoulder height => elbow at (~55/145, 70), wrist further out.
    rest = standing_pose()
    raised = standing_pose({
        "elbow_L": (60, 78),  "elbow_R": (140, 78),
        "wrist_L": (40, 72),  "wrist_R": (160, 72),
    })
    poses = [rest, raised, rest]
    dbL = dumbbell("db_L", 98, [T0, T1, T2], [pose["wrist_L"] for pose in poses])
    dbR = dumbbell("db_R", 99, [T0, T1, T2], [pose["wrist_R"] for pose in poses])
    return build_figure("shoulder_lateral_raise", [T0, T1, T2], poses,
                        extras=[dbL, dbR])


def build_front_raise():
    """Arms raise forward — rendered as raising up along vertical axis in front."""
    rest = standing_pose()
    raised = standing_pose({
        "elbow_L": (80, 78),  "elbow_R": (120, 78),
        "wrist_L": (80, 50),  "wrist_R": (120, 50),
    })
    poses = [rest, raised, rest]
    dbL = dumbbell("db_L", 98, [T0, T1, T2], [pose["wrist_L"] for pose in poses])
    dbR = dumbbell("db_R", 99, [T0, T1, T2], [pose["wrist_R"] for pose in poses])
    return build_figure("shoulder_front_raise", [T0, T1, T2], poses,
                        extras=[dbL, dbR])


def build_row():
    """Bent-over row: hinged torso, barbell pulls up to torso."""
    # Bent-over posture: shoulder_c forward (right of hip), torso angled.
    def bent_pose(wrist_y):
        p = standing_pose()
        # hinge: shoulders forward + down
        p["shoulder_c"] = (100, 95)
        p["hip_c"] = (100, 120)
        p["shoulder_L"] = (86, 96); p["shoulder_R"] = (114, 96)
        p["elbow_L"] = (80, 118);  p["elbow_R"] = (120, 118)
        p["wrist_L"] = (86, wrist_y); p["wrist_R"] = (114, wrist_y)
        p["head_c"] = (100, 72)
        # knees slightly bent
        p["knee_L"] = (88, 152); p["knee_R"] = (112, 152)
        return p
    down = bent_pose(150)
    up   = bent_pose(115)
    # Fix elbow y for 'up' pose (elbow higher than wrist when pulled up)
    up["elbow_L"] = (74, 108); up["elbow_R"] = (126, 108)
    poses = [down, up, down]
    bar_pts = [((pose["wrist_L"][0] + pose["wrist_R"][0]) / 2,
                (pose["wrist_L"][1] + pose["wrist_R"][1]) / 2) for pose in poses]
    bar = barbell("bar", 99, [T0, T1, T2], bar_pts, length=70)
    floor = prop_rect("floor", 0, (100, 182), (180, 4))
    return build_figure("row", [T0, T1, T2], poses, extras=[bar], props=[floor])


def build_pull_up():
    """Figure hangs from bar and pulls body up."""
    bar_y = 30
    # Hanging: arms fully extended up; body low.
    def hang_pose(body_y):
        return {
            "head_c":     (100, body_y - 30),
            "shoulder_c": (100, body_y - 10),
            "hip_c":      (100, body_y + 40),
            "shoulder_L": (86, body_y - 8), "shoulder_R": (114, body_y - 8),
            "elbow_L":    (86, (bar_y + body_y - 8) / 2),
            "elbow_R":    (114, (bar_y + body_y - 8) / 2),
            "wrist_L":    (86, bar_y), "wrist_R": (114, bar_y),
            "hip_L":      (92, body_y + 42), "hip_R": (108, body_y + 42),
            "knee_L":     (92, body_y + 72), "knee_R": (108, body_y + 72),
            "ankle_L":    (95, body_y + 100), "ankle_R": (105, body_y + 100),
        }
    down = hang_pose(80)
    up   = hang_pose(55)
    up["elbow_L"] = (78, 40); up["elbow_R"] = (122, 40)
    poses = [down, up, down]
    bar = prop_rect("bar", 0, (100, bar_y), (120, 6), color=SAGE)
    return build_figure("pull_up", [T0, T1, T2], poses, props=[bar])


def build_lat_pulldown():
    """Seated: pulling bar down from overhead to upper chest."""
    def seated(wrist_y, elbow_dx=8):
        p = standing_pose()
        p["hip_c"] = (100, 140); p["shoulder_c"] = (100, 90)
        p["head_c"] = (100, 62)
        p["shoulder_L"] = (86, 92); p["shoulder_R"] = (114, 92)
        p["wrist_L"] = (70, wrist_y); p["wrist_R"] = (130, wrist_y)
        # elbow between shoulder and wrist
        p["elbow_L"] = (76, (92 + wrist_y) / 2)
        p["elbow_R"] = (124, (92 + wrist_y) / 2)
        # sitting: knees bent forward
        p["hip_L"] = (92, 142); p["hip_R"] = (108, 142)
        p["knee_L"] = (78, 168); p["knee_R"] = (122, 168)
        p["ankle_L"] = (78, 188); p["ankle_R"] = (122, 188)
        return p
    up = seated(35)
    dn = seated(95)
    poses = [up, dn, up]
    bar_pts = [((pose["wrist_L"][0] + pose["wrist_R"][0]) / 2,
                (pose["wrist_L"][1] + pose["wrist_R"][1]) / 2) for pose in poses]
    bar = barbell("bar", 99, [T0, T1, T2], bar_pts, length=90)
    # Static top pulley bracket
    bracket = prop_rect("bracket", 0, (100, 15), (10, 20), color=SAGE, r=2)
    return build_figure("lat_pulldown", [T0, T1, T2], poses,
                        extras=[bar], props=[bracket])


def build_bicep_curl():
    """Standing, forearm rotates around elbow."""
    def pose_for(wrist_y):
        p = standing_pose()
        p["wrist_L"] = (80, wrist_y); p["wrist_R"] = (120, wrist_y)
        return p
    down = pose_for(128)
    up   = pose_for(72)
    poses = [down, up, down]
    dbL = dumbbell("db_L", 98, [T0, T1, T2], [pose["wrist_L"] for pose in poses])
    dbR = dumbbell("db_R", 99, [T0, T1, T2], [pose["wrist_R"] for pose in poses])
    return build_figure("bicep_curl", [T0, T1, T2], poses, extras=[dbL, dbR])


def build_tricep_extension():
    """One arm overhead, forearm extends down/back and returns."""
    def pose_for(wrist_y):
        p = standing_pose()
        # right arm overhead: elbow above shoulder, wrist behind head
        p["elbow_R"] = (108, 40); p["wrist_R"] = (92, wrist_y)
        # left arm at side (rest)
        return p
    bent = pose_for(48)     # forearm folded, wrist near head
    ext  = pose_for(20)     # forearm extended straight up
    poses = [bent, ext, bent]
    db = dumbbell("db", 99, [T0, T1, T2], [pose["wrist_R"] for pose in poses])
    return build_figure("tricep_extension", [T0, T1, T2], poses, extras=[db])


def build_squat():
    """Standing figure sinks (hips + knees bend) and rises."""
    def squat_at(depth):
        # depth in px the hips descend
        p = standing_pose()
        p["hip_c"] = (100, 118 + depth)
        p["hip_L"] = (92, 120 + depth); p["hip_R"] = (108, 120 + depth)
        p["shoulder_c"] = (100, 68 + depth * 0.5)
        p["shoulder_L"] = (86, 70 + depth * 0.5)
        p["shoulder_R"] = (114, 70 + depth * 0.5)
        p["head_c"] = (100, 42 + depth * 0.5)
        # knees push forward, ankles fixed
        p["knee_L"] = (82 - depth * 0.15, 150 + depth * 0.3)
        p["knee_R"] = (118 + depth * 0.15, 150 + depth * 0.3)
        p["ankle_L"] = (90, 180); p["ankle_R"] = (110, 180)
        p["elbow_L"] = (72, 100 + depth * 0.5)
        p["elbow_R"] = (128, 100 + depth * 0.5)
        p["wrist_L"] = (72, 128 + depth * 0.5)
        p["wrist_R"] = (128, 128 + depth * 0.5)
        return p
    up = squat_at(0)
    dn = squat_at(25)
    poses = [up, dn, up]
    floor = prop_rect("floor", 0, (100, 182), (180, 4))
    return build_figure("squat", [T0, T1, T2], poses, props=[floor])


def build_deadlift():
    """Figure hinges at hips to lift bar from floor to standing."""
    def stand_bar(bar_y, bend):
        p = standing_pose()
        # torso hinges; shoulders rotate forward when bent
        p["shoulder_c"] = (100, 68 + bend * 0.9)
        p["shoulder_L"] = (86, 70 + bend * 0.9); p["shoulder_R"] = (114, 70 + bend * 0.9)
        p["head_c"] = (100, 42 + bend * 0.9)
        p["hip_c"] = (100, 118 + bend * 0.2)
        p["hip_L"] = (92, 120 + bend * 0.2); p["hip_R"] = (108, 120 + bend * 0.2)
        # knees slight bend when bar low
        p["knee_L"] = (90, 150 + bend * 0.1); p["knee_R"] = (110, 150 + bend * 0.1)
        # arms hang straight to bar
        p["elbow_L"] = (86, (70 + bend * 0.9 + bar_y) / 2)
        p["elbow_R"] = (114, (70 + bend * 0.9 + bar_y) / 2)
        p["wrist_L"] = (86, bar_y); p["wrist_R"] = (114, bar_y)
        return p
    low  = stand_bar(bar_y=170, bend=60)
    high = stand_bar(bar_y=130, bend=0)
    poses = [low, high, low]
    bar_pts = [((pose["wrist_L"][0] + pose["wrist_R"][0]) / 2,
                (pose["wrist_L"][1] + pose["wrist_R"][1]) / 2) for pose in poses]
    bar = barbell("bar", 99, [T0, T1, T2], bar_pts, length=90)
    floor = prop_rect("floor", 0, (100, 182), (180, 4))
    return build_figure("deadlift", [T0, T1, T2], poses,
                        extras=[bar], props=[floor])


def build_lunge():
    """Step forward, lower back knee, return."""
    stand = standing_pose()
    # lunge: right leg forward+bent, left leg back+bent, hips lower.
    lunge = standing_pose({
        "hip_c": (100, 128),
        "hip_L": (92, 130), "hip_R": (108, 130),
        "knee_R": (130, 160), "ankle_R": (140, 180),
        "knee_L": (88, 170),  "ankle_L": (70, 180),
        "shoulder_c": (100, 78), "head_c": (100, 52),
        "shoulder_L": (86, 80), "shoulder_R": (114, 80),
        "elbow_L": (80, 110), "elbow_R": (120, 110),
        "wrist_L": (78, 138), "wrist_R": (122, 138),
    })
    poses = [stand, lunge, stand]
    floor = prop_rect("floor", 0, (100, 182), (180, 4))
    return build_figure("lunge", [T0, T1, T2], poses, props=[floor])


def build_leg_curl():
    """Prone figure curls lower leg up toward glutes."""
    def prone_curl(ankle_y):
        p = prone_pose(120, {
            "ankle_L": (176, 100 + (176 - ankle_y) * 0),  # not used
        })
        # override legs: knee stays put; ankle rises above knee (curled up)
        p["knee_L"] = (146, 112); p["knee_R"] = (146, 128)
        p["ankle_L"] = (150, ankle_y); p["ankle_R"] = (150, ankle_y + 12)
        # arms folded under chest for simplicity
        p["elbow_L"] = (60, 108); p["elbow_R"] = (60, 132)
        p["wrist_L"] = (52, 120); p["wrist_R"] = (52, 120)
        return p
    down = prone_curl(112)   # ankles extended (near knee level, legs straight)
    down["ankle_L"] = (176, 112); down["ankle_R"] = (176, 128)
    up   = prone_curl(80)    # ankles curled up above knees
    up["ankle_L"] = (140, 80); up["ankle_R"] = (140, 92)
    poses = [down, up, down]
    bench = prop_rect("bench", 0, (100, 132), (160, 8), r=2)
    return build_figure("leg_curl", [T0, T1, T2], poses, props=[bench])


def build_calf_raise():
    """Rises onto toes and lowers."""
    def pose(dy):
        p = standing_pose()
        # whole figure shifts up by dy; ankles stay lifted (heels up)
        for k in ("head_c", "shoulder_c", "shoulder_L", "shoulder_R",
                  "elbow_L", "elbow_R", "wrist_L", "wrist_R",
                  "hip_c", "hip_L", "hip_R", "knee_L", "knee_R"):
            x, y = p[k]; p[k] = (x, y - dy)
        # ankle y stays at 180 - dy (still lifted off "floor" showing toes)
        p["ankle_L"] = (90, 180 - dy); p["ankle_R"] = (110, 180 - dy)
        return p
    down = pose(0)
    up   = pose(8)
    poses = [down, up, down]
    floor = prop_rect("floor", 0, (100, 186), (180, 4))
    return build_figure("calf_raise", [T0, T1, T2], poses, props=[floor])


def build_plank():
    """Push-up top position with very subtle breathing motion."""
    def plank_pose(dy):
        p = prone_pose(120)
        # straight arms
        p["elbow_L"] = (66, 145); p["wrist_L"] = (66, 170)
        p["elbow_R"] = (66, 145); p["wrist_R"] = (66, 170)
        p["ankle_L"] = (176, 170); p["ankle_R"] = (176, 170)
        p["knee_L"] = (146, 165); p["knee_R"] = (146, 165)
        p["hip_L"] = (116, 122); p["hip_R"] = (116, 122)
        p["hip_c"] = (116, 122)
        p["shoulder_c"] = (66, 122 + dy); p["head_c"] = (40, 118 + dy)
        p["shoulder_L"] = (66, 122 + dy); p["shoulder_R"] = (66, 122 + dy)
        return p
    a = plank_pose(0)
    b = plank_pose(2)  # very subtle
    poses = [a, b, a]
    floor = prop_rect("floor", 0, (100, 178), (170, 4))
    return build_figure("plank", [T0, T1, T2], poses, props=[floor])


def build_crunch():
    """Supine with knees bent — torso curls up."""
    def supine_bent(shoulder_x):
        # bent knees, feet flat; head on left, feet on right.
        y = 130
        p = {
            "head_c":     (shoulder_x - 20, y - 20),
            "shoulder_c": (shoulder_x, y),
            "hip_c":      (shoulder_x + 40, y),
            "shoulder_L": (shoulder_x, y - 8), "shoulder_R": (shoulder_x, y + 8),
            "elbow_L":    (shoulder_x - 10, y - 18),
            "elbow_R":    (shoulder_x - 10, y + 18),
            "wrist_L":    (shoulder_x - 20, y - 22),
            "wrist_R":    (shoulder_x - 20, y + 22),
            "hip_L":      (shoulder_x + 40, y - 8), "hip_R": (shoulder_x + 40, y + 8),
            "knee_L":     (shoulder_x + 65, y - 30), "knee_R": (shoulder_x + 65, y + 30),
            "ankle_L":    (shoulder_x + 90, y - 8), "ankle_R": (shoulder_x + 90, y + 8),
        }
        return p
    flat = supine_bent(50)
    # curl: shoulders lift toward knees (shoulder_x moves right, y stays)
    up   = supine_bent(50)
    up["shoulder_c"] = (60, 118); up["head_c"] = (48, 100)
    up["shoulder_L"] = (60, 110); up["shoulder_R"] = (60, 126)
    up["elbow_L"]    = (55, 100); up["elbow_R"]    = (55, 128)
    up["wrist_L"]    = (48, 92);  up["wrist_R"]    = (48, 130)
    poses = [flat, up, flat]
    floor = prop_rect("floor", 0, (100, 178), (180, 4))
    return build_figure("crunch", [T0, T1, T2], poses, props=[floor])


def build_russian_twist():
    """Seated with legs raised; torso rotates side-to-side.

    Rendered as an isometric-ish view: hips stationary, shoulders swing
    horizontally, arms/hands together in front."""
    def twist(dx):
        y = 120
        p = {
            "head_c":     (100 + dx, 60),
            "shoulder_c": (100 + dx, 82),
            "hip_c":      (100, y),
            "shoulder_L": (86 + dx, 84), "shoulder_R": (114 + dx, 84),
            "elbow_L":    (86 + dx * 0.6, 108),
            "elbow_R":    (114 + dx * 0.6, 108),
            "wrist_L":    (95 + dx * 0.4, 118),
            "wrist_R":    (105 + dx * 0.4, 118),
            "hip_L":      (92, y), "hip_R": (108, y),
            "knee_L":     (78, 140), "knee_R": (122, 140),
            "ankle_L":    (60, 160), "ankle_R": (140, 160),
        }
        return p
    left  = twist(-18)
    right = twist(18)
    poses = [left, right, left]
    floor = prop_rect("floor", 0, (100, 176), (180, 4))
    return build_figure("russian_twist", [T0, T1, T2], poses, props=[floor])


BUILDERS = {
    "push_up":                 build_push_up,
    "bench_press":             build_bench_press,
    "incline_bench":           build_incline_bench,
    "chest_fly":               build_chest_fly,
    "overhead_press":          build_overhead_press,
    "shoulder_lateral_raise":  build_lateral_raise,
    "shoulder_front_raise":    build_front_raise,
    "row":                     build_row,
    "pull_up":                 build_pull_up,
    "lat_pulldown":            build_lat_pulldown,
    "bicep_curl":              build_bicep_curl,
    "tricep_extension":        build_tricep_extension,
    "squat":                   build_squat,
    "deadlift":                build_deadlift,
    "lunge":                   build_lunge,
    "leg_curl":                build_leg_curl,
    "calf_raise":              build_calf_raise,
    "plank":                   build_plank,
    "crunch":                  build_crunch,
    "russian_twist":           build_russian_twist,
}


def main() -> None:
    here = Path(__file__).resolve()
    repo_root = here.parents[2]
    out_dir = repo_root / "app" / "src" / "main" / "assets" / "ex" / "patterns"
    out_dir.mkdir(parents=True, exist_ok=True)

    for pattern, builder in BUILDERS.items():
        data = builder()
        path = out_dir / f"{pattern}.json"
        path.write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
        print(f"wrote {path.name} ({path.stat().st_size} bytes)")

    print(f"\n{len(BUILDERS)} pattern animations -> {out_dir}")


if __name__ == "__main__":
    main()
