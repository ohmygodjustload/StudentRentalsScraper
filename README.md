# Student Rentals Scraper

[![Java CI](https://github.com/ohmygodjustload/StudentRentalsScraper/actions/workflows/maven.yml/badge.svg)](https://github.com/ohmygodjustload/StudentRentalsScraper/actions/workflows/maven.yml)

A Java data pipeline that scrapes rental listings from [studentrentalslacrosse.com](https://www.studentrentalslacrosse.com), cleans and normalizes the data, and enriches it with geocoding and travel-time information — built to find deals that aren't surfaced by the site's own search.

## Motivation

The site only shows listings that landlords are actively paying to advertise. But every past and future listing still exists in the system under a sequential ID — it just isn't surfaced.

By iterating through all listing IDs directly (not just active ones), this tool captures two things the site intentionally hides:

1. **Units not yet listed** — landlords who will have availability in the coming months but haven't started advertising yet
2. **Inactive landlords** — property owners with no current listings who can be contacted directly before they start advertising

The goal was to build a contact list of landlords to reach out to in early summer — weeks or months before the majority of students begin their search in late summer. The timing advantage is the point.

## Pipeline

```
Scrape ─► Raw JSON ─► PostProcess ─► Clean ─► Cleaned JSON + CSV
       ─► Geocode ─► TravelTime / WalkScore ─► Final JSON + CSV
       ↳ Crime report (agency-level, separate file)
```

| Stage | What happens |
|---|---|
| **Scrape** | Iterates listing IDs, extracts raw address, price, features, and page title |
| **PostProcess** | Title fallbacks for missing landlord / bed-bath; quality flags |
| **Clean** | Normalizes addresses, landlord names, bed/bath (`Xbd/Yba`), rent |
| **Geocode** | Geocodio API (preferred) or manual Geocod.io CSV; distance to campus |
| **Enrich** | Walk/bike travel times (TravelTime) and Walk Score |
| **Crime** | FBI CDE agency stats written to `output/API/crime_report_*.json` |

Each stage saves its output independently, so you can resume from any point.

## Run Modes

```bash
mvn clean verify
mvn exec:java -Dexec.args="--help"
mvn exec:java -Dexec.args="--mode=scrape-clean"
mvn exec:java -Dexec.args="--mode=full --min-id=0 --max-id=25"
mvn exec:java -Dexec.args="--mode=clean"
mvn exec:java -Dexec.args="--mode=geocode"
mvn exec:java -Dexec.args="--mode=apis"
mvn exec:java -Dexec.args="--mode=resume"
```

| Mode | Description |
|---|---|
| `full` | Scrape through final enrichment |
| `scrape-clean` | Scrape and clean, then stop (default) |
| `clean` | Re-clean the latest raw JSON |
| `geocode` | Geocode the latest cleaned JSON |
| `apis` | Run API enrichment on the latest geocoded JSON |
| `resume` | Geocode + APIs from latest cleaned JSON |

Other flags: `--output-dir`, `--min-id`, `--max-id`, `--delay-ms`.

## Tech Stack

- **Java 21** with **Maven**
- [Jsoup](https://jsoup.org/) — HTML parsing and scraping
- [Jackson](https://github.com/FasterXML/jackson) — JSON serialization
- [OpenCSV](https://opencsv.sourceforge.net/) — CSV export/import
- [JUnit 5](https://junit.org/junit5/) — testing

## Project Structure

```
src/main/java/
├── main/                 Orchestrator, PipelineOptions, OutputPaths
├── scraper/              Raw-only scraper
├── clean/                PostProcessor + DataCleaner
├── geocoding/            DataMerger, GeoUtils
├── api/                  Geocodio, TravelTime, WalkScore, CrimeData
├── models/               Apartment, FeatureType, Flag
└── utils/                JsonUtils, CsvUtils

tickets/                  Deferred work items
output/                   Raw, Cleaned, Geocoded, API, Final
```

## Getting Started

### Prerequisites

- Java 21
- Maven 3.6+

### API Keys (optional)

Create property files in `config/` (gitignored). Environment variables override file values.

| File | Keys | Env overrides |
|---|---|---|
| `TravelTime.properties` | `api.key`, `api.id` | `TRAVELTIME_API_KEY`, `TRAVELTIME_API_ID` |
| `WalkScore.properties` | `api.key` | `WALKSCORE_API_KEY` |
| `Geocodio.properties` | `api.key` | `GEOCODIO_API_KEY` |
| `CrimeData.properties` | `api.key`, `api.ori` | `CRIMEDATA_API_KEY`, `CRIMEDATA_API_ORI` |

See the `*_example.properties` files for the expected format. Missing keys skip that stage.

Without a Geocodio key, geocoding pauses so you can place a Geocod.io CSV at `output/Geocoded/geocodio.csv`.

## Roadmap

Open tickets live in [`tickets/`](tickets/README.md), including deal scoring, API caching, logging, persistence, and change detection.
