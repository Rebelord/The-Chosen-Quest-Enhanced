#!/bin/sh
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
  echo "Java is required. Install Java 8 or newer from https://adoptium.net/"
  printf '\nPress Return to close...'
  read answer
  exit 1
fi
java -jar TheChosenQuest-Enhanced.jar
status=$?
if [ "$status" -ne 0 ]; then
  printf '\nThe game closed with an error. Press Return to close...'
  read answer
fi
exit "$status"
