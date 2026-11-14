#!/usr/bin/env bash
# StudentRentalsScraper launcher (macOS/Linux)
# Usage: ./srs.sh [command] [options]
#        ./srs.sh                 # interactive menu

set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

JAR="$ROOT/target/scraper-all.jar"

needs_rebuild() {
  if [[ ! -f "$JAR" ]]; then
    return 0
  fi
  if [[ "$ROOT/pom.xml" -nt "$JAR" ]]; then
    return 0
  fi
  if find "$ROOT/src" -type f -newer "$JAR" | grep -q .; then
    return 0
  fi
  return 1
}

if needs_rebuild; then
  echo "[srs] Building jar..."
  mvn -q -DskipTests package
fi

exec java -jar "$JAR" "$@"
