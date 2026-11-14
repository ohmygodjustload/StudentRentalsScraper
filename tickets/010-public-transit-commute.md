---
id: 010
title: Public transit commute times
area: api
priority: low
status: open
---

# Public transit commute times

## Context

TravelTime already supports walking and cycling. Public transit commute times to campus were on the original wishlist.

## Acceptance criteria

- Add a transit mode request (TravelTime `public_transport` or a local GTFS-based option).
- Store transit travel time fields on `Apartment`.
- Skip listings without coordinates; document API plan limits.

## Notes

- Build on finished `TravelTimeAPI` patterns (ID-keyed locations, config-driven campus coords).
