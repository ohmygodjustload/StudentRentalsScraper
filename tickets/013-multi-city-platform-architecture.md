---
id: 013
title: Multi-city platform architecture
area: marketplace
priority: low
status: open
---

# Multi-city platform architecture

## Context

The app should have one codebase and data model, with each city or domain getting its own branding and defaults. Part of epic 011.

## Acceptance criteria

- Architecture doc covering:
  - tech stack (backend, frontend, database, hosting) and why;
  - tenancy model: shared database with a `market` column, or separate schemas per market;
  - per-market configuration such as name, domain or subdomain, theme, default map center, anchor points (campus, downtown, transit hubs), and lease-cycle dates;
  - roles: renter, landlord, admin.
- Core data model: market, user, landlord profile, property, unit, listing, availability window, inquiry, saved search.
- A running skeleton app with two seeded markets (La Crosse plus one test market), served on separate subdomains or paths.
- Basic CI: build, tests, deploy to a staging environment.

## Notes

- Reuse ideas from this repo: geocoding (Census), travel time, and walk score enrichment can become background jobs that run when a listing is published.
- Store coordinates and enrichment results per unit so they are computed once, not per page view (see ticket 002 for the caching idea).
