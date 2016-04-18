#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v27 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

installTravisTools

case "$TARGET" in

CI) 
  
  # Do not deploy a SNAPSHOT version but the release version related to this build
  #set_maven_build_version $TRAVIS_BUILD_NUMBER 
  echo TRAVIS_BUILD_NUMBER:$TRAVIS_BUILD_NUMBER
  CURRENT_VERSION=`mvn help:evaluate -Dexpression="project.version" | grep -v '^\[\|Download\w\+\:' | grep -v '\[WARNING\]'`
  echo CURRENT_VERSION:$CURRENT_VERSION
  RELEASE_VERSION=`echo $CURRENT_VERSION | sed "s/-.*//g"`
  echo RELEASE_VERSION:$RELEASE_VERSION
  NEW_VERSION="$RELEASE_VERSION-build$TRAVIS_BUILD_NUMBER"
  echo NEW_VERSION:$NEW_VERSION 
  
  if [ "${TRAVIS_BRANCH}" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  echo '======= Build, deploy and analyze master'

  # Fetch all commit history so that SonarQube has exact blame information
  # for issue auto-assignment
  # This command can fail with "fatal: --unshallow on a complete repository does not make sense" 
  # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
  # For this reason errors are ignored with "|| true"
  git fetch --unshallow || true

  # Analyze with SNAPSHOT version as long as SQ does not correctly handle
  # purge of release data
  SONAR_PROJECT_VERSION=$CURRENT_VERSION

  # Do not deploy a SNAPSHOT version but the release version related to this build
  mvn versions:set -DgenerateBackupPoms=false -DnewVersion="$NEW_VERSION"

  export MAVEN_OPTS="-Xmx1536m -Xms128m"
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
      -Pcoverage-per-test,deploy-sonarsource \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -Dsonar.projectVersion=$SONAR_PROJECT_VERSION \
      -B -e -V

elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN:-}" ]; then
  echo '======= Build and analyze pull request, deploy'

  # Analyze with SNAPSHOT version as long as SQ does not correctly handle
  # purge of release data
  SONAR_PROJECT_VERSION=$CURRENT_VERSION

  # Do not deploy a SNAPSHOT version but the release version related to this build and PR
  mvn versions:set -DgenerateBackupPoms=false -DnewVersion="$NEW_VERSION"

  # No need for Maven phase "install" as the generated JAR files do not need to be installed
  # in Maven local repository. Phase "verify" is enough.

  export MAVEN_OPTS="-Xmx1G -Xms128m"
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
      -Pdeploy-sonarsource \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.analysis.mode=issues \
      -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
      -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
      -Dsonar.github.oauth=$GITHUB_TOKEN \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -B -e -V

else
  echo '======= Build, no analysis, no deploy'

  # No need for Maven phase "install" as the generated JAR files do not need to be installed
  # in Maven local repository. Phase "verify" is enough.

  mvn verify \
      -Dmaven.test.redirectTestOutputToFile=false \
      -B -e -V
  fi  
  ;;


*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
