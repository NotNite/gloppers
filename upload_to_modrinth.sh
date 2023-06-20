#!/usr/bin/env bash

set -a
source .env
set +a

if [ -z MODRINTH_TOKEN ]; then
  echo "No MODRINTH_TOKEN set in .env"
  exit 1
fi

./gradlew modrinth
