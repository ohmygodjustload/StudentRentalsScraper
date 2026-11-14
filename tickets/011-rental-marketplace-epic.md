---
id: 011
title: "Epic: independent-landlord rental marketplace"
area: marketplace
priority: low
status: open
---

# Epic: independent-landlord rental marketplace

## Context

Large listing sites skew toward commercial property managers. Independent landlords who rent a few units as a side gig, and renters looking for affordable units, are underserved. The scraper showed that many units in a college town never appear publicly because listing costs money and timing matters (renewal decisions happen months before leases end).

Proposal: a two-sided web app with a landlord interface and a renter interface, built on a generic core that can be branded per city or domain (college towns first, any city later).

## Goals

- Make it cheap and simple for independent landlords to list, including units that will be available in the future (renewal windows).
- Help renters find affordable, decent-quality units and contact landlords early.
- Launch in one city, prove demand, then add cities without rewriting the app.

## Child tickets

| ID | Title |
|----|-------|
| 012 | Market validation and differentiation |
| 013 | Multi-city platform architecture |
| 014 | Landlord portal MVP |
| 015 | Renter portal MVP |
| 016 | Trust, safety, and legal compliance |
| 017 | Monetization and La Crosse pilot |

## Acceptance criteria

- Child tickets 012–017 are done, or explicitly cancelled with reasons.
- A pilot city has live listings from real independent landlords and real renter inquiries.

## Notes

- The biggest risk is the two-sided cold start: renters won't come without listings, and landlords won't list without renters. Ticket 017 owns the plan for seeding supply.
- The existing scraper stays a separate personal tool. Do not republish scraped or hidden listings in the marketplace; listings must come from landlords who opt in.
