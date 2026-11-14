---
id: 019
title: Integration testing
area: testing
priority: medium
status: open
---

# Integration testing

## Context

Current tests are unit tests with a fake HTTP transport and fixture files. They do not exercise the whole pipeline, and bugs like stale zeros in old output files (fixed in the API output overhaul) only showed up on a live run. Integration tests would catch that class of problem automatically.

## Acceptance criteria

- A pipeline integration test that runs clean, geocode, enrich, and final output end to end using `FakeHttpTransport` and fixture data, with no network access, and asserts on the final JSON and CSV (field values, nulls instead of zeros, flags, `pricePerPerson`).
- A test that loads an older output file (including legacy fields like `dealScore` and `geocodioAddress`) and confirms stages clear stale values.
- A scraper test against saved HTML fixtures served by a local stub server, covering valid, missing, and malformed listing pages.
- Optional live smoke tests tagged so they never run in normal builds, for example JUnit `@Tag("live")` plus a Maven profile. They hit the real Census, TravelTime, WalkScore, and FBI endpoints with a tiny sample and skip when keys are missing.
- Maven wiring: unit tests in `mvn test`, integration tests in `mvn verify` (Failsafe plugin), live tests only via an explicit profile.

## Notes

- Keep fixtures small and committed under `src/test/resources/fixtures/`.
- Never commit real API keys; live tests read from `config/` or environment variables.
