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
  regular_mvn_build_deploy_analyze
  ;;


*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
