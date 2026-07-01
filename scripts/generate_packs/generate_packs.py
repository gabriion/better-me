#!/usr/bin/env python3
"""
generate_packs.py — produce offline content packs bundled into the app.

V1 outputs (writes to ../../app/src/main/assets/content/):
  - quotes.json      (~365 positive quotes across 5 themes)
  - meals.json       (~60 recipes with ingredients, macros, slot)
  - exercises.json   (~80 exercises with muscle group, equipment, reps)
  - workout_templates.json (PPL, U/L, full-body splits)

Each generator can run independently:
    py generate_packs.py quotes
    py generate_packs.py all

The LLM call is intentionally pluggable — set BETTERME_LLM_BACKEND to:
    openai  -> uses OPENAI_API_KEY
    azure   -> uses AZURE_OPENAI_* env vars
    stub    -> deterministic synthetic content for local dev (default)
"""

from __future__ import annotations
import argparse, json, os, sys
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets" / "content"
THEMES = ["zen", "mindfulness", "inner_child", "inner_peace", "empowerment"]


def stub_quotes(n: int = 365) -> list[dict]:
    seeds = [
        "Be gentle with yourself today.",
        "Small steps, taken kindly, become a life.",
        "The present moment is the only moment available to you.",
        "You don't have to be perfect to be enough.",
        "Your inner child still loves the simple things. Let them.",
        "Breathe. The hard part has passed.",
        "Discipline is the kindest gift you give your future self.",
        "Stillness is not absence. It is presence.",
        "What you practise, you become.",
        "Peace begins when expectation ends.",
    ]
    out = []
    for i in range(n):
        out.append({"text": seeds[i % len(seeds)], "theme": THEMES[i % len(THEMES)]})
    return out


def _pattern_for(g: str, e: str, vname: str) -> str:
    """Pick the closest movement-pattern animation for an exercise.

    Patterns are the 20 stick-figure Lottie files under
    ``app/src/main/assets/ex/patterns/``. Selection is name-driven so the
    catalogue can grow without touching this function.
    """
    n = vname.lower()

    # ---- core: pattern is driven mostly by the exercise name ----
    if g == "core":
        if "twist" in n or "woodchopper" in n:
            return "russian_twist"
        if "plank" in n or "hollow" in n or "mountain" in n:
            return "plank"
        # crunch, bicycle crunch, leg raise, hanging knee raise, ab crunch...
        return "crunch"

    # ---- arms: curls vs extensions ----
    if g == "arms":
        if "curl" in n:
            return "bicep_curl"
        if "push-up" in n or "push up" in n or "pushup" in n:
            return "push_up"
        # kickback, pushdown, overhead triceps ext, close-grip bench, dip
        return "tricep_extension"

    # ---- chest ----
    if g == "chest":
        if e == "bodyweight":
            return "push_up"
        if e == "barbell":
            return "bench_press"
        if e == "dumbbell":
            if "fly" in n or "pullover" in n:
                return "chest_fly"
            return "bench_press"
        if e == "cable":
            return "chest_fly"
        if e == "machine":
            # "chest press" -> bench, "pec deck" -> fly
            return "bench_press" if "press" in n else "chest_fly"
        return "bench_press"

    # ---- back ----
    if g == "back":
        if "deadlift" in n:
            return "deadlift"
        if "pull-up" in n or "pull up" in n or "pullup" in n:
            return "pull_up"
        if "lat pulldown" in n or "pulldown" in n:
            return "lat_pulldown"
        if "row" in n or "face pull" in n:
            return "row"
        return "row"

    # ---- shoulders ----
    if g == "shoulders":
        if "lateral raise" in n or "rear delt" in n:
            return "shoulder_lateral_raise"
        if "front raise" in n:
            return "shoulder_front_raise"
        if "upright row" in n:
            return "row"
        # presses, push press, arnold press, pike push-up
        return "overhead_press"

    # ---- legs ----
    if g == "legs":
        if "lunge" in n or "split squat" in n:
            return "lunge"
        if "deadlift" in n or "pull-through" in n or "pull through" in n:
            return "deadlift"
        if "curl" in n or "kickback" in n or "extension" in n:
            return "leg_curl"
        # back/front squat, goblet squat, hip thrust, leg press, air squat
        return "squat"

    return "squat"  # sensible default; never reached with current catalogue


