#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$ROOT/build.sh"
rm -rf "$ROOT/dist"
mkdir -p "$ROOT/dist"

jpackage \
  --type app-image \
  --name "The Chosen Quest Enhanced" \
  --input "$ROOT/build" \
  --main-jar "TheChosenQuest-Desktop.jar" \
  --main-class thechosenquest.desktop.App \
  --dest "$ROOT/dist"

echo "Packaged application in $ROOT/dist"
