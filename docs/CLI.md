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
| `apis` | Latest geocoded JSON | API + Final | TravelTime, WalkScore, crime report |
| `resume` | Latest cleaned JSON | Geocoded + API + Final | Skip re-scrape |
| `status` | `output/` | Console only | Latest files + keys + next step |
| `help [cmd]` | — | Console only | Overview or per-command help |

## Options

| Flag | Default | Meaning |
|------|---------|---------|
| `--min-id=<n>` | `0` | First listing ID to scrape |
| `--max-id=<n>` | `900` | Last listing ID to scrape |
| `--delay-ms=<n>` | `1000` | Delay between scrape requests |
| `--output-dir=<path>` | `output` | Output root directory |
| `--geocoder=<choice>` | `auto` | `auto`, `census`, `geocodio`, or `csv` |
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

| Symptom | Fix |
|---------|-----|
| Accidental long scrape | Use `--max-id=25` or the menu; default range is 0–900 |
| No geocoded file | Run `.\srs.ps1 geocode` or `.\srs.ps1 resume` |
| TravelTime / WalkScore skipped | Add keys under `config/` or set the env vars; check `.\srs.ps1 status` |
| Want manual Geocodio CSV | `.\srs.ps1 geocode --geocoder=csv` and place `geocodio.csv` in `output/Geocoded/` |
| Jar feels stale | Delete `target\` and run `.\srs.ps1` again; the launcher rebuilds when sources change |

## Background

Why the scraper indexes unlisted pages: [BACKGROUND.md](BACKGROUND.md).
