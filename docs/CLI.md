# CLI reference

## Quick start

```powershell
# Windows
.\srs.ps1                  # interactive menu
.\srs.ps1 help
.\srs.ps1 status
.\srs.ps1 scrape-clean --max-id=25
.\srs.ps1 geocode
.\srs.ps1 apis
.\srs.ps1 resume
```

```bash
# macOS / Linux
./srs.sh
./srs.sh help
./srs.sh status
```

The launcher rebuilds `target/scraper-all.jar` only when `src/` or `pom.xml` changed, then runs:

```text
java -jar target/scraper-all.jar <args>
```

You can still use Maven directly:

```powershell
mvn exec:java "-Dexec.args=status"
mvn exec:java "-Dexec.args=help geocode"
```

## Commands

| Command | Reads | Writes | Notes |
|---------|-------|--------|-------|
| `full` | Live site | Raw, Cleaned, Geocoded, API, Final | Full pipeline |
| `scrape-clean` | Live site | Raw + Cleaned | Default scrape path |
| `clean` | Latest raw JSON | Cleaned | Re-run cleaner after map edits |
| `geocode` | Latest cleaned JSON | Geocoded | Census by default |
| `apis` | Latest geocoded JSON | API + Final | TravelTime (30 min cap), WalkScore, crime report |
| `resume` | Latest cleaned JSON | Geocoded + API + Final | Skip re-scrape |
| `status` | `output/` | Console only | Latest files + keys + next step |
| `audit` | Latest raw JSON | Audit CSV | Compare bed/bath from title, field, and description (read-only) |
| `score` | Latest Final JSON | New Final JSON + CSV | Recompute deal scores after tuning thresholds (no API calls) |
| `help [cmd]` | — | Console only | Overview or per-command help |

## Options

| Flag | Default | Meaning |
|------|---------|---------|
| `--min-id=<n>` | `0` | First listing ID to scrape |
| `--max-id=<n>` | `900` | Last listing ID to scrape |
| `--delay-ms=<n>` | `1000` | Delay between scrape requests |
| `--output-dir=<path>` | `output` | Output root directory |
| `--geocoder=<choice>` | `auto` | `auto`, `census`, `geocodio`, or `csv` |
| `--save-html` | off | Save each listing's HTML block to `output/Html/scrape_<timestamp>/listing_<id>.html` |
| `--mode=<mode>` | — | Legacy alias for the positional command |
| `--help`, `-h` | — | Show overview help |

## Geocoder selection

| Choice | Behavior |
|--------|----------|
| `auto` (default) | Use Geocodio if `config/Geocodio.properties` has a key; otherwise US Census (no key) |
| `census` | Always use the free US Census Bureau batch geocoder |
| `geocodio` | Require a Geocodio API key |
| `csv` | Pause and merge `output/Geocoded/geocodio.csv` |

Census needs no signup. Geocodio is optional.

## API keys (optional)

Create files under `config/` (gitignored). Environment variables override file values.

| File | Keys | Env vars |
|------|------|----------|
| `TravelTime.properties` | `api.key`, `api.id` | `TRAVELTIME_API_KEY`, `TRAVELTIME_API_ID` |
| `WalkScore.properties` | `api.key` | `WALKSCORE_API_KEY` |
| `Geocodio.properties` | `api.key` | `GEOCODIO_API_KEY` |
| `CrimeData.properties` | `api.key`, `api.ori` | `CRIMEDATA_API_KEY`, `CRIMEDATA_API_ORI` |

Missing keys skip that stage with a warning. Census geocoding always works without keys.

TravelTime reports walk/bike times only when they are within `traveltime.maxSeconds` (default 1800 = 30 minutes). Listings farther away keep those fields null.

## Typical workflows

### First run (small smoke test)

```powershell
.\srs.ps1 scrape-clean --max-id=25 --delay-ms=200
.\srs.ps1 status
.\srs.ps1 resume
```

### Full scrape for the season

```powershell
.\srs.ps1 full
```

At the default 1000ms delay, IDs 0–900 take on the order of 15 minutes for scraping alone.

### Re-clean after editing landlord/address maps

```powershell
.\srs.ps1 clean
.\srs.ps1 resume
```

### Data quality audit (bed/bath)

The site sometimes disagrees with itself: a listing's title can say `5br/1ba` while its Bed/Bath field says `2/1`. The scraper stores the title (`rawTitle`) and the description text (`rawDescription`) in the raw and cleaned JSON so they can be compared.

```powershell
.\srs.ps1 scrape-clean --save-html   # fresh scrape, keeps listing HTML for offline re-parsing
.\srs.ps1 audit                      # writes output/Audit/bedbath_audit_<timestamp>.csv
```

For bedrooms and bathrooms, the audit CSV shows each source's value plus two categories:

- **Title vs field:** `agree`, `conflict`, `title_missing`, `field_missing`, or `both_missing`.
- **Description:** `matches_both`, `matches_title`, `matches_field`, `matches_neither`, `matches_only_source`, `ambiguous` (several different numbers), or `missing`.

