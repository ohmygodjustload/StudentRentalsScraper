# Student Rentals Scraper

[![Ask DeepWiki](https://devin.ai/assets/askdeepwiki.png)](https://deepwiki.com/ohmygodjustload/StudentRentalsScraper)

This repository contains a Java-based web scraper and data processing pipeline designed to extract, clean, and enrich rental listing information from `studentrentalslacrosse.com`. The tool automates the collection of apartment data, normalizes inconsistencies, and enhances it with external data sources like geocoding, travel times, and walk scores.

## Features

- **Web Scraping**: Scrapes rental listings, including details like price, address, landlord, bed/bath count, and amenities using Jsoup.
- **Data Cleaning**: Implements a robust cleaning pipeline to normalize inconsistent data, such as landlord name variations and address formats.
- **Modular Pipeline**: An orchestrator manages a multi-stage data processing pipeline, allowing for flexible execution of different stages (scraping, cleaning, geocoding, API enrichment).
- **Data Enrichment**: Integrates with external services to add valuable context:
    - **Geocoding**: Merges latitude/longitude data from Geocod.io.
    - **Travel Time**: Calculates walk and bike travel times to a specified destination.
    - **Walk Score**: Fetches Walk Score and Bike Score for each listing.
- **Deal Scoring**: A basic scoring system evaluates listings based on factors like price-per-bedroom and data completeness.
- **Persistent Outputs**: Saves data at various processing stages (raw, cleaned, geocoded, final) in both JSON and CSV formats.

## How It Works

The project is driven by the `Orchestrator` class, which executes a series of steps to process the data.

1.  **Scraping**: The `StudentRentalsScraper` iterates through listing IDs on `studentrentalslacrosse.com`, extracts raw data from valid listings, and saves the output to `output/Raw/listings_scraped_{timestamp}.json`.

2.  **Cleaning**: The `DataCleaner` processes the raw data, standardizing landlord names and addresses based on predefined mappings and rules. The cleaned data is saved to `output/Cleaned/listings_cleaned_{timestamp}.json` and `.csv`.

3.  **Geocoding (Manual Step)**: The pipeline pauses, requiring the user to place an enhanced CSV from a service like Geocod.io (named `geocodio.csv`) into the `output/Geocoded` directory. The `DataMerger` then integrates this location data. The result is saved in `output/Geocoded/listings_geocoded_{timestamp}.json`.

4.  **API Enrichment**: The orchestrator proceeds to call a series of external APIs to add more context to each listing:
    - `TravelTimeAPI` adds walk and bike travel times.
    - `WalkScoreAPI` adds Walk Score and Bike Score.
    - `CrimeDataAPI` can be used to add local crime statistics.
    Intermediate and final results are saved in `output/API` and `output/Final`.

## Project Structure

The project uses the standard Maven layout:

-   `src/main/java/main/Orchestrator.java`: The main entry point that controls the data processing pipeline.
-   `src/main/java/scraper/StudentRentalsScraper.java`: Handles the core logic for fetching and parsing HTML from the target website.
-   `src/main/java/clean/DataCleaner.java`: Contains logic for normalizing and standardizing scraped data like addresses and landlord names.
-   `src/main/java/geocoding/DataMerger.java`: Merges the cleaned data with external geocoding information from a CSV file.
-   `src/main/java/api/`: Contains classes for interacting with external APIs (Travel Time, Walk Score, etc.).
-   `src/main/java/models/`: Defines data structures, including `Apartment`, `FeatureType`, and `Flag`.
-   `src/main/java/utils/`: Provides helper classes for JSON (`JsonUtils`) and CSV (`CsvUtils`) file operations.
-   `src/test/java/`: JUnit 5 tests.
-   `output/`: The default directory where all generated files are stored in their respective subdirectories (`Raw`, `Cleaned`, `Geocoded`, `API`, `Final`).

## Prerequisites

-   **Java 21**
-   **Maven 3.6+**

## Build and Test

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package JAR (includes Main-Class in manifest)
mvn package
```

## How to Run

The entire pipeline is controlled by the `Orchestrator` class. Configure the `RUN_MODE` static final variable inside `src/main/java/main/Orchestrator.java` as needed.

1.  **Clone the repository.**
2.  **Create folder `config/` and add files** `CrimeData.properties`, `TravelTime.properties`, and `WalkScore.properties` (these contain API keys).
3.  **Run the application** (from the project root):
    -   `mvn exec:java` — runs `main.Orchestrator` (main class is configured in `pom.xml`).
    -   Or open the project in your IDE as a Maven project and run `main.Orchestrator`.
4.  **Set the `RUN_MODE`** in `Orchestrator.java` before running. Available modes:
    -   `FULL_PIPELINE`: Executes all steps from scraping to final API enrichment.
    -   `SCRAPE_AND_CLEAN_ONLY`: Scrapes data and cleans it, then stops.
    -   `CLEAN_ONLY`: Loads the latest raw scraped JSON and runs the cleaning process.
    -   `GEOCODE_ONLY`: Loads the latest cleaned data and merges it with the geocoding CSV.
    -   `APIS_ONLY`: Loads the latest geocoded data and runs all API enrichment steps.
    -   `RESUME_FROM_CLEANED`: Skips scraping and starts from the geocoding step.
5.  **(Optional) Configure the output directory**: Modify the `OUTPUT_DIR` constant in `Orchestrator.java` if you wish to save files to a different location.

If running a mode that includes geocoding, the program will pause and prompt you to place the `geocodio.csv` file in the `output/Geocoded` directory before continuing.

### Dependencies

Dependencies are managed by Maven; see `pom.xml`. They include Jsoup (HTML parsing), Jackson (JSON), OpenCSV, and JUnit 5 for tests.

