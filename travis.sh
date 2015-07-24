#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v14/install.sh | bash
  source /tmp/travis-utils/env.sh
}

case "$JOB" in

CI)
  mvn verify -B -e -V
  ;;

IT-SQDEV)
  installTravisTools

  mvn package -T2 -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  travis_build_green "SonarSource/sonarqube" "master"

  cd its
  mvn -Dsonar.runtimeVersion="DEV" -Dmaven.test.redirectTestOutputToFile=false verify
  ;;

IT-SQLTS)
  installTravisTools

  mvn package -T2 -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  travis_download_sonarqube_release "4.5.4"

  cd its/plugin
  mvn -DjavaVersion="DEV" -Dsonar.runtimeVersion="LATEST_RELEASE" -Dmaven.test.redirectTestOutputToFile=false verify
  ;;

esac
