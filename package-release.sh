#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION=$(sed -n 's/.*VERSION = "\([^"]*\)";.*/\1/p' \
  "$ROOT/src/thechosenquest/desktop/AppVersion.java")
NAME="The-Chosen-Quest-Enhanced-${VERSION}"
STAGE="$ROOT/build/release/$NAME"
ARCHIVE="$ROOT/dist/$NAME.zip"

if [ -z "$VERSION" ]; then
  echo "Unable to read AppVersion.VERSION" >&2
  exit 1
fi

"$ROOT/test.sh"
rm -rf "$ROOT/build/release"
mkdir -p "$STAGE" "$ROOT/dist"
cp "$ROOT/build/TheChosenQuest-Desktop.jar" "$STAGE/TheChosenQuest-Enhanced.jar"
cp "$ROOT/release/README.txt" "$STAGE/README.txt"
cp "$ROOT/output/pdf/The-Chosen-Quest-Enhanced-Tester-Guide.pdf" "$STAGE/TESTER-GUIDE.pdf"
cp "$ROOT/output/private-tester-kit/docs/The-Chosen-Quest-Private-Tester-Quick-Reference.pdf" \
  "$STAGE/QUICK-REFERENCE.pdf"
cp "$ROOT/output/private-tester-kit/docs/Private-Tester-Checklist.txt" \
  "$STAGE/TESTER-CHECKLIST.txt"
cp "$ROOT/release/RELEASE-NOTES-${VERSION}.md" "$STAGE/RELEASE-NOTES.md"
cp "$ROOT/CHANGELOG.md" "$STAGE/CHANGELOG.md"
cp "$ROOT/assets/credits/ATTRIBUTION.md" "$STAGE/CREDITS-AND-LICENSES.md"
cp "$ROOT/assets/audio/ATTRIBUTION.md" "$STAGE/AUDIO-ATTRIBUTION.md"
cp "$ROOT/release/Launch The Chosen Quest.command" "$STAGE/"
cp "$ROOT/release/Launch The Chosen Quest.sh" "$STAGE/"
cp "$ROOT/release/Launch The Chosen Quest.bat" "$STAGE/"
chmod +x "$STAGE/Launch The Chosen Quest.command" "$STAGE/Launch The Chosen Quest.sh"

rm -f "$ARCHIVE" "$ARCHIVE.sha256"
(
  cd "$ROOT/build/release"
  zip -qr "$ARCHIVE" "$NAME"
)
(
  cd "$ROOT/dist"
  shasum -a 256 "$NAME.zip" > "$NAME.zip.sha256"
)

echo "Packaged $ARCHIVE"
echo "Checksum: $ARCHIVE.sha256"
