#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v14/install.sh | bash
  source /tmp/travis-utils/env.sh
}

mvn verify -B -e -V

if [ "${RUN_ITS}" == "true" ]
then
  installTravisTools

  if [ "${SQ_VERSION}" == "DEV" ]
  then
    travis_build_green "SonarSource/sonarqube" "master"
  fi

  travis_start_xvfb

  cd its  
  mvn -Djenkins.runtimeVersion=$JENKINS_VERSION -Dsonar.runtimeVersion=$SQ_VERSION -Dmaven.test.redirectTestOutputToFile=false verify
fi