def stub_exercises() -> list[dict]:
    """Deterministic ~80-entry exercise catalogue.

    6 muscle groups × ~13 (equipment, variation) pairs each.
    Rep/set ranges by equipment family, with a core override.
    """
    # (equipment, variation_name)
    group_variations: dict[str, list[tuple[str, str]]] = {
        "chest": [
            ("barbell",    "Bench Press"),
            ("barbell",    "Incline Bench Press"),
            ("barbell",    "Decline Bench Press"),
            ("dumbbell",   "Bench Press"),
            ("dumbbell",   "Incline Press"),
            ("dumbbell",   "Fly"),
            ("dumbbell",   "Pullover"),
            ("cable",      "Crossover"),
            ("cable",      "Low-to-High Fly"),
            ("machine",    "Chest Press"),
            ("machine",    "Pec Deck"),
            ("bodyweight", "Push-Up"),
            ("bodyweight", "Decline Push-Up"),
        ],
        "back": [
            ("barbell",    "Deadlift"),
            ("barbell",    "Bent-Over Row"),
            ("barbell",    "Pendlay Row"),
            ("dumbbell",   "One-Arm Row"),
            ("dumbbell",   "Chest-Supported Row"),
            ("cable",      "Seated Row"),
            ("cable",      "Lat Pulldown"),
            ("cable",      "Straight-Arm Pulldown"),
            ("cable",      "Face Pull"),
            ("machine",    "Assisted Pull-Up"),
            ("machine",    "T-Bar Row"),
            ("bodyweight", "Pull-Up"),
            ("bodyweight", "Inverted Row"),
        ],
        "shoulders": [
            ("barbell",    "Overhead Press"),
            ("barbell",    "Push Press"),
            ("dumbbell",   "Shoulder Press"),
            ("dumbbell",   "Arnold Press"),
            ("dumbbell",   "Lateral Raise"),
            ("dumbbell",   "Front Raise"),
            ("dumbbell",   "Rear Delt Fly"),
            ("cable",      "Lateral Raise"),
            ("cable",      "Rear Delt Fly"),
            ("cable",      "Upright Row"),
            ("machine",    "Shoulder Press"),
            ("machine",    "Rear Delt Pec Deck"),
            ("bodyweight", "Pike Push-Up"),
        ],
        "legs": [
            ("barbell",    "Back Squat"),
            ("barbell",    "Front Squat"),
            ("barbell",    "Romanian Deadlift"),
            ("barbell",    "Hip Thrust"),
            ("dumbbell",   "Goblet Squat"),
            ("dumbbell",   "Walking Lunge"),
            ("dumbbell",   "Bulgarian Split Squat"),
            ("cable",      "Pull-Through"),
            ("cable",      "Glute Kickback"),
            ("machine",    "Leg Press"),
            ("machine",    "Leg Extension"),
            ("machine",    "Hamstring Curl"),
            ("bodyweight", "Air Squat"),
        ],
        "arms": [
            ("barbell",    "Curl"),
            ("barbell",    "Close-Grip Bench Press"),
            ("dumbbell",   "Curl"),
            ("dumbbell",   "Hammer Curl"),
            ("dumbbell",   "Incline Curl"),
            ("dumbbell",   "Overhead Triceps Extension"),
            ("dumbbell",   "Kickback"),
            ("cable",      "Triceps Pushdown"),
            ("cable",      "Rope Hammer Curl"),
            ("cable",      "Overhead Triceps Extension"),
            ("machine",    "Preacher Curl"),
            ("machine",    "Triceps Dip"),
            ("bodyweight", "Diamond Push-Up"),
        ],
        "core": [
            ("bodyweight", "Plank"),
            ("bodyweight", "Side Plank"),
            ("bodyweight", "Hollow Hold"),
            ("bodyweight", "Crunch"),
            ("bodyweight", "Bicycle Crunch"),
            ("bodyweight", "Leg Raise"),
            ("bodyweight", "Hanging Knee Raise"),
            ("bodyweight", "Mountain Climber"),
            ("bodyweight", "Russian Twist"),
            ("dumbbell",   "Russian Twist"),
            ("cable",      "Woodchopper"),
            ("cable",      "Crunch"),
            ("machine",    "Ab Crunch"),
        ],
    }

    out: list[dict] = []
    for g, variations in group_variations.items():
        for e, vname in variations:
            # Base ranges by equipment family
            if g == "core":
                reps_low, reps_high, sets = 10, 25, 3
            elif e == "bodyweight":
                reps_low, reps_high, sets = 8, 20, 3
            elif e == "barbell":
                reps_low, reps_high, sets = 6, 12, 4
            elif e == "dumbbell":
                reps_low, reps_high, sets = 6, 12, 3
            else:  # cable, machine
                reps_low, reps_high, sets = 8, 15, 3

            slug_var = vname.lower().replace(" ", "_").replace("-", "_").replace("/", "_")
            ex_id = f"{g}_{e}_{slug_var}"
            display_name = f"{e.title()} {vname}"
            out.append({
                "id": ex_id,
                "name": display_name,
                "muscleGroup": g,
                "equipment": e,
                "repsLow": reps_low,
                "repsHigh": reps_high,
                "setsDefault": sets,
                "lottie": f"ex/{ex_id}.json",
                "pattern": _pattern_for(g, e, vname),
            })
    return out


