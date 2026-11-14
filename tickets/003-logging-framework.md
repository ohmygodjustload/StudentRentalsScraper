---
id: 003
title: Logging framework (SLF4J)
area: infra
priority: medium
status: open
---

# Logging framework (SLF4J)

## Context

The pipeline uses `System.out` / `System.err` throughout. A real logger would support levels and quieter CI runs.

## Acceptance criteria

- Add SLF4J + a simple binding (e.g. slf4j-simple or logback).
- Replace `System.out`/`System.err` in orchestrator, scraper, and API classes.
- Default config logs INFO to console; DEBUG can be enabled via property.

## Notes

- Touch points: `Orchestrator`, `StudentRentalsScraper`, API clients, `DataMerger`.
