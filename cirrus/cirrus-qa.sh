#!/bin/bash

# Set up environment variables.
source cirrus-env QA
source set_maven_build_version $BUILD_NUMBER

# Get the Jenkins plugin HPI file. We will need it to install our plugin.
wget --user $ARTIFACTORY_DEPLOY_USERNAME --password $ARTIFACTORY_DEPLOY_PASSWORD $ARTIFACTORY_URL/sonarsource-public-qa/org/jenkins-ci/plugins/sonar/$NEW_VERSION/sonar-$NEW_VERSION.hpi
LOCAL_JARS=`pwd`/sonar-$CURRENT_VERSION.hpi

# Run ITs.
cd its
# As the job is running on Cirrus-CI, CIRRUS_CI env variable is set to true, but then the scanner executed in ITs is detecting this env variable and makes analysis failing with :
# "ERROR: Multiple CI environments are detected: [CirrusCI, Jenkins]. Please check environment variables or set property sonar.ci.autoconfig.disabled to true."
# We then need to explicly set the variable to false. Another solution would to do this in the IT itself, but couldn't find a way to do it yet.
CIRRUS_CI=false
mvn -B -e -Dsonar.runtimeVersion="$SQ_VERSION" clean verify


