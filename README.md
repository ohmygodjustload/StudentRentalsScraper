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
Scrape ─► Raw JSON ─► Clean ─► Cleaned JSON + CSV ─► Geocode ─► API Enrichment ─► Final JSON
```

| Stage | What happens |
|---|---|
| **Scrape** | Iterates listing IDs 0–900 on the target site, extracts address, price, landlord, bed/bath, amenities, and flags missing data |
| **Clean** | Normalizes addresses (expand abbreviations, correct known typos), maps landlord name variations to canonical names |
| **Geocode** | Merges lat/long from a user-provided [Geocod.io](https://www.geocod.io/) CSV export |
| **Enrich** | Adds walk and bike travel times to campus via the [TravelTime API](https://traveltime.com/) |

Each stage saves its output independently, so you can resume from any point.

## Run Modes

The pipeline is controlled by a `RUN_MODE` constant in `Orchestrator.java`:

| Mode | Description |
|---|---|
| `FULL_PIPELINE` | Scrape through final enrichment |
| `SCRAPE_AND_CLEAN_ONLY` | Scrape and clean, then stop |
| `CLEAN_ONLY` | Re-clean the latest raw JSON |
| `GEOCODE_ONLY` | Merge geocoding into the latest cleaned JSON |
| `APIS_ONLY` | Run API enrichment on the latest geocoded JSON |
| `RESUME_FROM_CLEANED` | Pick up from cleaning through final enrichment |

## Tech Stack

- **Java 21** with **Maven**
- [Jsoup](https://jsoup.org/) — HTML parsing and scraping
- [Jackson](https://github.com/FasterXML/jackson) — JSON serialization (including JSR-310 date support)
- [OpenCSV](https://opencsv.sourceforge.net/) — CSV export
- [JUnit 5](https://junit.org/junit5/) — testing

## Project Structure

```
src/main/java/
├── main/Orchestrator.java          Entry point; controls pipeline flow
├── scraper/
│   ├── ApartmentScraper.java       Scraper interface
│   └── StudentRentalsScraper.java  Jsoup-based implementation
├── clean/DataCleaner.java          Address and landlord normalization
├── geocoding/DataMerger.java       Merges Geocod.io CSV data
├── api/
│   ├── TravelTimeAPI.java          Walk/bike travel times to campus
│   ├── WalkScoreAPI.java           Walk Score integration (planned)
│   └── CrimeDataAPI.java           Crime data integration (planned)
├── models/
│   ├── Apartment.java              Core data model
│   ├── FeatureType.java            Enum for listing feature labels
│   └── Flag.java                   Data-quality annotations
└── utils/
    ├── JsonUtils.java              JSON read/write helpers
    └── CsvUtils.java               CSV export helpers

output/
├── Raw/        Unmodified scraper output
├── Cleaned/    Normalized JSON + CSV
├── Geocoded/   After geocoding merge
├── API/        Intermediate API results
└── Final/      Fully enriched output
```

## Getting Started

### Prerequisites

- Java 21
- Maven 3.6+

### Build & Run

```bash
# Compile and run tests
mvn clean verify

# Run the pipeline
mvn exec:java
```

### API Keys (optional)

For stages that call external APIs, create property files in `config/`:

| File | Keys |
|---|---|
| `TravelTime.properties` | `api.key`, `api.id` |
| `WalkScore.properties` | `api.key` |
| `CrimeData.properties` | `api.key`, `api.ori` |

See the `*_example.properties` files for the expected format. These files are gitignored.

## Roadmap

- [ ] Full bed/bath normalization (fractional values, studio detection)
- [ ] Walk Score and crime data API integration
- [ ] Deal scoring algorithm (price-per-bedroom, amenities, distance)
- [ ] GUI with map view and rent-vs-distance scatter plot
- [ ] SQLite/H2 persistence for historical price tracking
- [ ] Change detection and alerting between scrapes
