#!/usr/bin/env bash

# Script that runs, liquibase, deploys wars and runs integration test
#
# Usage: ./runIntegrationTests.sh [errorlog] [durations] [nounit]
#   errorlog  - strict server.log mode: any unexpected ERROR/WARN in a test's server.log window
#               FAILS that test (see ServerLogTestMarkerExtension / @ExpectedServerErrors).
#               Also disables failsafe reruns so strict failures cannot be masked as flakes.
#   durations - also activate the test-duration-tracking profile (TestDurationListener): ranked
#               per-test durations in listing-integration-test/target/test-results/test-durations.csv
#   nounit    - build the wars with -DskipTests (unit tests are not needed to deploy; the PR gate's
#               own `mvn clean install` still runs them). Saves the ~1 min unit-test pass per IT run.

CONTEXT_NAME=listing

SERVER_LOG_STRICT_PROPS=""
EXTRA_IT_PROFILES=""
BUILD_SKIP_TESTS=""
for arg in "$@"; do
  case "$arg" in
    errorlog)
      SERVER_LOG_STRICT_PROPS="-Dserver.log.failOnUnexpectedErrors=true -Dfailsafe.rerunFailingTestsCount=0"
      ;;
    durations)
      EXTRA_IT_PROFILES=",test-duration-tracking"
      ;;
    nounit)
      BUILD_SKIP_TESTS="-DskipTests"
      ;;
  esac
done

FRAMEWORK_LIBRARIES_VERSION=$(mvn help:evaluate -Dexpression=framework-libraries.version -q -DforceStdout)
FRAMEWORK_VERSION=$(mvn help:evaluate -Dexpression=framework.version -q -DforceStdout)
EVENT_STORE_VERSION=$(mvn help:evaluate -Dexpression=event-store.version -q -DforceStdout)

DOCKER_CONTAINER_REGISTRY_HOST_NAME=crmdvrepo01

LIQUIBASE_COMMAND=update
#LIQUIBASE_COMMAND=dropAll

#fail script on error
set -e

[ -z "$CPP_DOCKER_DIR" ] && echo "Please export CPP_DOCKER_DIR environment variable pointing to cpp-developers-docker repo (https://github.com/hmcts/cpp-developers-docker) checked out locally" && exit 1
WILDFLY_DEPLOYMENT_DIR="$CPP_DOCKER_DIR/containers/wildfly/deployments"

source $CPP_DOCKER_DIR/docker-utility-functions.sh
source $CPP_DOCKER_DIR/build-scripts/integration-test-scipt-functions.sh

# Local override of cpp-developers-docker's integrationTests(): identical mvn line plus the
# optional strict server.log properties (see "errorlog" usage above). Defined AFTER the source
# lines so this definition wins. The trailing ${SERVER_LOG_STRICT_PROPS} also overrides the
# rerun count when strict mode is on (later -D wins on the mvn command line).
integrationTests() {
  echo
  echo "Running Integration Tests${SERVER_LOG_STRICT_PROPS:+ (strict server.log mode: unexpected ERROR/WARN fails the owning test)}"
  mvn -B -C -U verify -pl ${CONTEXT_NAME}-integration-test -P${CONTEXT_NAME}-integration-test${EXTRA_IT_PROFILES} -DINTEGRATION_HOST_KEY=localhost -Dfailsafe.rerunFailingTestsCount=4 ${SERVER_LOG_STRICT_PROPS}
  echo "Finished executing Integration Tests"
}

# Local override of cpp-developers-docker's buildWars()/doBuildWars 'service': same clean + install,
# plus -DskipTests when "nounit" is given.
buildWars() {
  mvn -f ${CONTEXT_NAME}-service/pom.xml clean
  echo "Building wars..."
  mvn install -nsu ${BUILD_SKIP_TESTS}
  echo "Finished building wars"
}

runLiquibase() {
  runEventLogLiquibase
  runEventLogAggregateSnapshotLiquibase
  runEventBufferLiquibase
  runViewStoreLiquibase
  runSystemLiquibase
  runEventTrackingLiquibase
  printf "${CYAN}All liquibase $LIQUIBASE_COMMAND scripts run${NO_COLOUR}\n\n"
}

# TODO: Should probably move this function to cpp-developers-docker as it's already generic
deltaspikeIntegrationTests() {
  echo
  echo "Running Deltaspike Persistence Integration Tests"
  mvn -B verify -pl ${CONTEXT_NAME}-integration-test-persistence -P${CONTEXT_NAME}-integration-test -DINTEGRATION_HOST_KEY=localhost
  echo "Finished executing Deltaspike Persistence Integration Tests"
}

buildDeployAndTest() {
  loginToDockerContainerRegistry
  buildWars
  undeployWarsFromDocker
  buildAndStartContainers
  runLiquibase
#  deltaspikeIntegrationTests
  deployWiremock
  deployWars
  healthchecks
  integrationTests
}

buildDeployAndTest
