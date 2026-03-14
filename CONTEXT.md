# StudentRentalsScraper – Project Context

This document is the single source of truth for AI tools and contributors. It is derived from the actual codebase. Do not rely on README.md for accuracy.

---

## Purpose

Scrapes rental listings from studentrentalslacrosse.com (La Crosse area), cleans and normalizes them, optionally geocodes and enriches via external APIs, and outputs JSON and CSV. Target: finding and comparing student rentals.

---

## Tech stack

- **Java 21** (Eclipse project; no Maven/Gradle in repo).
- **Libraries:** Jsoup (scraping), Jackson (core, databind, annotations, jsr310 for JSON), OpenCSV.
- **Classpath:** Defined in `.classpath`; JARs in `lib/`.

---

## Project layout

| Path | Role |
|------|------|
| `src/main/Orchestrator.java` | Entry point; run mode and pipeline flow. |
| `src/scraper/` | `ApartmentScraper` interface; `StudentRentalsScraper` implementation. |
| `src/clean/DataCleaner.java` | Address, landlord, and bed/bath normalization. |
| `src/geocoding/DataMerger.java` | Merges Geocodio CSV into apartment list. |
| `src/api/` | TravelTimeAPI (partial), WalkScoreAPI (stub), CrimeDataAPI (stub). |
| `src/models/` | Apartment, FeatureType, Flag. |
| `src/utils/` | JsonUtils, CsvUtils. |
| `src/gui/GUI.java` | Placeholder (min/max spinners, run button; not wired to pipeline). |
| `config/` | API keys (gitignored); `*_example.properties` show required keys. |
| `output/Raw` | Raw scraped JSON. |
| `output/Cleaned` | Cleaned JSON and CSV. |
| `output/Geocoded` | Geocoded JSON; user-placed `geocodio.csv` here. |
| `output/API` | Intermediate JSON after TravelTime, after WalkScore. |
| `output/Final` | Final JSON after all APIs. |

---

## Pipeline and run modes

Run mode is set in `Orchestrator.java` via the `RUN_MODE` constant. Current default: **SCRAPE_AND_CLEAN_ONLY**.

| Mode | Behavior |
|------|----------|
| `FULL_PIPELINE` | Scrape → raw JSON → Clean → cleaned JSON+CSV → (interactive) Geocode → geocoded JSON → TravelTime → WalkScore → CrimeData → final JSON. |
| `SCRAPE_AND_CLEAN_ONLY` | Scrape → raw JSON → Clean → cleaned JSON+CSV → exit. |
| `CLEAN_ONLY` | Load latest `listings_scraped_*.json` from Raw → Clean → save cleaned JSON+CSV. |
| `GEOCODE_ONLY` | Load latest `listings_cleaned_*.json` from Cleaned → prompt for `geocodio.csv` → merge → save geocoded JSON. |
| `APIS_ONLY` | Load latest `listings_geocoded_*.json` from Geocoded → TravelTime → WalkScore → CrimeData → save final JSON. |
| `RESUME_FROM_CLEANED` | Load latest cleaned JSON → Geocode → APIs → final JSON. |

- **OUTPUT_DIR** is hardcoded in Orchestrator (absolute path).
- Geocoding step is **interactive**: program pauses and asks the user to place `geocodio.csv` in `output/Geocoded/` before continuing.

---

## Data model

### Apartment

- **Immutable:** `id`, `url`.
- **Core:** `address`, `price`.
- **Features:** `EnumMap<FeatureType, String>` (landlord, bed_bath, included, available, air_conditioning, bus, email, laundry, parking, pets, phone, sq_ft, property_type).
- **Flags:** `List<Flag>` for data-quality annotations.
- **Scoring:** `dealScore` (double; scoring logic to move to separate scorer).
- **Numeric bed/bath:** `bedrooms`, `bathrooms` (double; TODOs for fuller use).
- **Geocoding:** `latitude`, `longitude`, `accuracy`, `geocodioAddress`, `distanceToCampus`.
- **TravelTime:** `walkTravelTimeSeconds`, `walkTravelTimeFormatted`, `bikeTravelTimeSeconds`, `bikeTravelTimeFormatted`.
- **WalkScore:** `walkScore`, `walkScoreDescription`, `bikeScore`, `bikeScoreDescription`.

### FeatureType

Enum with label-based lookup from HTML: LANDLORD, BED_BATH, INCLUDED, AVAILABLE, AIR_CONDITIONING, BUS, EMAIL, LAUNDRY, PARKING, PETS, PHONE, SQ_FT, PROPERTY_TYPE. Used in `div.rm_listing_features` rows (left-column label → FeatureType, right-column value).

