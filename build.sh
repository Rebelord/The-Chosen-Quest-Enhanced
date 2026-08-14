#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD="$ROOT/build"
JAR="$BUILD/TheChosenQuest-Desktop.jar"
TEMP_JAR="$BUILD/.TheChosenQuest-Desktop.jar.$$"

# Never rewrite the JAR in place. Java 8 memory-maps ZIP/JAR metadata, and a
# running development build can crash in libzip with SIGBUS if its JAR is
# truncated while a later scene, sound, or class is being loaded. Building a
# complete sibling file and renaming it keeps the running process attached to
# the intact old inode while the next launch receives the new build.
trap 'rm -f "$TEMP_JAR"' EXIT HUP INT TERM

rm -rf "$BUILD/classes"
mkdir -p "$BUILD/classes"
cd "$ROOT"
find src -name '*.java' -print > "$BUILD/sources.txt"
javac -Xlint:all -encoding UTF-8 -source 8 -target 8 -d "$BUILD/classes" @"$BUILD/sources.txt"
mkdir -p "$BUILD/classes/assets"
cp -R "$ROOT/assets/." "$BUILD/classes/assets/"
jar cfe "$TEMP_JAR" thechosenquest.desktop.App -C "$BUILD/classes" .
mv -f "$TEMP_JAR" "$JAR"
trap - EXIT HUP INT TERM

echo "Built $JAR"
