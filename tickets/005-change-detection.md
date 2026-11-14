---
id: 005
title: Change detection between scrapes
area: storage
priority: medium
status: open
---

# Change detection between scrapes

## Context

Original roadmap called for detecting price/availability changes across scrapes.

## Acceptance criteria

- Compare latest scrape to previous stored snapshot (DB or prior JSON).
- Flag listings with price or availability changes.
- Emit a small change report (JSON or console summary).

## Notes

- Best built on top of ticket 004 persistence.
