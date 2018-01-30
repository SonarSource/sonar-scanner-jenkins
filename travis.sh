#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v45 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

installTravisTools
. ~/.local/bin/installMaven35

export DEPLOY_PULL_REQUEST=true

regular_mvn_build_deploy_analyze
