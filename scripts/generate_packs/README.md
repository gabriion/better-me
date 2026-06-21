# Content pack generation

Pre-generates offline content bundled with Better Me so the app runs without any LLM at runtime.

## Usage

```
py generate_packs.py all          # quotes, meals, exercises, workouts
py generate_packs.py quotes       # just quotes
```

## Backends

Set `BETTERME_LLM_BACKEND`:
- `stub` (default) — deterministic synthetic content for development
- `openai` — uses `OPENAI_API_KEY`
- `azure` — uses `AZURE_OPENAI_*`

V1 ships with the stub backend wired and a small curated quotes pack already in `app/src/main/assets/content/quotes.json`. Real LLM backends are scaffolded for the next iteration.
