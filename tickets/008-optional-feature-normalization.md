---
id: 008
title: Optional feature normalization
area: clean
priority: low
status: open
---

# Optional feature normalization

## Context

Cleaner currently normalizes address, landlord, bed/bath, and rent. Pets, laundry, parking, and bed/bath label variants were listed as optional follow-ups.

## Acceptance criteria

- Normalize pets (e.g. "No Pets" → "No"), laundry types, parking types.
- Accept additional bed/bath synonyms (`bdrm`, `bedroom`) if any remain unparsed.
- Add flags only when a value was rewritten.

## Notes

- Extend `DataCleaner`; keep scraper raw-only.
