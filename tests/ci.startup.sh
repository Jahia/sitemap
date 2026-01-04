#!/usr/bin/env bash

# TODO for now, use a copy of the one from jahia-cypress.
# once it's working, create a PR in jahia-cypress to support the DOCKER_COMPOSE_FILE env variable.

# This script controls the startup of the container environment
# It can be used as an alternative to having docker-compose up started by the CI environment

BASEDIR=$(dirname $(readlink -f $0))

source $BASEDIR/set-env.sh

# Set COMPOSE_FILE to DOCKER_COMPOSE_FILE if set, otherwise default to docker-compose.yml
if [[ -n "${DOCKER_COMPOSE_FILE}" ]]; then
    COMPOSE_FILE="${DOCKER_COMPOSE_FILE}"
else
    COMPOSE_FILE="docker-compose.yml"
fi

echo " ci.startup.sh == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_IMAGE: ${JAHIA_IMAGE}"
echo " JAHIA_CLUSTER_ENABLED: ${JAHIA_CLUSTER_ENABLED}"
echo " NEXUS_USERNAME: ${NEXUS_USERNAME:0:3}***${NEXUS_USERNAME:(-6)}"
echo " DOCKER_COMPOSE_FILE: ${DOCKER_COMPOSE_FILE}"
echo " COMPOSE_FILE: ${COMPOSE_FILE}"

echo "$(date +'%d %B %Y - %k:%M') [LICENSE] == Check if license exists in env variable (JAHIA_LICENSE) =="
if [[ -z ${JAHIA_LICENSE} ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [LICENSE] == Jahia license does not exist, checking if there is a license file in /tmp/license.xml =="
    if [[ -f /tmp/license.xml ]]; then
        echo "$(date +'%d %B %Y - %k:%M') [LICENSE] ==  License found in /tmp/license.xml, base64ing it"
        export JAHIA_LICENSE=$(base64 -i /tmp/license.xml)
    else
        echo "$(date +'%d %B %Y - %k:%M') [LICENSE]  == STARTUP FAILURE, unable to find license =="
        exit 1
    fi
fi

echo "$(date +'%d %B %Y - %k:%M') == Starting environment =="
docker-compose --file "${COMPOSE_FILE}" up --detach
# Attach (streaming logs) to the Cypress container
docker container attach cypress
