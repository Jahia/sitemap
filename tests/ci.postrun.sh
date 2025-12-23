#!/usr/bin/env bash

# This script is executed after the run
# It is mostly useful to export logs files

source ./set-env.sh

echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Value: ${JAHIA_CLUSTER_ENABLED} =="
if [[ "${JAHIA_CLUSTER_ENABLED}" == "true" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Fetching logs from the browsing nodes =="
    docker logs jahia-browsing-a >> ./artifacts/results/jahia-browsing-a.log
    docker logs jahia-browsing-b >> ./artifacts/results/jahia-browsing-b.log
    docker cp mariadb:/var/lib/mysql/general.log ./artifacts/results/mariadb-general.log
fi



