#!/bin/sh
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
  echo "Java is required. Install Java 8 or newer from https://adoptium.net/"
  exit 1
fi
exec java -jar TheChosenQuest-Enhanced.jar
