---
id: 007
title: Raw-vs-cleaned diff tool
area: tooling
priority: low
status: open
---

# Raw-vs-cleaned diff tool

## Context

Debugging normalization is hard when comparing large JSON dumps by eye. CI already has a small fixture test; a CLI diff would help on real runs.

## Acceptance criteria

- CLI or Maven goal that loads a raw and cleaned JSON pair and prints per-field diffs for address, landlord, bed/bath, price.
- Useful summary counts (unchanged / changed / newly flagged).

## Notes

- Related test: `RawVsCleanedFixtureTest`.
