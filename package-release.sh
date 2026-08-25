#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PYTHON=${RELEASE_PYTHON:-python3}
VERSION=$(sed -n 's/.*VERSION = "\([^"]*\)";.*/\1/p' \
  "$ROOT/src/thechosenquest/desktop/AppVersion.java")
NAME="The-Chosen-Quest-Enhanced-${VERSION}"
STAGE="$ROOT/build/release/$NAME"
ARCHIVE="$ROOT/dist/$NAME.zip"
INVITE_ARCHIVE="$ROOT/output/The-Chosen-Quest-Private-Tester-Invitation-Kit-${VERSION}.zip"

if [ -z "$VERSION" ]; then
  echo "Unable to read AppVersion.VERSION" >&2
  exit 1
fi

if ! "$PYTHON" -c 'import PIL, reportlab' >/dev/null 2>&1; then
  echo "Release document dependencies are missing for $PYTHON" >&2
  echo "Install them with: $PYTHON -m pip install -r $ROOT/requirements-release.txt" >&2
  exit 1
fi

"$ROOT/test.sh"
"$PYTHON" "$ROOT/tools/create_tester_guide.py"
"$PYTHON" "$ROOT/tools/create_private_tester_kit.py"
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
rm -f "$INVITE_ARCHIVE"
(
  cd "$ROOT/build/release"
  zip -qr "$ARCHIVE" "$NAME"
)
(
  cd "$ROOT/dist"
  shasum -a 256 "$NAME.zip" > "$NAME.zip.sha256"
)
(
  cd "$ROOT/output"
  zip -qr "$INVITE_ARCHIVE" "private-tester-kit"
)

echo "Packaged $ARCHIVE"
echo "Checksum: $ARCHIVE.sha256"
echo "Tester invitation kit: $INVITE_ARCHIVE"
