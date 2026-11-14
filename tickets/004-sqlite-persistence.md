---
id: 004
title: SQLite/H2 persistence
area: storage
priority: medium
status: open
---

# SQLite/H2 persistence

## Context

Outputs are timestamped JSON/CSV files only. Persisting listings would enable history and change detection.

## Acceptance criteria

- Optional SQLite (or H2) store for apartments after clean/enrich stages.
- Schema covers id, url, price, beds/baths, coords, and scrape timestamp.
- CLI flag to enable persistence without breaking file outputs.

## Notes

- Depends on stable numeric fields already on `Apartment` (`rentMonthly`, `bedrooms`, etc.).
