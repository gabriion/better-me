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


def stub_exercises() -> list[dict]:
    groups = ["chest", "back", "shoulders", "legs", "arms", "core"]
    equipment = ["barbell", "dumbbell", "bodyweight", "cable", "machine"]
    out = []
    for g in groups:
        for e in equipment:
            out.append({
                "id": f"{g}_{e}",
                "name": f"{e.title()} {g.title()} exercise",
                "muscleGroup": g,
                "equipment": e,
                "repsLow": 6, "repsHigh": 12, "setsDefault": 3,
                "lottie": f"ex/{g}_{e}.json",
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
        {"id": "ppl_3", "name": "Push/Pull/Legs (3 days)", "days": ["push", "pull", "legs"]},
        {"id": "ul_4",  "name": "Upper/Lower (4 days)",   "days": ["upper", "lower", "upper", "lower"]},
        {"id": "fb_3",  "name": "Full Body (3 days)",     "days": ["full", "full", "full"]},
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
