# Student Rentals Scraper

[![Java CI](https://github.com/ohmygodjustload/StudentRentalsScraper/actions/workflows/maven.yml/badge.svg)](https://github.com/ohmygodjustload/StudentRentalsScraper/actions/workflows/maven.yml)

A Java data pipeline that scrapes rental listings from [studentrentalslacrosse.com](https://www.studentrentalslacrosse.com), cleans and normalizes the data, and enriches it with geocoding and travel-time information — built to find deals that aren't surfaced by the site's own search.

## Motivation

The site only publicly shows a listing when the landlord is paying to advertise it **and** the unit is vacant. Every other listing page still exists under a sequential ID; it just isn't linked or indexed.

This tool walks those IDs, keeps every valid listing page (including occupied units), and builds a landlord contact list you can use in late summer — before most students start hunting for the next school year. Best case you lock in a unit early; worst case the tenants renew and you've still networked with a landlord who may offer a different unit that was never listed.

Full strategy write-up: [docs/BACKGROUND.md](docs/BACKGROUND.md).

## Pipeline

```
Scrape ─► Raw JSON ─► PostProcess ─► Clean ─► Cleaned JSON + CSV
       ─► Geocode (Census by default) ─► TravelTime / WalkScore ─► Final JSON + CSV
       ↳ Crime report (agency-level, separate file)
```

| Stage | What happens |
|---|---|
| **Scrape** | Iterates listing IDs, extracts raw address, price, features, and page title |
| **PostProcess** | Title fallbacks for missing landlord / bed-bath; quality flags |
| **Clean** | Normalizes addresses, landlord names, bed/bath (`Xbd/Yba`), rent |
| **Geocode** | US Census Geocoder by default (no key); optional Geocodio; distance to campus |
| **Enrich** | Walk/bike travel times (TravelTime) and Walk Score |
| **Crime** | FBI CDE agency stats written to `output/API/crime_report_*.json` |

## Quick start

```powershell
.\srs.ps1                  # interactive menu
.\srs.ps1 help
.\srs.ps1 status
.\srs.ps1 scrape-clean --max-id=25
.\srs.ps1 resume           # geocode (Census) + enrich
```

Full CLI reference: [docs/CLI.md](docs/CLI.md).

| Command | Description |
|---|---|
| `full` | Scrape through final enrichment |
| `scrape-clean` | Scrape and clean, then stop |
| `clean` | Re-clean the latest raw JSON |
| `geocode` | Geocode the latest cleaned JSON |
| `apis` | Run API enrichment on the latest geocoded JSON |
| `resume` | Geocode + APIs from latest cleaned JSON |
| `status` | Latest outputs, configured keys, suggested next step |
| `help [cmd]` | Built-in help |

## Tech Stack

- **Java 21** with **Maven**
- [Jsoup](https://jsoup.org/) — HTML parsing and scraping
- [Jackson](https://github.com/FasterXML/jackson) — JSON serialization
- [OpenCSV](https://opencsv.sourceforge.net/) — CSV export/import
- [JUnit 5](https://junit.org/junit5/) — testing
- US Census Bureau Geocoder — free batch geocoding (no signup)

## Project Structure

```
src/main/java/
├── main/                 Orchestrator, CLI, menu, status
├── scraper/              Raw-only scraper
├── clean/                PostProcessor + DataCleaner
├── geocoding/            DataMerger, GeoUtils
├── api/                  Census, Geocodio, TravelTime, WalkScore, CrimeData
├── models/               Apartment, FeatureType, Flag
└── utils/                JsonUtils, CsvUtils

docs/                     CLI.md, BACKGROUND.md
tickets/                  Deferred work items
srs.ps1 / srs.sh          Launchers
output/                   Raw, Cleaned, Geocoded, API, Final
```

## API Keys (optional)

Census geocoding needs **no key**. For enrichment stages, create property files in `config/` (gitignored). Environment variables override file values.

| File | Keys | Env overrides |
|---|---|---|
| `TravelTime.properties` | `api.key`, `api.id` | `TRAVELTIME_API_KEY`, `TRAVELTIME_API_ID` |
| `WalkScore.properties` | `api.key` | `WALKSCORE_API_KEY` |
| `Geocodio.properties` | `api.key` (optional) | `GEOCODIO_API_KEY` |
| `CrimeData.properties` | `api.key`, `api.ori` | `CRIMEDATA_API_KEY`, `CRIMEDATA_API_ORI` |

See the `*_example.properties` files for the expected format. Missing keys skip that stage. Check configuration with `.\srs.ps1 status`.

## Roadmap

Open tickets live in [`tickets/`](tickets/README.md), including deal scoring, API caching, logging, persistence, and change detection.
