---
id: 021
title: CLI progress bar and time estimates
area: cli
priority: low
status: open
---

# CLI progress bar and time estimates

## Context

Long stages print one line per item (the scrape logs a line for each of roughly 900 IDs, WalkScore logs nothing between start and finish), so it is hard to tell how far along a run is or how long it will take. A full scrape takes about 25 minutes and the API stage about 5 more. This is a quality-of-life improvement, not a correctness issue.

## Acceptance criteria

- A small `ProgressReporter` helper (in `main` or `utils`) that shows `done/total`, a percentage, elapsed time, and an estimated time remaining.
- Used by the three slow stages: the scrape (IDs min to max), WalkScore enrichment (one call per listing), and Census geocoding if the batch is split.
- On an interactive terminal, the bar redraws on one line using a carriage return. When output is piped or redirected, or stdout is not a console, it falls back to plain periodic lines (for example every 10%) so logs stay readable.
- The scrape keeps its existing `[VALID]` and `[ERROR]` lines for listings that matter. Routine `[INVALID LISTING]` lines are collapsed into the bar, with a count shown in the final summary.
- Errors and warnings still print on their own lines without breaking the bar.
- A final summary per stage: items processed, valid, skipped, errors, and elapsed time.
- Optional `--quiet` flag to suppress progress output, and `--verbose` to restore per-item lines.
- Unit tests for the formatting, the ETA calculation, and the non-interactive fallback (inject an output stream and a clock).

## Notes

- Keep it dependency-free if possible: plain `\r` redraws work in PowerShell and most terminals. A library such as JLine or picocli's ANSI support is only worth adding if it also helps another ticket.
- Related: ticket 003 (logging framework). Per-item detail belongs in the log, and the bar replaces it on screen, so the two should be designed together if both get done.
- The launcher scripts (`srs.ps1`, `srs.sh`) pass output straight through, so they need no changes.
