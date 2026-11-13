#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$PROJECT_ROOT/scripts/java-env.sh"
MAVEN_ARGS=()
if [[ -n "${PAGEWATCH_MAVEN_REPO:-}" ]]; then MAVEN_ARGS+=(-Dmaven.repo.local="$PAGEWATCH_MAVEN_REPO"); fi
if [[ -n "${PAGEWATCH_MAVEN_SETTINGS:-}" ]]; then MAVEN_ARGS+=(-s "$PAGEWATCH_MAVEN_SETTINGS"); fi
cd "$PROJECT_ROOT"
mvn "${MAVEN_ARGS[@]}" -pl backend-boot -am install -DskipTests && exec mvn "${MAVEN_ARGS[@]}" -f backend-boot/pom.xml spring-boot:run
