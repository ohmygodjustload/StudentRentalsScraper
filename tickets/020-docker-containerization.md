---
id: 020
title: Docker containerization
area: infra
priority: low
status: open
---

# Docker containerization

## Context

The CLI runs locally through `srs.ps1` / `srs.sh` and a shaded jar. Containerizing it is worthwhile only if the project moves to scheduled or hosted runs (for example periodic scrapes, change detection in ticket 005, or the marketplace in epic 011). It is not needed for personal local use today.

## Acceptance criteria

- Multi-stage `Dockerfile`: build the shaded jar with Maven, run on a slim Java 21 JRE image as a non-root user.
- The container runs the same commands as the launcher (`docker run ... status`, `resume`, and so on).
- `output/` is a mounted volume so results persist; `config/` keys are supplied through environment variables (already supported) or a read-only mount, and are never baked into the image.
- `.dockerignore` excludes `target/`, `output/`, `config/`, and `.git`.
- Optional `docker-compose.yml` for scheduled runs, and for a database once ticket 004 lands.
- Short docs section in `docs/CLI.md` with the build and run commands.

## Notes

- Depends on ticket 019 for a reliable automated check that the containerized build works.
- A CI job that builds the image and runs the test suite would be a natural follow-up.
- Revisit this ticket when scheduling or hosting becomes real; until then keep it as `low` priority.
