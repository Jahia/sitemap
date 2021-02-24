#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

if [[ ! -f .env ]]; then
 cp .env.example .env
fi

#!/usr/bin/env bash
START_TIME=$SECONDS

echo " == Using MANIFEST: ${MANIFEST}"
echo " == Using JAHIA_URL= ${JAHIA_URL}"

echo " == Waiting for Jahia to startup"
./node_modules/jahia-reporter/bin/run utils:alive --jahiaUrl=${JAHIA_URL}
ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo " == Jahia became alive in ${ELAPSED_TIME} seconds"

# Add the credentials to a temporary manifest for downloading files
# Execute jobs listed in the manifest
# If the file doesn't exist, we assume it is a URL and we download it locally
if [[ -e ${MANIFEST} ]]; then
  cp ${MANIFEST} /tmp/run-artifacts
else
  echo "Downloading: ${MANIFEST}"
  curl ${MANIFEST} --output /tmp/run-artifacts/curl-manifest
  MANIFEST="curl-manifest"
fi
sed -i -e "s/NEXUS_USERNAME/${NEXUS_USERNAME}/g" /tmp/run-artifacts/${MANIFEST}
sed -i -e "s/NEXUS_PASSWORD/${NEXUS_PASSWORD}/g" /tmp/run-artifacts/${MANIFEST}

echo " == Warming up the environement =="
./node_modules/jahia-reporter/bin/run utils:provision --jahiaUrl=${JAHIA_URL} --script=/tmp/run-artifacts/${MANIFEST}
echo " == Environment warmup complete =="

# If we're building the module (and manifest name contains build), then we'll end up pushing that module individually 
if [[ ${MANIFEST} == *"build"* ]]; then
  echo " == Submitting Sitemap module from: /tmp/artifacts/sitemap-SNAPSHOT.jar =="
  ./node_modules/jahia-reporter/bin/run utils:module --jahiaUrl=${JAHIA_URL} --moduleId=sitemap --moduleFile=/tmp/artifacts/sitemap-SNAPSHOT.jar
  echo " == Module submitted =="
fi

echo "== Run tests =="
CYPRESS_baseUrl=${JAHIA_URL} yarn e2e:ci
if [[ $? -eq 0 ]]; then
  echo "success" > /tmp/results/test_success
  exit 0
else
  echo "failure" > /tmp/results/test_failure
  exit 1
fi

# After the test ran, we're dropping a marker file to indicate if the test failed or succeeded based on the script test command exit code