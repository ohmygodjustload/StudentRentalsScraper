---
id: 009
title: Externalize landlord/address maps
area: clean
priority: medium
status: open
---

# Externalize landlord/address maps

## Context

`LANDLORD_MAP` and `ADDRESS_CORRECTIONS` are hardcoded in `DataCleaner`. Expanding them requires code changes.

## Acceptance criteria

- Move maps to resource files (CSV or properties) under `src/main/resources/`.
- Load at startup; fail clearly if a file is missing/malformed.
- Document how to add new landlord aliases and address fixes.

## Notes

- See `clean/DataCleaner.java` static blocks.
