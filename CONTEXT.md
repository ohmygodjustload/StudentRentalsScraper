# StudentRentalsScraper – Project Context

This document is the single source of truth for AI tools and contributors. It is derived from the actual codebase. Do not rely on README.md alone for accuracy.

---

## Purpose

Scrapes rental listings from studentrentalslacrosse.com (La Crosse area), cleans and normalizes them, geocodes and enriches via external APIs, and outputs JSON and CSV. Target: building a landlord contact list from listings that may not appear in the site’s public index. Strategy details: [docs/BACKGROUND.md](docs/BACKGROUND.md).

---

## Tech stack

- **Java 21** with **Maven** (`pom.xml`).
- **Libraries:** Jsoup, Jackson (databind + jsr310), OpenCSV, JUnit 5.
- Entry point: `main.Orchestrator`.
- Launchers: `srs.ps1` / `srs.sh` → `java -jar target/scraper-all.jar` (shaded via `maven-shade-plugin`).

---

## Project layout

| Path | Role |
|------|------|
| `src/main/java/main/Orchestrator.java` | Entry point; CLI-driven pipeline. |
| `src/main/java/main/PipelineOptions.java` | Positional commands + flags; `pipeline.properties` defaults. |
| `src/main/java/main/CliHelp.java` | Built-in help overview and per-command help. |
| `src/main/java/main/InteractiveMenu.java` | Numbered menu when launched with no args. |
| `src/main/java/main/StatusCommand.java` | Latest outputs, keys, next-step suggestion. |
| `src/main/java/main/OutputPaths.java` | Timestamped output paths. |
| `src/main/java/scraper/` | Raw extraction only. |
| `src/main/java/clean/` | Title fallbacks + normalization. |
| `src/main/java/geocoding/` | CSV merger fallback; `GeoUtils`. |
| `src/main/java/api/` | CensusGeocoder, Geocodio, TravelTime, WalkScore, CrimeData, HttpTransport, ApiConfig. |
| `src/main/resources/pipeline.properties` | Defaults (output dir, ID range, delay, campus coords). |
| `docs/CLI.md` | Full CLI reference. |
| `docs/BACKGROUND.md` | Why the scraper indexes unlisted pages. |
| `tickets/` | Open work items (former TODOs). |
| `output/` | Raw, Cleaned, Geocoded, API, Final. |

---

## Pipeline and commands

```
Scrape → Raw JSON → PostProcess → Clean → Cleaned JSON+CSV
  → Geocode (Census default / Geocodio if keyed / CSV) → distanceToCampus
  → TravelTime → WalkScore → Final JSON+CSV
  ↳ CrimeData writes a separate crime_report JSON
```

```powershell
.\srs.ps1 <command> [options]
.\srs.ps1                  # interactive menu
```

| Command | Behavior |
|---------|----------|
| `full` | Scrape → clean → geocode → APIs → final |
| `scrape-clean` | Scrape → clean → exit |
| `clean` | Load latest raw → post-process → clean |
| `geocode` | Load latest cleaned → geocode |
| `apis` | Load latest geocoded → enrich |
| `resume` | Load latest cleaned → geocode → enrich |
| `status` | Latest files + keys + next step |
| `help [cmd]` | Built-in help |

Flags: `--min-id`, `--max-id`, `--delay-ms`, `--output-dir`, `--geocoder=auto|census|geocodio|csv`, `--mode=` (legacy), `--help`.

### Geocoder selection (`--geocoder`, default `auto`)

- `auto`: Geocodio if key present, otherwise US Census (no key).
- `census`: always Census batch geocoder.
- `geocodio`: require Geocodio key.
- `csv`: interactive pause for `output/Geocoded/geocodio.csv` only.

---

## Data model

### Apartment

- Immutable: `id`, `url`.
- Core: `address`, `price`, `rentMonthly`.
- Features / flags / `rawTitle`.
- Numeric: `bedrooms`, `bathrooms`.
- Geocoding: `latitude`, `longitude`, `accuracy`, `geocodedAddress` (JSON alias `geocodioAddress` for old files), `distanceToCampus`.
- TravelTime / WalkScore enrichment fields (nullable boxed types).

### Flags

Includes title-fallback and cleaner flags, plus `GEOCODE_APPROXIMATE` and `GEOCODE_NO_MATCH` from Census.

---

## Stage behavior

- **Scraper:** Raw HTML only; stores `rawTitle`.
- **PostProcessor:** Title fallback for missing landlord / bed-bath.
- **Cleaner:** Address, landlord, bed/bath → `Xbd/Yba` or `Studio/Yba`, rent parsing.
- **Geocode:** Census batch (default), optional Geocodio, or CSV; then haversine `distanceToCampus`.
- **TravelTime / WalkScore / CrimeData:** as before; missing keys skip with a warning.

---

## Config and secrets

- Real `config/*.properties` are gitignored; `*_example.properties` are tracked.
- Env vars override file values (`TRAVELTIME_API_KEY`, etc.).
- Geocodio is optional. Census needs no key.

---

## Open work

See [tickets/README.md](tickets/README.md). Do not leave TODOs in source; add a ticket instead.