### Flag

MISSING_LANDLORD, MISSING_BEDBATH, LANDLORD_FROM_TITLE, BEDBATH_FROM_TITLE. Indicate missing data or that a value was taken from the page title fallback.

---

## Scraper behavior

- **Source:** studentrentalslacrosse.com; URL pattern uses listing ID query param; IDs scanned from MIN_ID to MAX_ID (0–900 in code).
- **Politeness:** Delay between requests (configurable; currently 100 ms; TODO suggests 800–1200 ms for production).
- **Invalid listing:** 404, or placeholder title, or missing `div.rm_listing_features` → skip.
- **Extraction:** Address from last span in `div.sp_rp_bread`; price via regex on body text; features from `div.rm_listing_features > div.left-listing-row` (label → FeatureType.fromLabel, value stripped).
- **Fallbacks:** If landlord or bed/bath missing from features, parse from page title (e.g. "Landlord - 2br/1ba - ..."); set feature and add LANDLORD_FROM_TITLE or BEDBATH_FROM_TITLE; if still missing, add MISSING_LANDLORD or MISSING_BEDBATH.
- **Design intent (TODOs):** Scraper should do **raw extraction only**; no normalization (e.g. bed/bath formatting belongs in DataCleaner). Store raw strings; use fallbacks only to avoid nulls; add flags for fallback/missing.

---

## Cleaner behavior

- **DataCleaner** applies: `normalizeAddress`, `normalizeLandlord`, `normalizeBedBath` (then sets address, landlord, and BED_BATH feature back on each Apartment).
- **Address:** Trim, collapse spaces; apply explicit `ADDRESS_CORRECTIONS` map; insert space between letter and number; expand N/S/E/W; add state (WI/MN) if missing; optional regex format.
- **Landlord:** Lookup in `LANDLORD_MAP`; unmapped names left as-is. (Note: one mapping typo "Favre Retnals" in LANDLORD_MAP.)
- **Bed/bath:** `normalizeBedBath` is currently a placeholder (returns raw). TODOs: full normalization here (fractional beds/baths, Studio, Xbd/Yba format); scraper should not normalize.

---

## Geocoding

- **DataMerger.mergeGeocodioData(apartments, csvPath):** Matches CSV rows to apartments by listing URL (first column). Expects CSV headers: Geocodio Latitude, Geocodio Longitude, Geocodio Accuracy Score, Geocodio Address Line 1. Sets latitude, longitude, accuracy, geocodioAddress on each Apartment.
- Geocodio CSV is produced **externally**; user places it at `output/Geocoded/geocodio.csv` when the pipeline prompts.

---

## APIs

- **TravelTimeAPI:** Partial implementation: builds request (campus as arrival, listings as departures), HTTP POST to Travel Time API v4, response types defined. Credentials from `config/TravelTime.properties`. Campus coordinates and a geocoded filename are hardcoded in the class. `addTravelTimes` currently returns the list unchanged in the main pipeline.
- **WalkScoreAPI:** Stub; `addWalkScores` returns list unchanged.
- **CrimeDataAPI:** Stub; `addCrimeStats` returns list unchanged.

---

## Config and secrets

- `config/*.properties` are gitignored. Do not commit real keys.
- **TravelTime:** `api.key`, `api.id` (see `config/TravelTime_example.properties`).
- **WalkScore:** `api.key` (see `config/WalkScore_example.properties`).
- **CrimeData:** `api.key`, `api.ori` (see `config/CrimeData_example.properties`).

---

## Known TODOs and design direction

- **Scraper:** Raw-only extraction; move any normalization out; add ApartmentPostProcessor for fallback logic and flags.
- **Cleaner:** Implement full bed/bath normalization (fractional bedrooms/bathrooms, Studio, Xbd/Yba); own all normalization.
- **Apartment:** Use double bedrooms/bathrooms for analytics; keep string bed/bath for CSV/GUI.
- **Flags:** Add e.g. RAW_BEDBATH_FALLBACK, BEDBATH_NORMALIZED, FRACTIONAL_BEDROOM/BATHROOM.
- **Output contract:** Raw output = exactly what scraper extracted; cleaned output = normalized bed/bath, cleaned addresses, mapped landlords.
- **Tests:** CI compare raw vs cleaned; tests for bed/bath normalization and fractional support; sanity checks for bedroom counts.
- **Infrastructure:** TravelTime response caching; optional DB persistence (e.g. SQLite/H2).
- **GUI / deal scoring:** Future work; deal-scoring logic should move to a dedicated scorer class; GUI is placeholder.
