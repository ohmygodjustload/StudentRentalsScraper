---
id: 002
title: TravelTime/WalkScore response caching
area: api
priority: medium
status: open
---

# TravelTime/WalkScore response caching

## Context

Repeated full-pipeline runs re-query TravelTime and WalkScore for the same coordinates, burning quota.

## Acceptance criteria

- File-based cache keyed by listing id + lat/lng (+ mode for TravelTime).
- Cache hits skip HTTP; misses write results under `output/cache/` (or similar).
- Cache can be bypassed with a CLI flag (e.g. `--no-cache`).

## Notes

- `TravelTimeAPI` and `WalkScoreAPI` already take `HttpTransport`; a caching decorator is a natural fit.
