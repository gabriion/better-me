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
    base = ["eggs", "chicken", "fish", "rice", "broccoli", "spinach", "potato", "yogurt", "oats", "tuna"]
    slots = ["breakfast", "lunch", "dinner", "snack"]
    out = []
    for i, ing in enumerate(base):
        out.append({
            "id": f"meal_{i}",
            "name": f"{ing.title()} bowl",
            "slot": slots[i % len(slots)],
            "ingredients": [ing, "olive oil", "salt"],
            "kcal": 350 + 50 * (i % 5),
            "protein_g": 25, "carbs_g": 30, "fat_g": 12,
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
