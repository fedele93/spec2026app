#!/usr/bin/env bash
# Crea (una volta) lo script ./gradlew e gradle/wrapper/gradle-wrapper.jar, da committare.
# Senza il wrapper nel repo ogni clone deve avere Gradle installato per compilare.
# Uso:  tools/bootstrap-gradlew.sh        (richiede Java; scarica Gradle se non è installato)
set -euo pipefail
cd "$(dirname "$0")/.."
VERSION="$(sed -n 's|.*gradle-\([0-9.]*\)-bin.zip|\1|p' gradle/wrapper/gradle-wrapper.properties)"
VERSION="${VERSION:-9.3.1}"

if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "Wrapper già presente (./gradlew). Niente da fare."; exit 0
fi

if command -v gradle >/dev/null 2>&1; then
  GRADLE=gradle
else
  TMP="$(mktemp -d)"
  echo "==> Gradle non installato: scarico la distribuzione $VERSION in $TMP"
  curl -fsSL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$TMP/gradle.zip"
  unzip -q "$TMP/gradle.zip" -d "$TMP"
  GRADLE="$TMP/gradle-$VERSION/bin/gradle"
fi

"$GRADLE" wrapper --gradle-version "$VERSION" --distribution-type bin
echo "==> Creati ./gradlew, ./gradlew.bat e gradle/wrapper/gradle-wrapper.jar."
echo "    Committali:  git add gradlew gradlew.bat gradle/wrapper && git commit -m 'build: aggiunge il Gradle wrapper'"
