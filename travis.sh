#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v19 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

if [ -n "${PR_ANALYSIS:-}" ] && [ "${PR_ANALYSIS}" == true ]
then
  if [ "$TRAVIS_PULL_REQUEST" != "false" ]
  then
    # PR analysis
    # Accessing Dory with SSL requires Java 8
    source $HOME/.jdk_switcher_rc
    jdk_switcher use oraclejdk8    
    mvn verify sonar:sonar -B -e -V \
      -Dsonar.analysis.mode=issues \
      -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
      -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
      -Dsonar.github.login=$SONAR_GITHUB_LOGIN \
      -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_LOGIN \
      -Dsonar.password=$SONAR_PASSWD
  fi
else
  # Regular CI
  mvn verify -B -e -V
fi

if [ "${RUN_ITS}" == "true" ]; then
  installTravisTools

  if [ "${SQ_VERSION}" == "DEV" ]
  then
    build_snapshot "SonarSource/sonarqube"
  fi

  start_xvfb

  cd its
  mvn -Djenkins.runtimeVersion="$JENKINS_VERSION" -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false verify
fi
