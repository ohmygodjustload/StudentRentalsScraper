/**
 * Built-in CLI help text for overview and per-command details.
 *
 * @author Andrew Peirce
 */
package main;

public final class CliHelp {

    private CliHelp() {
    }

    public static String overview() {
        return """
                StudentRentalsScraper

                Usage:
                  .\\srs.ps1 <command> [options]     (Windows)
                  ./srs.sh <command> [options]      (macOS/Linux)
                  mvn exec:java -Dexec.args="<command> [options]"

                Commands:
                  full           Scrape -> clean -> geocode -> enrich -> final output
                  scrape-clean   Scrape and clean only (default if you pass only flags)
                  clean          Re-clean the latest raw JSON
                  geocode        Geocode the latest cleaned JSON
                  apis           Enrich the latest geocoded JSON (travel, walk, crime)
                  resume         Geocode + enrich from the latest cleaned JSON
                  status         Show latest outputs, configured keys, and next step
                  audit          Compare bed/bath from title, Bed/Bath field, and description
                  score          Recompute deal scores on the latest Final output (no API calls)
                  help [cmd]     This overview, or details for one command

                Common options:
                  --min-id=<n>        First listing ID to scrape (default: 0)
                  --max-id=<n>        Last listing ID to scrape (default: 900)
                  --delay-ms=<n>      Delay between scrape requests (default: 1000)
                  --output-dir=<path> Output root (default: output)
                  --geocoder=<choice> auto | census | geocodio | csv (default: auto)
                  --save-html         Save each listing's HTML block to output/Html/ while scraping
                  --no-cache          Bypass TravelTime/WalkScore cache (still writes fresh entries)
                  --help, -h          Show this help

                Examples:
                  .\\srs.ps1 status
                  .\\srs.ps1 scrape-clean --max-id=25
                  .\\srs.ps1 full --max-id=25 --geocoder=census
                  .\\srs.ps1 help geocode

                Run with no arguments for an interactive menu.
                Full reference: docs/CLI.md
                """;
    }

    public static String forCommand(String command) {
        if (command == null || command.isBlank()) {
            return overview();
        }
        PipelineOptions.Mode mode;
        try {
            mode = PipelineOptions.Mode.fromCli(command);
        } catch (IllegalArgumentException e) {
            return "Unknown command: " + command + "\n\n" + overview();
        }
        return switch (mode) {
            case FULL_PIPELINE -> detail(
                    "full",
                    "Runs the entire pipeline: scrape -> post-process -> clean -> geocode -> TravelTime -> WalkScore -> final JSON/CSV, plus a separate crime report.",
                    "None (starts fresh scrape).",
                    "output/Raw, Cleaned, Geocoded, API, Final.",
                    "None required. Census geocoder needs no key. Optional: Geocodio, TravelTime, WalkScore, CrimeData.",
                    "Depends on ID range (~1s per ID with default delay) plus API time.",
                    ".\\srs.ps1 full --max-id=25");
            case SCRAPE_AND_CLEAN_ONLY -> detail(
                    "scrape-clean",
                    "Scrapes listing IDs, saves raw JSON, then post-processes and cleans.",
                    "None.",
                    "output/Raw/listings_scraped_*.json and output/Cleaned/listings_cleaned_*.{json,csv}.",
                    "None.",
                    "~1 second per listing ID at the default 1000ms delay.",
                    ".\\srs.ps1 scrape-clean --min-id=0 --max-id=25 --delay-ms=200\n  .\\srs.ps1 scrape-clean --save-html");
            case CLEAN_ONLY -> detail(
                    "clean",
                    "Loads the latest raw scrape, applies title fallbacks and cleaning, and writes cleaned outputs.",
                    "Latest output/Raw/listings_scraped_*.json.",
                    "output/Cleaned/listings_cleaned_*.{json,csv}.",
                    "None.",
                    "Seconds.",
                    ".\\srs.ps1 clean");
            case GEOCODE_ONLY -> detail(
                    "geocode",
                    "Geocodes the latest cleaned listings and computes distance to campus.",
                    "Latest output/Cleaned/listings_cleaned_*.json.",
                    "output/Geocoded/listings_geocoded_*.json.",
                    "None for Census (default). Geocodio key optional. --geocoder=csv needs a manual CSV.",
                    "Usually under a minute for ~450 addresses via Census.",
                    ".\\srs.ps1 geocode\n  .\\srs.ps1 geocode --geocoder=census");
            case APIS_ONLY -> detail(
                    "apis",
                    "Adds walk/bike travel times and Walk Scores; writes a separate FBI crime report when configured. TravelTime and WalkScore results are cached under output/Cache/ (keyed by listing id + coordinates); use --no-cache to force fresh API calls.",
                    "Latest output/Geocoded/listings_geocoded_*.json.",
                    "output/API/*, output/Cache/*, and output/Final/listings_final_*.{json,csv}.",
                    "Optional: TravelTime (api.key + api.id), WalkScore (api.key), CrimeData (api.key + api.ori).",
                    "Depends on listing count, cache hits, and WalkScore rate limiting.",
                    ".\\srs.ps1 apis\n  .\\srs.ps1 apis --no-cache");
            case RESUME_FROM_CLEANED -> detail(
                    "resume",
                    "Geocode then enrich starting from the latest cleaned JSON (skip re-scraping).",
                    "Latest output/Cleaned/listings_cleaned_*.json.",
                    "output/Geocoded, API, Final.",
                    "Same as geocode + apis.",
                    "Geocode + API time.",
                    ".\\srs.ps1 resume");
            case STATUS -> detail(
                    "status",
                    "Shows the newest file in each output stage, which API keys are configured, and a suggested next command.",
                    "output/ directories (read-only).",
                    "Console summary only.",
                    "None.",
                    "Instant.",
                    ".\\srs.ps1 status");
            case AUDIT -> detail(
                    "audit",
                    "Compares bedroom and bathroom counts from the listing title, the site's Bed/Bath field, and the description text. Read-only: it does not change any listing data.",
                    "Latest output/Raw/listings_scraped_*.json (needs a scrape made with rawDescription).",
                    "output/Audit/bedbath_audit_*.csv and a console summary.",
                    "None.",
                    "Seconds.",
                    ".\\srs.ps1 audit");
            case SCORE -> detail(
                    "score",
                    "Recomputes the 0-100 deal score for the latest Final listings using the thresholds in pipeline.properties. No API calls, so tuning takes seconds.",
                    "Latest output/Final/listings_final_*.json.",
                    "A new output/Final/listings_final_<timestamp>.{json,csv} and a console summary (distribution and top 10).",
                    "Thresholds: scoring.* keys in src/main/resources/pipeline.properties.",
                    "Seconds.",
                    ".\\srs.ps1 score");
            case HELP -> overview();
        };
    }

    private static String detail(
            String name,
            String what,
            String reads,
            String writes,
            String keys,
            String runtime,
            String example) {
        return """
                Command: %s

                What it does:
                  %s

                Reads:
                  %s

                Writes:
                  %s

                Keys / config:
                  %s

                Typical runtime:
                  %s

                Example:
                  %s
                """.formatted(name, what, reads, writes, keys, runtime, example);
    }
}
