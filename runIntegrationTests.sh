#!/usr/bin/env bash

# Script that runs, liquibase, deploys wars and runs integration test

CONTEXT_NAME=listing

FRAMEWORK_LIBRARIES_VERSION=17.5.5
FRAMEWORK_VERSION=17.5.5
EVENT_STORE_VERSION=17.5.4

DOCKER_CONTAINER_REGISTRY_HOST_NAME=crmdvrepo01

LIQUIBASE_COMMAND=update
#LIQUIBASE_COMMAND=dropAll

#fail script on error
set -e

[ -z "$CPP_DOCKER_DIR" ] && echo "Please export CPP_DOCKER_DIR environment variable pointing to cpp-developers-docker repo (https://github.com/hmcts/cpp-developers-docker) checked out locally" && exit 1
WILDFLY_DEPLOYMENT_DIR="$CPP_DOCKER_DIR/containers/wildfly/deployments"

source $CPP_DOCKER_DIR/docker-utility-functions.sh
source $CPP_DOCKER_DIR/build-scripts/integration-test-scipt-functions.sh

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
