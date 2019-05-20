#!/bin/bash
set -euo pipefail
echo "Running with SQ=$SQ_VERSION"

#deploy the version built by travis
CURRENT_VERSION=`mvn help:evaluate -Dexpression="project.version" | grep -v '^\[\|Download\w\+\:'  | grep -v '\[WARNING\]'`
echo "CURRENT_VERSION=$CURRENT_VERSION"

cd its
curl --fail --user $ARTIFACTORY_QA_READER_USERNAME:$ARTIFACTORY_QA_READER_PASSWORD -sSLO https://repox.jfrog.io/repox/sonarsource-public-qa/org/jenkins-ci/plugins/sonar/$CURRENT_VERSION/sonar-$CURRENT_VERSION.hpi
echo "Downloaded sonar-$CURRENT_VERSION.hpi"

export LOCAL_JARS=$PWD/sonar-$CURRENT_VERSION.hpi

export BROWSER=chrome

mvn -B -e -Dsonar.runtimeVersion="$SQ_VERSION" clean verify
