#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD="$ROOT/build"

rm -rf "$BUILD/classes"
mkdir -p "$BUILD/classes"
cd "$ROOT"
find src -name '*.java' -print > "$BUILD/sources.txt"
javac -Xlint:all -encoding UTF-8 -source 8 -target 8 -d "$BUILD/classes" @"$BUILD/sources.txt"
mkdir -p "$BUILD/classes/assets"
cp -R "$ROOT/assets/." "$BUILD/classes/assets/"
jar cfe "$BUILD/TheChosenQuest-Desktop.jar" thechosenquest.desktop.App -C "$BUILD/classes" .

echo "Built $BUILD/TheChosenQuest-Desktop.jar"
