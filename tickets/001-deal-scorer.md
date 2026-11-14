---
id: 001
title: DealScorer class
area: scoring
priority: high
status: open
---

# DealScorer class

## Context

`dealScore` was removed from `Apartment` during the overhaul. Scoring should live in a dedicated class, not in the scraper or model.

## Acceptance criteria

- New `scoring/DealScorer` (or similar) computes a 0–100 score from price-per-bed, distance to campus, walk/bike travel time, missing-data penalties, and amenities.
- Score is written onto apartments after enrichment (or as an optional pipeline stage).
- Unit tests cover price-per-person brackets and edge cases (missing beds, missing distance).

## Notes

- Former prototype lived in deleted `DealScoreTest` (price / (beds + 1) adjustments).
- Related fields: `rentMonthly`, `bedrooms`, `distanceToCampus`, travel times, `flags`.
