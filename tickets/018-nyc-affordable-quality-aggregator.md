---
id: 018
title: NYC affordable-but-quality apartment finder
area: nyc
priority: low
status: open
---

# NYC affordable-but-quality apartment finder

## Context

A separate personal tool for the planned move to NYC. Cheap listings on large sites are often cheap for a reason: poor building condition, safety, or long commutes. The goal is a shortlist of low-rent units that also score well on objective quality data.

This is independent of the marketplace epic (011), but could later become a feature of it.

## Acceptance criteria

- Ingest listings only from sources that allow it (official APIs, feeds, or sites whose terms permit collection), or from manual entry.
- Enrich each address with NYC Open Data:
  - HPD housing maintenance code violations and complaints for the building;
  - DOB violations;
  - 311 service requests near the address (noise, heat and hot water, pests);
  - NYPD complaint data within a radius, which is address-level unlike the agency-wide FBI data used here.
- Commute time to a chosen work location using transit (see ticket 010).
- A quality score combining building condition, nearby reported crime, and commute, shown next to rent. Results rank by value (quality per dollar).
- Output as CSV or JSON plus a simple map view.

## Notes

- Reuse this repo's pipeline pattern (ingest, clean, geocode, enrich, score) and API client style.
- Score on building and incident data only, not neighborhood demographics.
- NYC Open Data is at data.cityofnewyork.us and many datasets have a free API (Socrata); check rate limits and app-token requirements.
