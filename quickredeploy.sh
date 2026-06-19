#!/usr/bin/env bash

# Quick redeploy script for faster feedback loop
# Use this after initial setup with ./runIntegrationTests.sh
# This only rebuilds and redeploys the WAR to running containers

CONTEXT_NAME=listing

#fail script on error
set -e

[ -z "$CPP_DOCKER_DIR" ] && echo "Please export CPP_DOCKER_DIR environment variable pointing to cpp-developers-docker repo" && exit 1
WILDFLY_DEPLOYMENT_DIR="$CPP_DOCKER_DIR/containers/wildfly/deployments"

# Source required functions
source $CPP_DOCKER_DIR/docker-utility-functions.sh
source $CPP_DOCKER_DIR/build-scripts/deployment-functions.sh

echo "🚀 Quick redeploy starting..."

# Step 1: Build the project (skip tests for speed)
echo "📦 Building project (skipping tests)..."
mvn clean install -DskipTests -nsu

# Step 2: Remove old WAR files from deployment directory
echo "🧹 Cleaning old deployments..."
undeployWarsFromDocker

# Step 3: Deploy new WAR files
echo "🔄 Deploying new WAR files..."
deployWars

echo "✅ Quick redeploy completed! Your changes should be available shortly."
echo "💡 You can now run your tests or check the application."