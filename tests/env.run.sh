#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

if [[ ! -f .env ]]; then
 cp .env.example .env
fi

#!/usr/bin/env bash


echo "== Run tests =="
yarn e2e:ci
if [[ $? -eq 0 ]]; then
  echo "success" > /tmp/results/test_success
  exit 0
else
  echo "failure" > /tmp/results/test_failure
  exit 1
fi

# After the test ran, we're dropping a marker file to indicate if the test failed or succeeded based on the script test command exit code
