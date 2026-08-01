#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$ROOT/build.sh"
rm -rf "$ROOT/dist"
mkdir -p "$ROOT/dist"

ICON_ARGS=""
case "$(uname -s)" in
  Linux) ICON_ARGS="--icon $ROOT/assets/app-icon.png" ;;
esac

# macOS uses the bundled runtime/window icon until a signed ICNS installer pass;
# Linux app images can consume the canonical PNG directly.
# shellcheck disable=SC2086
jpackage \
  --type app-image \
  --name "The Chosen Quest Enhanced" \
  --input "$ROOT/build" \
  --main-jar "TheChosenQuest-Desktop.jar" \
  --main-class thechosenquest.desktop.App \
  $ICON_ARGS \
  --dest "$ROOT/dist"

echo "Packaged application in $ROOT/dist"
