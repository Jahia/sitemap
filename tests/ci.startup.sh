#!/usr/bin/env bash

# This script controls the startup of the container environment
# It can be used as an alternative to having docker-compose up started by the CI environment

source ./set-env.sh

echo " == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_IMAGE: ${JAHIA_IMAGE}"
echo " JPDA: ${JPDA}"

echo "$(date +'%d %B %Y - %k:%M') [LICENSE] == Check if license exists in env variable (JAHIA_LICENSE) =="
if [[ -z ${JAHIA_LICENSE} ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [LICENSE] == Jahia license does not exist, checking if there is a license file in /tmp/license.xml =="
    if [[ -f /tmp/license.xml ]]; then
        echo "$(date +'%d %B %Y - %k:%M') [LICENSE] ==  License found in /tmp/license.xml, base64ing it"
        # Detect OS and use appropriate base64 flag
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            export JAHIA_LICENSE=$(base64 -i /tmp/license.xml)
        else
            # Linux and other systems
            export JAHIA_LICENSE=$(base64 /tmp/license.xml)
        fi
    else
        echo "$(date +'%d %B %Y - %k:%M') [LICENSE]  == STARTUP FAILURE, unable to find license =="
        exit 1
    fi
fi

docker-compose up -d mariadb haproxy jahia

if [[ "${JAHIA_CLUSTER_ENABLED}" == "true" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Starting a cluster of one processing and two browsing =="
    docker-compose up -d jahia-browsing-a jahia-browsing-b
fi

if [[ $1 != "notests" ]]; then
    export JAHIA_URL=http://haproxy:8080
    export JAHIA_PROCESSING_URL=http://jahia:8080
    echo "$(date +'%d %B %Y - %k:%M') [TESTS] == Starting cypress tests =="
    docker-compose up --abort-on-container-exit --renew-anon-volumes cypress
fi

# To run Cypress manually: JAHIA_URL=http://localhost:8080 SUPER_USER_PASSWORD=root1234 yarn e2e:debug
