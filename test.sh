#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$ROOT/build.sh"
cd "$ROOT"
rm -rf "$ROOT/build/test-classes"
mkdir -p "$ROOT/build/test-classes"
find test -name '*.java' -print > "$ROOT/build/test-sources.txt"
javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$ROOT/build/classes" \
  -d "$ROOT/build/test-classes" \
  @"$ROOT/build/test-sources.txt"
java -cp "$ROOT/build/classes:$ROOT/build/test-classes" \
  thechosenquest.desktop.GameEngineSmokeTest
java -cp "$ROOT/build/classes:$ROOT/build/test-classes" \
  thechosenquest.desktop.GameEngineRegressionTest
java -cp "$ROOT/build/classes:$ROOT/build/test-classes" \
  thechosenquest.desktop.UpdateServiceRegressionTest
java -cp "$ROOT/build/classes:$ROOT/build/test-classes" \
  thechosenquest.desktop.BalanceSimulationTest "$ROOT/build/reports/balance-report.md"
java -cp "$ROOT/build/classes:$ROOT/build/test-classes" \
  thechosenquest.desktop.ExplorationSimulationTest "$ROOT/build/reports/exploration-report.md"
java -Djava.awt.headless=true -cp "$ROOT/build/classes:$ROOT/build/test-classes" \
  thechosenquest.desktop.PerformanceSmokeTest "$ROOT/build/reports/performance-report.md"
java -Djava.awt.headless=true -cp "$ROOT/build/classes:$ROOT/build/test-classes" \
  thechosenquest.desktop.EnhancedUiSmokeTest "$ROOT/build/character-creation-preview.png"
