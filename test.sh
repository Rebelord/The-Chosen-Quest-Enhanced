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
java -Djava.awt.headless=true -cp "$ROOT/build/classes:$ROOT/build/test-classes" \
  thechosenquest.desktop.EnhancedUiSmokeTest "$ROOT/build/character-creation-preview.png"
