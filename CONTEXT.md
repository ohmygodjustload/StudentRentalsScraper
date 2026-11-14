# StudentRentalsScraper – Project Context

This document is the single source of truth for AI tools and contributors. It is derived from the actual codebase. Do not rely on README.md alone for accuracy.

---

## Purpose

Scrapes rental listings from studentrentalslacrosse.com (La Crosse area), cleans and normalizes them, optionally geocodes and enriches via external APIs, and outputs JSON and CSV. Target: finding and comparing student rentals.

---

## Tech stack

- **Java 21** with **Maven** (`pom.xml`).
- **Libraries:** Jsoup, Jackson (databind + jsr310), OpenCSV, JUnit 5.
- Entry point: `main.Orchestrator` (`mvn exec:java`).

---

## Project layout

| Path | Role |
|------|------|
| `src/main/java/main/Orchestrator.java` | Entry point; CLI-driven pipeline. |
| `src/main/java/main/PipelineOptions.java` | CLI + `pipeline.properties` defaults. |
| `src/main/java/main/OutputPaths.java` | Timestamped output paths. |
| `src/main/java/scraper/` | `ApartmentScraper` + `StudentRentalsScraper` (raw extraction only). |
| `src/main/java/clean/` | `ApartmentPostProcessor` (title fallbacks) + `DataCleaner` (normalization). |
| `src/main/java/geocoding/` | `GeocodioAPI` path via `api/GeocodioAPI`; `DataMerger` for CSV fallback; `GeoUtils`. |
| `src/main/java/api/` | TravelTime, WalkScore, CrimeData, Geocodio, HttpTransport, ApiConfig. |
| `src/main/java/models/` | Apartment, FeatureType, Flag. |
| `src/main/java/utils/` | JsonUtils, CsvUtils. |
| `src/main/resources/pipeline.properties` | Default output dir, ID range, delay, campus coords. |
| `config/*_example.properties` | Example API key files (real keys gitignored). |
| `tickets/` | Open work items (former TODOs). |
| `output/` | Raw, Cleaned, Geocoded, API, Final. |

---

## Pipeline and run modes

```
Scrape → Raw JSON → PostProcess → Clean → Cleaned JSON+CSV
  → Geocode (API or manual CSV) → distanceToCampus
  → TravelTime → WalkScore → Final JSON+CSV
  ↳ CrimeData writes a separate crime_report JSON
```

Run via CLI (defaults from `pipeline.properties`):

```bash
mvn exec:java -Dexec.args="--mode=scrape-clean"
mvn exec:java -Dexec.args="--mode=full --min-id=0 --max-id=25"
```

| Mode | CLI value | Behavior |
|------|-----------|----------|
| Full pipeline | `full` | Scrape → clean → geocode → APIs → final |
| Scrape + clean | `scrape-clean` | Scrape → clean → exit (default) |
| Clean only | `clean` | Load latest raw JSON → post-process → clean |
| Geocode only | `geocode` | Load latest cleaned → geocode |
| APIs only | `apis` | Load latest geocoded → enrich |
| Resume | `resume` | Load latest cleaned → geocode → enrich |

Other flags: `--output-dir`, `--min-id`, `--max-id`, `--delay-ms`, `--help`.

Output directory is relative (`output` by default). Geocoding uses Geocodio API when `config/Geocodio.properties` (or `GEOCODIO_API_KEY`) is present; otherwise the pipeline pauses for a manual `output/Geocoded/geocodio.csv`.

---

## Data model

### Apartment

- Immutable: `id`, `url`.
- Core: `address`, `price`, `rentMonthly` (parsed int).
- Features: `EnumMap<FeatureType, String>`.
- Flags: `List<Flag>`.
- `rawTitle` (serialized in raw JSON for re-runnable fallbacks).
- Numeric: `bedrooms`, `bathrooms` (Double).
- Geocoding: `latitude`, `longitude`, `accuracy`, `geocodioAddress`, `distanceToCampus`.
- TravelTime / WalkScore enrichment fields (nullable boxed types).

### FeatureType / Flag

See `models/FeatureType` and `models/Flag`. Flags include title-fallback markers plus cleaner flags (`STUDIO`, `FRACTIONAL_*`, `BEDBATH_UNPARSEABLE`, `UNUSUAL_BEDROOM_COUNT`).

---

## Stage behavior

- **Scraper:** Raw HTML extraction only; stores `rawTitle`; no normalization.
- **PostProcessor:** Title fallback for missing landlord / bed-bath; sets flags. No-ops when `rawTitle` is absent (legacy raw files).
- **Cleaner:** Address corrections, landlord map, bed/bath → `Xbd/Yba` or `Studio/Yba`, numeric beds/baths, rent parsing.
- **Geocode:** Geocodio batch API or CSV merge; then haversine `distanceToCampus`.
- **TravelTime:** Walk + bike time-filter to campus; skips ungeocoded listings.
- **WalkScore:** Per-listing walk/bike scores.
- **CrimeData:** Agency-level FBI CDE report to `output/API/crime_report_*.json` (not per listing).

---

## Config and secrets

- Real `config/*.properties` are gitignored; `*_example.properties` are tracked.
- Env vars override file values: `TRAVELTIME_API_KEY`, `TRAVELTIME_API_ID`, `WALKSCORE_API_KEY`, `GEOCODIO_API_KEY`, `CRIMEDATA_API_KEY`, `CRIMEDATA_API_ORI`.
- Missing credentials skip that stage with a warning.

---

## Open work

See [tickets/README.md](tickets/README.md). Do not leave TODOs in source; add a ticket instead.
