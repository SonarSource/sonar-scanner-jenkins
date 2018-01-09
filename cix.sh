#!/bin/bash
set -euo pipefail
echo "Running with SQ=$SQ_VERSION"

#deploy the version built by travis
CURRENT_VERSION=`mvn help:evaluate -Dexpression="project.version" | grep -v '^\[\|Download\w\+\:'  | grep -v '\[WARNING\]'`
echo "CURRENT_VERSION=$CURRENT_VERSION"
rm -rf target
mkdir -p target
cd target
curl --user $ARTIFACTORY_QA_READER_USERNAME:$ARTIFACTORY_QA_READER_PASSWORD -sSLO https://repox.sonarsource.com/sonarsource-public-qa/org/jenkins-ci/plugins/sonar/$CURRENT_VERSION/sonar-$CURRENT_VERSION.hpi
echo "Downloaded sonar-$CURRENT_VERSION.hpi"
mv sonar-$CURRENT_VERSION.hpi sonar.hpi
cd ..

cd its
mvn -B -e -Djenkins.runtimeVersion="$JENKINS_VERSION" -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false clean verify -Dwebdriver.firefox.marionette=false