The audit also compares the landlord and street address:

- **Landlord:** the title text before the first separator against the site's Landlord field. Case, punctuation, and `LLC`/`Inc` are ignored, and the names agree when one contains the other.
- **Address:** the title's street segment against the breadcrumb address, ignoring city and state. `N`/`North`, `St`/`Street`, and similar abbreviations are treated as equal.

Both use the categories `agree`, `conflict`, `title_missing`, or `field_missing`. The console summary lists conflicting listing IDs for each check.

The audit only reads data. It never changes listings.

### Bed/bath flags and overrides

During the cleaning step (`clean`, `scrape-clean`, and `full`), each listing's title and Bed/Bath field are compared. Three flags record the outcome:

| Flag | Meaning |
|------|---------|
| `bedbath_conflict` | Title and site field disagree on beds or baths. The site field is kept unless an override applies. |
| `bedbath_overridden` | A hand-verified override replaced the site field. The conflict flag stays so the disagreement remains visible. |
| `bedbath_override_stale` | An override exists, but the site field no longer matches the value it was verified against. The override was skipped and a warning was printed. |

Hand-verified corrections live in `data/bedbath_overrides.csv`:

```csv
id,expectedField,beds,baths,note
424,2/1,5,1,"Title says 5br and description says 'five bedrooms'"
868,3/1,Studio,1,"Description lists only studio floorplans"
```

- `expectedField` is the raw site field the override was checked against. The override applies only when the current raw field matches it (spaces and case are ignored).
- `beds` is a number or `Studio`. `baths` is a number.
- Blank lines and lines starting with `#` are ignored. A malformed row stops the run with the file name and line number.
- If the file is missing, there are no overrides.

Only add a row after checking the listing page by hand. Descriptions and titles are evidence, not automatic truth.

### Deal score

Every listing gets a 0-100 `dealScore` at the end of the enrich stage (`full`, `resume`, `apis`), and `dealScoreDetail` explains the parts, for example `price 50/50, location 22/30, amenities 12/20, penalty -3`. Scores are computed from the listing data only.

| Component | Points | How it scores |
|-----------|--------|---------------|
| Price per person | 50 | Full at $450 or less, zero at $750 or more |
| Walk time | 15 | Full at 8 min or less, zero at 25 min or more |
| Bike time | 5 | Full at 3 min or less, zero at 10 min or more |
| Distance to campus | 10 | Full at 0.35 mi or less, zero at 1.5 mi or more |
| Air conditioning | 5 | Central, wall/window unit, or yes |
| Laundry | 5 | In-unit = 5; on-site, coin, or free = 3; hook-ups only = 0 |
| Parking | 4 | Off-street, garage, or on-site = 4; on-street = 1 |
| Dishwasher | 3 | Listed under Included |
| Utilities | 3 | Heat or electric included = 3; water only = 1 |
| Penalty | up to -15 | Missing rent or bedrooms -8, no coordinates -4, unresolved bed/bath conflict -3 |

A listing beyond the TravelTime cap gets 0 travel points but no penalty. A listing with coordinates and no travel time is treated that way, too.

Thresholds are the `scoring.*` keys in `src/main/resources/pipeline.properties`. Change them there, then run `score` to rescore the latest Final output without calling any API:

```powershell
.\srs.ps1 score
```

The command writes a new `output/Final/listings_final_<timestamp>.{json,csv}` and prints the score distribution and the top 10 listings. The points for each component are fixed in code. Changing them means editing `scoring/DealScorer.java`.

### Resume after an API failure

```powershell
.\srs.ps1 status          # see which stage you have
.\srs.ps1 apis            # if geocoded exists
.\srs.ps1 resume          # if only cleaned exists
```

## Interactive menu

Running `.\srs.ps1` with no arguments opens a numbered menu (when a console is attached). For scrape commands it asks for min/max ID (Enter keeps the defaults) and prints the equivalent one-line command before running it.

In non-interactive environments (CI, piped input) it prints help instead.

## Troubleshooting

| Symptom                        | Fix                                                                                   |
| ------------------------------ | ------------------------------------------------------------------------------------- |
| Accidental long scrape         | Use `--max-id=25` or the menu; default range is 0–900                                 |
| No geocoded file               | Run `.\srs.ps1 geocode` or `.\srs.ps1 resume`                                         |
| TravelTime / WalkScore skipped | Add keys under `config/` or set the env vars; check `.\srs.ps1 status`                |
| Want manual Geocodio CSV       | `.\srs.ps1 geocode --geocoder=csv` and place `geocodio.csv` in `out put/Geocoded/`    |
| Jar feels stale                | Delete `target\` and run `.\srs.ps1` again; the launcher rebuilds when sources change |

## Background

Why the scraper indexes unlisted pages: [BACKGROUND.md](BACKGROUND.md).