def stub_meals() -> list[dict]:
    """Deterministic ~30-meal pack mixing proteins, carbs and vegetables.

    Slot distribution (target): 6 breakfast, 10 lunch, 10 dinner, 4 snack.
    Kcal ranges 200..650 depending on slot to feel realistic.
    """
    # (slot, protein, carb_or_base, veg_or_extra, name_template, kcal, p, c, f)
    recipes: list[tuple] = [
        # --- breakfast (6) ---
        ("breakfast", "eggs",   "oats",     "spinach",   "Eggs & spinach oats",        420, 28, 45, 14),
        ("breakfast", "yogurt", "oats",     None,        "Yogurt oats bowl",           360, 22, 50, 8),
        ("breakfast", "eggs",   "potato",   "peppers",   "Eggs, potato & peppers hash",480, 26, 40, 22),
        ("breakfast", "yogurt", None,       "cucumber",  "Yogurt & cucumber plate",    260, 18, 18, 10),
        ("breakfast", "eggs",   None,       "tomato",    "Eggs with tomato",           320, 22, 8,  22),
        ("breakfast", "oats",   None,       None,        "Classic oats porridge",      300, 12, 55, 6),
        # --- lunch (10) ---
        ("lunch",     "chicken","rice",     "broccoli",  "Chicken, rice & broccoli",   560, 42, 60, 14),
        ("lunch",     "chicken","quinoa",   "spinach",   "Chicken quinoa salad",       520, 40, 50, 16),
        ("lunch",     "tuna",   "rice",     "cucumber",  "Tuna rice bowl",             500, 36, 55, 12),
        ("lunch",     "tuna",   None,       "tomato",    "Tuna & tomato salad",        340, 32, 12, 18),
        ("lunch",     "chicken","potato",   "peppers",   "Chicken with potato & peppers",600,44, 55, 20),
        ("lunch",     "fish",   "quinoa",   "broccoli",  "Fish quinoa plate",          540, 38, 48, 18),
        ("lunch",     "eggs",   "rice",     "spinach",   "Egg fried rice with spinach",470, 24, 55, 16),
        ("lunch",     "chicken","rice",     "peppers",   "Chicken pepper rice",        580, 42, 60, 18),
        ("lunch",     "tuna",   "quinoa",   "spinach",   "Tuna quinoa spinach bowl",   510, 38, 45, 16),
        ("lunch",     "chicken",None,       "tomato",    "Chicken tomato salad",       380, 40, 12, 16),
        # --- dinner (10) ---
        ("dinner",    "fish",   "potato",   "broccoli",  "Baked fish, potato & broccoli",560,38, 50, 20),
        ("dinner",    "chicken","potato",   "spinach",   "Chicken, potato & spinach",  610, 44, 55, 22),
        ("dinner",    "fish",   "rice",     "spinach",   "Fish rice spinach plate",    520, 36, 55, 16),
        ("dinner",    "chicken","quinoa",   "broccoli",  "Chicken quinoa broccoli",    540, 42, 50, 16),
        ("dinner",    "tuna",   "potato",   "peppers",   "Tuna potato pepper bake",    500, 34, 50, 16),
        ("dinner",    "eggs",   "potato",   "broccoli",  "Eggs, potato & broccoli",    470, 26, 50, 18),
        ("dinner",    "fish",   "quinoa",   "tomato",    "Fish quinoa tomato bowl",    530, 36, 50, 18),
        ("dinner",    "chicken","rice",     "spinach",   "Chicken rice & spinach",     580, 44, 60, 16),
        ("dinner",    "tuna",   "rice",     "broccoli",  "Tuna rice broccoli",         490, 34, 55, 14),
        ("dinner",    "fish",   "potato",   "peppers",   "Fish, potato & peppers",     520, 36, 50, 18),
        # --- snack (4) ---
        ("snack",     "yogurt", None,       None,        "Greek yogurt cup",           200, 18, 14, 6),
        ("snack",     "tuna",   None,       "cucumber",  "Tuna cucumber bites",        240, 26, 6,  10),
        ("snack",     "eggs",   None,       None,        "Two boiled eggs",            220, 18, 2,  16),
        ("snack",     "yogurt", "oats",     None,        "Yogurt & oats snack",        280, 14, 30, 8),
    ]

    out: list[dict] = []
    for i, (slot, protein, carb, veg, name, kcal, p, c, f) in enumerate(recipes):
        ings: list[str] = []
        for x in (protein, carb, veg):
            if x and x not in ings:
                ings.append(x)
        # pantry staples — repo strips these out for the ingredient cloud
        ings += ["olive oil", "salt"]
        out.append({
            "id": f"meal_{i}",
            "name": name,
            "slot": slot,
            "ingredients": ings,
            "kcal": kcal,
            "protein_g": p, "carbs_g": c, "fat_g": f,
        })
    return out


def stub_workouts() -> list[dict]:
    return [
        {"id": "ul_2",  "name": "Upper/Lower (2 days)",     "days": ["upper", "lower"]},
        {"id": "ppl_3", "name": "Push/Pull/Legs (3 days)",  "days": ["push", "pull", "legs"]},
        {"id": "fb_3",  "name": "Full Body (3 days)",       "days": ["full", "full", "full"]},
        {"id": "ul_4",  "name": "Upper/Lower (4 days)",     "days": ["upper", "lower", "upper", "lower"]},
        {"id": "bro_5", "name": "Bro Split (5 days)",       "days": ["push", "pull", "legs", "upper", "lower"]},
    ]


GENERATORS = {
    "quotes": (lambda: stub_quotes(365), "quotes.json"),
    "meals": (stub_meals, "meals.json"),
    "exercises": (stub_exercises, "exercises.json"),
    "workouts": (stub_workouts, "workout_templates.json"),
}


def run(name: str) -> None:
    fn, filename = GENERATORS[name]
    ASSETS.mkdir(parents=True, exist_ok=True)
    data = fn()
    path = ASSETS / filename
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"wrote {len(data)} entries -> {path.relative_to(Path.cwd()) if str(Path.cwd()) in str(path) else path}")


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("target", choices=list(GENERATORS.keys()) + ["all"])
    args = p.parse_args()
    targets = list(GENERATORS.keys()) if args.target == "all" else [args.target]
    for t in targets:
        run(t)
    return 0


if __name__ == "__main__":
    sys.exit(main())
